# AGENTS.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Build & Run

This is an Android Kotlin project using Gradle. Build and install via:

```
./gradlew assembleDebug
./gradlew installDebug
```

Run tests:

```
./gradlew test              # Unit tests
./gradlew connectedCheck    # Instrumented tests (requires device/emulator)
```

The project is typically built and run from Android Studio.

## Environment Setup

Ditto credentials are loaded from a `.env` file at the project root (gitignored). Required keys:

```
DITTO_APP_ID=<app-id>
DITTO_PLAYGROUND_TOKEN=<token>
DITTO_AUTH_URL=https://<subdomain>.cloud.dittolive.app
DITTO_WEBSOCKET_URL=wss://<subdomain>.cloud.dittolive.app
```

These are injected as `BuildConfig` fields via `app/build.gradle.kts`. If `.env` is missing, the
build falls back to reading the same keys from system environment variables.

## Dependencies

- **Ditto SDK** (`live.ditto:ditto:4.13.1`) — peer-to-peer sync database
- **Ditto Tools** (`live.ditto:ditto-tools-android:SNAPSHOT`) — debug tools viewer. Uses
  `mavenLocal()` for local dev; change version in `libs.versions.toml` for release.
- **Coil** — async image loading for movie posters
- **Timber** — all logging uses Timber (never `android.util.Log`)
- **DataStore Preferences** — persists sync toggle and import-complete flag

## Architecture

### Initialization Sequence (critical)

`MoviesApplication.onCreate()` launches Ditto setup asynchronously on `Dispatchers.IO`. Setup
includes identity creation, transport config, disabling DQL strict mode, and `disableSyncWithV3()`.
A `CompletableDeferred` at `DittoHandler.ready` signals completion.

**Any code that touches `DittoHandler.ditto` must `DittoHandler.ready.await()` first.** The
ViewModel init blocks do this. Skipping this causes DQL errors (`INSERT` requires
`disableSyncWithV3`).

### Data Layer

- **`Movie`** — data class with `fromJson()` for parsing Ditto store results. Fields: `_id`,
  `title`, `year`, `plot`, `genres`, `rated`, `runtime`, `poster`, `directors`, `cast`,
  `imdbRating`, `watched`, `deleted`.
- **`MoviesRepository`** — singleton (`MoviesRepository.instance`). All Ditto CRUD goes through
  here. Uses DQL (Ditto Query Language) for all operations. Key patterns:
    - `observeMovies(showWatched, showDeleted)` returns `Flow<List<Movie>>` via `callbackFlow` +
      `flowOn(Dispatchers.IO)`
    - Dynamic query building via `buildQuery()` — constructs WHERE clauses based on filter flags
    - Soft-delete pattern: `deleted = true` (never hard-deletes)
    - `watched` is a boolean toggled independently from other updates
    - Sync subscription uses a broad unfiltered query so all docs sync regardless of UI filters
    - Seed data import uses `android.util.JsonReader` to stream-parse
      `app/src/main/assets/all_movies.json` (~39MB, ~21K movies). This avoids OOM — never load this
      file into a String.
    - Import is gated by a DataStore `data_imported` flag; runs only on first launch

### UI Layer

Jetpack Compose with Navigation Compose. All screens use `StateFlow` + `collectAsState()` (no
LiveData).

**Navigation routes** (defined in `Root.kt`):

- `movies` → list screen
- `movies/{movieId}` → detail screen (blurred poster background, watched checkbox, overflow menu
  with Edit/Delete)
- `movies/edit` → new movie
- `movies/edit/{movieId}` → edit existing movie
- `toolsviewer` → Ditto Tools debug screen

**Screen → ViewModel patterns:**

- ViewModels use `_loaded` guard flags in `loadMovie()` to prevent re-loading on recomposition
- Screens use `LaunchedEffect(key)` to trigger one-time loads
- Edit screen uses a single `EditMovieState` data class in one `MutableStateFlow`, updated via
  `updateState { copy(...) }`
- List screen filter changes (`showWatched`, `showDeleted`) cancel and restart the observer job with
  a new DQL query

### Ditto Collection Schema

Collection name: `movies`. Documents have this shape:

```
_id: String
title: String
year: Int
plot: String
genres: [String]
rated: String
runtime: Int
poster: String (URL)
directors: [String]
cast: [String]
imdbRating: Double
watched: Boolean
deleted: Boolean
```

All mutations use DQL with parameterized queries (`:param` syntax). Insert uses
`INSERT INTO movies INITIAL DOCUMENTS (:doc)` with a single document map — batch inserts must loop (
Ditto DQL does not accept arrays in DOCUMENTS).
