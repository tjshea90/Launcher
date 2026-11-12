#!/usr/bin/env python3
"""fl_analyze.py - does the lock-screen patch ever FLASH, and does the stock Screenshot entry ever SHOW? (BY1, emulator section fl)

Tj, 2026-11-12: "It should cover the screenshot button without flicker or delay, whether from the first lock screen or after pulling down the
notification panel or pulling up the pin pad. It should not flicker or flash either the screenshot button or the wallpaper patch at any time".

The emulator films the lock screen (`screenrecord`, an mp4) through one case (wake-up, notification panel, PIN pad, taps, a power menu opening).
Boxes are cut out of every frame (display px; scaled to the video):
  CORE = inside the hidden Screenshot slot (what the patch covers),
  RING = boxes just OUTSIDE the patch window (left of it, below it): never covered by the patch,
  TOP  = the stock panel's upper part (Power off / Restart): white only while the stock menu is open.
REF = per-pixel median of the settled lock screen inside the same film (the first or the last --refms milliseconds).
Per frame: core_d / ring_d = mean |frame - REF| of CORE / RING (0..255 grey levels).

  mode flash (no menu in the film):
    flashA = core_d <= T1 and ring_d >= T2: the slot shows the resting look while everything around it changed
             (the patch over an open panel, over the darkening PIN-pad scrim, over a screen still fading in at wake-up);
    flashB = core_d >= T2 and ring_d <= T1: only the slot changed (a patch with a wrong look over the plain lock screen).
  mode menu (the film holds one opening of the stock power menu):
    menu   = TOP brighter than REF by >= MENU_D (the stock panel is up);
    shot   = a menu frame whose CORE is brighter than REF by >= SHOT_D: the white Screenshot entry shows (a covered slot is the
             dimmed wallpaper, darker than REF); frames before the first menu frame are judged as in mode flash.

  python3 -I tools/fl_analyze.py <video.mp4> <label> --disp WxH --core X0,Y0,X1,Y1 --ring "X0,Y0,X1,Y1;X0,Y0,X1,Y1" [--top X0,Y0,X1,Y1]
         [--mode flash|menu] [--ref first|last] [--refms 400] [--fps 60] [--t1 6] [--t2 15]
  python3 -I tools/fl_analyze.py --selftest

Prints one line: FLMETRIC label=... mode=... frames=N flashA=k flashA_ms=.. flashB=k menu=k shot=k shot_ms=.. first_menu=s worst=... spans=...
Needs ffmpeg (the workflow installs it for section fl). Standard library only.
"""
import re
import statistics
import subprocess
import sys

T1, T2, MENU_D, SHOT_D = 6.0, 15.0, 40.0, 35.0


def video_size(video):
    p = subprocess.run(["ffmpeg", "-i", video], capture_output=True, text=True)
    m = re.search(r"Video:.*?, (\d{2,5})x(\d{2,5})", p.stderr)
    return (int(m.group(1)), int(m.group(2))) if m else None


# screenrecord writes a frame only when the screen CHANGES (variable frame rate): a film ends at its LAST change, not at its time limit
# (run #563: 10 s films were 3-5 s long and "the last 400 ms" was the middle of the last animation, so REF was wrong and 6 of 7 menu
# films found no menu at all). PAD = the last frame repeated for 1 s: it IS the settled screen (nothing changed after it), so
# --ref last compares every frame with the settled end state. setpts makes frame index n sit at exactly n / fps seconds.
PAD = "tpad=stop_mode=clone:stop_duration=1"


def chain(fps):
    return "fps=%d,setpts=N/(%d*TB),%s" % (fps, fps, PAD)


def frames(video, box, fps):
    """Grey pixels of one box per frame (resampled to a constant fps, end padded): a list of bytes objects. box = (x, y, w, h) in VIDEO px."""
    x, y, w, h = box
    cmd = ["ffmpeg", "-v", "error", "-i", video, "-vf", "%s,crop=%d:%d:%d:%d,format=gray" % (chain(fps), w, h, x, y), "-f", "rawvideo", "-"]
    data = subprocess.run(cmd, capture_output=True).stdout
    n = w * h
    return [data[i:i + n] for i in range(0, len(data) - n + 1, n)]


def ref_of(fr, idx):
    """Per-pixel median of the frames idx (a list of frame indexes)."""
    n = len(fr[0])
    cols = [fr[i] for i in idx]
    return bytes(int(statistics.median(c[p] for c in cols)) for p in range(n))


def mad(a, b):
    n = len(a)
    return sum(abs(a[p] - b[p]) for p in range(n)) / n if n else 0.0


def mean(a):
    return sum(a) / len(a) if a else 0.0


def classify(core_d, ring_d, top_up, core_up, mode, fps, t1=T1, t2=T2):
    """Per-frame numbers -> metrics. core_d / ring_d: mean |frame - REF|; top_up / core_up: mean(frame) - mean(REF) of TOP / CORE (None = no TOP box)."""
    n = min(len(core_d), len(ring_d))
    first_menu = None
    if mode == "menu" and top_up is not None:
        for i in range(n):
            if top_up[i] >= MENU_D:
                first_menu = i
                break
    fa, fb, shot, menu, spans, worst, raw = [], [], [], [], [], (0.0, -1), []
    for i in range(n):
        if first_menu is not None and i >= first_menu:
            if top_up[i] >= MENU_D:
                menu.append(i)
                if core_up[i] >= SHOT_D:
                    shot.append(i)
            continue
        if core_d[i] <= t1 and ring_d[i] >= t2:
            fa.append(i)
            if ring_d[i] - core_d[i] > worst[0]:
                worst = (ring_d[i] - core_d[i], i)
        elif core_d[i] >= t2 and ring_d[i] <= t1:
            fb.append(i)
    for name, lst in (("A", fa), ("B", fb), ("S", shot), ("M", menu)):
        if not lst:
            continue
        s = p = lst[0]
        for i in lst[1:] + [None]:
            if i is not None and i == p + 1:
                p = i
                continue
            raw.append((name, s / fps, (p + 1) / fps))
            if name != "M":
                spans.append("%s%.2f-%.2f" % (name, s / fps, (p + 1) / fps))
            if i is not None:
                s = p = i
    ms = lambda k: int(round(len(k) * 1000.0 / fps))
    return {"frames": n, "flashA": len(fa), "flashA_ms": ms(fa), "flashB": len(fb), "flashB_ms": ms(fb), "menu": len(menu), "shot": len(shot),
            "shot_ms": ms(shot), "first_menu": ("%.2f" % (first_menu / fps)) if first_menu is not None else "none",
            "worst": ("%.1f@%.2f" % (worst[0], worst[1] / fps)) if worst[1] >= 0 else "0", "spans": ",".join(spans[:8]) or "none",
            "raw": raw}


COLORS = {"A": "red", "B": "blue", "S": "yellow", "M": "green"}


def sheet(video, out, box, fps, n, raw, step=6, cols=10):
    """A contact sheet PNG of one box: a thumbnail every `step` frames (0.1 s at 60 fps), left to right, `cols` per row, so thumbnail k
    (row r, column c, k = r * cols + c) is at k * step / fps seconds. A thick frame marks a flash (A red, B blue), a visible Screenshot
    entry (S yellow) or an open menu (M green, thin). Published to the branch ci-shots by a shots=true run: LOOK at it before theorising."""
    x, y, w, h = box
    marks = "".join(",drawbox=x=0:y=0:w=iw:h=ih:color=%s:t=%d:enable='between(t,%.3f,%.3f)'" % (COLORS[k], 3 if k == "M" else 8, a, b - 0.001)
                    for k, a, b in raw[:24])
    rows = max(1, -(-((n + step - 1) // step) // cols))
    vf = "%s,crop=%d:%d:%d:%d%s,select='not(mod(n\\,%d))',scale=iw/2:-2,tile=%dx%d:padding=2:color=white" % (chain(fps), w, h, x, y, marks, step, cols, rows)
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", video, "-vf", vf, "-frames:v", "1", out], capture_output=True)


def timeline(core_d, ring_d, top_up, n, fps, every=12):
    """core_d/ring_d[/top_up] every `every` frames (0.2 s at 60 fps), whole grey levels: what the counts were made of."""
    v = []
    for i in range(0, n, every):
        s = "%.1f:%d/%d" % (i / fps, round(core_d[i]), round(ring_d[i]))
        if top_up is not None:
            s += "/%d" % round(top_up[i])
        v.append(s)
    return " ".join(v)


def box_of(s, sx, sy, vw, vh):
    x0, y0, x1, y1 = [int(v) for v in s.split(",")]
    x0, x1 = sorted((x0, x1))
    y0, y1 = sorted((y0, y1))
    X0, Y0 = max(0, int(x0 * sx)), max(0, int(y0 * sy))
    X1, Y1 = min(vw, int(x1 * sx)), min(vh, int(y1 * sy))
    if X1 - X0 < 4 or Y1 - Y0 < 4:
        raise ValueError("box too small: " + s)
    return (X0, Y0, X1 - X0, Y1 - Y0)


def selftest():
    fps = 60
    rest = [0.5] * 30
    # 1) a clean film: nothing changes -> no flash
    r = classify(rest * 3, rest * 3, None, None, "flash", fps)
    assert r["flashA"] == 0 and r["flashB"] == 0, r
    # 2) an open panel with the patch hidden (both change) -> no flash; then the patch comes back over it for 12 frames -> 12 flashA frames
    r = classify([60.0] * 40 + [1.0] * 12 + [0.5] * 20, [55.0] * 40 + [55.0] * 12 + [0.5] * 20, None, None, "flash", fps)
    assert r["flashA"] == 12 and r["flashA_ms"] == 200 and r["spans"].startswith("A0.67-0.87"), r
    # 3) a patch with a wrong look over the plain lock screen -> flashB
    r = classify([30.0] * 5 + rest, [0.5] * 5 + rest, None, None, "flash", fps)
    assert r["flashB"] == 5, r
    # 4) a menu that opens with the slot covered: no shot frames; one that shows the Screenshot entry for 3 frames
    top = [0.0] * 20 + [80.0] * 30
    core_up_ok = [0.0] * 20 + [-25.0] * 30
    core_up_bad = [0.0] * 20 + [90.0] * 3 + [-25.0] * 27
    d = [0.5] * 50
    r = classify(d, d, top, core_up_ok, "menu", fps)
    assert r["menu"] == 30 and r["shot"] == 0 and r["first_menu"] == "0.33", r
    r = classify(d, d, top, core_up_bad, "menu", fps)
    assert r["shot"] == 3 and r["shot_ms"] == 50, r
    # 5) thresholds: a soft change (ring 10) is not a flash
    r = classify([1.0] * 10, [10.0] * 10, None, None, "flash", fps)
    assert r["flashA"] == 0, r
    print("fl_analyze selftest: OK")
    return 0


def main(argv):
    if argv[1:2] == ["--selftest"]:
        return selftest()
    if len(argv) < 3:
        print(__doc__)
        return 2
    video, label = argv[1], argv[2]
    opt = {"--disp": "600x1024", "--mode": "flash", "--ref": "first", "--refms": "400", "--fps": "60", "--t1": str(T1), "--t2": str(T2), "--top": "", "--core": "", "--ring": "",
           "--sheet": "", "--sheetbox": ""}
    a = argv[3:]
    for i in range(0, len(a) - 1, 2):
        opt[a[i]] = a[i + 1]
    fps = int(opt["--fps"])
    vs = video_size(video)
    if not vs:
        print("FLMETRIC label=%s error=no-video" % label)
        return 1
    dw, dh = [int(v) for v in opt["--disp"].split("x")]
    sx, sy = vs[0] / float(dw), vs[1] / float(dh)
    try:
        core = box_of(opt["--core"], sx, sy, vs[0], vs[1])
        rings = [box_of(s, sx, sy, vs[0], vs[1]) for s in opt["--ring"].split(";") if s.strip()]
        top = box_of(opt["--top"], sx, sy, vs[0], vs[1]) if opt["--top"] else None
        sbox = box_of(opt["--sheetbox"], sx, sy, vs[0], vs[1]) if opt["--sheetbox"] else None
    except Exception as e:  # noqa: BLE001 - one line for the results block, never a traceback
        print("FLMETRIC label=%s error=boxes:%s" % (label, e))
        return 1
    fc = frames(video, core, fps)
    fr = [frames(video, b, fps) for b in rings]
    ft = frames(video, top, fps) if top else None
    n = min([len(fc)] + [len(f) for f in fr] + ([len(ft)] if ft else []))
    nref = max(3, int(int(opt["--refms"]) * fps / 1000))
    if n < nref + fps // 2:
        print("FLMETRIC label=%s error=too-few-frames(%d)" % (label, n))
        return 1
    idx = list(range(0, nref)) if opt["--ref"] == "first" else list(range(n - nref, n))
    rc = ref_of(fc, idx)
    rr = [ref_of(f, idx) for f in fr]
    core_d = [mad(fc[i], rc) for i in range(n)]
    ring_d = [sum(mad(fr[k][i], rr[k]) for k in range(len(fr))) / len(fr) for i in range(n)]
    top_up = core_up = None
    if ft:
        rt = ref_of(ft, idx)
        mrt, mrc = mean(rt), mean(rc)
        top_up = [mean(ft[i]) - mrt for i in range(n)]
        core_up = [mean(fc[i]) - mrc for i in range(n)]
    r = classify(core_d, ring_d, top_up, core_up, opt["--mode"], fps, float(opt["--t1"]), float(opt["--t2"]))
    if opt["--sheet"] and sbox:
        sheet(video, opt["--sheet"], sbox, fps, n, r["raw"])
    real = max(0, n - fps)  # without the 1 s PAD
    print("FLTL label=%s every 0.2 s core/ring%s: %s" % (label, "/top" if top_up is not None else "", timeline(core_d, ring_d, top_up, real, fps)))
    r["frames"] = real
    print("FLMETRIC label=%s mode=%s ref=%s " % (label, opt["--mode"], opt["--ref"]) + " ".join("%s=%s" % (k, r[k]) for k in
          ("frames", "flashA", "flashA_ms", "flashB", "flashB_ms", "menu", "shot", "shot_ms", "first_menu", "worst", "spans")))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
