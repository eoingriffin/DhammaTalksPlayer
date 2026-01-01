# Test File Index

## Directory Structure

```
app/src/test/java/com/dhammaplayer/
├── BaseTest.kt                                          [Base class for all tests]
├── README.md                                            [Test documentation]
│
├── data/
│   ├── remote/
│   │   └── RssServiceTest.kt                           [32 test stubs]
│   │
│   └── repository/
│       ├── DownloadRepositoryTest.kt                   [56 test stubs]
│       ├── ScheduleRepositoryTest.kt                   [47 test stubs]
│       ├── SettingsRepositoryTest.kt                   [9 test stubs]
│       └── TracksRepositoryTest.kt                     [37 test stubs]
│
├── receiver/
│   ├── BootCompletedReceiverTest.kt                    [9 test stubs]
│   └── ScheduledPlaybackReceiverTest.kt                [53 test stubs]
│
├── util/
│   └── AlbumArtExtractorTest.kt                        [46 test stubs]
│
└── viewmodel/
    ├── PlayerViewModelTest.kt                          [81 test stubs]
    ├── ScheduleViewModelTest.kt                        [33 test stubs]
    ├── SettingsViewModelTest.kt                        [13 test stubs]
    └── TracksViewModelTest.kt                          [34 test stubs]
```

## Test Files Overview

### Core Infrastructure

| File        | Purpose                           | Test Count |
|-------------|-----------------------------------|------------|
| BaseTest.kt | Base test class with common setup | N/A        |

### Data Layer Tests

| File                      | Component             | Test Count | Key Features Tested                                         |
|---------------------------|-----------------------|------------|-------------------------------------------------------------|
| TracksRepositoryTest.kt   | Track data operations | 37         | Retrieval, refreshing, progress tracking, unfinished lookup |
| DownloadRepositoryTest.kt | Download management   | 56         | Downloads, auto-cache, conversion, file operations          |
| ScheduleRepositoryTest.kt | Schedule management   | 47         | CRUD, alarms, rescheduling                                  |
| SettingsRepositoryTest.kt | Settings storage      | 9          | Theme mode preferences                                      |
| RssServiceTest.kt         | RSS feed parsing      | 32         | Fetching, XML parsing, error handling                       |

### Presentation Layer Tests

| File                     | Component     | Test Count | Key Features Tested                    |
|--------------------------|---------------|------------|----------------------------------------|
| TracksViewModelTest.kt   | Track list UI | 34         | State management, filtering, downloads |
| PlayerViewModelTest.kt   | Player UI     | 81         | Playback control, state, album art     |
| ScheduleViewModelTest.kt | Schedule UI   | 33         | Schedule management, permissions       |
| SettingsViewModelTest.kt | Settings UI   | 13         | Theme cycling, state flow              |

### System Integration Tests

| File                             | Component          | Test Count | Key Features Tested                              |
|----------------------------------|--------------------|------------|--------------------------------------------------|
| ScheduledPlaybackReceiverTest.kt | Scheduled playback | 53         | Intent handling, track selection, network checks |
| BootCompletedReceiverTest.kt     | Boot handling      | 9          | Alarm rescheduling                               |

### Utility Tests

| File                     | Component            | Test Count | Key Features Tested               |
|--------------------------|----------------------|------------|-----------------------------------|
| AlbumArtExtractorTest.kt | Album art extraction | 46         | Extraction, caching, URI handling |

## Quick Navigation

### By Feature Area

**Track Management**

- TracksRepositoryTest.kt
- TracksViewModelTest.kt
- RssServiceTest.kt

**Audio Playback**

- PlayerViewModelTest.kt
- ScheduledPlaybackReceiverTest.kt
- AlbumArtExtractorTest.kt

**Downloads**

- DownloadRepositoryTest.kt
- TracksViewModelTest.kt (download operations)

**Scheduling**

- ScheduleRepositoryTest.kt
- ScheduleViewModelTest.kt
- ScheduledPlaybackReceiverTest.kt
- BootCompletedReceiverTest.kt

**Settings**

- SettingsRepositoryTest.kt
- SettingsViewModelTest.kt

### By Complexity

**Simple (Good Starting Point)**

1. SettingsRepositoryTest.kt - 9 tests
2. BootCompletedReceiverTest.kt - 9 tests
3. SettingsViewModelTest.kt - 13 tests

**Medium Complexity**

1. RssServiceTest.kt - 32 tests
2. ScheduleViewModelTest.kt - 33 tests
3. TracksViewModelTest.kt - 34 tests
4. TracksRepositoryTest.kt - 37 tests
5. AlbumArtExtractorTest.kt - 46 tests

**Complex (Advanced)**

1. ScheduleRepositoryTest.kt - 47 tests
2. ScheduledPlaybackReceiverTest.kt - 53 tests
3. DownloadRepositoryTest.kt - 56 tests
4. PlayerViewModelTest.kt - 81 tests

## Test Naming Patterns

All tests follow this structure:

```kotlin
@DisplayName("ComponentName")
class ComponentNameTest : BaseTest() {

    @Nested
    @DisplayName("Feature Area")
    inner class FeatureArea {

        @Test
        fun `should describe expected behavior`() {
            // TODO: Implement test
        }
    }
}
```

## Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.dhammaplayer.data.repository.TracksRepositoryTest"

# Run specific nested class
./gradlew test --tests "com.dhammaplayer.data.repository.TracksRepositoryTest\$TrackRetrieval"

# Run specific test method
./gradlew test --tests "com.dhammaplayer.data.repository.TracksRepositoryTest.should return all tracks from database"

# Run tests with logging
./gradlew test --info

# Run tests and generate report
./gradlew test
# Report will be at: app/build/reports/tests/testDebugUnitTest/index.html
```

## Implementation Priority Suggestions

1. **Phase 1 - Foundation** (Start here)
    - SettingsRepositoryTest.kt
    - SettingsViewModelTest.kt
    - RssServiceTest.kt

2. **Phase 2 - Core Features**
    - TracksRepositoryTest.kt
    - TracksViewModelTest.kt
    - AlbumArtExtractorTest.kt

3. **Phase 3 - Advanced Features**
    - DownloadRepositoryTest.kt
    - PlayerViewModelTest.kt
    - ScheduleRepositoryTest.kt

4. **Phase 4 - System Integration**
    - ScheduleViewModelTest.kt
    - ScheduledPlaybackReceiverTest.kt
    - BootCompletedReceiverTest.kt

## Coverage Goals by Component

| Component    | Target Coverage | Priority |
|--------------|-----------------|----------|
| Repositories | 85%+            | High     |
| ViewModels   | 80%+            | High     |
| RSS Service  | 85%+            | High     |
| Receivers    | 70%+            | Medium   |
| Utilities    | 80%+            | Medium   |

---

**Total Test Stubs: 449**
**Files Created: 13** (12 test files + 1 base class)
**Lines of Test Code: ~3,500** (stubs only, will grow with implementation)

