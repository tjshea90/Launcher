#!/usr/bin/env python3
"""Draws the launcher icon of the music app ("Music": the blue speaker) and writes it as an ADAPTIVE icon (white background + the speaker as the foreground).

WHY (TASKS BW1, Tj 2026-11-11): the music app must show the speaker icon of the "ATG music player" he already has. Three web searches found no copy of
that icon online, so it is REDRAWN from Tj's own screenshots (his drawer, where the icon is 39 px wide on Launcher's white plate, and the Files app).
Nothing of the screenshots is stored in the repo: only the NUMBERS measured from them (the table below) and the result of this script.
WHY ADAPTIVE (CI run #554, the real drawer on the emulator): Launcher3 wraps a LEGACY icon on its white plate and shrinks it, so a round legacy icon can never be
bigger than about 64 % of the plate (ours came out at 62 %); his speaker is 71.5 % of the plate (39.34 of 55 px). An adaptive icon is not shrunk, so the size is exact:
the 108 dp foreground layer shows its middle 72 dp (= the plate), the speaker is R_OUT / 55 of that window wide (FOREGROUND_RADIUS) and the background layer is pure white.

HOW the numbers were measured (research/MUSIC_ICON_2026-11-11.md): the speaker is a round driver, 39.5 px wide in the 600x1024 drawer. Mean colour per
radius (r, in drawer pixels, centre = the fitted centre of the thin black ring) separately for the top and the bottom of the picture:
    cone  = mean(r) + dev(r) * (dy / r)      (dy > 0 = below the centre: the cone is lit from below: top dark, bottom light, sides in between)
    bezel = mean(r) + dev(r) * (-dy / r)     (the grey ring is lighter on top)
    dust cap = 42 + 60 * (up / 4.75)         (a glossy dome: lighter on top, black at the bottom), 4.75 px radius, a dark edge ring around it
    thin black ring at r = 16.1 px (about 1.1 px wide), outer edge of the bezel at r = 19.7 px.
Run:  python3 -I tools/make_music_icon.py            -> writes eleven/src/main/res/mipmap-*/ic_launcher_foreground.png (108 dp layer: 108 / 162 / 216 / 324 / 432 px),
          mipmap-anydpi/ic_launcher.xml and values/ic_launcher_background.xml (white)
      python3 -I tools/make_music_icon.py --compare <drawer screenshot, 600x1024> <centre x> <centre y> [<out.png>]
          -> draws the speaker at the drawer's own size on a white plate and prints the difference to the screenshot (the fit check of BW1; the screenshot stays outside the repo)
      python3 -I tools/make_music_icon.py --drawer-sim <out.png> [<plate px, default 52>]
          -> a synthetic drawer: the written foreground cut to the plate window on a white plate, on grey (self-test input of tools/music_icon_check.py, tools/test_music_icon_check.sh)
"""
import math
import os
import sys

import numpy as np

R_OUT = 19.67  # outer radius of the artwork in drawer pixels (the unit of every table below)

# MEASURED (drawer pixels, centre of the speaker = the origin, f = dy / r: +1 straight below the centre, -1 straight above, 0 at the sides).
# Fit check (the --compare mode): mean absolute difference to the screenshot 4.8 levels per channel over the 1400 pixels of the speaker.
# Per radius the colour of every pixel is a straight line in f (least squares over all angles, 0.5 px bins; the residual is 2 to 5 levels).
# THE CONE: colour = M(r) + D(r) * f with the SAME D for red, green and blue: the speaker is lit from below by a white glow (lighter at the bottom, darker at the top).
CONE_R = [5.8, 6.2, 6.8, 7.2, 7.8, 8.2, 8.8, 9.2, 9.8, 10.2, 10.8, 11.2, 11.8, 12.2, 12.8, 13.2, 13.8, 14.2, 14.8, 15.4]
CONE_M = [  # mean colour (R, G, B)
    (98, 118, 163), (103, 124, 174), (99, 120, 168), (95, 117, 167), (92, 115, 167), (91, 115, 170), (89, 113, 171), (87, 114, 174),
    (85, 112, 175), (83, 111, 176), (80, 108, 174), (77, 106, 172), (73, 101, 166), (69, 97, 160), (63, 89, 149), (59, 84, 140),
    (52, 73, 124), (48, 69, 116), (41, 60, 102), (24, 34, 58)]
CONE_D = [63, 69, 69, 67, 65, 63, 60, 56, 52, 49, 45, 42, 38, 36, 32, 31, 26, 26, 22, 14]   # glow amplitude, the same in all three channels
# THE BEZEL (the grey ring): colour = M(r) + D(r) * f, grey, lighter at the top (D < 0)
BEZEL_R = [16.7, 17.2, 17.8, 18.2, 18.8, 19.3, 19.7]
BEZEL_M = [70, 90, 93, 97, 105, 125, 140]
BEZEL_D = [-8, -18, -33, -34, -39, -39, -30]
BLACK_R0, BLACK_R1 = 15.54, 16.70      # the thin black ring
BLACK = (1, 1, 3)
CAP_R = 4.03                          # the dust cap: a glossy dome, 38.3 + 11.8 * (height above the centre in px), black at the bottom
CAP_BASE, CAP_GAIN = 38.3, 11.8
CAP_EDGE_R = 5.3                      # the dark edge ring around the dome ends here
CAP_EDGE = (19, 19, 23)
def shade(x, y):
    """RGB (floats 0..255) and coverage (0 or 1) of the speaker at points (x, y) in drawer pixels, centre = (0, 0), y down. No anti-aliasing here (the caller supersamples)."""
    r = np.hypot(x, y)
    f = y / np.maximum(r, 1e-6)                    # +1 below the centre, -1 above
    alpha = (r <= R_OUT).astype(float)
    rgb = np.zeros(x.shape + (3,), float)
    # bezel (grey ring)
    g = np.interp(r, BEZEL_R, BEZEL_M) + np.interp(r, BEZEL_R, BEZEL_D) * f
    rgb = np.where((r >= BLACK_R1)[..., None], np.stack([g, g, g], axis=-1), rgb)
    # cone (blue, lit from below)
    m = np.stack([np.interp(r, CONE_R, [c[k] for c in CONE_M]) for k in range(3)], axis=-1)
    cone = np.clip(m + (np.interp(r, CONE_R, CONE_D) * f)[..., None], 0, 255)
    rgb = np.where(((r >= CAP_EDGE_R) & (r < BLACK_R0))[..., None], cone, rgb)
    # the thin black ring
    rgb = np.where(((r >= BLACK_R0) & (r < BLACK_R1))[..., None], np.array(BLACK, float), rgb)
    # the dust cap
    v = np.clip(CAP_BASE - CAP_GAIN * y, 8, 92)
    rgb = np.where((r < CAP_R)[..., None], np.stack([v, v, v], axis=-1), rgb)
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


# The plate of Tj's drawer: a pure-white box 55 x 55 px (its edges fall exactly on pixel boundaries: the row of pixels through the middle goes 190 / 174 (shadow) / 255 ... 255 / 178 / 193), the speaker is 2 * R_OUT = 39.34 px wide on it.
PLATE_PX = 55.0
# An adaptive icon is a 108 dp layer of which only the middle 72 dp (the "window", the plate) is visible, so the speaker's outer radius is this fraction of the layer:
FOREGROUND_RADIUS = R_OUT / PLATE_PX * 72.0 / 108.0          # 0.2384; the speaker is 0.4769 of the layer = 71.5 % of the plate
ADAPTIVE_XML = """<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by tools/make_music_icon.py (TASKS BW1): a pure white plate with the blue speaker of Tj's "Music", exactly the size it has in his drawer. -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
"""
BACKGROUND_XML = """<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by tools/make_music_icon.py (TASKS BW1): the plate behind the speaker is pure white, as in Tj's drawer. -->
<resources>
    <color name="ic_launcher_background">#FFFFFF</color>
</resources>
"""
LAYER_DP = 108
DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}


def layer_px(dens):
    return int(round(LAYER_DP * DENSITIES[dens]))


def foreground(px):
    """The foreground layer (RGBA, px x px = 108 dp): only the speaker, centred; everything else transparent."""
    return render(px, px * FOREGROUND_RADIUS, ssaa={108: 8, 162: 8, 216: 6}.get(px, 4))


def write_icons(res_dir):
    from PIL import Image
    for dens in DENSITIES:
        px = layer_px(dens)
        d = os.path.join(res_dir, "mipmap-" + dens)
        os.makedirs(d, exist_ok=True)
        Image.fromarray(foreground(px), "RGBA").save(os.path.join(d, "ic_launcher_foreground.png"), optimize=True)
        print("wrote", os.path.join(d, "ic_launcher_foreground.png"), px)
    os.makedirs(os.path.join(res_dir, "mipmap-anydpi"), exist_ok=True)
    with open(os.path.join(res_dir, "mipmap-anydpi", "ic_launcher.xml"), "w", encoding="utf-8") as f:
        f.write(ADAPTIVE_XML)
    os.makedirs(os.path.join(res_dir, "values"), exist_ok=True)
    with open(os.path.join(res_dir, "values", "ic_launcher_background.xml"), "w", encoding="utf-8") as f:
        f.write(BACKGROUND_XML)
    print("wrote the adaptive icon xml and the white background colour")


def drawer_sim(out, plate=52, shrink=1.0):
    """A synthetic 600 x 1024 drawer: grey backdrop, a white plate (plate x plate px, rounded corners) with the written foreground cut to its window, a label-less single entry at (160, 230).
    What Launcher3 does with an adaptive icon: the 72 dp window of the layer fills the plate. `shrink` scales the speaker alone about the plate's centre (0.87 = what Launcher3's wrapper
    did to the first, legacy icon: the self-test's negative control). Used as input of tools/music_icon_check.py by tools/test_music_icon_check.sh."""
    from PIL import Image, ImageDraw
    big = foreground(layer_px("xxxhdpi"))                      # 432 px = 108 dp
    layer = int(round(plate * 108 / 72.0))
    lay = Image.fromarray(big, "RGBA").resize((layer, layer), Image.LANCZOS)
    win = lay.crop(((layer - plate) // 2, (layer - plate) // 2, (layer - plate) // 2 + plate, (layer - plate) // 2 + plate))
    if shrink != 1.0:
        n = max(2, int(round(plate * shrink)))
        small = win.resize((n, n), Image.LANCZOS)
        win = Image.new("RGBA", (plate, plate), (0, 0, 0, 0))
        win.paste(small, ((plate - n) // 2, (plate - n) // 2), small)
    img = Image.new("RGB", (600, 1024), (169, 160, 163))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((160, 230, 160 + plate - 1, 230 + plate - 1), radius=max(3, plate // 12), fill=(255, 255, 255))
    img.paste(win, (160, 230), win)
    img.save(out)
    print("wrote", out, "plate", plate, "shrink", shrink)


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
    elif len(sys.argv) >= 3 and sys.argv[1] == "--drawer-sim":
        drawer_sim(sys.argv[2], int(sys.argv[3]) if len(sys.argv) > 3 else 52, float(sys.argv[4]) if len(sys.argv) > 4 else 1.0)
    elif len(sys.argv) == 1:
        here = os.path.dirname(os.path.abspath(__file__))
        write_icons(os.path.join(here, "..", "eleven", "src", "main", "res"))
    else:
        print(__doc__)
        sys.exit(2)
