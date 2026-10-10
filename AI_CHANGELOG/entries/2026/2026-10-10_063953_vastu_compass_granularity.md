# AI Changelog Entry: CL-AND-20261010-063953-003

- **Entry ID**: CL-AND-20261010-063953-003
- **Timestamp**: 2026-10-10 06:39:53 IST (UTC+05:30)
- **Status**: COMPLETED
- **Risk Class**: R2
- **Author**: Subagent (parent orchestrator, user-directed)

## 1. Problem & Context
User correction on the Vastu compass (commit 8d4ab9d had shipped 4 visual styles: classic/minimal/mandala/digital): he did NOT want visual styles — he wants compasses by DIRECTION COUNT, matching the website's compass modal granularity selector (4/8/10/16). Task: replace the style switcher with a direction-granularity selector in `CompassModeScreen`.

## 2. Design decisions (evidence-backed)
- The website's facing `<select>` (`pg-widget-vastu.php`, workspace copy) has exactly 8 options (N..NW), and the backend `vastu-engine.php::norm_dir()` accepts only `N,NE,E,SE,S,SW,W,NW,C` — a 16-direction code like "NNE" is normalized to `''` (facing treated as "not provided"). VERIFIED by reading both files.
- Therefore "Use This Direction" always passes the nearest 8-direction code (`directionCode()`), while the dial/readout/photo stamp show the precise granular sector (e.g. NNE). Same contract as the website.
- 10-direction: 8 ring sectors + center Urdhva/Adho indicator with an honest note that up/down cannot be sensor-measured (no fabricated sensor data).

## 3. Changes (all in place, no new files)
- `ui/screens/vastu/CompassSensor.kt`: added `SIXTEEN_CODES` + `granularCode(heading, ringSectors)` (4/8/16 sector math; 10 → 8). `directionCode()` and `roundedDegrees()` untouched.
- `ui/screens/vastu/CompassModeScreen.kt`:
  - `CompassStyle` enum → `CompassGranularity` (FOUR/EIGHT/TEN/SIXTEEN, `sectors`, `ringSectors`, `sectorCodes()`).
  - `CompassStyleSwitcher` → `CompassGranularitySelector` (4/8/10/16 numeral buttons + bilingual label).
  - Deleted `CompassDialClassic/Minimal/Mandala/Digital`, `MandalaDeityOverlay`, `DIAL_CODES`; added single `CompassDialGranular` (exact sector count, gold live-sector wedge, dikpala deity names for 8/10, center degrees + granular direction + Urdhva/Adho line for 10).
  - Removed now-unused imports (`layout.offset`, `drawscope.rotate`).
  - Screen wiring: `passCode` (8-dir, to form) vs `granularCode` (display/stamp); photo stamp uses the granular sector; TEN shows the up/down honesty note; camera preview/capture/gallery-save/dialog flow untouched.
- `ui/i18n/FeatureStrings.kt`: DIR_HI/DIR_EN extended with NNE/ENE/ESE/SSE/SSW/WSW/WNW/NNW/UP/DOWN; added `vastu_compass_granularity`, `vastu_compass_up_down`, `vastu_compass_up_down_note` (HI+EN); removed dead `vastu_compass_style_*` keys.
- `CHANGELOG.md`: new entry (Before/After/Verification).

## 4. Verification evidence
- Brace/paren/bracket deltas are 0 in all 3 edited files.
- Zero remaining references to `CompassStyle`, the 4 old dials, `MandalaDeityOverlay`, `DIAL_CODES`, `vastu_compass_style_*` (grep exit 1).
- All 3 new keys resolve exactly once in each of the HI and EN maps; all 10 new direction codes resolve exactly once in each of DIR_HI/DIR_EN.
- Sector math independently verified in Python (mirrors the Kotlin formulas): exhaustive 0.1° sweep over 0–360° maps every heading to its nearest sector center for 4/8/16; exact boundary checks pass (4-dir: 45/135/225/315; 16-dir: ±11.25° boundaries, e.g. 11.25→NNE, 348.75→N); dial wedge span matches each sector's angular span.
- Not device-verified (no device in this environment): confirm on a physical device that (1) each granularity button renders the correct sector count, (2) rotating the phone highlights the correct sector incl. NNE etc. at 16, (3) Use This Direction fills the facing field with the nearest 8-direction code, (4) photo stamp shows the granular direction + degrees, (5) Hindi language switches all new strings.
- CI (`assembleDebug` + `assembleRelease` on push to main) is the compile gate — must be green after push.

## 5. Rejected approaches
- Passing the raw 16-direction code to the form: REJECTED — backend `norm_dir()` would silently drop it, losing the facing verdict (verified by reading `vastu-engine.php`).
- Keeping 4 visual styles alongside granularity: REJECTED — user explicitly corrected that he never wanted visual styles.
- Deity names on the 16-direction dial: REJECTED — only the 8 classical directions have dikpala lords; sub-sectors show codes only (honest).
