# Nutrition Tracker — Android app

Native Kotlin + Jetpack Compose app. Data is saved on-device
(SharedPreferences, JSON) and survives app restarts. No AI, no internet
permission, no calendar permission (dates are just internal math).

## Open it

1. Delete any previously-unzipped copy of this project first.
2. Unzip fresh, then Android Studio -> Open -> select the `NutritionTracker` folder.
3. Let Gradle sync fully (check the Build tab at the bottom).
4. Run with ▶, or Build -> Build APK(s). Output:
   `app/build/outputs/apk/debug/app-debug.apk`.

## What's in this version

- **Home**: today's date, equal-weight macro stat cards, and a set of
  "hemisphere" gauges (kcal/protein/carbs/fat vs today's target). No
  weekly chart/list here anymore — that's the Plan tab's job now.
- **Plan is now a real dated log**, not just a weekly template:
  - Browse any past date; future dates are visible (showing what your
    weekly rotation would give) but locked — no editing, no warning
    styling, since they haven't happened.
  - Tapping a meal type opens a dialog: pick a **Recipe** (filtered to
    ones tagged for that meal type) or log **Quick calories** (just a
    kcal number; macros come from your Preferences quick-log split).
  - A **"Repeat every [Weekday]"** switch: on = this becomes the
    standing weekly default for that weekday+meal type; off = it's a
    one-off entry just for that date.
- **Recipes** can be built 3 ways (ingredients / grams / calories+%),
  each recipe can be tagged with which **meal types** it's valid for,
  and ingredient rows use a **×1 / ×1.5 / ×2 servings stepper** instead
  of typing grams.
- **Preferences** (renamed from Notes) is now a menu → sub-screens:
  - *Daily Targets*: kcal/protein/carbs/fat goals **per weekday**
    ("macro rotation" — e.g. higher carbs on training days).
  - *Meal Types*: add/rename/remove the categories used everywhere else.
  - *Quick-Log Macro Split*: the % breakdown used for "Quick calories" entries.
  - *Notes*: free-text rules/substitutions.
- **Excel import** (from Home) now also supports:
  - Tagging imported recipes with meal type(s) via a mapped column.
  - A second recipe mode, **"Built from ingredients"**: a long-format
    sheet (one row per ingredient, grouped by recipe name) instead of
    just total macros — ingredient names are matched against your
    Ingredients sheet/list by name.
- **No macro is visually favored** — kcal/protein/carbs/fat each get
  their own consistent color everywhere (stat cards, chips, gauges),
  no special-cased warnings.

## Where things live

- `data/Models.kt` — Ingredient, Recipe, MealEntry, Preferences, AppData
- `data/Calc.kt` — macro math + date-based rotation/override resolution
- `data/Repository.kt` — JSON persistence
- `data/ImportExcel.kt` — dependency-free .xlsx reader
- `NutritionViewModel.kt` — state + CRUD + `setMealEntry(date, mealType, entry, repeatWeekly)`
- `ui/screens/HomeScreen.kt`, `PlanScreen.kt`, `RecipesScreen.kt`,
  `IngredientsScreen.kt`, `PreferencesScreen.kt`, `ImportScreen.kt`

## Heads-up

- This is a data-model change from the previous version (dated log +
  rotation, instead of a flat weekly plan). There's no migration step —
  if you'd already entered data in the earlier build, it won't carry
  over automatically. Since nothing's been imported yet, this only
  matters going forward.
- minSdk 26 (Android 8+).
