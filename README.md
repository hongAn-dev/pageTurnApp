# PageTurn Android App

PageTurn is a multi-module Android reading app for eBook and PDF content. It uses Jetpack Compose for UI, Room for offline storage, Kotlin Coroutines and Flow for async state, Hilt for dependency injection, Retrofit and OkHttp for backend communication, DataStore for preferences, and WorkManager for background sync and reminders.

This README documents the Android app in `pageTurnApp/`. The backend API lives in the sibling `../pageturn-api/` repository.

## Project Structure

```text
pageTurnApp/
├── app/                         # Android application entry point and navigation host
│   └── src/main/java/com/pageturn/app/
│       ├── MainActivity.kt       # Compose root, Navigation, theme setup
│       └── PageTurnApplication.kt# Hilt application class
├── core/
│   ├── model/                    # Pure Kotlin domain models: Book, Chapter, Bookmark, Highlight
│   ├── domain/                   # Repository contracts and use cases
│   ├── data/                     # Repository implementations, sync orchestration, workers
│   ├── database/                 # Room database, entities, DAOs
│   ├── network/                  # Retrofit services, OkHttp client, API adapters
│   ├── common/                   # DataStore preferences, notifications, TTS, dispatchers
│   └── designsystem/             # Compose theme, color, typography
├── feature/
│   ├── library/                  # Auth, home, discover, library, archive, settings UI
│   └── reader/                   # Reader UI, pagination, bookmarks, highlights, TTS controls
├── gradle/libs.versions.toml     # Central dependency and plugin versions
├── settings.gradle.kts           # Module registration
├── build.gradle.kts              # Root Gradle plugin declarations
└── local.properties              # Local Android SDK path, not for version control
```

## Module Responsibilities

| Module | Responsibility |
| --- | --- |
| `app` | Wires the app shell, Hilt application, Compose theme, and navigation between auth, library, and reader screens. |
| `core:model` | Contains platform-light data models used across the app. |
| `core:domain` | Defines business-facing repository interfaces and use cases such as recent reads, chapter loading, progress updates, bookmarks, and highlights. |
| `core:data` | Implements repositories, local file handling, EPUB extraction, cloud sync, and WorkManager sync tasks. |
| `core:database` | Provides Room database `PageTurnDatabase` with `books`, `bookmarks`, and `highlights` tables. Current schema version: `3`. |
| `core:network` | Provides Retrofit services for authentication, store/library APIs, and reading sync. |
| `core:common` | Shared app services: DataStore preferences, notification scheduling, TTS helper, and coroutine dispatchers. |
| `core:designsystem` | Shared Compose theme, typography, and color definitions. |
| `feature:library` | User-facing library flows: login/register, discover store, local library, collections, highlights, settings. |
| `feature:reader` | Reader experience: text/PDF reading, pagination, font/theme settings, bookmarks, highlights, notes, and TTS. |

## Infrastructure

### Android Runtime

- Application ID: `com.pageturn.app`
- Minimum SDK: `26`
- Target SDK: `34`
- Compile SDK: `34`
- Kotlin JVM target: `17`
- Compose compiler extension: `1.5.14`
- Dependency versions are centralized in `gradle/libs.versions.toml`.

### Local Storage

- Room database: `PageTurnDatabase`
- Tables: `books`, `bookmarks`, `highlights`
- Downloaded book files are stored under Android app internal storage, usually `context.filesDir/<bookId>/`.
- Supported local file names include `book.epub`, `book.pdf`, and cached `book_content.txt` for extracted EPUB text.

### Preferences and Settings

- User preferences use Jetpack DataStore in `core:common`.
- Stored settings include font size, font family, reading theme, sync settings, notification settings, profile data, auth tokens, and custom collections.

### Backend API

The Android client currently points to:

```text
https://pageturn.ddns.net/
```

The base URL is defined in:

```text
core/network/src/main/java/com/pageturn/core/network/di/NetworkModule.kt
```

For a local backend from an Android emulator, change the base URL to:

```kotlin
private const val BASE_URL = "http://10.0.2.2:8080/"
```

For a physical device, use the backend machine LAN IP instead, for example `http://192.168.1.10:8080/`.

### Backend Services

The sibling backend repository is `../pageturn-api/`. It provides authentication, cloud library storage, public store APIs, reading progress sync, bookmarks, highlights, collections, transfers, and cleanup jobs.

The backend Docker Compose stack includes:

- `pageturn-db`: PostgreSQL 16
- `pageturn-app`: Spring Boot API container
- Volumes for database data, uploads, and public files

## Guide to Run

### 1. Prerequisites

Install or verify:

- Android Studio with Android SDK Platform 34
- Android SDK Build Tools 34.0.0
- JDK 17 recommended, or a JDK supported by the local Gradle/Android plugin setup
- Android emulator or physical Android device
- Docker, only if running the backend locally

Check Java:

```bash
java -version
```

If `JAVA_HOME` is set, it must point to the JDK directory, not the `java` binary. Example:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

### 2. Configure Android SDK Path

Create or verify `local.properties`:

```properties
sdk.dir=/home/duongduy/Android/Sdk
```

Use your own SDK path on another machine.

### 3. Run the Backend Locally (Optional)

From the backend repo:

```bash
cd ../pageturn-api
cp .env.example .env   # if an example env file exists
# edit DB_USERNAME, DB_PASSWORD, DB_NAME, DB_PORT, APP_PORT, JWT_SECRET, storage values
docker compose up -d
```

If running against this local backend, update `NetworkModule.kt` in the Android app as described above.

### 4. Build the Android App

From `pageTurnApp/`:

```bash
bash ./gradlew assembleDebug
```

If the wrapper has execute permission, this also works:

```bash
./gradlew assembleDebug
```

### 5. Run on an Emulator or Device

Using Gradle:

```bash
bash ./gradlew installDebug
```

Then open `PageTurn` on the device.

Using Android Studio:

1. Open `pageTurnApp/` as the project root.
2. Let Gradle sync finish.
3. Select the `app` run configuration.
4. Choose an emulator or connected device.
5. Press Run.

### 6. Run Tests

Run all unit tests:

```bash
bash ./gradlew test
```

Run one module test task:

```bash
bash ./gradlew :core:data:testDebugUnitTest
```

Run instrumentation tests on a connected device or emulator:

```bash
bash ./gradlew connectedDebugAndroidTest
```

## Development Notes

- Keep dependencies declared through `gradle/libs.versions.toml`.
- Keep domain contracts in `core:domain`; implementations belong in `core:data`.
- Keep Android-specific database code in `core:database` and Retrofit code in `core:network`.
- UI state should flow from repository/use case to ViewModel to Compose through `Flow` or `StateFlow`.
- Do not commit secrets, local SDK paths, keystores, or machine-specific config.
- If using a local backend, avoid leaving emulator-only URLs committed unless the team agrees.
