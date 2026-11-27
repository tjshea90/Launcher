#!/usr/bin/env python3
"""trace_top.py - summarise an ART method trace taken with `am profile start --sampling N` (or Debug.startMethodTracingSampling).

DA1 (Tj, 2026-11-27: "Look for any possible ways ... to make this launcher as efficient and fast and lightweight as possible"): the five earlier speed passes read
code; nothing ever measured WHICH of our methods burn CPU. The emulator section `pr` records the launcher process while a script presses, swipes and opens
things, and this tool turns each trace into a few short lines (the CI RESULTS block can only carry short INFO lines).

  python3 -I tools/trace_top.py FILE [--phase NAME] [--top N] [--width W]     print `INFO  PR <phase> ...` lines (the scenario pipes them into results.txt)
  python3 -I tools/trace_top.py FILE --phase NAME --detail [--top 40]         the same as a long plain-text report (the scenario publishes it as pr-NAME.txt)
  python3 -I tools/trace_top.py --cpu A B --phase NAME --secs S                two `comm|stat` snapshots of /proc/PID/task -> one INFO line: CPU per thread name in between
  python3 -I tools/trace_top.py --selftest                                    parse a synthetic trace and check the arithmetic (tools/test_trace_top.sh)

Format (art/runtime/trace.cc, non-streaming): a text header (`*version`, 2|3, key=value lines, `*threads` with `id<TAB>name`, `*methods` with
`0xID<TAB>class<TAB>name<TAB>signature<TAB>file`, `*end`), then the binary part: magic 'SLOW' (u4), version (u2), offset to data (u2), start time (u8), v3: record
size (u2); records at `offset`: thread id (u2), method id | action (u4; action 0 = enter, 1 = exit, 2 = unwind), v3: thread-CPU time (u4) and wall time (u4), v2: one
time (u4). Little endian. Times are microseconds since the trace (wall) / since the thread's first record (CPU). A sampling trace has a record per CHANGE of a
thread's stack at each sample, so a method's time = the samples that saw it on the stack (quantised to the sampling interval).
Every number printed is a sampled estimate, not an exact count; the ranking is what matters.
"""
import re
import struct
import sys

MAGIC = 0x574f4c53
MINE = re.compile(r'^(com\.tj\.launcher|com\.android\.launcher3|com\.android\.wallpaperpicker|launcher_log|androidx\.)')
IDLE = re.compile(r'(nativePollOnce|Object\.wait|Thread\.sleep|Unsafe\.park|LockSupport\.park|Thread\.parkFor|nativeWait|Object\.wait|futex|MessageQueue\.next)')


def parse(blob):
    """-> dict(version, kv, threads{id: name}, methods{id: (cls, name, sig)}, records[(tid, mid, action, cpu, wall)])."""
    end = blob.find(b'*end\n')
    if end < 0:
        raise ValueError('no *end marker: not a non-streaming ART trace (%d bytes, starts %r)' % (len(blob), blob[:24]))
    head = blob[:end].decode('utf-8', 'replace').split('\n')
    body = blob[end + 5:]
    ver = None
    kv = {}
    threads = {}
    methods = {}
    mode = ''
    for ln in head:
        if ln.startswith('*'):
            mode = ln[1:].strip()
            continue
        if not ln.strip():
            continue
        if mode == 'version':
            if ver is None and ln.strip().isdigit():
                ver = int(ln.strip())
            elif '=' in ln:
                k, v = ln.split('=', 1)
                kv[k.strip()] = v.strip()
        elif mode == 'threads':
            p = ln.split('\t', 1)
            if len(p) == 2 and p[0].strip().isdigit():
                threads[int(p[0])] = p[1].strip()
        elif mode == 'methods':
            p = ln.split('\t')
            if len(p) >= 3 and p[0].startswith('0x'):
                methods[int(p[0], 16)] = (p[1], p[2], p[3] if len(p) > 3 else '')
    if len(body) < 32:
        raise ValueError('binary part too short (%d bytes)' % len(body))
    magic, bver, offset, start = struct.unpack_from('<IHHQ', body, 0)
    if magic != MAGIC:
        raise ValueError('bad magic %08x (want %08x)' % (magic, MAGIC))
    rec = struct.unpack_from('<H', body, 16)[0] if bver >= 3 else 10
    if rec not in (10, 14):
        rec = 14 if bver >= 3 else 10
    clock = kv.get('clock', 'dual' if bver >= 3 else 'wall')
    records = []
    pos = offset
    n = len(body)
    while pos + rec <= n:
        tid, mv = struct.unpack_from('<HI', body, pos)
        if bver >= 3:
            cpu, wall = struct.unpack_from('<II', body, pos + 6)
        else:
            t = struct.unpack_from('<I', body, pos + 6)[0]
            cpu, wall = (t, t) if clock == 'thread-cpu' else (0, t)
        records.append((tid, mv & ~3, mv & 3, cpu, wall))
        pos += rec
    return {'version': bver, 'kv': kv, 'threads': threads, 'methods': methods, 'records': records, 'start': start}


def name_of(tr, mid):
    m = tr['methods'].get(mid)
    if not m:
        return '?%x' % mid
    cls = m[0].replace('com.android.launcher3.', 'L3.').replace('com.tj.launcher.', 'tj.')
    return cls + '.' + m[1]


def replay(tr):
    """Walk the records per thread -> per (tid, mid) sums of self / inclusive CPU and wall (microseconds) and the thread totals."""
    stacks = {}
    last = {}
    self_cpu, self_wall, incl_cpu, incl_wall = {}, {}, {}, {}
    total = {}

    def close(tid, st, cpu, wall):
        mid, c0, w0, cc, cw = st.pop()
        ic, iw = max(0, cpu - c0), max(0, wall - w0)
        key = (tid, mid)
        self_cpu[key] = self_cpu.get(key, 0) + max(0, ic - cc)
        self_wall[key] = self_wall.get(key, 0) + max(0, iw - cw)
        incl_cpu[key] = incl_cpu.get(key, 0) + ic
        incl_wall[key] = incl_wall.get(key, 0) + iw
        if st:
            st[-1][3] += ic
            st[-1][4] += iw
        else:
            total[tid] = total.get(tid, 0) + ic
    for tid, mid, act, cpu, wall in tr['records']:
        st = stacks.setdefault(tid, [])
        last[tid] = (cpu, wall)
        if act == 0:
            st.append([mid, cpu, wall, 0, 0])
        else:
            # exit (1) and unwind (2, an exception passed through) close the topmost frame of that method and everything above it
            # (a record whose method is not on the stack at all is ignored rather than allowed to corrupt the rest)
            idx = next((i for i in range(len(st) - 1, -1, -1) if st[i][0] == mid), None)
            while idx is not None and len(st) > idx:
                close(tid, st, cpu, wall)
    for tid, st in stacks.items():
        cpu, wall = last.get(tid, (0, 0))
        while st:
            close(tid, st, cpu, wall)
    return {'self_cpu': self_cpu, 'self_wall': self_wall, 'incl_cpu': incl_cpu, 'incl_wall': incl_wall, 'total': total}


def ms(us):
    return '%d' % round(us / 1000.0) if us >= 1000 else '%.1f' % (us / 1000.0)


def chunks(prefix, items, width):
    """items = list of strings -> lines no longer than width, each starting with prefix."""
    out, cur = [], prefix
    for it in items:
        add = (' | ' if cur != prefix else ' ') + it
        if len(cur) + len(add) > width and cur != prefix:
            out.append(cur)
            cur = prefix + ' ' + it
        else:
            cur += add
    if cur != prefix:
        out.append(cur)
    return out


def summarize(tr, phase, top=14, width=300):
    rp = replay(tr)
    names = tr['threads']
    lines = []
    wall_window = max([r[4] for r in tr['records']] or [0])
    ovf = tr['kv'].get('data-file-overflow', '?')
    tot = sorted(rp['total'].items(), key=lambda kv: -kv[1])
    lines.append('INFO  PR %s: %d records, window %.1f s wall, overflow=%s; CPU by thread: %s' % (
        phase, len(tr['records']), wall_window / 1e6, ovf,
        ', '.join('%s %s ms' % (names.get(t, str(t)), ms(c)) for t, c in tot[:7] if c > 0) or 'none'))
    main = next((t for t, n in names.items() if n == 'main'), None)
    if main is None and tot:
        main = tot[0][0]

    def rank(d, tid=None, mine=False, idle_ok=True, k=top):
        rows = [(v, key[0], key[1]) for key, v in d.items() if v > 0 and (tid is None or key[0] == tid)]
        rows.sort(key=lambda r: -r[0])
        out = []
        for v, t, mid in rows:
            nm = name_of(tr, mid)
            raw = tr['methods'].get(mid, ('', '', ''))[0]
            if mine and not MINE.match(raw):
                continue
            if not idle_ok and IDLE.search(nm):
                continue
            out.append('%s %s%s' % (nm, ms(v), '' if tid is not None else ' [%s]' % names.get(t, t)))
            if len(out) >= k:
                break
        return out
    if main is not None:
        lines += chunks('INFO  PR %s main SELF cpu ms:' % phase, rank(rp['self_cpu'], main), width)
        lines += chunks('INFO  PR %s main INCL cpu ms (ours):' % phase, rank(rp['incl_cpu'], main, mine=True), width)
        lines += chunks('INFO  PR %s main SELF wall ms (blocked, idle off):' % phase, rank(rp['self_wall'], main, idle_ok=False, k=8), width)
    others = {k: v for k, v in rp['self_cpu'].items() if k[0] != main}
    lines += chunks('INFO  PR %s other threads SELF cpu ms:' % phase, rank(others, None, k=8), width)
    return lines


def detail(tr, phase, top=40):
    """A longer plain-text report for the published pr-*.txt file (the INFO lines are short on purpose)."""
    rp = replay(tr)
    names = tr['threads']
    main = next((t for t, n in names.items() if n == 'main'), None)
    out = ['== %s: %d records, version %s, overflow=%s, clock=%s' % (phase, len(tr['records']), tr['version'], tr['kv'].get('data-file-overflow', '?'), tr['kv'].get('clock', '?'))]
    out.append('threads by CPU (ms): ' + ', '.join('%s=%s' % (names.get(t, t), ms(c)) for t, c in sorted(rp['total'].items(), key=lambda kv: -kv[1]) if c > 0))

    def table(title, d, tid=None, mine=False, idle_ok=True, k=top):
        rows = sorted(((v, key[0], key[1]) for key, v in d.items() if v > 0 and (tid is None or key[0] == tid)), key=lambda r: -r[0])
        out.append('-- ' + title)
        n = 0
        for v, t, mid in rows:
            raw = tr['methods'].get(mid, ('', '', ''))
            if mine and not MINE.match(raw[0]):
                continue
            if not idle_ok and IDLE.search(raw[0] + '.' + raw[1]):
                continue
            out.append('%8s ms  %s%s' % (ms(v), raw[0] + '.' + raw[1] + raw[2] if raw[0] else '?%x' % mid, '' if tid is not None else '  [%s]' % names.get(t, t)))
            n += 1
            if n >= k:
                break
    if main is not None:
        table('main thread: SELF cpu', rp['self_cpu'], main)
        table('main thread: INCLUSIVE cpu, our code only (what each of our entry points cost)', rp['incl_cpu'], main, mine=True)
        table('main thread: INCLUSIVE cpu, everything', rp['incl_cpu'], main, k=25)
        table('main thread: SELF wall, idle waits left out (time spent blocked)', rp['self_wall'], main, idle_ok=False, k=25)
    table('all other threads: SELF cpu', {k: v for k, v in rp['self_cpu'].items() if k[0] != main}, None, k=30)
    table('all other threads: INCLUSIVE cpu, our code only', {k: v for k, v in rp['incl_cpu'].items() if k[0] != main}, None, mine=True, k=25)
    return out


def cpu_diff(a_path, b_path, phase, secs):
    """Two snapshots of /proc/PID/task/*/{comm,stat} written by the scenario as `comm|stat` lines -> one INFO line: CPU per thread name in between."""
    def load(p):
        d = {}
        for ln in open(p, errors='replace'):
            if '|' not in ln:
                continue
            comm, stat = ln.rstrip('\n').split('|', 1)
            try:
                tid = int(stat.split()[0])
                rest = stat.rsplit(') ', 1)[1].split()
                d[tid] = (comm.strip(), int(rest[11]) + int(rest[12]))   # utime + stime in clock ticks (10 ms)
            except Exception:
                pass
        return d
    a, b = load(a_path), load(b_path)
    by = {}
    for tid, (name, t) in b.items():
        dt = t - a.get(tid, (name, 0))[1]
        if dt > 0:
            key = re.sub(r'\d+', '#', name)
            by[key] = by.get(key, 0) + dt * 10
    tot = sum(by.values())
    return 'INFO  PR %s: %d ms CPU in %d s (%.2f %% of one core) by %d of %d threads: %s' % (
        phase, tot, secs, 100.0 * tot / (secs * 1000.0) if secs else 0, len(by), len(b),
        ', '.join('%s %d' % (k, v) for k, v in sorted(by.items(), key=lambda kv: -kv[1])[:10]) or 'none')


# ------------------------------------------------------------------ self-test: a synthetic trace built with the layout described above
def build(version, threads, methods, recs, clock='dual'):
    head = '*version\n%d\ndata-file-overflow=false\nclock=%s\nelapsed-time-usec=9999\nnum-method-calls=%d\nclock-call-overhead-nsec=100\nvm=art\npid=1\n*threads\n' % (version, clock, len(recs))
    head += ''.join('%d\t%s\n' % (i, n) for i, n in threads.items())
    head += '*methods\n' + ''.join('0x%x\t%s\t%s\t()V\tF.java\n' % (i, c, n) for i, (c, n) in methods.items()) + '*end\n'
    size = 14 if version >= 3 else 10
    b = struct.pack('<IHHQ', MAGIC, version, 32, 0) + (struct.pack('<H', size) if version >= 3 else b'')
    b = b.ljust(32, b'\0')
    for tid, mid, act, cpu, wall in recs:
        b += struct.pack('<HI', tid, mid | act) + (struct.pack('<II', cpu, wall) if version >= 3 else struct.pack('<I', wall))
    return head.encode() + b


def selftest():
    methods = {0x1000: ('com.android.launcher3.Launcher', 'onResume'), 0x2000: ('android.view.View', 'measure'), 0x3000: ('android.os.MessageQueue', 'nativePollOnce')}
    # thread 1: onResume 0..500 us wall (cpu 0..300), inside it measure 100..300 (cpu 50..250); then idle in nativePollOnce 500..900 (cpu 300..300)
    recs = [(1, 0x1000, 0, 0, 0), (1, 0x2000, 0, 50, 100), (1, 0x2000, 1, 250, 300), (1, 0x1000, 1, 300, 500), (1, 0x3000, 0, 300, 500), (1, 0x3000, 1, 300, 900),
            (2, 0x2000, 0, 0, 0)]  # thread 2 never exits: closed at its last stamp (0)
    for ver in (3, 2):
        blob = build(ver, {1: 'main', 2: 'RenderThread'}, methods, recs, 'dual' if ver == 3 else 'wall')
        tr = parse(blob)
        assert len(tr['records']) == len(recs), tr['records']
        assert tr['threads'][1] == 'main' and tr['methods'][0x1000][1] == 'onResume'
        rp = replay(tr)
        if ver == 3:
            assert rp['self_cpu'][(1, 0x1000)] == 100, rp['self_cpu']        # 300 incl - 200 child
            assert rp['self_cpu'][(1, 0x2000)] == 200, rp['self_cpu']
            assert rp['incl_cpu'][(1, 0x1000)] == 300
            assert rp['total'][1] == 300 + 0
        assert rp['self_wall'][(1, 0x1000)] == 300, rp['self_wall']           # 500 incl - 200 child
        assert rp['self_wall'][(1, 0x3000)] == 400
        out = summarize(tr, 't', 5, 200)
        assert out[0].startswith('INFO  PR t: ') and any('L3.Launcher.onResume' in x for x in out), out
        assert not any('nativePollOnce' in x for x in out if 'blocked' in x), out
    d = detail(parse(build(3, {1: 'main'}, methods, recs)), 'd', 5)
    assert d[0].startswith('== d: 7 records') and any('com.android.launcher3.Launcher.onResume' in x for x in d), d
    import os, tempfile
    with tempfile.TemporaryDirectory() as td:
        pa, pb = os.path.join(td, 'a'), os.path.join(td, 'b')
        open(pa, 'w').write('main|10 (main) S 1 1 1 0 -1 0 0 0 0 0 5 3 0 0\nBinder:12_3|11 (Binder:12_3) S 1 1 1 0 -1 0 0 0 0 0 1 1 0 0\n')
        open(pb, 'w').write('main|10 (main) S 1 1 1 0 -1 0 0 0 0 0 9 4 0 0\nBinder:12_3|11 (Binder:12_3) S 1 1 1 0 -1 0 0 0 0 0 1 1 0 0\nnew thr|12 (new thr) S 1 1 1 0 -1 0 0 0 0 0 2 0 0 0\n')
        line = cpu_diff(pa, pb, 'idle', 10)
        assert 'main 50' in line and 'new thr 20' in line and '70 ms CPU' in line and 'Binder' not in line, line
    try:
        parse(b'not a trace')
        assert False
    except ValueError:
        pass
    print('trace_top selftest: ok')


def main(argv):
    if '--selftest' in argv:
        selftest()
        return 0
    opt = {}
    for i, a in enumerate(argv):
        if a in ('--phase', '--top', '--width', '--secs') and i + 1 < len(argv):
            opt[a] = argv[i + 1]
    args = [a for a in argv if not a.startswith('--')]
    files = [a for a in args if a not in opt.values()]
    if '--cpu' in argv:
        if len(files) < 2:
            print(__doc__)
            return 2
        print(cpu_diff(files[0], files[1], opt.get('--phase', 'cpu'), int(opt.get('--secs', 0))))
        return 0
    if not files:
        print(__doc__)
        return 2
    phase = opt.get('--phase', files[0])
    try:
        tr = parse(open(files[0], 'rb').read())
    except Exception as e:   # never fail the scenario: say what the file looked like
        raw = open(files[0], 'rb').read()
        print('INFO  PR %s: trace not readable (%s); %d bytes; head %r' % (phase, e, len(raw), raw[:60]))
        return 0
    if '--detail' in argv:
        for ln in detail(tr, phase, int(opt.get('--top', 40))):
            print(ln)
        return 0
    for ln in summarize(tr, phase, int(opt.get('--top', 14)), int(opt.get('--width', 300))):
        print(ln)
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
