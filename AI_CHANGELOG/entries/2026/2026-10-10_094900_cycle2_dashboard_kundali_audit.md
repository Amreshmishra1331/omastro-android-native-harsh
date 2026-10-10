# AI Changelog Entry: CL-AND-20261010-094900-004

- **Entry ID**: CL-AND-20261010-094900-004
- **Timestamp**: 2026-10-10 09:49:00 IST (UTC+05:30)
- **Status**: COMPLETED (pending CI verification)
- **Risk Class**: R2
- **Author**: Subagent (parent orchestrator, audit loop Cycle 2 of 10)

## 1. Problem & Context
Audit scope (Cycle 2 of 10): PalmistryScreen, FaceReadingScreen, VastuScreen, DashboardScreen, KundaliScreen (+ KundaliExtendedTabs, part of the Kundali screen). For every UI element: button handlers, i18n coverage, loading/empty/error states, multipart upload paths, navigation validity, mock data. Fix rules: bilingual via W.t(), no emojis, no fake data, CHANGELOG before/after, commit + push, CI green.

## 2. Findings
- PalmistryScreen / FaceReadingScreen: ZERO issues. All buttons traced to real actions; all strings already bilingual; loading/refusal/error/402 states present; refusals never saved to history; share button hides on refusals; multipart uses `image` + `hand`/`notes` + `lang` parts (`VisionRepository`).
- VastuScreen: one i18n gap — the pada line hardcoded the English word "Pada". All else verified: manual/photo tab switching, compass overlay (real magnetometer + CameraX), room add/remove, `/vastu/analyze-photo` multipart (`photo` + `notes` + `lang`), null-score dial shows "insufficient data" (never faked).
- DashboardScreen: ~40 hardcoded English strings (tab pills, sub-tabs, Add Money buttons, Recent Kundalis, Loading..., empty states, stats labels, Profile/Password/Plan/Billing/Account tab labels, icon content descriptions). Quick-action titles already bilingual via `LangPrefs.label()` — verified, untouched.
- KundaliScreen: hardcoded English in birth form (gender options, date/time/place fields, hints, coordinate placeholders, loading text, save dialog), Dasha tab ("ACTIVE" badges, level names, "N years", "Phal:", "Upay (Remedies)", "Active"/"Not Active", Ashtakavarga strength/house/bindu labels), Karakamsha "House N" labels, Saturn alert fallback.
- No dead buttons, no mock data, no emojis anywhere in the five screens. All nav actions resolve to registered `Screen.kt` routes.

## 3. Changes (all in place, no new files)
- `ui/i18n/FeatureStrings.kt`: 90+ new HI translations ("UI audit 2026-10-10 (Cycle 2)" block). EN map not needed — `W.t` falls back to the English key itself.
- `ui/screens/dashboard/DashboardScreen.kt`: all strings wrapped in `W.t()`/`W.tf()`; `DashTab`/`ActivitySubTab` enums now expose `title` via `W.t(key)` (pills auto-switch with Settings language); counts via `W.tf("dash_count_kundalis"/"dash_count_chats")`; paid/bonus via `W.tf("dash_paid_bonus_fmt")`.
- `ui/screens/kundali/KundaliScreen.kt`: form + save dialog + header wrapped in `W.t()`; dasha levels/ACTIVE/years/"Phal"/"Upay"/severity chips/strength words/house labels/bindus via `W.t()`/`W.tf()`.
- `ui/screens/kundali/KundaliExtendedTabs.kt`: Saturn alert fallback + Karakamsha house labels bilingual.
- `ui/screens/vastu/VastuScreen.kt`: pada line via `W.tf("vastu_pada_fmt", W.t("vastu_pada_word"), …)`.
- `ui/components/PlaceSearchField.kt`: "Searching places..." bilingual (added `W` import).
- `CHANGELOG.md`: new entry with Before/After for every fix.

## 4. Verification evidence
- Residual literal scan over all 7 screen files: only server data, format placeholders ("DD-MM-YYYY"), AI-context labels (English prompt context, not UI), photo-stamp text, and `LangPrefs.label()`-wrapped titles remain.
- `git diff --stat`: 6 files, +225/-119.
- No local JDK in this environment — CI (`assembleDebug` + `assembleRelease` on push to main) is the compile gate; must be green after push.

## 5. Rejected approaches
- Leaving "Kundali · Dasha" etc. AI-context labels in English: KEPT — they are prompt context identifiers, not user-facing UI.
- Per-screen language toggles: REJECTED — global Settings language drives everything; no per-screen state.
