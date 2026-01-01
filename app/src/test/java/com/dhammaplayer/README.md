# Dhamma Player - Unit Test Suite

This directory contains comprehensive unit tests for all major features of the Dhamma Player Android
application.

## Test Infrastructure

### Base Test Class

- **BaseTest.kt**: Provides common setup for all unit tests
    - Configures coroutine test dispatcher for main thread
    - Initializes MockK annotations
    - Provides automatic cleanup after each test
    - Exposes `testScheduler` for advancing time in tests

### Test Dependencies

- **JUnit 6 (Jupiter)**: Modern testing framework with improved assertions and test organization
- **MockK**: Kotlin-first mocking library for creating test doubles
- **Kotlin Coroutines Test**: Testing utilities for coroutines and Flow
- **Turbine**: Testing library for Flow assertions
- **AndroidX Test Core**: Core testing utilities for Android

## Test Structure

### Repository Tests (`data/repository/`)

Tests for data layer repositories that manage business logic and data operations.

#### TracksRepositoryTest.kt

- Track retrieval operations (all, by source, by ID)
- Track refreshing from RSS feeds
- Progress tracking (save, retrieve, mark as finished)
- Unfinished track lookup (all, by source)

#### DownloadRepositoryTest.kt

- Download status checking (is downloaded, file paths)
- Manual download operations (download, save, progress reporting)
- Auto-cache operations (caching, cache limit enforcement)
- Auto-cache to manual conversion
- Download removal
- File system operations

#### ScheduleRepositoryTest.kt

- Schedule CRUD operations (create, read, update, delete)
- Schedule enable/disable functionality
- Alarm scheduling (exact alarms, multi-day schedules)
- Alarm cancellation
- Alarm rescheduling after system events
- Schedule update operations

#### SettingsRepositoryTest.kt

- Theme mode operations (get, save, flow behavior)
- DataStore preferences management

### ViewModel Tests (`viewmodel/`)

Tests for ViewModels that manage UI state and handle user interactions.

#### TracksViewModelTest.kt

- Initialization and default state
- UI state management (tracks, progress, downloads)
- Source filtering (Evening/Morning talks)
- Track refreshing and error handling
- Download operations (start, track progress, remove)
- Track retrieval and audio URI resolution
- Flow combining for reactive UI updates

#### PlayerViewModelTest.kt

- Service connection and MediaController lifecycle
- Track selection (viewing without affecting playback)
- Track playing vs viewing distinction
- Playback control (play, pause, seek, skip)
- Player listener events
- Progress tracking and auto-save
- Album art management
- Auto-caching for streamed tracks
- ViewModel cleanup
- UI state synchronization

#### ScheduleViewModelTest.kt

- Schedule list management
- Adding schedules with default values
- Updating schedule properties
- Deleting schedules
- Enabling/disabling schedules
- Exact alarm permission checking (Android 12+)
- Schedule time formatting
- Day selection (Sunday=0 to Saturday=6)

#### SettingsViewModelTest.kt

- Theme mode management and flow behavior
- Theme mode cycling (AUTO → LIGHT → DARK → AUTO)
- StateFlow behavior and emission patterns

### Service & Remote Tests (`data/remote/`)

Tests for network and RSS parsing services.

#### RssServiceTest.kt

- RSS feed fetching from multiple sources
- RSS XML parsing (title, link, pubDate, audioUrl, description, guid)
- HTML stripping from descriptions
- Malformed XML handling
- Multiple items parsing
- Namespace handling

### Receiver Tests (`receiver/`)

Tests for broadcast receivers that handle system events.

#### ScheduledPlaybackReceiverTest.kt

- Intent handling and action filtering
- Schedule lookup and talk source resolution
- Network availability checking
- Online track selection (prefer local, fallback to stream)
- Offline track selection (downloaded only)
- Playback service starting
- Alarm rescheduling
- Error handling and coroutine management

#### BootCompletedReceiverTest.kt

- Boot intent handling (BOOT_COMPLETED, QUICKBOOT_POWERON)
- Alarm rescheduling after device boot
- Coroutine management on IO dispatcher

### Utility Tests (`util/`)

Tests for utility classes and helper functions.

#### AlbumArtExtractorTest.kt

- Local file extraction (absolute paths, file URIs)
- Cache management (session cache, no re-extraction)
- Remote URL handling (avoid network for non-playing)
- Playing track album art (allow network for active playback)
- Error handling and resource cleanup
- URI format handling (file://, http://, https://)
- MediaMetadataRetriever lifecycle
- Coroutine context (IO dispatcher)

## Running Tests

### Run All Tests

```bash
./gradlew test
```

### Run Tests for Specific Package

```bash
./gradlew test --tests "com.dhammaplayer.data.repository.*"
./gradlew test --tests "com.dhammaplayer.viewmodel.*"
```

### Run Tests for Specific Class

```bash
./gradlew test --tests "com.dhammaplayer.data.repository.TracksRepositoryTest"
```

### Run Tests with Coverage

```bash
./gradlew testDebugUnitTest jacocoTestReport
```

## Test Naming Convention

Tests follow a descriptive naming pattern using nested classes and display names:

- **Outer class**: Component under test (e.g., `TracksRepositoryTest`)
- **Nested classes**: Feature areas (e.g., `Track Retrieval`, `Progress Tracking`)
- **Test methods**: Specific behaviors using backticks for readability (e.g.,
  `` `should return all tracks from database` ``)

## Implementation Guidelines

### When Implementing Tests:

1. **Use MockK for mocking**:
   ```kotlin
   @MockK
   private lateinit var repository: TracksRepository
   ```

2. **Use coroutine test utilities**:
   ```kotlin
   @Test
   fun `should do something`() = runTest {
       // Test code with suspend functions
   }
   ```

3. **Test Flow emissions with Turbine**:
   ```kotlin
   @Test
   fun `should emit values`() = runTest {
       repository.getAllTracks().test {
           assertEquals(expectedTracks, awaitItem())
           awaitComplete()
       }
   }
   ```

4. **Advance time for delayed operations**:
   ```kotlin
   testScheduler.advanceUntilIdle()
   ```

5. **Verify mock interactions**:
   ```kotlin
   verify { repository.saveTracks(any()) }
   ```

## Test Coverage Goals

- **Repositories**: 80%+ coverage of business logic
- **ViewModels**: 80%+ coverage of state management and user interactions
- **Services**: 80%+ coverage of parsing and network logic
- **Receivers**: 70%+ coverage of intent handling
- **Utilities**: 80%+ coverage of extraction and transformation logic

## Notes

- All test stubs are currently marked with `// TODO: Implement test`
- Each stub represents a distinct test case that should be implemented
- Tests should be independent and not rely on execution order
- Use descriptive assertions with meaningful failure messages
- Consider edge cases, error conditions, and happy paths

