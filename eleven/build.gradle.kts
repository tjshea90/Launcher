plugins {
    id("com.android.application")
}

// Eleven, the LineageOS music app, vendored from branch lineage-17.1 (see UPSTREAM.md; TASKS BV1; plan in research/ELEVEN_PORT_2026-11-11.md).
// Its own APK (package org.lineageos.eleven, so it shows up in the custom drawer like any app), signed with the launcher's key and attached to the same
// Release as the Launcher APK. Look and function are upstream's, EXCEPT that it never loads or shows album art (CHANGES.md; guard test in src/test).
android {
    namespace = "org.lineageos.eleven"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.lineageos.eleven"
        minSdk = 28        // upstream's value (Android 9); the tablet runs Android 10
        targetSdk = 30     // upstream's value; on the tablet (API 29) it behaves exactly like 29
        versionCode = 401  // upstream's; raise it whenever this module changes, or the tablet refuses the update
        versionName = "4.0.1-launcher1"
    }

    // Same key as the launcher and the phone guide (BRIEF.md): wired only when the keystore exists, so tests and lint work in any container.
    val ks = file("../app/sideload.jks")
    if (ks.exists()) {
        signingConfigs.create("sideload") {
            storeFile = ks
            storePassword = "launcher"
            keyAlias = "launcher"
            keyPassword = "launcher"
        }
    }

    buildTypes {
        release {
            // R8 off, like the launcher: a minifier-only crash on the real tablet cannot be tested here.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard.cfg")
            signingConfigs.findByName("sideload")?.let { signingConfig = it }
        }
        debug {
            // Upstream's debug build adds the suffix ".dev"; here debug and release are the SAME package (org.lineageos.eleven), so the emulator tests the real app.
            signingConfigs.findByName("sideload")?.let { signingConfig = it }
        }
    }

    buildFeatures {
        aidl = true          // IElevenService.aidl, MusicPlaybackTrack.aidl (src/main/aidl)
        buildConfig = true   // upstream's Gradle build generated BuildConfig by default; AGP 8 does not
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += setOf("META-INF/LICENSE.txt", "META-INF/LICENSE-FIREBASE.txt", "META-INF/NOTICE.txt", ".readme")
        }
    }

    lint {
        abortOnError = false
        warningsAsErrors = false
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "ExpiredTargetSdkVersion", "OldTargetApi")
        disable += setOf("MissingTranslation", "ExtraTranslation")   // as upstream
    }

    testOptions { unitTests { isReturnDefaultValues = true } }
}

dependencies {
    // Upstream's dependency list, versions unchanged (look and function "exactly as coded").
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.preference:preference:1.1.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.2")
    implementation("com.google.android.material:material:1.4.0")
    testImplementation("junit:junit:4.13.2")
}
