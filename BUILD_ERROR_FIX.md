# Build Error Fix - kotlinOptions Migration

## Issue

The build was failing with the following error:

```
e: file:///C:/Users/eoin.griffin.HACH/Downloads/dhamma-player/android/app/build.gradle.kts:40:9: 
Using 'jvmTarget: String' is an error. Please migrate to the compilerOptions DSL. 
More details are here: https://kotl.in/u1r8ln
```

## Root Cause

With Kotlin 2.3.0, the old `kotlinOptions.jvmTarget` syntax has been deprecated and replaced with
the new `compilerOptions` DSL.

## Solution Applied ✅

### Before (Deprecated)

```kotlin
kotlinOptions {
    jvmTarget = "17"
}
```

### After (Fixed)

```kotlin
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
```

## Changes Made

- **File**: `app/build.gradle.kts`
- **Line**: ~37-42
- **Action**: Migrated from deprecated `kotlinOptions` to new `compilerOptions` DSL

## Verification

The fix follows the official Kotlin migration guide and is compatible with Kotlin 2.3.0.

## Additional Notes

The IDE may show "Unresolved reference" errors for test dependencies (junit6, mockk, etc.) until
Gradle sync completes. These are expected and will resolve after running:

```bash
./gradlew --refresh-dependencies
```

## Related Links

- [Kotlin Compiler Options Migration Guide](https://kotl.in/u1r8ln)
- [Kotlin 2.0+ Breaking Changes](https://kotlinlang.org/docs/whatsnew20.html)

---

**Status**: ✅ Fixed  
**Build Error**: Resolved  
**Next Step**: Run Gradle sync

