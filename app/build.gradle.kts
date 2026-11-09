plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.protobuf")
}

android {
    // Resource namespace stays com.android.launcher3 (the AOSP code imports com.android.launcher3.R);
    // the installed package is com.tj.launcher (applicationId below), so it can sit beside the stock launcher.
    namespace = "com.android.launcher3"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tj.launcher"
        // Android 10 (API 29) Go edition is the target device (BRIEF.md). targetSdk is 28 (AOSP value), below the
        // runtime so no newer-Android compatibility behaviour is silently assumed; revisit if
        // the app is ever to run on a newer phone.
        minSdk = 29
        targetSdk = 28   // AOSP Launcher3 Go value; <=28 lets the kill switch toggle Wi-Fi/Bluetooth (BRIEF.md)
        versionCode = 38
        versionName = "0.8.13"
    }

    // The keystore is NOT in git (BRIEF.md). Signing is wired only when the file exists, so
    // tests, lint and debug builds work in any fresh container; a release build without it
    // is refused by tools/ensure-build-env.sh --release and by android.yml's gate, never
    // silently produced as an unsigned APK that looks installable.
    val ks = file("sideload.jks")
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
            // Go edition: small APK and low RAM matter. R8 shrinks code and resources.
            // R8 off for now: a minifier-only crash on the real tablet is a failure class we cannot test here, and 1.3 GB free is plenty for a ~10 MB APK. Revisit with an emulator run on the minified build.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("sideload")?.let { signingConfig = it }
        }
        debug {
            // Same key as release when present, so a debug install can never block a later
            // release install over a certificate mismatch.
            signingConfigs.findByName("sideload")?.let { signingConfig = it }
        }
    }

    sourceSets {
        getByName("main") {
            // AOSP Launcher3 Go flavor: src + src_ui_overrides + go/src, resources res + go/res (go wins), plugin API sources
            java.srcDirs("../launcher3/src", "../launcher3/src_ui_overrides", "../launcher3/go/src",
                "../launcher3/src_plugins", "../launcher3/plugin_core_src",
                "../wallpaperpicker/src")   // AO1b: the AOSP wallpaper picker (android-10.0.0_r9) without "My photos"; see wallpaperpicker/PATCHES.md
            res.srcDirs("../launcher3/res", "../wallpaperpicker/res")
            (this as org.gradle.api.plugins.ExtensionAware).extensions.getByName("proto")
                .let { (it as org.gradle.api.file.SourceDirectorySet).srcDir("../launcher3/protos").srcDir("../launcher3/proto_overrides") }
        }
    }

    // Go resource overrides must OVERLAY the base resources by name (AAPT2 rejects duplicates within one source set),
    // so they live in the build-type source sets, which rank above main.
    sourceSets {
        getByName("debug") { res.srcDirs("../launcher3/go/res") }
        getByName("release") { res.srcDirs("../launcher3/go/res") }
    }

    buildFeatures { buildConfig = true }   // Launcher3 reads BuildConfig.APPLICATION_ID (authority = com.tj.launcher.settings)

    lint {
        abortOnError = false   // AOSP Launcher3 code has many upstream lint findings; our own code is checked by tests (revisit)
        warningsAsErrors = false
        // Pinned toolchain (BRIEF.md): upgrades are deliberate, not lint-driven.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
        // Play Store rule only: this app is sideloaded onto a fixed Android 10 device, never
        // published, and targetSdk 28 is deliberate (BRIEF.md). Remove if that ever changes.
        disable += "ExpiredTargetSdkVersion"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions { unitTests { isReturnDefaultValues = true } }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:3.0.0-alpha-3" }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("javanano") {
                    option("java_package=launcher_log_extension.proto|com.android.launcher3.userevent.nano")
                    option("java_package=launcher_log.proto|com.android.launcher3.userevent.nano")
                    option("java_package=launcher_dump.proto|com.android.launcher3.model.nano")
                    option("enum_style=java")
                }
            }
        }
    }
}

dependencies {
    implementation(project(":iconloaderlib"))
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.preference:preference:1.1.1")
    implementation("com.google.protobuf.nano:protobuf-javanano:3.0.0-alpha-7")
    // Deliberately nothing but the Kotlin stdlib (added by the plugin): a Go launcher must stay
    // small and light on RAM, and the UI toolkit is an OPEN decision (BRIEF.md). Add a
    // dependency only with a reason written in BRIEF.md.
    testImplementation("junit:junit:4.13.2")
}
