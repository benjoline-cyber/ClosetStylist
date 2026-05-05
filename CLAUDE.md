# Closet Stylist — Project Guide

This is a single-user Android app that helps me pick outfits from clothes I own,
based on weather and what I haven't worn recently. Built solo, for personal use first.

## Stack

- Kotlin + Jetpack Compose (Material 3)
- Single-activity architecture; navigation via `androidx.navigation:navigation-compose`
- Room for local DB
- ViewModel + `StateFlow` for state; no LiveData
- Coil for image loading
- Retrofit + kotlinx.serialization for networking (Claude API + OpenWeatherMap)
- EncryptedSharedPreferences for API keys
- Coroutines + `viewModelScope` for async work
- Min SDK 26, Target SDK 34

## Architecture

```
ui/        Composable screens, one folder per tab (closet/, inspiration/, suggest/, settings/)
data/      Room entities, DAOs, database, repositories
network/   Retrofit interfaces + DTOs for Claude API and OpenWeatherMap
domain/    Pure-Kotlin logic (outfit filtering, prompt building) — no Android imports
di/        Manual dependency injection via a single AppContainer object (no Hilt)
util/      Image compression, location helpers, date utils
```

Pure logic in `domain/` must be unit-testable without Robolectric. Anything that touches
Android (Context, Bitmap, Location) lives in `data/`, `network/`, or `util/`.

## Data model (Room, current version: 2)

- `ClothingItem`: id (UUID string), imagePath (String, file:// in app's internal storage),
  category (enum: TOP, BOTTOM, DRESS, OUTERWEAR, SHOES, ACCESSORY), colorTags (List<String>),
  seasonTags (List<Season>), description (String, AI-generated once on add), lastWornDate (LocalDate?, nullable)
- `InspirationPhoto`: id, imagePath, addedAt (Instant)
- `WearLog`: id, itemId, wornOn (LocalDate) — separate table so we can show history later
- `OutfitFeedback`: id, itemIds (comma-joined sorted item IDs), feedbackType (WORE/DISMISSED/REJECTED_COMBO),
  createdAt (epoch millis), persona (StylistPersona.name) — added in migration 1→2

Store images as files in `context.filesDir/closet/` and `context.filesDir/inspiration/`.
Never store image bytes in the DB.

## How outfit suggestions work (the important flow)

1. User taps "Suggest 3 Outfits" on the Suggest screen.
2. Fetch 12-hour forecast via OpenWeatherMap (`data/2.5/forecast`, cnt=4) using `FusedLocationProvider`
   (request permission first). Computes current temp, 12-hour min/max, and first rain slot.
3. Query DB: all `ClothingItem`s where `lastWornDate` is null OR older than 3 days.
4. Load up to 10 recent REJECTED_COMBO feedbacks for the active persona from `OutfitFeedback`.
5. Build a text-only prompt for Claude: item descriptions, weather (including layering/rain notes
   when swing > 8°C or rain expected), active stylist persona, and rejected combos to avoid.
   Attach 2–3 inspiration photos as base64 images. Do NOT send closet photos — descriptions only.
   This keeps each suggestion under ~$0.02.
6. Ask Claude to return JSON: `{ outfits: [{ itemIds: [...], rationale: "..." }, ...] }` (3 outfits).
7. Parse, validate (every id must exist, no duplicate items within an outfit), display as 3 cards.
8. Each card has 3 actions:
   - **Wore it**: records today's date to `lastWornDate` + `WearLog`; card collapses
   - **Not today**: dismisses card only; no DB write
   - **Never this combo**: saves REJECTED_COMBO to `OutfitFeedback`; card collapses
   All 3 show a Snackbar with Undo. Undo reverses the DB write and restores the card.

If Claude returns invalid JSON, retry once with a stricter "respond with ONLY JSON" reminder,
then show a friendly error. Never crash on bad model output.

## Stylist personas

`StylistPersona` enum lives in `domain/` (pure Kotlin). Current values: MINIMALIST, CLEAN_GIRL,
INDIE, PREP, STREETWEAR, ROMANTIC, EDITORIAL. Selected persona is persisted in
`EncryptedSharedPreferences` under key `stylist_persona` (default: MINIMALIST). The Suggest screen
shows a horizontal chip row to switch personas; the active persona's `description` is injected
into the Claude prompt and used to scope REJECTED_COMBO history.

## Weather strip

A tappable strip above the persona picker shows the fetched weather summary (current temp,
12-hour range, rain time if any). Tapping re-runs the full suggestion flow. The summary is
stored in a separate `weatherSummary: StateFlow<String>` in `SuggestViewModel` so it persists
across reloads without clearing mid-flight.

## Item descriptions (one-time, on add)

When the user adds a clothing item, send the photo to Claude with a prompt like:
"Describe this clothing item in one sentence covering type, color, pattern, and material if visible.
No preamble." Save to `description`. This runs once per item, not per suggestion.

## API key storage

`SettingsRepository` wraps `EncryptedSharedPreferences`. Keys: `claude_api_key`, `weather_api_key`.
Never log keys. Never include them in error messages shown to the user.

## Conventions

- Composables: PascalCase, one screen per file, `XxxScreen.kt` + `XxxViewModel.kt`
- One Composable per file for screens; small reusable Composables can share a file
- No `!!` operator. Use `?:` with sensible defaults or fail loudly with `error("...")`
- All user-facing strings in `res/values/strings.xml` from day one (don't hardcode)
- Errors shown to the user are short and actionable ("No internet — try again") — never raw exceptions
- Format: `ktlint` defaults

## Testing

- Unit tests for everything in `domain/` (outfit filter, prompt builder, JSON parser)
- One instrumented test per DAO confirming insert/query/update works
- Skip UI tests for v1 — manual testing on the emulator is fine

## Build order (work top-down, don't skip)

1. Settings screen + EncryptedSharedPreferences (need keys before anything else works)
2. Room setup: entities, DAOs, database, AppContainer wiring
3. Closet tab: add item (camera/gallery → save image → Claude describes it → save to DB), list view, filters
4. Inspiration tab: add photo, gallery view, delete
5. Suggest tab: location + weather, prompt builder, Claude call, JSON parse, 3-card display, "wore this" action
6. Polish: loading states, empty states, error toasts, app icon

## Things to ask me before doing

- Adding any new third-party dependency
- Changing the data model after step 2
- Anything that costs API money in a loop (e.g., re-describing all items)

## Things you can do without asking

- Refactor within a file
- Add tests
- Improve error messages
- Suggest UI tweaks (but wait for me to approve before applying)
