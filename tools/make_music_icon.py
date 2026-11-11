#!/usr/bin/env python3
"""Draws the launcher icon of the music app ("Music": the blue speaker) and writes the five legacy PNGs.

WHY (TASKS BW1, Tj 2026-11-11): the music app must show the speaker icon of the "ATG music player" he already has. Two web searches found no copy of
that icon online, so it is REDRAWN from Tj's own screenshots (his drawer, where the icon is 39 px wide on Launcher's white plate, and the Files app).
Nothing of the screenshots is stored in the repo: only the NUMBERS measured from them (the table below) and the result of this script.

HOW the numbers were measured (research/MUSIC_ICON_2026-11-11.md): the speaker is a round driver, 39.5 px wide in the 600x1024 drawer. Mean colour per
radius (r, in drawer pixels, centre = the fitted centre of the thin black ring) separately for the top and the bottom of the picture:
    cone  = mean(r) + dev(r) * (dy / r)      (dy > 0 = below the centre: the cone is lit from below: top dark, bottom light, sides in between)
    bezel = mean(r) + dev(r) * (-dy / r)     (the grey ring is lighter on top)
    dust cap = 42 + 60 * (up / 4.75)         (a glossy dome: lighter on top, black at the bottom), 4.75 px radius, a dark edge ring around it
    thin black ring at r = 16.1 px (about 1.1 px wide), outer edge of the bezel at r = 19.7 px.
Run:  python3 -I tools/make_music_icon.py            -> writes eleven/src/main/res/mipmap-*/ic_launcher.png (48 / 72 / 96 / 144 / 192 px)
      python3 -I tools/make_music_icon.py --compare <drawer screenshot, 600x1024> <centre x> <centre y> [<out.png>]
          -> draws the speaker at the drawer's own size on a white plate and prints the difference to the screenshot (the fit check of BW1; the screenshot stays outside the repo)
"""
import math
import os
import sys

import numpy as np

R_OUT = 19.7  # outer radius of the artwork in drawer pixels (the unit of every table below)

# (r in drawer px, top colour, bottom colour) of the blue cone, measured: mean of the north and south sectors, +-30 degrees, 1 px steps
CONE = [
    # r      north (R,G,B)        south (R,G,B)
    (5.0, (30, 44, 80), (110, 118, 140)),
    (6.0, (41, 60, 108), (164, 181, 218)),
    (7.0, (35, 53, 99), (161, 181, 224)),
    (8.0, (35, 54, 105), (152, 174, 221)),
    (9.0, (36, 59, 117), (143, 167, 219)),
    (10.0, (39, 64, 130), (133, 160, 215)),
    (11.0, (39, 65, 134), (120, 149, 207)),
    (12.0, (36, 61, 129), (105, 133, 190)),
    (13.0, (31, 53, 113), (88, 114, 165)),
    (14.0, (24, 41, 91), (70, 92, 137)),
    (15.0, (15, 26, 59), (53, 71, 108)),
    (15.6, (6, 9, 18), (20, 28, 44)),
]
# (r, grey at the top, grey at the bottom) of the bezel between the black ring and the edge
BEZEL = [
    (16.6, 66, 52),
    (17.0, 83, 54),
    (18.0, 127, 63),
    (19.0, 153, 72),
    (19.7, 160, 78),
]
BLACK_R0, BLACK_R1 = 15.6, 16.65      # the thin black ring
BLACK = (3, 4, 6)
CAP_R = 4.75                          # the dust cap
CAP_EDGE_R = 5.1                      # its dark edge ring ends here
CAP_EDGE = (18, 18, 22)


def interp(table, r, k):
    """Linear interpolation of column k (1 or 2 = the two sides) of the table at radius r (numpy arrays); clamped at both ends."""
    xs = np.array([row[0] for row in table], float)
    out = []
    for c in range(3):
        ys = np.array([row[k][c] if isinstance(row[k], tuple) else row[k] for row in table], float)
        out.append(np.interp(r, xs, ys))
    return np.stack(out, axis=-1)


def interp1(table, r, k):
    xs = np.array([row[0] for row in table], float)
    ys = np.array([row[k] for row in table], float)
    return np.interp(r, xs, ys)


def shade(x, y):
    """RGBA (floats 0..255, alpha 0..1) of the speaker at points (x, y) in drawer pixels, centre = (0, 0), y down. No anti-aliasing here (the caller supersamples)."""
    r = np.hypot(x, y)
    safe = np.maximum(r, 1e-6)
    f = y / safe                                   # +1 below the centre, -1 above
    rgb = np.zeros(x.shape + (3,), float)
    alpha = (r <= R_OUT).astype(float)
    # bezel
    top = interp1(BEZEL, r, 1)
    bot = interp1(BEZEL, r, 2)
    g = (top + bot) / 2 + (top - bot) / 2 * (-f)
    bez = np.stack([g, g, g], axis=-1)
    rgb = np.where((r >= BLACK_R1)[..., None], bez, rgb)
    # cone
    north = interp(CONE, r, 1)
    south = interp(CONE, r, 2)
    cone = (north + south) / 2 + (south - north) / 2 * f[..., None]
    cone = np.clip(cone, 0, 255)
    rgb = np.where(((r >= CAP_EDGE_R) & (r < BLACK_R0))[..., None], cone, rgb)
    # thin black ring
    rgb = np.where(((r >= BLACK_R0) & (r < BLACK_R1))[..., None], np.array(BLACK, float), rgb)
    # dust cap: glossy dome, light on top
    up = -y
    cap_v = np.clip(42 + 60 * (up / CAP_R), 8, 92)
    cap = np.stack([cap_v, cap_v, cap_v], axis=-1)
    rgb = np.where((r < CAP_R)[..., None], cap, rgb)
    rgb = np.where(((r >= CAP_R) & (r < CAP_EDGE_R))[..., None], np.array(CAP_EDGE, float), rgb)
    return rgb, alpha


def render(size, radius_px, cx=None, cy=None, ssaa=8, background=None):
    """size x size picture; the speaker has outer radius radius_px (output pixels) and is centred at (cx, cy) (default: the middle).
    background None = transparent PNG (RGBA), else an (R, G, B) tuple the speaker is composited on (the comparison with a screenshot)."""
    cx = size / 2 if cx is None else cx
    cy = size / 2 if cy is None else cy
    scale = R_OUT / radius_px                       # drawer pixels per output pixel
    n = size * ssaa
    ys, xs = np.mgrid[0:n, 0:n]
    px = ((xs + 0.5) / ssaa - cx) * scale
    py = ((ys + 0.5) / ssaa - cy) * scale
    rgb, a = shade(px, py)
    # box-filter down to the output size (premultiplied so the edge colour is not dragged towards black)
    pre = rgb * a[..., None]
    pre = pre.reshape(size, ssaa, size, ssaa, 3).mean(axis=(1, 3))
    aa = a.reshape(size, ssaa, size, ssaa).mean(axis=(1, 3))
    if background is not None:
        bg = np.array(background, float)
        out = pre + bg * (1 - aa[..., None])
        return np.clip(np.rint(out), 0, 255).astype(np.uint8)
    col = np.where(aa[..., None] > 0, pre / np.maximum(aa[..., None], 1e-6), 0)
    rgba = np.concatenate([np.clip(np.rint(col), 0, 255), np.rint(aa[..., None] * 255)], axis=-1)
    return rgba.astype(np.uint8)


def write_icons(res_dir):
    from PIL import Image
    sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for dens, px in sizes.items():
        d = os.path.join(res_dir, "mipmap-" + dens)
        os.makedirs(d, exist_ok=True)
        # the artwork fills 92 % of the canvas (the usual legacy launcher icon proportion); Launcher3 normalises the visible size itself
        img = render(px, px * 0.46, ssaa=8 if px >= 96 else 12)
        Image.fromarray(img, "RGBA").save(os.path.join(d, "ic_launcher.png"), optimize=True)
        print("wrote", os.path.join(d, "ic_launcher.png"), px)


def compare(shot, cx, cy, out=None):
    """Fit check: draw the speaker as the drawer shows it (white plate around it) and print the difference to the screenshot region."""
    from PIL import Image
    ref = np.asarray(Image.open(shot).convert("RGB")).astype(float)
    x0, y0 = int(cx) - 26, int(cy) - 26
    size = 53
    tile = ref[y0:y0 + size, x0:x0 + size]
    mine = render(size, R_OUT, cx - x0, cy - y0, ssaa=8, background=(255, 255, 255)).astype(float)
    yy, xx = np.mgrid[0:size, 0:size]
    inside = np.hypot(xx + 0.5 - (cx - x0), yy + 0.5 - (cy - y0)) <= R_OUT + 1.5
    d = np.abs(mine - tile)
    print("pixels compared: %d" % inside.sum())
    print("mean abs difference per channel (0..255): R %.1f  G %.1f  B %.1f" % tuple(d[inside].mean(axis=0)))
    print("95th percentile of the difference: %.0f" % np.percentile(d[inside], 95))
    for name, (lo, hi) in {"cap r<5": (0, 5), "cone 5-15.6": (5, 15.6), "black ring": (15.6, 16.65), "bezel 16.65-19.7": (16.65, 19.7)}.items():
        rr = np.hypot(xx + 0.5 - (cx - x0), yy + 0.5 - (cy - y0))
        m = (rr >= lo) & (rr < hi)
        print("  %-18s mean abs difference %.1f (n=%d)" % (name, d[m].mean(), m.sum()))
    if out:
        both = np.concatenate([tile, mine, np.clip(d * 3, 0, 255)], axis=1).astype(np.uint8)
        Image.fromarray(both).resize((both.shape[1] * 6, both.shape[0] * 6), Image.NEAREST).save(out)
        print("side by side (screenshot | drawn | difference x3):", out)


if __name__ == "__main__":
    if len(sys.argv) >= 5 and sys.argv[1] == "--compare":
        compare(sys.argv[2], float(sys.argv[3]), float(sys.argv[4]), sys.argv[5] if len(sys.argv) > 5 else None)
    elif len(sys.argv) == 1:
        here = os.path.dirname(os.path.abspath(__file__))
        write_icons(os.path.join(here, "..", "eleven", "src", "main", "res"))
    else:
        print(__doc__)
        sys.exit(2)
