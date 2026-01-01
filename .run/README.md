# JetBrains Run Configurations

This directory contains pre-configured run configurations for Android Studio and IntelliJ IDEA.

## Available Configurations

### 1. All Unit Tests

**File**: `All_Unit_Tests.xml`

Runs all unit tests in the project using the `testDebugUnitTest` Gradle task.

- Uses `--continue` flag to run all tests even if some fail
- Generates test reports in `app/build/reports/tests/testDebugUnitTest/`

**Usage**:

- Select "All Unit Tests" from the run configuration dropdown
- Click the Run button (▶️) or press Shift+F10

### 2. All Tests with Coverage

**File**: `All_Tests_with_Coverage.xml`

Runs all unit tests and generates a code coverage report using JaCoCo.

- Executes both `testDebugUnitTest` and `jacocoTestReport` tasks
- Coverage report available at `app/build/reports/jacoco/jacocoTestReport/html/index.html`

**Usage**:

- Select "All Tests with Coverage" from the dropdown
- Run the configuration
- Open the coverage report in your browser

### 3. Implemented Tests Only (147 tests)

**File**: `Implemented_Tests_Only.xml`

Runs only the fully implemented test suites:

- SettingsRepositoryTest (9 tests)
- BootCompletedReceiverTest (9 tests)
- SettingsViewModelTest (13 tests)
- TracksRepositoryTest (37 tests)
- RssServiceTest (32 tests)
- ScheduleRepositoryTest (47 tests)

**Total**: 147 implemented tests

**Usage**:

- Ideal for CI/CD pipelines
- Quick verification of completed tests
- Avoids running TODO stubs

### 4. Compile Tests

**File**: `Compile_Tests.xml`

Compiles all test sources without running them.

- Useful for checking compilation errors quickly
- Faster than running full test suite
- Executes `compileDebugUnitTestKotlin` task

**Usage**:

- Quick syntax and type checking
- Verify imports and dependencies
- Faster feedback loop during development

## How to Use

### In Android Studio / IntelliJ IDEA:

1. **Automatic Loading**: These configurations are automatically loaded when you open the project
2. **Run Configuration Dropdown**: Find them in the top toolbar next to the Run button
3. **Run/Debug**: Click the Run (▶️) or Debug (🐛) button
4. **Keyboard Shortcuts**:
    - Run: Shift+F10 (Windows/Linux) or Ctrl+R (Mac)
    - Debug: Shift+F9 (Windows/Linux) or Ctrl+D (Mac)

### From Command Line:

You can also run these tasks directly using Gradle:

```bash
# All unit tests
./gradlew :app:testDebugUnitTest --continue

# With coverage
./gradlew :app:testDebugUnitTest :app:jacocoTestReport

# Compile only
./gradlew :app:compileDebugUnitTestKotlin

# Specific test classes
./gradlew :app:testDebugUnitTest --tests "com.dhammaplayer.data.repository.TracksRepositoryTest"
```

## Test Reports

After running tests, reports are generated at:

- **Test Results**: `app/build/reports/tests/testDebugUnitTest/index.html`
- **Coverage Report**: `app/build/reports/jacoco/jacocoTestReport/html/index.html`

## Customization

To create your own run configuration:

1. Run → Edit Configurations
2. Click the + button
3. Select "Gradle"
4. Configure the tasks and parameters
5. Save to `.run/` directory for sharing with the team

## Notes

- These configurations are tracked in Git for team consistency
- The `.run/` directory is the modern approach (replaces `.idea/runConfigurations/`)
- Configurations are project-specific and IDE-version independent
- The `--continue` flag ensures all tests run even if some fail

## Test Status

As of the latest update:

- **Total Test Stubs**: 449
- **Implemented Tests**: 147 (33%)
- **Remaining**: 302 (67%)

See `TEST_IMPLEMENTATION_FINAL_REPORT.md` for detailed status.

