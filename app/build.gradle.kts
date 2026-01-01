plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dhammaplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dhammaplayer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildToolsVersion = "36.0.0"
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Image loading
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // ----------------------------
    // Unit Testing (Jupiter / "JUnit 6")
    // ----------------------------
    // Align all JUnit Jupiter + JUnit Platform versions
    testImplementation(platform(libs.junit.bom))

    // API + params
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)

    // Engines + launcher (launcher is the critical fix for your error)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Mocking / Coroutines / Turbine
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.android) // ok for JVM tests if you rely on it; otherwise you can remove it
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)

    // AndroidX Test Core is sometimes useful even in local JVM tests
    testImplementation(libs.androidx.test.core)

    // ----------------------------
    // Instrumented (device/emulator) tests (src/androidTest/...)
    // ----------------------------
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
}
