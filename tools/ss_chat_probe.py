#!/usr/bin/env python3
"""BN1l: measures the chat of the Display size page in a 600 x 1024 screenshot and compares it with Tj's own screenshot of the stock page.

    python3 -I tools/ss_chat_probe.py [--check] <shot.png>

Prints ONE line:  blue_tops=125,313 green_tops=219,431 bubble_h=75,75   (a '?' where nothing was found).
  blue_tops  = top row of each blue "A" disc (outgoing messages 1 and 3), green_tops = top row of each green "P" disc (incoming messages 2 and 4),
  bubble_h   = height in pixels of each green bubble (the two incoming messages are one line each at 600 px width).
With --check the exit code is 0 only when all six numbers exist and are within TOL px of Tj's: his Default screenshot of 2026-11-09 (density 170 dpi, font
scale 1.2, 600 x 1024) reads blue_tops=125,313 green_tops=219,431 bubble_h=75,75. Run #458 measured bubble_h=83,83 and green_tops=227,455: every text line
carried the platform's default font padding (+8 px); the stock page has none. The five densities of his Display size shots (144 / 170 / 198 / 226 / 254)
fit one rule: bubble = 16 + 12 + 3 dp of padding plus the bare text lines (63 / 75 / 88 / 100 px for one line).
The pixels come from ImageMagick `convert` (the CI runner has it; Pillow is not guaranteed). Run with python3 -I.
"""
import subprocess
import sys

W, H = 600, 1024
TOL = 2
TJ_DEFAULT = {"blue_tops": [125, 313], "green_tops": [219, 431], "bubble_h": [75, 75]}
BLUE = (66, 133, 244)      # the outgoing "A" disc
GREEN = (104, 159, 56)     # the incoming "P" disc and bubble


def pixels(path):
    out = subprocess.run(["convert", path, "-alpha", "off", "-depth", "8", "rgb:-"], capture_output=True, check=True).stdout
    if len(out) != W * H * 3:
        raise ValueError("expected a %d x %d screenshot, got %d bytes of pixels" % (W, H, len(out)))
    return out


def hit(px, x, y, c, tol=14):
    i = (y * W + x) * 3
    return abs(px[i] - c[0]) < tol and abs(px[i + 1] - c[1]) < tol and abs(px[i + 2] - c[2]) < tol


def rows_with(px, xs, c, y0=95, y1=760):
    """The rows (ascending) in which any sampled column has the colour."""
    return [y for y in range(y0, y1) if any(hit(px, x, y, c) for x in xs)]


def runs(rows, gap):
    """Groups ascending row numbers into (first, last) runs; a jump of more than `gap` starts a new run."""
    out = []
    start = last = None
    for y in rows:
        if start is None:
            start = last = y
        elif y - last > gap:
            out.append((start, last))
            start = last = y
        else:
            last = y
    if start is not None:
        out.append((start, last))
    return out


def measure(path):
    px = pixels(path)
    blue = runs(rows_with(px, range(520, 585, 4), BLUE), 6)
    green_disc = runs(rows_with(px, range(22, 76, 4), GREEN), 6)
    bubble = runs(rows_with(px, range(120, 190, 6), GREEN), 30)
    return {"blue_tops": [s for s, _ in blue], "green_tops": [s for s, _ in green_disc], "bubble_h": [e - s + 1 for s, e in bubble]}


def line(m):
    return " ".join("%s=%s" % (k, ",".join(str(v) for v in m[k]) or "?") for k in ("blue_tops", "green_tops", "bubble_h"))


def main(argv):
    check = "--check" in argv
    paths = [a for a in argv if not a.startswith("--")]
    if len(paths) != 1:
        print("usage: ss_chat_probe.py [--check] <600x1024 shot.png>")
        return 2
    try:
        m = measure(paths[0])
    except (OSError, ValueError, subprocess.CalledProcessError) as e:
        print("unreadable: %s" % e)
        return 1
    print(line(m))
    if not check:
        return 0
    for k, want in TJ_DEFAULT.items():
        got = m[k]
        if len(got) != len(want) or any(abs(g - w) > TOL for g, w in zip(got, want)):
            print("FAIL %s: want %s (+-%d), got %s" % (k, want, TOL, got))
            return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
