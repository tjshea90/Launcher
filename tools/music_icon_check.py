#!/usr/bin/env python3
"""MU-21 (TASKS BW1d): is the music app's drawer icon the speaker of Tj's "Music"?

Reads the emulator picture of the custom drawer (600x1024) and the uiautomator dump taken at the same moment, finds the drawer entry with the given label, finds the white plate
Launcher3 draws under a legacy icon (the plate is pure white, 55 px wide on Tj's tablet) and measures the speaker on it. The REFERENCE numbers below were measured on Tj's own drawer
screenshot (his existing "Music", the ATG music player), not on anything this repo draws; the screenshot itself is not stored anywhere in the repo.

Standard library only (ImageMagick `convert` / `identify` give the pixels), so it runs on the plain CI runner.
  python3 -I tools/music_icon_check.py <drawer.png> <drawer dump.txt> [label]   -> prints one line of numbers, exit 0 = it is the speaker, 1 = it is not, 2 = could not measure
  python3 -I tools/music_icon_check.py --at <png> <centre x> <centre y> <plate width> -> the numbers for a known place (used once to measure Tj's screenshot)
"""
import math
import re
import subprocess
import sys

PLATE_REF = 55.0          # plate width on Tj's tablet (px)
R_REF = 19.67             # outer radius of the speaker in plate pixels on Tj's tablet (tools/make_music_icon.py)

def load(png):
    out = subprocess.run(["identify", "-format", "%w %h", png], capture_output=True, text=True, check=True).stdout.split()
    w, h = int(out[0]), int(out[1])
    raw = subprocess.run(["convert", png, "-depth", "8", "rgb:-"], capture_output=True, check=True).stdout
    if len(raw) != w * h * 3:
        raise SystemExit("picture is not plain RGB: %s" % png)
    return w, h, raw


def at(img, x, y):
    w, h, raw = img
    x = max(0, min(w - 1, int(x)))
    y = max(0, min(h - 1, int(y)))
    i = (y * w + x) * 3
    return raw[i], raw[i + 1], raw[i + 2]


def white(p):
    return p[0] >= 250 and p[1] >= 250 and p[2] >= 250


def plate_of(img, l, t, r, b):
    """Bounding box of the pure-white plate inside the entry's bounds: rows and columns that carry at least 30 pure-white pixels (letters of the label never do)."""
    rows = [y for y in range(t, b) if sum(1 for x in range(l, r) if white(at(img, x, y))) >= 30]
    if not rows:
        return None
    # the longest run of consecutive rows
    runs, start = [], rows[0]
    for a, c in zip(rows, rows[1:] + [None]):
        if c is None or c != a + 1:
            runs.append((start, a))
            if c is not None:
                start = c
    y0, y1 = max(runs, key=lambda q: q[1] - q[0])
    cols = [x for x in range(l, r) if sum(1 for y in range(y0, y1 + 1) if white(at(img, x, y))) >= 20]
    if not cols:
        return None
    return cols[0], y0, cols[-1], y1


def lum(img, x, y):
    return sum(at(img, x, y)) / 3.0


def ring_level(img, cx, cy, s):
    """Mean luminance on the thin black ring (16.1 px from the centre on Tj's tablet), 24 directions."""
    return sum(lum(img, cx + 16.1 * s * math.cos(math.radians(a)), cy + 16.1 * s * math.sin(math.radians(a))) for a in range(0, 360, 15)) / 24.0


def measure(img, cx, cy, plate_w):
    """The numbers: blue cone pixels, dust cap level (centre), thin black ring, bezel grey on top / bottom, cone brightness above / below the cap.
    cx, cy: centre of the plate in pixel-edge coordinates; the centre of the speaker is searched within 1.5 px of it (the darkest thin ring), because the plate is not exactly centred on the artwork."""
    s = plate_w / PLATE_REF
    best = None
    for i in range(-6, 7):
        for j in range(-6, 7):
            lv = ring_level(img, cx + i * 0.25, cy + j * 0.25, s)
            if best is None or lv < best[0]:
                best = (lv, cx + i * 0.25, cy + j * 0.25)
    ring, cx, cy = best
    R = R_REF * s

    def spot(rad, up):                      # mean luminance of 5 points at distance rad (Tj's pixels) straight above (up=-1) or below (up=+1) the centre
        return sum(lum(img, cx + k * s, cy + up * rad * s) for k in range(-2, 3)) / 5.0

    blue = 0
    for y in range(int(cy - R - 1), int(cy + R + 2)):
        for x in range(int(cx - R - 1), int(cx + R + 2)):
            if math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= R:
                p = at(img, x, y)
                if p[2] - p[0] >= 50 and p[2] >= 110:
                    blue += 1
    capvals = [lum(img, cx + dx * s, cy + dy * s) for dx in (-1, 0, 1) for dy in (-1, 0, 1)]
    return {
        "blue": blue / (s * s),                                      # normalised to Tj's plate size
        "cap": sum(capvals) / len(capvals),
        "ring": ring,
        "bezel_top": spot(18.3, -1),
        "bezel_bottom": spot(18.3, 1),
        "cone_top": spot(10.8, -1),
        "cone_bottom": spot(10.8, 1),
    }


# MEASURED on Tj's drawer screenshot with `--at 413.5 583.5 55` (plate 55 x 55 at x 386-440, y 556-610; the speaker is 39.3 px wide; kept as literals so the check needs no screenshot).
# The same measuring code gives blue 518 / cap 38 / ring 10 / bezel 129 and 78 / cone 76 and 161 on tools/make_music_icon.py's own drawing at that size.
MEASURED = {"blue": 528.0, "cap": 41.7, "ring": 7.3, "bezel_top": 130.4, "bezel_bottom": 67.8, "cone_top": 77.9, "cone_bottom": 160.9}


def verdict(m):
    """Windows around Tj's numbers, wide on purpose: the emulator draws the same plate and scales the same artwork, but anti-aliasing and the wallpaper edge differ a little.
    The pink note icon of upstream's Eleven fails every one of them (no blue, no dark cap, no black ring)."""
    why = []
    if not (0.6 * MEASURED["blue"] <= m["blue"] <= 1.6 * MEASURED["blue"]):
        why.append("blue cone pixels %.0f (Tj's: %.0f)" % (m["blue"], MEASURED["blue"]))
    for key, lo, hi in (("cap", 10, 90), ("ring", 0, 60), ("bezel_top", 95, 175), ("bezel_bottom", 35, 110), ("cone_top", 45, 115), ("cone_bottom", 125, 200)):
        if not (lo <= m[key] <= hi):
            why.append("%s %.0f outside %d..%d (Tj's: %.0f)" % (key, m[key], lo, hi, MEASURED[key]))
    return why


def main(argv):
    if len(argv) >= 6 and argv[1] == "--at":
        img = load(argv[2])
        m = measure(img, float(argv[3]), float(argv[4]), float(argv[5]))
        print(" ".join("%s=%.1f" % kv for kv in m.items()))
        return 0
    if len(argv) < 3:
        print(__doc__)
        return 2
    png, dump = argv[1], argv[2]
    label = argv[3] if len(argv) > 3 else "Music"
    node = None
    for line in open(dump, errors="replace"):
        if 'text="%s"' % label in line:
            mm = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', line)
            if mm:
                node = tuple(int(v) for v in mm.groups())
                break
    if not node:
        print("no entry '%s' with bounds in the dump" % label)
        return 2
    img = load(png)
    plate = plate_of(img, *node)
    if not plate:
        print("no white plate in the entry %s" % (node,))
        return 2
    x0, y0, x1, y1 = plate
    pw, ph = x1 - x0 + 1, y1 - y0 + 1
    cx, cy = (x0 + x1 + 1) / 2.0, (y0 + y1 + 1) / 2.0
    m = measure(img, cx, cy, pw)
    why = verdict(m)
    print("entry=%s plate=%dx%d at %d,%d %s -> %s" % (node, pw, ph, x0, y0, " ".join("%s=%.0f" % kv for kv in m.items()), "SPEAKER" if not why else "NOT THE SPEAKER: " + "; ".join(why)))
    return 0 if not why else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
