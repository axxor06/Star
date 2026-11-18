# STAR Menu Generator (Android)

Native Kotlin + Jetpack Compose app for STAR Catering & Events. Create catering menus (individual
items, packages / Sadhya, buffets), calculate exact totals, place them on the two original templates
(Purple Arch Floral, Green & Gold Reception) and export PDF / PNG. Fully offline: the app has **no
INTERNET permission at all**, so Android itself guarantees it can never go online.

> ## STATUS: SOURCE PROJECT — NOT YET COMPILED
> This project was written in an environment with no Android SDK, no Gradle, no Kotlin compiler and
> no internet, so **it has never been built or run**. Everything below under "Tested / not tested" is
> exact. Expect that the first Gradle sync / build on your PC may show a few small compile errors;
> send me the messages and I will fix them. See [What has and has not been tested](#what-has-and-has-not-been-tested).

---

## 1. Build and run on Windows (step by step)

**What you need:** a Windows PC with internet (only for the first Gradle download), about 6 GB free disk,
and your Android phone (Android 8.0 or newer) with a USB cable.

### A. Install Android Studio
1. Download **Android Studio** (latest stable, Ladybug 2024.2.1 or newer) from https://developer.android.com/studio
2. Run the installer; in the setup wizard choose **Standard** and accept the licences. This installs the
   Android SDK and a bundled JDK. Wait for the downloads to finish.

### B. Open the project
1. Extract `StarMenuGenerator.zip` to a **short folder path without OneDrive**, e.g. `C:\StarMenuGenerator`.
   Inside it you must see `settings.gradle.kts`, `build.gradle.kts` and an `app` folder.
2. Start Android Studio → **Open** → select the `StarMenuGenerator` folder (the one containing
   `settings.gradle.kts`) → **OK** → **Trust Project**.
3. Wait for **Gradle sync** (bottom status bar). The first sync downloads Gradle 8.9 and libraries
   (~1 GB) and can take 5–15 minutes.
4. If a banner says **"Install missing SDK package(s)"** (Android SDK Platform 35 / Build-Tools), click
   **Install** and accept. If Studio offers to *upgrade the Android Gradle Plugin*, you may choose
   **Remind me later**; the project is pinned to a consistent set of versions (see §5).
5. Check the JDK: **File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**
   must be the bundled `jbr-17` (or "Embedded JDK", any version 17+).

### C. Run the unit tests first (recommended)
In the **Project** panel open `app/src/test/java/com/star/menugenerator`, right-click the folder →
**Run 'Tests in 'menugenerator''**. All tests should be green (about 60 tests: the original project's 13
tests ported, plus extra edge cases and golden pagination checks). If any fail, send me the red output.

### D. Install on your phone (USB)
1. On the phone: **Settings → About phone → tap "Build number" 7 times** → back → **Developer options →
   USB debugging ON**.
2. Connect the phone by USB, tap **Allow** on the "Allow USB debugging?" prompt. (Some brands need their
   Windows USB driver, or in Studio: **Tools → SDK Manager → SDK Tools → Google USB Driver**.)
3. Your phone appears in the device drop-down at the top of Android Studio. Press the green **Run ▶** button.
   The app installs and opens.

### E. Or build an APK file to copy to the phone
1. **Build → Build Bundle(s) / APK(s) → Build APK(s)**, then click **locate** in the popup.
2. The file is `app\build\outputs\apk\debug\app-debug.apk`. Copy it to the phone (USB / Drive / WhatsApp),
   open it, and allow *Install unknown apps* for the app you opened it from.
3. (Optional) for a release-style APK run `assembleRelease` from the Gradle panel
   (**Gradle → app → Tasks → build → assembleRelease**) → `app\build\outputs\apk\release\app-release.apk`.
   It is signed with the local debug key, which is fine for your own phone. For Google Play you must
   create your own upload key.

> There is no `gradlew.bat` in the ZIP because the Gradle wrapper JAR is a binary I could not download here.
> Android Studio does not need it: it reads `gradle/wrapper/gradle-wrapper.properties` and fetches Gradle 8.9 itself.
> If Studio complains, choose **Gradle → "Use Gradle from: Specified location"** and point to a Gradle 8.9 install,
> or run `gradle wrapper --gradle-version 8.9` once in the project folder.

### Where your files go
* **PDF / PNG:** saved to the phone's **Downloads › STAR Menus** folder (Android 10+), and the result dialog
  offers **Share** (WhatsApp, Drive, email…) and **Print** (PDF). On Android 8/9 files are created for
  Share / Print only (no public-folder write without a storage permission, which the app deliberately does not ask for).
* **Backup:** one `STAR-menus-backup-YYYY-MM-DD.json` file; you choose where to save it.

---

## 2. How it works (workflow)

**Create Menu → Menu Title → Add Meals → Add Menu Entries → Calculate Totals → Select Template → Preview → Export PDF/PNG**

| Step | Screen | Notes |
|---|---|---|
| Splash | Full STAR logo | brand splash, then Home |
| Home | Dashboard | create menu, My Menus, Backup & Restore, recent menus |
| 1 Menu | Create your menu | title, meals (Breakfast / Lunch / Evening / Dinner / Late Night / Custom; add, delete, duplicate, reorder), entries (Individual, Package/Sadhya, Buffet; add, delete, duplicate, reorder), live totals bar |
| 2 Template | Select a template | the two supplied designs |
| 3 Preview | Preview & export | every page rendered live, tap to zoom, Customize sheet, Save PDF, Save PNG, Share, Print |
| Extras | My Menus | search, sort, open, duplicate, delete (with confirmation) |
| Extras | Backup & Restore | JSON backup; restore with Merge or Replace |

Menus **autosave** to the on-phone database 1 second after your last edit and when the app goes to the
background. Only valid menus are written ("Not saved yet — fix the highlighted fields").

### Business rules (ported from the React project's `shared/calc.js` and `shared/validate.js`)
* Money is whole **paise** in `BigInteger`; no floating point ever touches a total.
* Numbers: max ₹/count **10,000,000**; quantity ≤ **3 decimals**; price ≤ **2 decimals**; plate counts must be **whole numbers**; negatives and non-numbers rejected; commas stripped (`1,000` → 1000).
* **Individual** total = quantity × price per unit, rounded **half-up** to the paisa.
* **Package / Sadhya** total = price per plate × number of plates. **Buffet** total = price per plate × plates (never per item).
* Missing price → *unpriced*; missing quantity/plates → *incomplete*; invalid → *invalid*. None of these is ever treated as 0 or 1: they are excluded from totals and listed with the reason.
* Separate **Individual / Package / Buffet** totals plus **Grand Total**; the grand total can be printed on the card (optional) with a "Partial total" note if anything is not counted.
* Limits: menu name 120, meal name 80, entry name 200, section name 100, unit 30, description / note 500, item name 200; max 50 meals, 300 entries per meal, 50 sections, 300 items per list.
* Items split on commas or new lines; Indian digit grouping (₹12,34,567.89).
* Pagination: real measured text heights; a heading never ends a page; a meal that continues on a new page gets a "(contd.)" heading; long item lists are split into chunks of 12; text is never shrunk. Hiding prices/quantities on the card never changes stored data or totals.

### Templates
The two supplied PNGs (1054 × 1492) are used **unmodified** as backgrounds
(`app/src/main/res/drawable-nodpi/template_purple.png`, `template_green.png`), with the content areas, page-number
positions and text colours taken from the original project. Preview, PNG and PDF all use one rendering function, so they cannot disagree.
* **PNG:** 2108 × 2984 px per page (2×). **PDF:** A4, real vector text on the template artwork.

### Fonts
The three original card fonts (Elegant = Playfair Display, Clean = DM Sans, Traditional = Noto Serif Malayalam)
were not in the supplied files and I could not download them. By default the card uses the phone's built-in serif / sans-serif
fonts (which contain Malayalam). To get the exact original look, drop the TTF files into `app/src/main/assets/fonts/` —
see `app/src/main/assets/fonts/README.txt` for the exact names. No code change needed.

### Launcher icon and logo
* In-app (splash, home, headers): your official full logo (`star_logo.png`, a down-scaled copy of the original; the untouched original is in `design-assets/`).
* Launcher icon: the logo's triangle-and-child mark with its stars, cut from the official logo, on brand purple (adaptive + themed/monochrome layer). The wide "STAR" lettering would be unreadable at icon size, so only the mark is used there.

---

## 3. Project structure

```
app/src/main/java/com/star/menugenerator/
  MainActivity.kt, StarApp.kt
  domain/   Money.kt (calc) · Validation.kt · Api.kt (stored shape + JSON) · Models.kt · Blocks.kt (print blocks + pagination) · Templates.kt
  data/     Database.kt (Room) · MenuRepository.kt · BackupCodec.kt
  render/   MenuRenderer.kt (draws pages) · Exporter.kt (PNG/PDF/share/print) · Fonts.kt
  ui/       AppViewModel.kt · StarRoot.kt · theme/ · components/ · screens/
app/src/test/            JVM unit tests (calc, validation, blocks/pagination, backup)
app/src/androidTest/     Room + repository tests (need a phone/emulator)
design-assets/           original STAR logo
```
`domain/` and `data/BackupCodec.kt` contain no Android classes, which is why they are unit-testable on the PC.

## 4. Backup format
Same format as the web project (`star-menu-backup`, version 1, SHA-256 checksum), so backups from the web version should restore too. Restore validates the whole file *before* touching anything; a damaged or edited file is rejected.
*Merge* keeps your menus, adds new ones and updates menus the backup has a newer copy of. *Replace* deletes all menus first. Either way it is one database transaction: all or nothing.

## 5. Versions used
Android Gradle Plugin 8.7.3 · Gradle 8.9 · Kotlin 2.0.21 (Compose compiler plugin) · KSP 2.0.21-1.0.28 · Compose BOM 2024.10.01 (Material 3 1.3) · Room 2.6.1 · kotlinx.serialization 1.7.3 · compileSdk/targetSdk 35 · minSdk 26 (Android 8.0). Package / application id: `com.star.menugenerator`.

---

## What has and has not been tested

**Actually done / verified**
* Read the original React project's calculation, validation, block-building, pagination, template-geometry and backup code and ported it.
* Ran the original project's own 13 tests (Node): **13/13 pass**.
* Ran the original JavaScript to produce *golden* outputs (formatted currency, validation messages, rounding cases, the exact blocks and page layouts for the long test menu) and checked every hand-written expectation in the Kotlin tests against the JS results.
* Static checks on all Kotlin/Gradle files: brackets balanced, every XML file well-formed, package names match directories, every project symbol is imported, no TODO / stub / placeholder code.
* Template images confirmed byte-identical to the originals; launcher icon layers inspected visually (round mask, 36 px size) in a PC simulation.

**NOT done — no tool was available**
* **The project has not been compiled.** No Gradle sync, no build, no APK. Compile errors are possible.
* **The Kotlin unit tests have not been run** (they are written; the JS equivalents pass).
* **Not run on any phone or emulator:** Room database, saving / reopening / autosave, screens, navigation, keyboard handling, on-device PDF/PNG rendering, text layout and Malayalam shaping, the Downloads save, share sheet and print dialog, the launcher icon on a real launcher, and the splash.
* "Works offline" rests on the app declaring no INTERNET permission and having no network code; it has not been observed in airplane mode.
* Page layout of real text (as opposed to the pagination algorithm) depends on Android's text engine and has not been seen. The first PDF you export is the best check: compare it against the template.

If anything fails to build, run, or looks off, send me the Android Studio error text or a screenshot and I will fix it.
