#!/usr/bin/env python3
"""BO2: measures the coloured ("accent", teal) part of the seek bars in a 600 x 1024 screenshot of a look-alike Settings page.

    python3 -I tools/ss_slider_probe.py <shot.png>                       one line:  bars=2 y=343,434 end=170,194 rgb=0,150,136
    python3 -I tools/ss_slider_probe.py --point N <shot.png>             "x y" inside the thumb of bar N (1-based), for `input swipe`
    python3 -I tools/ss_slider_probe.py --moved A.png B.png [--bar N] [--min PX]   exit 0 when bar N reaches at least PX further right in B than in A and every other bar stays within 4 px
    python3 -I tools/ss_slider_probe.py --same-colour A.png B.png [--tol T]        exit 0 when both shots have a seek bar and their accent colours differ by at most T per channel

A bar = a band of rows with a horizontal run of >= 12 accent-coloured pixels (accent = clearly green-blue, not grey): the progress part of the track and the thumb. `end` = the right edge of the accent part of
that band (the thumb's right edge), `y` = its track row (the row with the longest run), `rgb` = the most common accent colour of the shot. A disabled bar has no accent pixels: it is not found (that is the point:
Tj wants the TTS sliders to look like the Sound sliders, active). The pixels come from ImageMagick `convert` (the CI runner has it). Run with python3 -I.
"""
import subprocess
import sys
from collections import Counter

W, H = 600, 1024
X0, Y0, Y1 = 60, 120, 900   # the page area: left gutter and status/toolbar above, navigation bar below


def pixels(path):
    out = subprocess.run(["convert", path, "-alpha", "off", "-depth", "8", "rgb:-"], capture_output=True, check=True).stdout
    if len(out) != W * H * 3:
        raise ValueError("expected a %d x %d screenshot, got %d bytes of pixels" % (W, H, len(out)))
    return out


def accent(r, g, b):
    return g >= 100 and g >= r + 50 and b >= r + 40


def measure(path):
    px = pixels(path)
    rows = []   # (y, longest run length, right end of the accent pixels)
    colours = Counter()
    for y in range(Y0, Y1):
        best, run, end, last = 0, 0, -1, -1
        for x in range(X0, W):
            i = (y * W + x) * 3
            if accent(px[i], px[i + 1], px[i + 2]):
                colours[(px[i], px[i + 1], px[i + 2])] += 1
                run += 1
                if run > best:
                    best = run
                end = x
            else:
                run = 0
        if best >= 12:
            rows.append((y, best, end))
    bands, cur = [], []
    for r in rows:
        if cur and r[0] - cur[-1][0] > 6:
            bands.append(cur)
            cur = []
        cur.append(r)
    if cur:
        bands.append(cur)
    bars = []
    for b in bands:
        track = max(b, key=lambda r: r[1])
        bars.append({"y": track[0], "end": max(r[2] for r in b)})
    rgb = colours.most_common(1)[0][0] if colours else None
    return {"bars": bars, "rgb": rgb}


def line(m):
    j = lambda k: ",".join(str(b[k]) for b in m["bars"]) or "?"
    return "bars=%d y=%s end=%s rgb=%s" % (len(m["bars"]), j("y"), j("end"), ",".join(str(c) for c in m["rgb"]) if m["rgb"] else "?")


def option(argv, name, default):
    return type(default)(argv[argv.index(name) + 1]) if name in argv else default


def main(argv):
    paths = [a for i, a in enumerate(argv) if not a.startswith("--") and (i == 0 or argv[i - 1] not in ("--point", "--bar", "--min", "--tol"))]
    try:
        if "--moved" in argv:
            a, b = measure(paths[0]), measure(paths[1])
            n, need = option(argv, "--bar", 1) - 1, option(argv, "--min", 100)
            if len(a["bars"]) < 1 or len(a["bars"]) != len(b["bars"]):
                print("FAIL bars before %s, after %s" % (line(a), line(b)))
                return 1
            for k, (p, q) in enumerate(zip(a["bars"], b["bars"])):
                d = q["end"] - p["end"]
                if (k == n and d < need) or (k != n and abs(d) > 4):
                    print("FAIL bar %d moved by %d px (bar %d wanted >= %d, the others within 4): %s -> %s" % (k + 1, d, n + 1, need, line(a), line(b)))
                    return 1
            return 0
        if "--same-colour" in argv:
            a, b, tol = measure(paths[0]), measure(paths[1]), option(argv, "--tol", 8)
            if not a["bars"] or not b["bars"] or not a["rgb"] or not b["rgb"]:
                print("FAIL no seek bar in one of the shots: %s | %s" % (line(a), line(b)))
                return 1
            if any(abs(p - q) > tol for p, q in zip(a["rgb"], b["rgb"])):
                print("FAIL accent colours differ: %s | %s" % (line(a), line(b)))
                return 1
            return 0
        if "--point" in argv:
            m = measure(paths[0])
            n = option(argv, "--point", 1) - 1
            if n >= len(m["bars"]):
                print("none")
                return 1
            print("%d %d" % (m["bars"][n]["end"] - 10, m["bars"][n]["y"]))
            return 0
        if len(paths) != 1:
            print("usage: ss_slider_probe.py [--point N | --moved A B | --same-colour A B] <shot.png>")
            return 2
        print(line(measure(paths[0])))
        return 0
    except (OSError, ValueError, subprocess.CalledProcessError, IndexError) as e:
        print("unreadable: %s" % e)
        return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
