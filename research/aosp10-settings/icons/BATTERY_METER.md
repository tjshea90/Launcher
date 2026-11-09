# Big battery icon of the Battery page (AOSP 10, android-10.0.0_r47)

It is not an XML drawable: it is drawn in code on a **12 x 20 unit grid**, from four path strings in the framework config, and
tinted by a colour filter. Paths below are relative to the clone roots (`settings10/` = Settings clone, `fwbase/` = frameworks subset).

Sources read:
- `settings10/src/com/android/settings/fuelgauge/BatteryMeterView.java` (130 lines): an ImageView; picks the colour filter. Its inner
  `BatteryMeterDrawable` only overrides the intrinsic size.
- `fwbase/packages/SettingsLib/src/com/android/settingslib/graph/ThemedBatteryDrawable.kt` (424 lines): does all the drawing.
- Strings: `fwbase/core/res/res/values/config.xml` lines 4051-4070 (verbatim block in `VALUES_EXCERPTS.md`, section C).
- `BatteryMeterDrawableBase.java` is the legacy drawable and is NOT used by this page.

## 1. Literal path strings (surrounding whitespace and line breaks of the XML value removed, nothing else changed)

`config_batterymeterPerimeterPath` (`config_batterymeterErrorPerimeterPath` is a reference to this very string):
```
M3.5,2 v0 H1.33 C0.6,2 0,2.6 0,3.33 V13v5.67 C0,19.4 0.6,20 1.33,20 h9.33 C11.4,20 12,19.4 12,18.67 V13V3.33 C12,2.6 11.4,2 10.67,2 H8.5 V0 H3.5 z M2,18v-7V4h8v9v5H2L2,18z
```
`config_batterymeterFillMask`:
```
M2,18 v-14 h8 v14 z
```
`config_batterymeterBoltPath`:
```
M5,17.5 V12 H3 L7,4.5 V10 h2 L5,17.5 z
```
`config_batterymeterPowersavePath`:
```
M9,10l-2,0l0,-2l-2,0l0,2l-2,0l0,2l2,0l0,2l2,0l0,-2l2,0z
```
Flags: `config_batterymeterDualTone` = false (the single-tone branch of `draw()` runs), `config_criticalBatteryWarningLevel` = 5
(`config.xml` line 1290). Level colour arrays (`fwbase/packages/SettingsLib/res/values/arrays.xml` lines 573-581): levels `[15, 100]`,
values `[?android:attr/colorError, @android:color/white]`.

## 2. Geometry (grid units)

- **Perimeter**: ONE filled path, no stroke, two contours.
  - Contour 1 = the body, x 0..12, y 2..20 (corner radius about 1.33) plus the terminal knob x 3.5..8.5, y 0..2. It runs left, down, right, up (counter-clockwise on screen).
  - Contour 2 = the rectangle x 2..10, y 4..18, wound the other way (clockwise), so with the default non-zero rule it is a hole.
  - Result: a wall 2 units thick on every side, cavity 8 x 14 units.
- **Fill mask** = the cavity rectangle, x 2..10, y 4..18.
- **Bolt** = polygon (5,17.5) (5,12) (3,12) (7,4.5) (7,10) (9,10).
- **Power-save plus** = a cross: vertical bar x 5..7, y 8..14; horizontal bar x 3..9, y 10..12.
- **Scale**: `scaleMatrix.setScale(bounds.right / 12, bounds.bottom / 20)`, NOT uniform in general. The Settings meter's intrinsic size is
  66 x 100 dp (`battery_meter_width` / `battery_meter_height`, `settings10/res/values/dimens.xml` lines 322-323) = 5.5 x 5.0 dp per unit;
  on the 170 dpi tablet 70 x 106 px = about 5.83 x 5.30 px per unit. A VectorDrawable with `viewportWidth` 12, `viewportHeight` 20 and size 66 x 100 dp
  scales the same way.

## 3. Drawing rules (non-dual-tone branch of `draw()`, everything inside one `saveLayer(null, null)`)

1. `fillFraction = level / 100`. `fillTop = fillRect.top` if level >= 95, else `fillRect.top + fillRect.height * (1 - fillFraction)`.
   `levelRect` = the fill-mask bounds with `top = floor(fillTop)` (the floor is in PIXELS, after scaling).
2. `unifiedPath = scaledPerimeter UNION levelRect`.
3. Charging only: `unifiedPath = unifiedPath DIFFERENCE scaledBolt`; if `invertFillIcon` is false the bolt is first drawn as a solid shape with the fill paint.
4. Draw `unifiedPath` with the fill paint (the one colour, see section 4).
5. Level <= 15 (`CRITICAL_LEVEL`) and not charging: clip to the scaled fill mask and draw `levelRect` a second time with the fill paint
   (no visible change with an opaque colour; with the 54 % error colour it doubles the fill's opacity to about 79 %).
6. Charging only: `clipOutPath(scaledBolt)`, then stroke the bolt outline: width `max(bounds.right / 12 * 3, 6)` px (17.5 px on the tablet),
   round join, miter 5. If `invertFillIcon` the stroke is the fill colour (BlendMode.SRC); otherwise it is BlendMode.CLEAR (a transparent halo around the bolt).
7. Not charging and power save on: draw `scaledErrorPerimeter` and `scaledPlus` with `errorPaint` (colour = `batterymeter_plus_color` = colorError,
   BlendMode.SRC, no colour filter).
8. `invertFillIcon` hysteresis (in `setBatteryLevel`): becomes true when level >= 67, false when level <= 33, unchanged in between; it starts false.

## 4. Colours (the colour filter decides; the paints' own colours are overridden)

`BatteryMeterView` never calls `setColors`, so the per-level paint colours and the frame colour (`meter_background_color` = colorForeground at alpha 0.3)
do not show. `setColorFilter` reaches `fillPaint`, `fillColorStrokePaint` and `dualToneBackgroundFill`, not `errorPaint`. After every level or
power-save change `updateColorFilter()` picks:

| State | Filter | Drawn colour of perimeter and fill |
| :- | :- | :- |
| power save on | `PorterDuffColorFilter(colorForeground, SRC)` | #FF000000; then the error perimeter and the plus are painted over it in #FFFF5722 |
| level < 5 (`criticalLevel`), power save off | `PorterDuffColorFilter(color/battery_icon_color_error, SRC_IN)` | #8AFF5722 (colorError at `secondaryContentAlpha` 0.54) |
| anything else | `PorterDuffColorFilter(colorAccent, SRC)` | #FF008577 |

Resolved theme colours (light tablet): colorAccent #FF008577, colorError #FFFF5722, colorForeground #FF000000
(chain in `VALUES_EXCERPTS.md`, section B). The state-list files behind the names are copied here:
`battery_icon_color_error.xml`, `batterymeter_plus_color.xml`, `meter_background_color.xml`.

## 5. Where it sits and what was measured

- Layout `settings10/res/layout/battery_header.xml`: a horizontal LinearLayout with style `EntityHeader`
  (`fwbase/packages/SettingsLib/LayoutPreference/res/values/styles.xml` lines 18-24: background colorPrimaryDark #FFE0E0E0, paddingTop 24 dp,
  paddingBottom 16 dp, paddingEnd 16 dp). Left column 170 dp wide, marginStart 72 dp, marginEnd 8 dp (percentage 36 sp, two summary lines).
  Then `BatteryMeterView` with `layout_width` match_parent, `layout_height` 100 dp (`battery_meter_height`), `layout_gravity` center, default scaleType FIT_CENTER:
  the 66 x 100 dp icon is centred in the width that is left over.
- Tablet (600 px wide): measured accent #008577, 70 x 106 px at x 390-459, y 136-241 (centre x 424.5). The rule gives (72 + 170 + 8) dp = 265.6 px,
  minus paddingEnd 17 px leaves 317 px, centre 265.6 + 158.7 = 424.3 px, so the layout rule reproduces the measurement.
