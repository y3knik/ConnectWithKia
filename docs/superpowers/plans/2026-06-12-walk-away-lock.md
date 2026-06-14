# Walk-Away Lock Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android app that locks a Kia EV9 about five minutes after Android Auto disconnects, with cancel-on-reconnect, configurable settings, and a CI/release pipeline.

**Architecture:** Two-module Gradle project. `:kia` is pure-Kotlin/JVM (Retrofit-based client for `kiaconnect.ca`) and `:app` is the Android UI + foreground service + scheduler. State machine lives in `:app` and uses `AlarmManager.setExactAndAllowWhileIdle` for the five-minute delay. Credentials persist in `EncryptedSharedPreferences`. CarConnection LiveData (androidx.car.app) detects Android Auto state.

**Tech Stack:** Kotlin 2.0.21, AGP 8.7.0, Jetpack Compose (Material 3, BOM 2024.10.01), Retrofit 2.11.0, OkHttp 4.12.0, kotlinx.serialization 1.7.3, androidx.security:security-crypto 1.1.0-alpha06, androidx.car.app:app 1.4.0, MockWebServer 4.12.0, Turbine 1.1.0, Robolectric 4.14, JUnit 4.13.2, ktlint plugin 12.1.1, detekt 1.23.7. Min SDK 29, target SDK 34, compile SDK 34.

**Spec:** `docs/superpowers/specs/2026-06-12-connectwithkia-design.md`

---

## Conventions used throughout

- **Package root:** `com.github.y3knik.connectwithkia`
- **Sub-packages:** `.kia` (in `:kia` module), `.data`, `.detect`, `.scheduler`, `.service`, `.ui.status`, `.ui.credentials`, `.ui.settings`, `.ui.theme` (all in `:app`)
- **All Kotlin source under `src/main/kotlin/com/github/y3knik/connectwithkia/...`** (not `src/main/java/`)
- **TDD discipline:** write the failing test first, see it fail, write minimal implementation, see it pass, then commit. The phrase "TDD" in this plan always means this loop.
- **Commit per task** unless a task explicitly chains. Use Conventional Commits (`feat:`, `chore:`, `test:`, `docs:`, `ci:`).
- **Pure-JVM tests** (no Android deps): use plain JUnit 4 + MockWebServer. Fast.
- **Android-dependent tests**: use Robolectric where reasonable; otherwise mark as a manual smoke step.
- **Branch:** all work happens on `feat/walk-away-lock` off `main`. Open one PR at the end (or at logical phase boundaries if you prefer review breakpoints).

Before starting Phase 0, create the working branch:

```bash
git checkout main
git pull
git checkout -b feat/walk-away-lock
```

---

## Phase 0 — Housekeeping (Task 1)

### Task 1: Fix README typo and add `.gitignore`

**Files:**
- Modify: `README.md`
- Create: `.gitignore`

- [ ] **Step 1: Replace README contents**

Overwrite `README.md` with:

```markdown
# ConnectWithKia

Android app that locks a Kia EV9 about five minutes after Android Auto disconnects from it. Built for personal use in Canada (Kia Connect Canada / `kiaconnect.ca`).

- **Trigger:** Android Auto disconnect from the car (wired or wireless)
- **Delay:** 5 minutes (1–15 min, configurable in Settings)
- **Cancel:** Android Auto reconnects within the window, or you tap **Cancel** on the countdown notification
- **Lock:** sent via Kia Connect Canada — your email, password, and 4-digit vehicle PIN are stored encrypted on-device

This is a personal-use sideload. Not affiliated with Kia.

## Design and plan

- Design: [`docs/superpowers/specs/2026-06-12-connectwithkia-design.md`](docs/superpowers/specs/2026-06-12-connectwithkia-design.md)
- Implementation plan: [`docs/superpowers/plans/2026-06-12-walk-away-lock.md`](docs/superpowers/plans/2026-06-12-walk-away-lock.md)

## Building

Requires JDK 17 and the Android SDK.

```bash
./gradlew :app:assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`.

## License

MIT — see [`LICENSE`](LICENSE).
```

- [ ] **Step 2: Create `.gitignore`**

Create `.gitignore` with:

```
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar
local.properties

# Android Studio / IntelliJ
.idea/
*.iml
captures/
.cxx/

# Keystores and signing
*.jks
*.keystore
keystore.properties

# OS junk
.DS_Store
Thumbs.db

# Editor / agent
.claude/
.vscode/
```

- [ ] **Step 3: Commit**

```bash
git add README.md .gitignore
git commit -m "chore: fix readme typo and add gitignore"
```

---

## Phase 1 — Gradle scaffold (Tasks 2–4)

### Task 2: Add Gradle wrapper and root build files

**Files:**
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradle/wrapper/gradle-wrapper.jar` (binary, via `gradle wrapper`)
- Create: `gradlew`, `gradlew.bat`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (root)
- Create: `gradle/libs.versions.toml`
- Create: `gradle.properties`

- [ ] **Step 1: Generate the wrapper**

Requires Gradle 8.10 installed locally (sdkman, scoop, or chocolatey). Run:

```bash
gradle wrapper --gradle-version 8.10 --distribution-type bin
```

This creates `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, and `gradle/wrapper/gradle-wrapper.properties`.

- [ ] **Step 2: Create `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ConnectWithKia"
include(":app", ":kia")
```

- [ ] **Step 3: Create `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.7.0"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.25"
serialization = "1.7.3"
coroutines = "1.9.0"
composeBom = "2024.10.01"
activityCompose = "1.9.3"
lifecycle = "2.8.6"
navigationCompose = "2.8.3"
carApp = "1.4.0"
securityCrypto = "1.1.0-alpha06"
datastore = "1.1.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
retrofitSerialization = "1.0.0"
junit = "4.13.2"
mockwebserver = "4.12.0"
turbine = "1.1.0"
robolectric = "4.14"
androidxTestExt = "1.2.1"
androidxTestCore = "1.6.1"
ktlint = "12.1.1"
detekt = "1.23.7"

[libraries]
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }

androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.13.1" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-car-app = { group = "androidx.car.app", name = "app", version.ref = "carApp" }
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }

retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.jakewharton.retrofit", name = "retrofit2-kotlinx-serialization-converter", version.ref = "retrofitSerialization" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }

junit = { group = "junit", name = "junit", version.ref = "junit" }
mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "mockwebserver" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestExt" }
androidx-test-core = { group = "androidx.test", name = "core", version.ref = "androidxTestCore" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint" }
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
```

- [ ] **Step 4: Create root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply true
    alias(libs.plugins.detekt) apply true
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
    }
}
```

- [ ] **Step 5: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
kotlin.code.style=official
```

- [ ] **Step 6: Create `config/detekt/detekt.yml`**

Run:

```bash
mkdir -p config/detekt
./gradlew detektGenerateConfig
```

This generates a default config at `config/detekt/detekt.yml`. Edit it to disable `MagicNumber` (too noisy for UI code):

In `config/detekt/detekt.yml`, find the `MagicNumber:` block under `style:` and set `active: false`.

- [ ] **Step 7: Verify the build resolves**

Run:

```bash
./gradlew help
```

Expected: build succeeds. No tasks fail.

- [ ] **Step 8: Commit**

```bash
git add gradle/ gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties config/
git commit -m "chore: add gradle wrapper, version catalog, root build"
```

---

### Task 3: Create `:kia` module skeleton

**Files:**
- Create: `kia/build.gradle.kts`
- Create: `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/.gitkeep`
- Create: `kia/src/test/kotlin/com/github/y3knik/connectwithkia/kia/.gitkeep`

- [ ] **Step 1: Create `kia/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 2: Create package directories**

```bash
mkdir -p kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/internal
mkdir -p kia/src/test/kotlin/com/github/y3knik/connectwithkia/kia
mkdir -p kia/src/test/resources/fixtures
touch kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/.gitkeep
touch kia/src/test/kotlin/com/github/y3knik/connectwithkia/kia/.gitkeep
```

- [ ] **Step 3: Verify build resolves**

```bash
./gradlew :kia:build
```

Expected: PASS (nothing to compile yet, but no errors).

- [ ] **Step 4: Commit**

```bash
git add kia/
git commit -m "chore(kia): add kia module skeleton"
```

---

### Task 4: Create `:app` module skeleton

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/MainActivity.kt`
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/ConnectWithKiaApp.kt`
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/theme/Theme.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/xml/data_extraction_rules.xml`
- Create: `app/src/main/res/xml/backup_rules.xml`
- Create: `app/proguard-rules.pro`

- [ ] **Step 1: Create `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.github.y3knik.connectwithkia"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.github.y3knik.connectwithkia"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets["main"].kotlin.srcDirs("src/main/kotlin")
    sourceSets["test"].kotlin.srcDirs("src/test/kotlin")

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "NETWORK_LOGS", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("boolean", "NETWORK_LOGS", "false")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":kia"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.car.app)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockwebserver)
}
```

- [ ] **Step 2: Create `AndroidManifest.xml`**

`app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

    <application
        android:name=".ConnectWithKiaApp"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:theme="@style/Theme.ConnectWithKia"
        tools:targetApi="34">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.ConnectWithKia">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Foreground service + receivers wired in later tasks -->

    </application>
</manifest>
```

- [ ] **Step 3: Create `ConnectWithKiaApp.kt`**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/ConnectWithKiaApp.kt`:

```kotlin
package com.github.y3knik.connectwithkia

import android.app.Application

class ConnectWithKiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
```

- [ ] **Step 4: Create `MainActivity.kt`**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/MainActivity.kt`:

```kotlin
package com.github.y3knik.connectwithkia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.github.y3knik.connectwithkia.ui.theme.ConnectWithKiaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConnectWithKiaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text("ConnectWithKia")
                }
            }
        }
    }
}
```

- [ ] **Step 5: Create theme**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/theme/Theme.kt`:

```kotlin
package com.github.y3knik.connectwithkia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun ConnectWithKiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        darkTheme -> dynamicDarkColorScheme(context)
        else -> dynamicLightColorScheme(context)
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
```

- [ ] **Step 6: Create resource files**

`app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">ConnectWithKia</string>
</resources>
```

`app/src/main/res/values/themes.xml`:

```xml
<resources>
    <style name="Theme.ConnectWithKia" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

`app/src/main/res/xml/data_extraction_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="sharedpref" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="sharedpref" />
    </device-transfer>
</data-extraction-rules>
```

`app/src/main/res/xml/backup_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <exclude domain="sharedpref" />
</full-backup-content>
```

`app/proguard-rules.pro`:

```
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-keepclasseswithmembers,allowshrinking,allowobfuscation class **$$serializer { *; }
-keep,includedescriptorclasses class com.github.y3knik.connectwithkia.**$$serializer { *; }
-keepclasseswithmembers class com.github.y3knik.connectwithkia.** {
    kotlinx.serialization.KSerializer serializer(...);
}
```

Create the launcher icons placeholder: use the default Android Studio template, or copy `app/src/main/res/mipmap-*` from any `gradle init` Android project. If you don't have one handy, generate by running:

```bash
mkdir -p app/src/main/res/mipmap-anydpi-v26
```

Create `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@android:color/system_neutral2_500" />
    <foreground android:drawable="@android:color/system_accent1_200" />
</adaptive-icon>
```

And `mipmap-anydpi-v26/ic_launcher_round.xml` with identical content.

- [ ] **Step 7: Build debug APK**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL. APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 8: Commit**

```bash
git add app/
git commit -m "chore(app): add android app module skeleton"
```

---

## Phase 2 — CI (Task 5)

### Task 5: GitHub Actions CI workflow

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Create CI workflow**

`.github/workflows/ci.yml`:

```yaml
name: ci

on:
  pull_request:
  push:
    branches: [main]

permissions:
  contents: read

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    timeout-minutes: 25
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Cache Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Make gradlew executable
        run: chmod +x ./gradlew

      - name: Lint + tests + build
        run: |
          ./gradlew \
            :kia:test \
            :app:testDebugUnitTest \
            ktlintCheck \
            detekt \
            :app:lintDebug \
            :app:assembleDebug \
            --stacktrace

      - name: Upload debug APK
        uses: actions/upload-artifact@v4
        with:
          name: app-debug-apk
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 90

      - name: Upload test reports on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: |
            **/build/reports/tests/
            **/build/reports/ktlint/
            **/build/reports/detekt/
            **/build/reports/lint-results-*.html
          retention-days: 14
```

- [ ] **Step 2: Commit and push to trigger CI**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add github actions workflow for build and tests"
git push -u origin feat/walk-away-lock
```

- [ ] **Step 3: Verify CI passes on GitHub**

Open the Actions tab. The `ci / build-and-test` job should complete successfully and produce an `app-debug-apk` artifact.

Expected: green check. If red, fix locally before continuing.

---

## Phase 3 — Kia client (Tasks 6–11)

### Task 6: Define KiaClient interface, Vehicle data class, TokenStorage SPI

**Files:**
- Create: `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/KiaClient.kt`
- Create: `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/TokenStorage.kt`
- Create: `kia/src/test/kotlin/com/github/y3knik/connectwithkia/kia/KiaClientContractTest.kt`

- [ ] **Step 1: Write the failing contract test**

`kia/src/test/kotlin/com/github/y3knik/connectwithkia/kia/KiaClientContractTest.kt`:

```kotlin
package com.github.y3knik.connectwithkia.kia

import org.junit.Test
import kotlin.reflect.full.declaredFunctions
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KiaClientContractTest {
    @Test
    fun `KiaClient exposes login, vehicles, lock`() {
        val methods = KiaClient::class.declaredFunctions.map { it.name }.toSet()
        assertTrue("login" in methods, "missing login")
        assertTrue("vehicles" in methods, "missing vehicles")
        assertTrue("lock" in methods, "missing lock")
    }

    @Test
    fun `Vehicle data class has id, nickname, vin`() {
        val v = Vehicle(id = "abc", nickname = "EV9", vin = "VIN")
        assertEquals("abc", v.id)
        assertEquals("EV9", v.nickname)
        assertEquals("VIN", v.vin)
    }

    @Test
    fun `TokenStorage is referenced and instantiable as a stub`() {
        val storage = object : TokenStorage {
            private var token: String? = null
            override fun readAccessToken(): String? = token
            override fun writeAccessToken(token: String, expiresAtEpochMs: Long) {
                this.token = token
            }
            override fun clear() {
                token = null
            }
        }
        storage.writeAccessToken("tk", 1)
        assertNotNull(storage.readAccessToken())
    }
}
```

- [ ] **Step 2: Run the test (it should fail to compile)**

```bash
./gradlew :kia:test
```

Expected: FAIL — `KiaClient`, `Vehicle`, `TokenStorage` unresolved.

- [ ] **Step 3: Create the types**

`kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/KiaClient.kt`:

```kotlin
package com.github.y3knik.connectwithkia.kia

interface KiaClient {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun vehicles(): Result<List<Vehicle>>
    suspend fun lock(vehicleId: String, pin: String): Result<Unit>
}

data class Vehicle(
    val id: String,
    val nickname: String,
    val vin: String,
)
```

`kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/TokenStorage.kt`:

```kotlin
package com.github.y3knik.connectwithkia.kia

/**
 * Persistence SPI for the access token. The :kia module owns the protocol;
 * the :app module supplies an Android Keystore-backed implementation.
 */
interface TokenStorage {
    fun readAccessToken(): String?
    fun writeAccessToken(token: String, expiresAtEpochMs: Long)
    fun clear()
}
```

- [ ] **Step 4: Run the test again**

```bash
./gradlew :kia:test
```

Expected: PASS — three tests.

- [ ] **Step 5: Commit**

```bash
git add kia/src/
git commit -m "feat(kia): add KiaClient interface, Vehicle, TokenStorage SPI"
```

---

### Task 7: Implement DefaultKiaClient.login() with MockWebServer

**Files:**
- Create: `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/internal/KiaApi.kt`
- Create: `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/internal/KiaDto.kt`
- Create: `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/DefaultKiaClient.kt`
- Create: `kia/src/test/resources/fixtures/login_success.json`
- Create: `kia/src/test/kotlin/com/github/y3knik/connectwithkia/kia/DefaultKiaClientLoginTest.kt`

- [ ] **Step 1: Write the fixture**

`kia/src/test/resources/fixtures/login_success.json`:

```json
{
  "accessToken": "TEST_ACCESS_TOKEN",
  "expiresIn": 3600
}
```

- [ ] **Step 2: Write the failing test**

`kia/src/test/kotlin/com/github/y3knik/connectwithkia/kia/DefaultKiaClientLoginTest.kt`:

```kotlin
package com.github.y3knik.connectwithkia.kia

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultKiaClientLoginTest {
    private lateinit var server: MockWebServer
    private lateinit var storage: InMemoryTokenStorage
    private lateinit var client: KiaClient

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        storage = InMemoryTokenStorage()
        client = DefaultKiaClient(baseUrl = server.url("/").toString(), tokenStorage = storage, clockMs = { 1_000L })
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `login persists token on success`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(fixture("fixtures/login_success.json")),
        )

        val result = client.login("user@example.com", "pw")

        assertTrue(result.isSuccess, "login should succeed")
        assertEquals("TEST_ACCESS_TOKEN", storage.readAccessToken())
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertTrue(recorded.path?.endsWith("lgn") == true, "wrong path: ${recorded.path}")
        val body = recorded.body.readUtf8()
        assertTrue("user@example.com" in body)
        assertTrue("pw" in body)
    }

    @Test
    fun `login returns failure on 401`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"bad creds"}"""))

        val result = client.login("user@example.com", "wrong")

        assertTrue(result.isFailure)
    }
}

internal class InMemoryTokenStorage : TokenStorage {
    private var token: String? = null
    private var expires: Long = 0
    override fun readAccessToken(): String? = token
    override fun writeAccessToken(token: String, expiresAtEpochMs: Long) {
        this.token = token
        this.expires = expiresAtEpochMs
    }
    override fun clear() { token = null; expires = 0 }
    fun expiresAt(): Long = expires
}

internal fun fixture(path: String): String =
    DefaultKiaClientLoginTest::class.java.classLoader!!.getResourceAsStream(path)!!
        .bufferedReader().use { it.readText() }
```

- [ ] **Step 3: Run the test (it should fail to compile)**

```bash
./gradlew :kia:test --tests "*Login*"
```

Expected: FAIL — `DefaultKiaClient` unresolved.

- [ ] **Step 4: Create DTOs**

`kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/internal/KiaDto.kt`:

```kotlin
package com.github.y3knik.connectwithkia.kia.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
internal data class LoginResponse(
    val accessToken: String,
    val expiresIn: Long,
)
```

- [ ] **Step 5: Create the Retrofit API interface**

`kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/internal/KiaApi.kt`:

```kotlin
package com.github.y3knik.connectwithkia.kia.internal

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface KiaApi {
    @POST("tods/api/lgn")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
```

- [ ] **Step 6: Create DefaultKiaClient (login only for now)**

`kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/DefaultKiaClient.kt`:

```kotlin
package com.github.y3knik.connectwithkia.kia

import com.github.y3knik.connectwithkia.kia.internal.KiaApi
import com.github.y3knik.connectwithkia.kia.internal.LoginRequest
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class DefaultKiaClient internal constructor(
    baseUrl: String,
    private val tokenStorage: TokenStorage,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    okHttpClient: OkHttpClient = OkHttpClient(),
) : KiaClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val api: KiaApi = Retrofit.Builder()
        .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(KiaApi::class.java)

    constructor(tokenStorage: TokenStorage) : this(
        baseUrl = "https://kiaconnect.ca/",
        tokenStorage = tokenStorage,
    )

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val response = api.login(LoginRequest(email, password))
        val body = response.body()
        require(response.isSuccessful && body != null) { "login failed: HTTP ${response.code()}" }
        tokenStorage.writeAccessToken(
            token = body.accessToken,
            expiresAtEpochMs = clockMs() + body.expiresIn * 1000,
        )
    }

    override suspend fun vehicles(): Result<List<Vehicle>> =
        Result.failure(NotImplementedError("vehicles() implemented in Task 8"))

    override suspend fun lock(vehicleId: String, pin: String): Result<Unit> =
        Result.failure(NotImplementedError("lock() implemented in Task 9"))
}
```

- [ ] **Step 7: Run the tests**

```bash
./gradlew :kia:test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add kia/
git commit -m "feat(kia): implement DefaultKiaClient.login with retrofit + okhttp"
```

---

### Task 8: Implement vehicles()

**Files:**
- Modify: `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/internal/KiaApi.kt`
- Modify: `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/internal/KiaDto.kt`
- Modify: `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/DefaultKiaClient.kt`
- Create: `kia/src/test/resources/fixtures/vehicles_one_ev9.json`
- Create: `kia/src/test/kotlin/com/github/y3knik/connectwithkia/kia/DefaultKiaClientVehiclesTest.kt`

- [ ] **Step 1: Write the fixture**

`kia/src/test/resources/fixtures/vehicles_one_ev9.json`:

```json
{
  "vehicles": [
    {
      "vehicleId": "VID-EV9-001",
      "nickName": "EV9",
      "vin": "KNDPC3DG7P0000001"
    }
  ]
}
```

- [ ] **Step 2: Write the failing test**

`kia/src/test/kotlin/com/github/y3knik/connectwithkia/kia/DefaultKiaClientVehiclesTest.kt`:

```kotlin
package com.github.y3knik.connectwithkia.kia

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultKiaClientVehiclesTest {
    private lateinit var server: MockWebServer
    private lateinit var storage: InMemoryTokenStorage
    private lateinit var client: KiaClient

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        storage = InMemoryTokenStorage().also {
            it.writeAccessToken("TEST_ACCESS_TOKEN", expiresAtEpochMs = 9_999_999_999L)
        }
        client = DefaultKiaClient(baseUrl = server.url("/").toString(), tokenStorage = storage)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `vehicles returns parsed list and sends access token`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(fixture("fixtures/vehicles_one_ev9.json")),
        )

        val result = client.vehicles()

        assertTrue(result.isSuccess, "vehicles should succeed")
        val list = result.getOrThrow()
        assertEquals(1, list.size)
        assertEquals("VID-EV9-001", list[0].id)
        assertEquals("EV9", list[0].nickname)
        assertEquals("KNDPC3DG7P0000001", list[0].vin)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertTrue(recorded.path?.endsWith("vhcllst") == true)
        assertEquals("TEST_ACCESS_TOKEN", recorded.getHeader("Accesstoken"))
    }
}
```

- [ ] **Step 3: Run the test**

```bash
./gradlew :kia:test --tests "*Vehicles*"
```

Expected: FAIL — `NotImplementedError`.

- [ ] **Step 4: Extend DTOs**

Append to `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/internal/KiaDto.kt`:

```kotlin
@Serializable
internal data class VehiclesResponse(
    val vehicles: List<VehicleDto>,
)

@Serializable
internal data class VehicleDto(
    @SerialName("vehicleId") val vehicleId: String,
    @SerialName("nickName") val nickName: String,
    val vin: String,
)
```

- [ ] **Step 5: Extend KiaApi**

Append to `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/internal/KiaApi.kt`:

```kotlin
    @POST("tods/api/vhcllst")
    suspend fun vehicles(@retrofit2.http.Header("Accesstoken") accessToken: String): Response<VehiclesResponse>
```

- [ ] **Step 6: Implement vehicles() in DefaultKiaClient**

Replace the placeholder `vehicles()` in `DefaultKiaClient`:

```kotlin
    override suspend fun vehicles(): Result<List<Vehicle>> = runCatching {
        val token = requireNotNull(tokenStorage.readAccessToken()) { "not logged in" }
        val response = api.vehicles(token)
        val body = response.body()
        require(response.isSuccessful && body != null) { "vehicles failed: HTTP ${response.code()}" }
        body.vehicles.map { Vehicle(id = it.vehicleId, nickname = it.nickName, vin = it.vin) }
    }
```

- [ ] **Step 7: Run all tests**

```bash
./gradlew :kia:test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add kia/
git commit -m "feat(kia): implement vehicles() endpoint"
```

---

### Task 9: Implement lock() with PIN preauth

**Files:**
- Modify: `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/internal/KiaApi.kt`
- Modify: `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/internal/KiaDto.kt`
- Modify: `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/DefaultKiaClient.kt`
- Create: `kia/src/test/resources/fixtures/vrfypin_success.json`
- Create: `kia/src/test/resources/fixtures/drlck_success.json`
- Create: `kia/src/test/kotlin/com/github/y3knik/connectwithkia/kia/DefaultKiaClientLockTest.kt`

- [ ] **Step 1: Write fixtures**

`kia/src/test/resources/fixtures/vrfypin_success.json`:

```json
{ "pAuth": "TEST_PAUTH_TOKEN" }
```

`kia/src/test/resources/fixtures/drlck_success.json`:

```json
{ "status": "OK" }
```

- [ ] **Step 2: Write the failing tests**

`kia/src/test/kotlin/com/github/y3knik/connectwithkia/kia/DefaultKiaClientLockTest.kt`:

```kotlin
package com.github.y3knik.connectwithkia.kia

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultKiaClientLockTest {
    private lateinit var server: MockWebServer
    private lateinit var storage: InMemoryTokenStorage
    private lateinit var client: KiaClient

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        storage = InMemoryTokenStorage().also {
            it.writeAccessToken("TEST_ACCESS_TOKEN", 9_999_999_999L)
        }
        client = DefaultKiaClient(baseUrl = server.url("/").toString(), tokenStorage = storage)
    }

    @After fun tearDown() { server.shutdown() }

    @Test
    fun `lock performs preauth then drlck`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("fixtures/vrfypin_success.json")))
        server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("fixtures/drlck_success.json")))

        val result = client.lock(vehicleId = "VID-EV9-001", pin = "1234")

        assertTrue(result.isSuccess)

        val pinReq = server.takeRequest()
        assertEquals("POST", pinReq.method)
        assertTrue(pinReq.path?.endsWith("vrfypin") == true)
        assertTrue("1234" in pinReq.body.readUtf8())
        assertEquals("TEST_ACCESS_TOKEN", pinReq.getHeader("Accesstoken"))

        val lockReq = server.takeRequest()
        assertEquals("POST", lockReq.method)
        assertTrue(lockReq.path?.endsWith("drlck") == true)
        assertEquals("TEST_ACCESS_TOKEN", lockReq.getHeader("Accesstoken"))
        assertEquals("TEST_PAUTH_TOKEN", lockReq.getHeader("pAuth"))
        assertEquals("VID-EV9-001", lockReq.getHeader("vehicleId"))
    }

    @Test
    fun `lock fails when preauth returns non-200`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"bad pin"}"""))

        val result = client.lock(vehicleId = "VID-EV9-001", pin = "0000")

        assertTrue(result.isFailure)
    }
}
```

- [ ] **Step 3: Run tests**

```bash
./gradlew :kia:test --tests "*Lock*"
```

Expected: FAIL — `NotImplementedError`.

- [ ] **Step 4: Extend DTOs**

Append to `KiaDto.kt`:

```kotlin
@Serializable
internal data class PinRequest(val pin: String)

@Serializable
internal data class PinResponse(val pAuth: String)

@Serializable
internal data class LockResponse(val status: String)
```

- [ ] **Step 5: Extend KiaApi**

Append to `KiaApi.kt`:

```kotlin
    @POST("tods/api/vrfypin")
    suspend fun verifyPin(
        @retrofit2.http.Header("Accesstoken") accessToken: String,
        @Body request: PinRequest,
    ): Response<PinResponse>

    @POST("tods/api/drlck")
    suspend fun lock(
        @retrofit2.http.Header("Accesstoken") accessToken: String,
        @retrofit2.http.Header("pAuth") pAuth: String,
        @retrofit2.http.Header("vehicleId") vehicleId: String,
    ): Response<LockResponse>
```

- [ ] **Step 6: Implement lock() in DefaultKiaClient**

Replace the placeholder `lock()`:

```kotlin
    override suspend fun lock(vehicleId: String, pin: String): Result<Unit> = runCatching {
        val token = requireNotNull(tokenStorage.readAccessToken()) { "not logged in" }
        val pinResponse = api.verifyPin(token, com.github.y3knik.connectwithkia.kia.internal.PinRequest(pin))
        val pAuth = pinResponse.body()?.pAuth
        require(pinResponse.isSuccessful && pAuth != null) { "pin verify failed: HTTP ${pinResponse.code()}" }
        val lockResponse = api.lock(token, pAuth, vehicleId)
        require(lockResponse.isSuccessful) { "lock failed: HTTP ${lockResponse.code()}" }
    }
```

- [ ] **Step 7: Run all tests**

```bash
./gradlew :kia:test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add kia/
git commit -m "feat(kia): implement lock() with pin preauth"
```

---

### Task 10: Auto re-login on 401 from preauth

**Files:**
- Modify: `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/DefaultKiaClient.kt`
- Create: `kia/src/test/kotlin/com/github/y3knik/connectwithkia/kia/DefaultKiaClientReauthTest.kt`

The client should re-run `login()` once if `verifyPin` returns 401, then retry. To do this it needs the credentials. Add an optional `CredentialProvider` SPI.

- [ ] **Step 1: Write the failing test**

`kia/src/test/kotlin/com/github/y3knik/connectwithkia/kia/DefaultKiaClientReauthTest.kt`:

```kotlin
package com.github.y3knik.connectwithkia.kia

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultKiaClientReauthTest {
    private lateinit var server: MockWebServer
    private lateinit var storage: InMemoryTokenStorage
    private lateinit var client: KiaClient

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        storage = InMemoryTokenStorage().also {
            it.writeAccessToken("EXPIRED", 9_999_999_999L)
        }
        client = DefaultKiaClient(
            baseUrl = server.url("/").toString(),
            tokenStorage = storage,
            credentialProvider = { Credentials("user@example.com", "pw") },
        )
    }

    @After fun tearDown() { server.shutdown() }

    @Test
    fun `lock re-logs in on 401 then retries`() = runTest {
        // 1. verifyPin → 401
        server.enqueue(MockResponse().setResponseCode(401))
        // 2. login → 200 with fresh token
        server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("fixtures/login_success.json")))
        // 3. verifyPin retry → 200
        server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("fixtures/vrfypin_success.json")))
        // 4. drlck → 200
        server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("fixtures/drlck_success.json")))

        val result = client.lock(vehicleId = "VID-EV9-001", pin = "1234")

        assertTrue(result.isSuccess)
        assertEquals("TEST_ACCESS_TOKEN", storage.readAccessToken())
        assertEquals(4, server.requestCount)
    }
}
```

- [ ] **Step 2: Run the test**

```bash
./gradlew :kia:test --tests "*Reauth*"
```

Expected: FAIL — `credentialProvider` parameter does not exist.

- [ ] **Step 3: Add CredentialProvider SPI to KiaClient.kt**

Append to `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/KiaClient.kt`:

```kotlin
data class Credentials(val email: String, val password: String)

fun interface CredentialProvider {
    fun current(): Credentials?
}
```

- [ ] **Step 4: Update DefaultKiaClient**

In `DefaultKiaClient.kt`, change the constructor and lock implementation:

```kotlin
class DefaultKiaClient internal constructor(
    baseUrl: String,
    private val tokenStorage: TokenStorage,
    private val credentialProvider: CredentialProvider = CredentialProvider { null },
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    okHttpClient: OkHttpClient = OkHttpClient(),
) : KiaClient {

    // ... existing api setup unchanged ...

    constructor(tokenStorage: TokenStorage, credentialProvider: CredentialProvider) : this(
        baseUrl = "https://kiaconnect.ca/",
        tokenStorage = tokenStorage,
        credentialProvider = credentialProvider,
    )

    // ... existing login(), vehicles() unchanged ...

    override suspend fun lock(vehicleId: String, pin: String): Result<Unit> = runCatching {
        suspend fun attempt(): retrofit2.Response<com.github.y3knik.connectwithkia.kia.internal.PinResponse> {
            val token = requireNotNull(tokenStorage.readAccessToken()) { "not logged in" }
            return api.verifyPin(token, com.github.y3knik.connectwithkia.kia.internal.PinRequest(pin))
        }

        var pinResponse = attempt()
        if (pinResponse.code() == 401) {
            val creds = credentialProvider.current() ?: error("401 from preauth and no credentials available to refresh")
            login(creds.email, creds.password).getOrThrow()
            pinResponse = attempt()
        }
        val pAuth = pinResponse.body()?.pAuth
        require(pinResponse.isSuccessful && pAuth != null) { "pin verify failed: HTTP ${pinResponse.code()}" }

        val token = requireNotNull(tokenStorage.readAccessToken())
        val lockResponse = api.lock(token, pAuth, vehicleId)
        require(lockResponse.isSuccessful) { "lock failed: HTTP ${lockResponse.code()}" }
    }
}
```

- [ ] **Step 5: Run all tests**

```bash
./gradlew :kia:test
```

Expected: PASS — all kia tests including the new reauth test.

- [ ] **Step 6: Commit**

```bash
git add kia/
git commit -m "feat(kia): re-login on 401 from preauth and retry once"
```

---

### Task 11: Add network logging + secret redaction

**Files:**
- Create: `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/internal/RedactingInterceptor.kt`
- Modify: `kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/DefaultKiaClient.kt`
- Create: `kia/src/test/kotlin/com/github/y3knik/connectwithkia/kia/RedactingInterceptorTest.kt`

- [ ] **Step 1: Write the failing test**

`kia/src/test/kotlin/com/github/y3knik/connectwithkia/kia/RedactingInterceptorTest.kt`:

```kotlin
package com.github.y3knik.connectwithkia.kia

import com.github.y3knik.connectwithkia.kia.internal.RedactingInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class RedactingInterceptorTest {
    private lateinit var server: MockWebServer

    @Before fun setUp() { server = MockWebServer().also { it.start() } }
    @After fun tearDown() { server.shutdown() }

    @Test
    fun `password and tokens redacted from log output`() {
        val log = StringBuilder()
        val logger = HttpLoggingInterceptor { log.appendLine(it) }
            .setLevel(HttpLoggingInterceptor.Level.BODY)
        val client = OkHttpClient.Builder()
            .addInterceptor(RedactingInterceptor())
            .addInterceptor(logger)
            .build()

        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val body = """{"email":"a@b.com","password":"hunter2"}"""
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(server.url("/lgn"))
            .header("Accesstoken", "SECRET_TOKEN")
            .header("pAuth", "PAUTH_TOKEN")
            .post(body)
            .build()

        client.newCall(request).execute().close()

        val text = log.toString()
        assertFalse("hunter2" in text, "password leaked: $text")
        assertFalse("SECRET_TOKEN" in text, "access token leaked")
        assertFalse("PAUTH_TOKEN" in text, "pAuth leaked")
        assertTrue("REDACTED" in text)
    }
}
```

- [ ] **Step 2: Run the test**

```bash
./gradlew :kia:test --tests "*Redacting*"
```

Expected: FAIL — `RedactingInterceptor` unresolved.

- [ ] **Step 3: Create RedactingInterceptor**

`kia/src/main/kotlin/com/github/y3knik/connectwithkia/kia/internal/RedactingInterceptor.kt`:

```kotlin
package com.github.y3knik.connectwithkia.kia.internal

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer

/**
 * Replaces sensitive header and body fields with REDACTED so they don't appear
 * in logs from HttpLoggingInterceptor. Must be installed BEFORE the logger.
 */
internal class RedactingInterceptor : Interceptor {
    private val secretHeaders = setOf("Accesstoken", "pAuth")
    private val secretJsonFields = listOf("password", "pin", "accessToken", "pAuth")

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val redacted = redactRequest(original)
        return chain.proceed(redacted)
    }

    private fun redactRequest(request: Request): Request {
        val builder = request.newBuilder()
        for (name in secretHeaders) {
            if (request.header(name) != null) builder.header(name, "REDACTED")
        }
        val body = request.body
        if (body != null) {
            val buffer = Buffer().also { body.writeTo(it) }
            val text = buffer.readUtf8()
            val sanitized = secretJsonFields.fold(text) { acc, field ->
                acc.replace(Regex("\"$field\"\\s*:\\s*\"[^\"]*\""), "\"$field\":\"REDACTED\"")
            }
            builder.method(request.method, sanitized.toRequestBody(body.contentType() ?: "application/json".toMediaTypeOrNull()))
        }
        return builder.build()
    }
}
```

- [ ] **Step 4: Wire it into DefaultKiaClient**

Replace the `okHttpClient` default in `DefaultKiaClient` constructor:

```kotlin
class DefaultKiaClient internal constructor(
    baseUrl: String,
    private val tokenStorage: TokenStorage,
    private val credentialProvider: CredentialProvider = CredentialProvider { null },
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    enableLogging: Boolean = false,
    okHttpClient: OkHttpClient = buildDefaultOkHttpClient(enableLogging),
) : KiaClient {
    // ... rest unchanged ...

    companion object {
        private fun buildDefaultOkHttpClient(enableLogging: Boolean): OkHttpClient {
            val builder = OkHttpClient.Builder()
            if (enableLogging) {
                builder.addInterceptor(com.github.y3knik.connectwithkia.kia.internal.RedactingInterceptor())
                builder.addInterceptor(
                    okhttp3.logging.HttpLoggingInterceptor()
                        .setLevel(okhttp3.logging.HttpLoggingInterceptor.Level.BODY),
                )
            }
            return builder.build()
        }
    }
}
```

Add a matching secondary constructor:

```kotlin
    constructor(tokenStorage: TokenStorage, credentialProvider: CredentialProvider, enableLogging: Boolean) : this(
        baseUrl = "https://kiaconnect.ca/",
        tokenStorage = tokenStorage,
        credentialProvider = credentialProvider,
        enableLogging = enableLogging,
    )
```

- [ ] **Step 5: Run all kia tests**

```bash
./gradlew :kia:test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add kia/
git commit -m "feat(kia): add redacting interceptor and optional debug logging"
```

---

## Phase 4 — App data layer (Tasks 12–15)

### Task 12: SecurePreferences (EncryptedSharedPreferences wrapper)

**Files:**
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/data/SecurePreferences.kt`
- Create: `app/src/test/kotlin/com/github/y3knik/connectwithkia/data/SecurePreferencesTest.kt`

`EncryptedSharedPreferences` is hard to instantiate under Robolectric without the Android Keystore. The test uses a swappable `PreferencesSource` interface, with `SecurePreferences` as the production implementation and an in-memory implementation for tests. The production class is verified by manual smoke during onboarding (Task 27).

- [ ] **Step 1: Write the failing test**

`app/src/test/kotlin/com/github/y3knik/connectwithkia/data/SecurePreferencesTest.kt`:

```kotlin
package com.github.y3knik.connectwithkia.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SecurePreferencesTest {
    @Test
    fun `getString returns null when absent`() {
        val prefs = InMemoryPreferences()
        assertNull(prefs.getString("missing"))
    }

    @Test
    fun `putString then getString round-trips`() {
        val prefs = InMemoryPreferences()
        prefs.putString("key", "value")
        assertEquals("value", prefs.getString("key"))
    }

    @Test
    fun `remove deletes a key`() {
        val prefs = InMemoryPreferences()
        prefs.putString("key", "value")
        prefs.remove("key")
        assertNull(prefs.getString("key"))
    }

    @Test
    fun `clear deletes all keys`() {
        val prefs = InMemoryPreferences()
        prefs.putString("a", "1")
        prefs.putString("b", "2")
        prefs.clear()
        assertNull(prefs.getString("a"))
        assertNull(prefs.getString("b"))
    }
}
```

- [ ] **Step 2: Run the test**

```bash
./gradlew :app:testDebugUnitTest --tests "*SecurePreferences*"
```

Expected: FAIL — `InMemoryPreferences` unresolved.

- [ ] **Step 3: Create the interface, prod impl, and test impl**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/data/SecurePreferences.kt`:

```kotlin
package com.github.y3knik.connectwithkia.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface PreferencesSource {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
    fun clear()
}

class SecurePreferences(context: Context) : PreferencesSource {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "connectwithkia_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    override fun remove(key: String) { prefs.edit().remove(key).apply() }
    override fun clear() { prefs.edit().clear().apply() }
}

class InMemoryPreferences : PreferencesSource {
    private val store = mutableMapOf<String, String>()
    override fun getString(key: String): String? = store[key]
    override fun putString(key: String, value: String) { store[key] = value }
    override fun remove(key: String) { store.remove(key) }
    override fun clear() { store.clear() }
}
```

- [ ] **Step 4: Run the test**

```bash
./gradlew :app:testDebugUnitTest --tests "*SecurePreferences*"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(data): add PreferencesSource interface, secure + in-memory impls"
```

---

### Task 13: KeystoreTokenStorage (implements TokenStorage from :kia)

**Files:**
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/data/KeystoreTokenStorage.kt`
- Create: `app/src/test/kotlin/com/github/y3knik/connectwithkia/data/KeystoreTokenStorageTest.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/kotlin/com/github/y3knik/connectwithkia/data/KeystoreTokenStorageTest.kt`:

```kotlin
package com.github.y3knik.connectwithkia.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KeystoreTokenStorageTest {
    @Test
    fun `read returns null before any write`() {
        val storage = KeystoreTokenStorage(InMemoryPreferences())
        assertNull(storage.readAccessToken())
    }

    @Test
    fun `write then read returns token`() {
        val prefs = InMemoryPreferences()
        val storage = KeystoreTokenStorage(prefs)
        storage.writeAccessToken("TOKEN", expiresAtEpochMs = 12345L)
        assertEquals("TOKEN", storage.readAccessToken())
    }

    @Test
    fun `clear removes token`() {
        val prefs = InMemoryPreferences()
        val storage = KeystoreTokenStorage(prefs)
        storage.writeAccessToken("TOKEN", 1L)
        storage.clear()
        assertNull(storage.readAccessToken())
    }
}
```

- [ ] **Step 2: Run the test**

```bash
./gradlew :app:testDebugUnitTest --tests "*KeystoreTokenStorage*"
```

Expected: FAIL — class unresolved.

- [ ] **Step 3: Create KeystoreTokenStorage**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/data/KeystoreTokenStorage.kt`:

```kotlin
package com.github.y3knik.connectwithkia.data

import com.github.y3knik.connectwithkia.kia.TokenStorage

class KeystoreTokenStorage(private val prefs: PreferencesSource) : TokenStorage {
    private val keyToken = "access_token"
    private val keyExpires = "access_token_expires_at"

    override fun readAccessToken(): String? = prefs.getString(keyToken)

    override fun writeAccessToken(token: String, expiresAtEpochMs: Long) {
        prefs.putString(keyToken, token)
        prefs.putString(keyExpires, expiresAtEpochMs.toString())
    }

    override fun clear() {
        prefs.remove(keyToken)
        prefs.remove(keyExpires)
    }
}
```

- [ ] **Step 4: Run the test**

```bash
./gradlew :app:testDebugUnitTest --tests "*KeystoreTokenStorage*"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(data): add KeystoreTokenStorage implementing kia TokenStorage SPI"
```

---

### Task 14: AppSettings — typed access to user-facing settings

**Files:**
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/data/AppSettings.kt`
- Create: `app/src/test/kotlin/com/github/y3knik/connectwithkia/data/AppSettingsTest.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/kotlin/com/github/y3knik/connectwithkia/data/AppSettingsTest.kt`:

```kotlin
package com.github.y3knik.connectwithkia.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsTest {
    @Test
    fun `defaults are sensible`() {
        val s = AppSettings(InMemoryPreferences())
        assertTrue(s.enabled)
        assertTrue(s.highProminenceCountdown)
        assertTrue(s.successNotification)
        assertEquals(5, s.lockDelayMinutes)
    }

    @Test
    fun `setting values round-trips`() {
        val prefs = InMemoryPreferences()
        val s = AppSettings(prefs)
        s.enabled = false
        s.highProminenceCountdown = false
        s.successNotification = false
        s.lockDelayMinutes = 3
        val s2 = AppSettings(prefs)
        assertFalse(s2.enabled)
        assertFalse(s2.highProminenceCountdown)
        assertFalse(s2.successNotification)
        assertEquals(3, s2.lockDelayMinutes)
    }

    @Test
    fun `lockDelayMinutes clamps to 1 to 15`() {
        val s = AppSettings(InMemoryPreferences())
        s.lockDelayMinutes = 0
        assertEquals(1, s.lockDelayMinutes)
        s.lockDelayMinutes = 99
        assertEquals(15, s.lockDelayMinutes)
    }
}
```

- [ ] **Step 2: Run the test**

```bash
./gradlew :app:testDebugUnitTest --tests "*AppSettings*"
```

Expected: FAIL — class unresolved.

- [ ] **Step 3: Create AppSettings**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/data/AppSettings.kt`:

```kotlin
package com.github.y3knik.connectwithkia.data

class AppSettings(private val prefs: PreferencesSource) {

    var enabled: Boolean
        get() = prefs.getString(KEY_ENABLED)?.toBoolean() ?: true
        set(value) { prefs.putString(KEY_ENABLED, value.toString()) }

    var highProminenceCountdown: Boolean
        get() = prefs.getString(KEY_HIGH_PROMINENCE)?.toBoolean() ?: true
        set(value) { prefs.putString(KEY_HIGH_PROMINENCE, value.toString()) }

    var successNotification: Boolean
        get() = prefs.getString(KEY_SUCCESS_NOTIF)?.toBoolean() ?: true
        set(value) { prefs.putString(KEY_SUCCESS_NOTIF, value.toString()) }

    var lockDelayMinutes: Int
        get() = prefs.getString(KEY_DELAY_MIN)?.toIntOrNull() ?: DEFAULT_DELAY_MIN
        set(value) { prefs.putString(KEY_DELAY_MIN, value.coerceIn(MIN_DELAY_MIN, MAX_DELAY_MIN).toString()) }

    private companion object {
        const val KEY_ENABLED = "settings_enabled"
        const val KEY_HIGH_PROMINENCE = "settings_high_prominence"
        const val KEY_SUCCESS_NOTIF = "settings_success_notif"
        const val KEY_DELAY_MIN = "settings_delay_min"
        const val DEFAULT_DELAY_MIN = 5
        const val MIN_DELAY_MIN = 1
        const val MAX_DELAY_MIN = 15
    }
}
```

- [ ] **Step 4: Run the test**

```bash
./gradlew :app:testDebugUnitTest --tests "*AppSettings*"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(data): add AppSettings with defaults and bounds-clamping"
```

---

### Task 15: CredentialsRepository — typed access to Kia credentials + vehicleId

**Files:**
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/data/CredentialsRepository.kt`
- Create: `app/src/test/kotlin/com/github/y3knik/connectwithkia/data/CredentialsRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/kotlin/com/github/y3knik/connectwithkia/data/CredentialsRepositoryTest.kt`:

```kotlin
package com.github.y3knik.connectwithkia.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CredentialsRepositoryTest {
    @Test
    fun `has returns false when nothing stored`() {
        val repo = CredentialsRepository(InMemoryPreferences())
        assertFalse(repo.hasCompleteCredentials)
        assertNull(repo.read())
    }

    @Test
    fun `write then read round-trips`() {
        val prefs = InMemoryPreferences()
        val repo = CredentialsRepository(prefs)
        repo.write(email = "a@b.com", password = "pw", pin = "1234")
        val out = repo.read()!!
        assertEquals("a@b.com", out.email)
        assertEquals("pw", out.password)
        assertEquals("1234", out.pin)
        assertTrue(repo.hasCompleteCredentials)
    }

    @Test
    fun `vehicleId persists separately`() {
        val repo = CredentialsRepository(InMemoryPreferences())
        repo.vehicleId = "VID-123"
        assertEquals("VID-123", repo.vehicleId)
    }

    @Test
    fun `clear removes everything`() {
        val prefs = InMemoryPreferences()
        val repo = CredentialsRepository(prefs)
        repo.write("a@b.com", "pw", "1234")
        repo.vehicleId = "VID-123"
        repo.clear()
        assertNull(repo.read())
        assertNull(repo.vehicleId)
    }
}
```

- [ ] **Step 2: Run the test**

```bash
./gradlew :app:testDebugUnitTest --tests "*CredentialsRepository*"
```

Expected: FAIL — class unresolved.

- [ ] **Step 3: Create CredentialsRepository**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/data/CredentialsRepository.kt`:

```kotlin
package com.github.y3knik.connectwithkia.data

data class StoredCredentials(val email: String, val password: String, val pin: String)

class CredentialsRepository(private val prefs: PreferencesSource) {

    val hasCompleteCredentials: Boolean
        get() = read() != null

    fun read(): StoredCredentials? {
        val email = prefs.getString(KEY_EMAIL) ?: return null
        val password = prefs.getString(KEY_PASSWORD) ?: return null
        val pin = prefs.getString(KEY_PIN) ?: return null
        return StoredCredentials(email, password, pin)
    }

    fun write(email: String, password: String, pin: String) {
        prefs.putString(KEY_EMAIL, email)
        prefs.putString(KEY_PASSWORD, password)
        prefs.putString(KEY_PIN, pin)
    }

    var vehicleId: String?
        get() = prefs.getString(KEY_VEHICLE_ID)
        set(value) {
            if (value == null) prefs.remove(KEY_VEHICLE_ID)
            else prefs.putString(KEY_VEHICLE_ID, value)
        }

    fun clear() {
        prefs.remove(KEY_EMAIL)
        prefs.remove(KEY_PASSWORD)
        prefs.remove(KEY_PIN)
        prefs.remove(KEY_VEHICLE_ID)
    }

    private companion object {
        const val KEY_EMAIL = "kia_email"
        const val KEY_PASSWORD = "kia_password"
        const val KEY_PIN = "kia_pin"
        const val KEY_VEHICLE_ID = "kia_vehicle_id"
    }
}
```

- [ ] **Step 4: Run the test**

```bash
./gradlew :app:testDebugUnitTest --tests "*CredentialsRepository*"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(data): add CredentialsRepository for email/password/pin/vehicleId"
```

---

## Phase 5 — Detection + state + scheduling (Tasks 16–20)

### Task 16: CarConnectionObserver

**Files:**
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/detect/CarConnectionObserver.kt`

This is a thin Android wrapper. It's tested via manual verification (Phase 9) since `CarConnection` is essentially impossible to mock without a connected car.

- [ ] **Step 1: Create CarConnectionObserver**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/detect/CarConnectionObserver.kt`:

```kotlin
package com.github.y3knik.connectwithkia.detect

import android.content.Context
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class CarConnectionState { NOT_CONNECTED, PROJECTION, NATIVE }

interface CarConnectionStream {
    fun observe(owner: LifecycleOwner, onChange: (CarConnectionState) -> Unit)
    fun asFlow(): Flow<CarConnectionState>
}

class CarConnectionObserver(context: Context) : CarConnectionStream {
    private val carConnection = CarConnection(context.applicationContext)

    override fun observe(owner: LifecycleOwner, onChange: (CarConnectionState) -> Unit) {
        carConnection.type.observe(owner, Observer { type -> onChange(mapType(type)) })
    }

    override fun asFlow(): Flow<CarConnectionState> = callbackFlow {
        val observer = Observer<Int> { type -> trySend(mapType(type)) }
        carConnection.type.observeForever(observer)
        awaitClose { carConnection.type.removeObserver(observer) }
    }

    private fun mapType(type: Int): CarConnectionState = when (type) {
        CarConnection.CONNECTION_TYPE_PROJECTION -> CarConnectionState.PROJECTION
        CarConnection.CONNECTION_TYPE_NATIVE -> CarConnectionState.NATIVE
        else -> CarConnectionState.NOT_CONNECTED
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/
git commit -m "feat(detect): add CarConnectionObserver wrapping androidx.car.app CarConnection"
```

---

### Task 17: LockState enum + state transitions (pure Kotlin, fully TDD)

**Files:**
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/scheduler/LockState.kt`
- Create: `app/src/test/kotlin/com/github/y3knik/connectwithkia/scheduler/LockStateMachineTest.kt`

This is the heart of the scheduler — make it side-effect free and fully unit tested. Side effects (alarms, services) come in the next task.

- [ ] **Step 1: Write the failing test**

`app/src/test/kotlin/com/github/y3knik/connectwithkia/scheduler/LockStateMachineTest.kt`:

```kotlin
package com.github.y3knik.connectwithkia.scheduler

import org.junit.Test
import kotlin.test.assertEquals

class LockStateMachineTest {

    @Test
    fun `disabled stays disabled on any event when not configured`() {
        val sm = LockStateMachine(initial = LockState.Disabled(configured = false))
        assertEquals(LockState.Disabled(configured = false), sm.transition(LockEvent.AaConnected))
        assertEquals(LockState.Disabled(configured = false), sm.transition(LockEvent.AaDisconnected))
    }

    @Test
    fun `enabled with credentials and AA connected goes to Connected`() {
        val sm = LockStateMachine(initial = LockState.Idle)
        assertEquals(LockState.Connected, sm.transition(LockEvent.AaConnected))
    }

    @Test
    fun `connected then AA disconnected arms pending lock`() {
        val sm = LockStateMachine(initial = LockState.Connected)
        val out = sm.transition(LockEvent.AaDisconnected)
        assertEquals(LockState.PendingLock, out)
    }

    @Test
    fun `pending lock then AA reconnected goes back to Connected`() {
        val sm = LockStateMachine(initial = LockState.PendingLock)
        assertEquals(LockState.Connected, sm.transition(LockEvent.AaConnected))
    }

    @Test
    fun `pending lock then user cancel goes to Idle`() {
        val sm = LockStateMachine(initial = LockState.PendingLock)
        assertEquals(LockState.Idle, sm.transition(LockEvent.UserCancelled))
    }

    @Test
    fun `pending lock then alarm fires goes to Locking`() {
        val sm = LockStateMachine(initial = LockState.PendingLock)
        assertEquals(LockState.Locking, sm.transition(LockEvent.AlarmFired))
    }

    @Test
    fun `locking then success goes to Done(success)`() {
        val sm = LockStateMachine(initial = LockState.Locking)
        assertEquals(LockState.Done(success = true), sm.transition(LockEvent.LockSucceeded))
    }

    @Test
    fun `locking then failure goes to Done(failure)`() {
        val sm = LockStateMachine(initial = LockState.Locking)
        assertEquals(LockState.Done(success = false, reason = "boom"), sm.transition(LockEvent.LockFailed("boom")))
    }

    @Test
    fun `Done collapses on next AA connected`() {
        val sm = LockStateMachine(initial = LockState.Done(success = true))
        assertEquals(LockState.Connected, sm.transition(LockEvent.AaConnected))
    }

    @Test
    fun `master disable returns to Disabled`() {
        val sm = LockStateMachine(initial = LockState.PendingLock)
        assertEquals(LockState.Disabled(configured = true), sm.transition(LockEvent.MasterDisabled))
    }
}
```

- [ ] **Step 2: Run the test**

```bash
./gradlew :app:testDebugUnitTest --tests "*LockStateMachine*"
```

Expected: FAIL — types unresolved.

- [ ] **Step 3: Create the state machine**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/scheduler/LockState.kt`:

```kotlin
package com.github.y3knik.connectwithkia.scheduler

sealed class LockState {
    data class Disabled(val configured: Boolean) : LockState()
    data object Idle : LockState()
    data object Connected : LockState()
    data object PendingLock : LockState()
    data object Locking : LockState()
    data class Done(val success: Boolean, val reason: String? = null) : LockState()
}

sealed class LockEvent {
    data object AaConnected : LockEvent()
    data object AaDisconnected : LockEvent()
    data object UserCancelled : LockEvent()
    data object AlarmFired : LockEvent()
    data object LockSucceeded : LockEvent()
    data class LockFailed(val reason: String) : LockEvent()
    data object MasterEnabled : LockEvent()
    data object MasterDisabled : LockEvent()
}

class LockStateMachine(initial: LockState) {
    var state: LockState = initial
        private set

    fun transition(event: LockEvent): LockState {
        state = next(state, event)
        return state
    }

    private fun next(state: LockState, event: LockEvent): LockState {
        if (state is LockState.Disabled && !state.configured) return state
        if (event is LockEvent.MasterDisabled) return LockState.Disabled(configured = true)
        if (event is LockEvent.MasterEnabled && state is LockState.Disabled) return LockState.Idle

        return when (state) {
            is LockState.Disabled -> state
            LockState.Idle -> when (event) {
                LockEvent.AaConnected -> LockState.Connected
                else -> state
            }
            LockState.Connected -> when (event) {
                LockEvent.AaDisconnected -> LockState.PendingLock
                else -> state
            }
            LockState.PendingLock -> when (event) {
                LockEvent.AaConnected -> LockState.Connected
                LockEvent.UserCancelled -> LockState.Idle
                LockEvent.AlarmFired -> LockState.Locking
                else -> state
            }
            LockState.Locking -> when (event) {
                LockEvent.LockSucceeded -> LockState.Done(success = true)
                is LockEvent.LockFailed -> LockState.Done(success = false, reason = event.reason)
                else -> state
            }
            is LockState.Done -> when (event) {
                LockEvent.AaConnected -> LockState.Connected
                LockEvent.AaDisconnected -> LockState.PendingLock
                else -> state
            }
        }
    }
}
```

- [ ] **Step 4: Run the test**

```bash
./gradlew :app:testDebugUnitTest --tests "*LockStateMachine*"
```

Expected: PASS — all ten tests.

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(scheduler): add LockState/LockEvent and pure state machine"
```

---

### Task 18: LockScheduler — wires state machine to alarms and the kia client

**Files:**
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/scheduler/LockScheduler.kt`
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/scheduler/AlarmDriver.kt`
- Create: `app/src/test/kotlin/com/github/y3knik/connectwithkia/scheduler/LockSchedulerTest.kt`

The scheduler accepts an `AlarmDriver` SPI so we can fake the alarm in tests.

- [ ] **Step 1: Write the failing test**

`app/src/test/kotlin/com/github/y3knik/connectwithkia/scheduler/LockSchedulerTest.kt`:

```kotlin
package com.github.y3knik.connectwithkia.scheduler

import com.github.y3knik.connectwithkia.kia.KiaClient
import com.github.y3knik.connectwithkia.kia.Vehicle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LockSchedulerTest {

    private class FakeAlarmDriver : AlarmDriver {
        var armedAt: Long? = null
        var cancelled: Boolean = false
        override fun arm(targetEpochMs: Long) { armedAt = targetEpochMs; cancelled = false }
        override fun cancel() { cancelled = true; armedAt = null }
    }

    private class FakeKiaClient(var lockResult: Result<Unit>) : KiaClient {
        var lockCalls = 0
        override suspend fun login(email: String, password: String): Result<Unit> = Result.success(Unit)
        override suspend fun vehicles(): Result<List<Vehicle>> = Result.success(emptyList())
        override suspend fun lock(vehicleId: String, pin: String): Result<Unit> {
            lockCalls++
            return lockResult
        }
    }

    @Test
    fun `AaDisconnected arms alarm 5 minutes out`() = runTest {
        val driver = FakeAlarmDriver()
        val scheduler = LockScheduler(
            kia = FakeKiaClient(Result.success(Unit)),
            alarmDriver = driver,
            delayMinutes = { 5 },
            credentials = { Triple("vid", "1234", true) },
            clockMs = { 1_000_000L },
        )
        scheduler.onEvent(LockEvent.AaConnected)
        scheduler.onEvent(LockEvent.AaDisconnected)
        assertEquals(1_000_000L + 5 * 60_000L, driver.armedAt)
    }

    @Test
    fun `AA reconnect cancels armed alarm`() = runTest {
        val driver = FakeAlarmDriver()
        val scheduler = LockScheduler(
            kia = FakeKiaClient(Result.success(Unit)),
            alarmDriver = driver,
            delayMinutes = { 5 },
            credentials = { Triple("vid", "1234", true) },
            clockMs = { 1L },
        )
        scheduler.onEvent(LockEvent.AaConnected)
        scheduler.onEvent(LockEvent.AaDisconnected)
        scheduler.onEvent(LockEvent.AaConnected)
        assertTrue(driver.cancelled)
    }

    @Test
    fun `alarm fired triggers kia lock and emits Done(success)`() = runTest {
        val kia = FakeKiaClient(Result.success(Unit))
        val driver = FakeAlarmDriver()
        val scheduler = LockScheduler(
            kia = kia,
            alarmDriver = driver,
            delayMinutes = { 1 },
            credentials = { Triple("vid", "1234", true) },
            clockMs = { 1L },
        )
        scheduler.onEvent(LockEvent.AaConnected)
        scheduler.onEvent(LockEvent.AaDisconnected)
        scheduler.onEvent(LockEvent.AlarmFired)
        scheduler.awaitLock()
        assertEquals(1, kia.lockCalls)
        assertEquals(LockState.Done(success = true), scheduler.state.first())
    }

    @Test
    fun `alarm fired with lock failure emits Done(failure)`() = runTest {
        val kia = FakeKiaClient(Result.failure(RuntimeException("boom")))
        val driver = FakeAlarmDriver()
        val scheduler = LockScheduler(
            kia = kia,
            alarmDriver = driver,
            delayMinutes = { 1 },
            credentials = { Triple("vid", "1234", true) },
            clockMs = { 1L },
        )
        scheduler.onEvent(LockEvent.AaConnected)
        scheduler.onEvent(LockEvent.AaDisconnected)
        scheduler.onEvent(LockEvent.AlarmFired)
        scheduler.awaitLock()
        assertEquals(LockState.Done(success = false, reason = "boom"), scheduler.state.first())
    }
}
```

- [ ] **Step 2: Run the test**

```bash
./gradlew :app:testDebugUnitTest --tests "*LockScheduler*"
```

Expected: FAIL — `LockScheduler`, `AlarmDriver` unresolved.

- [ ] **Step 3: Create AlarmDriver SPI**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/scheduler/AlarmDriver.kt`:

```kotlin
package com.github.y3knik.connectwithkia.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

interface AlarmDriver {
    fun arm(targetEpochMs: Long)
    fun cancel()
}

class AndroidAlarmDriver(private val context: Context) : AlarmDriver {
    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, LockAlarmReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun arm(targetEpochMs: Long) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            targetEpochMs,
            pendingIntent,
        )
    }

    override fun cancel() {
        alarmManager.cancel(pendingIntent)
    }

    private companion object {
        const val REQUEST_CODE = 0x10C4
    }
}
```

(`LockAlarmReceiver` is created in Task 19; the compiler will error until then — that's expected at this step. Step 5 of this task introduces a stub to satisfy compilation.)

- [ ] **Step 4: Create LockScheduler**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/scheduler/LockScheduler.kt`:

```kotlin
package com.github.y3knik.connectwithkia.scheduler

import com.github.y3knik.connectwithkia.kia.KiaClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LockScheduler(
    private val kia: KiaClient,
    private val alarmDriver: AlarmDriver,
    private val delayMinutes: () -> Int,
    /** Returns vehicleId, pin, configured-flag. Null if not configured. */
    private val credentials: () -> Triple<String, String, Boolean>?,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val _state = MutableStateFlow<LockState>(
        if (credentials() != null) LockState.Idle else LockState.Disabled(configured = false),
    )
    val state: StateFlow<LockState> = _state.asStateFlow()
    private val machine = LockStateMachine(initial = _state.value)
    private var lockJob: Job? = null

    fun onEvent(event: LockEvent) {
        val previous = _state.value
        val next = machine.transition(event)
        _state.value = next
        applySideEffects(previous, next, event)
    }

    /** Test hook: waits for the active lock job (if any) to finish. */
    suspend fun awaitLock() {
        lockJob?.join()
    }

    private fun applySideEffects(previous: LockState, next: LockState, event: LockEvent) {
        if (next is LockState.PendingLock && previous !is LockState.PendingLock) {
            val targetMs = clockMs() + delayMinutes().coerceAtLeast(1) * 60_000L
            alarmDriver.arm(targetMs)
        }
        if (previous is LockState.PendingLock && next !is LockState.PendingLock && event != LockEvent.AlarmFired) {
            alarmDriver.cancel()
        }
        if (next is LockState.Locking) {
            performLock()
        }
    }

    private fun performLock() {
        val creds = credentials() ?: run {
            onEvent(LockEvent.LockFailed("no credentials"))
            return
        }
        val (vehicleId, pin, _) = creds
        lockJob = scope.launch {
            val result = kia.lock(vehicleId, pin)
            result.fold(
                onSuccess = { onEvent(LockEvent.LockSucceeded) },
                onFailure = { onEvent(LockEvent.LockFailed(it.message ?: "unknown")) },
            )
        }
    }
}
```

- [ ] **Step 5: Add a stub LockAlarmReceiver so AlarmDriver compiles**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/scheduler/LockAlarmReceiver.kt`:

```kotlin
package com.github.y3knik.connectwithkia.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LockAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // wired in Task 19
    }
}
```

- [ ] **Step 6: Run all scheduler tests**

```bash
./gradlew :app:testDebugUnitTest --tests "*Lock*"
```

Expected: PASS — state machine tests + scheduler tests.

- [ ] **Step 7: Commit**

```bash
git add app/
git commit -m "feat(scheduler): add LockScheduler with AlarmDriver SPI"
```

---

### Task 19: LockAlarmReceiver — fire the AlarmFired event

**Files:**
- Modify: `app/src/main/kotlin/com/github/y3knik/connectwithkia/scheduler/LockAlarmReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Create (or modify): `app/src/main/kotlin/com/github/y3knik/connectwithkia/di/AppContainer.kt`

The receiver needs access to the singleton `LockScheduler`. Introduce a tiny service locator now.

- [ ] **Step 1: Create AppContainer**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/di/AppContainer.kt`:

```kotlin
package com.github.y3knik.connectwithkia.di

import android.content.Context
import com.github.y3knik.connectwithkia.BuildConfig
import com.github.y3knik.connectwithkia.data.AppSettings
import com.github.y3knik.connectwithkia.data.CredentialsRepository
import com.github.y3knik.connectwithkia.data.KeystoreTokenStorage
import com.github.y3knik.connectwithkia.data.SecurePreferences
import com.github.y3knik.connectwithkia.kia.CredentialProvider
import com.github.y3knik.connectwithkia.kia.Credentials
import com.github.y3knik.connectwithkia.kia.DefaultKiaClient
import com.github.y3knik.connectwithkia.kia.KiaClient
import com.github.y3knik.connectwithkia.scheduler.AndroidAlarmDriver
import com.github.y3knik.connectwithkia.scheduler.LockScheduler

class AppContainer private constructor(context: Context) {
    val prefs = SecurePreferences(context.applicationContext)
    val settings = AppSettings(prefs)
    val credentials = CredentialsRepository(prefs)
    val tokenStorage = KeystoreTokenStorage(prefs)

    val kiaClient: KiaClient = DefaultKiaClient(
        tokenStorage = tokenStorage,
        credentialProvider = CredentialProvider {
            credentials.read()?.let { Credentials(it.email, it.password) }
        },
        enableLogging = BuildConfig.NETWORK_LOGS,
    )

    val alarmDriver = AndroidAlarmDriver(context.applicationContext)

    val scheduler = LockScheduler(
        kia = kiaClient,
        alarmDriver = alarmDriver,
        delayMinutes = { settings.lockDelayMinutes },
        credentials = {
            val c = credentials.read() ?: return@LockScheduler null
            val vid = credentials.vehicleId ?: return@LockScheduler null
            Triple(vid, c.pin, settings.enabled)
        },
    )

    companion object {
        @Volatile private var instance: AppContainer? = null
        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
    }
}
```

- [ ] **Step 2: Update ConnectWithKiaApp**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/ConnectWithKiaApp.kt`:

```kotlin
package com.github.y3knik.connectwithkia

import android.app.Application
import com.github.y3knik.connectwithkia.di.AppContainer

class ConnectWithKiaApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.get(this)
    }
}
```

- [ ] **Step 3: Update LockAlarmReceiver**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/scheduler/LockAlarmReceiver.kt`:

```kotlin
package com.github.y3knik.connectwithkia.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.y3knik.connectwithkia.di.AppContainer

class LockAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppContainer.get(context).scheduler.onEvent(LockEvent.AlarmFired)
    }
}
```

- [ ] **Step 4: Register receiver in manifest**

Inside `<application>` in `app/src/main/AndroidManifest.xml`:

```xml
        <receiver
            android:name=".scheduler.LockAlarmReceiver"
            android:exported="false" />
```

- [ ] **Step 5: Build**

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL and tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/
git commit -m "feat(scheduler): wire AppContainer and LockAlarmReceiver"
```

---

### Task 20: BootReceiver — re-arm pending alarm after reboot

**Files:**
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/scheduler/BootReceiver.kt`
- Modify: `app/src/main/kotlin/com/github/y3knik/connectwithkia/scheduler/LockScheduler.kt`
- Modify: `app/src/main/kotlin/com/github/y3knik/connectwithkia/data/AppSettings.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/test/kotlin/com/github/y3knik/connectwithkia/scheduler/LockSchedulerPersistenceTest.kt`

We need the scheduler to remember a pending-lock target time across process death and reboot.

- [ ] **Step 1: Add pending-lock target persistence to AppSettings**

Append to `AppSettings.kt` body (before companion object):

```kotlin
    var pendingLockTargetMs: Long?
        get() = prefs.getString(KEY_PENDING_TARGET)?.toLongOrNull()
        set(value) {
            if (value == null) prefs.remove(KEY_PENDING_TARGET)
            else prefs.putString(KEY_PENDING_TARGET, value.toString())
        }
```

And add to the companion:

```kotlin
        const val KEY_PENDING_TARGET = "pending_lock_target_ms"
```

- [ ] **Step 2: Write the failing test**

`app/src/test/kotlin/com/github/y3knik/connectwithkia/scheduler/LockSchedulerPersistenceTest.kt`:

```kotlin
package com.github.y3knik.connectwithkia.scheduler

import com.github.y3knik.connectwithkia.kia.KiaClient
import com.github.y3knik.connectwithkia.kia.Vehicle
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LockSchedulerPersistenceTest {

    private class FakeAlarmDriver : AlarmDriver {
        var armedAt: Long? = null
        var cancelled = false
        override fun arm(targetEpochMs: Long) { armedAt = targetEpochMs; cancelled = false }
        override fun cancel() { cancelled = true; armedAt = null }
    }

    private class NoopKia : KiaClient {
        override suspend fun login(email: String, password: String) = Result.success(Unit)
        override suspend fun vehicles() = Result.success(emptyList<Vehicle>())
        override suspend fun lock(vehicleId: String, pin: String) = Result.success(Unit)
    }

    @Test
    fun `arming a pending lock persists target time`() {
        val driver = FakeAlarmDriver()
        var stored: Long? = null
        val scheduler = LockScheduler(
            kia = NoopKia(),
            alarmDriver = driver,
            delayMinutes = { 5 },
            credentials = { Triple("vid", "1234", true) },
            clockMs = { 1_000_000L },
            persistedTarget = object : LockScheduler.PersistedTarget {
                override fun read(): Long? = stored
                override fun write(value: Long?) { stored = value }
            },
        )
        scheduler.onEvent(LockEvent.AaConnected)
        scheduler.onEvent(LockEvent.AaDisconnected)
        assertEquals(1_000_000L + 5 * 60_000L, stored)
    }

    @Test
    fun `cancelling a pending lock clears persisted target`() {
        val driver = FakeAlarmDriver()
        var stored: Long? = null
        val scheduler = LockScheduler(
            kia = NoopKia(),
            alarmDriver = driver,
            delayMinutes = { 5 },
            credentials = { Triple("vid", "1234", true) },
            clockMs = { 1_000_000L },
            persistedTarget = object : LockScheduler.PersistedTarget {
                override fun read(): Long? = stored
                override fun write(value: Long?) { stored = value }
            },
        )
        scheduler.onEvent(LockEvent.AaConnected)
        scheduler.onEvent(LockEvent.AaDisconnected)
        scheduler.onEvent(LockEvent.UserCancelled)
        assertNull(stored)
    }
}
```

- [ ] **Step 3: Run the test**

```bash
./gradlew :app:testDebugUnitTest --tests "*LockSchedulerPersistence*"
```

Expected: FAIL — `persistedTarget` parameter and `PersistedTarget` nested interface unresolved.

- [ ] **Step 4: Extend LockScheduler**

Update `LockScheduler.kt` — add `PersistedTarget` and the constructor parameter:

```kotlin
class LockScheduler(
    private val kia: KiaClient,
    private val alarmDriver: AlarmDriver,
    private val delayMinutes: () -> Int,
    private val credentials: () -> Triple<String, String, Boolean>?,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val persistedTarget: PersistedTarget = NoopPersistedTarget,
) {
    interface PersistedTarget {
        fun read(): Long?
        fun write(value: Long?)
    }
    private object NoopPersistedTarget : PersistedTarget {
        override fun read(): Long? = null
        override fun write(value: Long?) {}
    }

    // ... existing fields ...

    private fun applySideEffects(previous: LockState, next: LockState, event: LockEvent) {
        if (next is LockState.PendingLock && previous !is LockState.PendingLock) {
            val targetMs = clockMs() + delayMinutes().coerceAtLeast(1) * 60_000L
            alarmDriver.arm(targetMs)
            persistedTarget.write(targetMs)
        }
        if (previous is LockState.PendingLock && next !is LockState.PendingLock && event != LockEvent.AlarmFired) {
            alarmDriver.cancel()
            persistedTarget.write(null)
        }
        if (next is LockState.Locking) {
            persistedTarget.write(null)
            performLock()
        }
    }

    /** Called by BootReceiver. Returns true if an alarm was re-armed. */
    fun rearmIfPending(): Boolean {
        val target = persistedTarget.read() ?: return false
        val now = clockMs()
        return when {
            target > now -> {
                alarmDriver.arm(target)
                _state.value = LockState.PendingLock
                true
            }
            now - target <= STALE_LIMIT_MS -> {
                _state.value = LockState.PendingLock
                onEvent(LockEvent.AlarmFired)
                true
            }
            else -> {
                persistedTarget.write(null)
                false
            }
        }
    }

    private companion object {
        const val STALE_LIMIT_MS = 30L * 60_000L
    }
}
```

- [ ] **Step 5: Wire `PersistedTarget` in AppContainer**

In `AppContainer.kt`, replace the `scheduler` instantiation:

```kotlin
    val scheduler = LockScheduler(
        kia = kiaClient,
        alarmDriver = alarmDriver,
        delayMinutes = { settings.lockDelayMinutes },
        credentials = {
            val c = credentials.read() ?: return@LockScheduler null
            val vid = credentials.vehicleId ?: return@LockScheduler null
            Triple(vid, c.pin, settings.enabled)
        },
        persistedTarget = object : LockScheduler.PersistedTarget {
            override fun read(): Long? = settings.pendingLockTargetMs
            override fun write(value: Long?) { settings.pendingLockTargetMs = value }
        },
    )
```

- [ ] **Step 6: Create BootReceiver**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/scheduler/BootReceiver.kt`:

```kotlin
package com.github.y3knik.connectwithkia.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.y3knik.connectwithkia.di.AppContainer

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return
        AppContainer.get(context).scheduler.rearmIfPending()
    }
}
```

- [ ] **Step 7: Register BootReceiver in manifest**

Inside `<application>`:

```xml
        <receiver
            android:name=".scheduler.BootReceiver"
            android:exported="true"
            android:permission="android.permission.RECEIVE_BOOT_COMPLETED">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
```

- [ ] **Step 8: Run all tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add app/
git commit -m "feat(scheduler): persist pending-lock target across reboot"
```

---

## Phase 6 — Foreground service + notifications (Tasks 21–22)

### Task 21: NotificationHelper — channel + countdown + outcome notifications

**Files:**
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/service/NotificationHelper.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add strings**

Append to `app/src/main/res/values/strings.xml`:

```xml
    <string name="channel_countdown_name">Walk-away lock countdown</string>
    <string name="channel_countdown_desc">Shows a countdown when the lock is pending.</string>
    <string name="channel_outcomes_name">Walk-away lock results</string>
    <string name="channel_outcomes_desc">Shows success or failure after a lock attempt.</string>
    <string name="notif_countdown_title">EV9 locking soon</string>
    <string name="notif_countdown_text">Locking in %1$s</string>
    <string name="notif_cancel_action">Cancel</string>
    <string name="notif_success_title">EV9 locked</string>
    <string name="notif_failure_title">EV9 lock failed</string>
```

- [ ] **Step 2: Create NotificationHelper**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/service/NotificationHelper.kt`:

```kotlin
package com.github.y3knik.connectwithkia.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.github.y3knik.connectwithkia.MainActivity
import com.github.y3knik.connectwithkia.R

class NotificationHelper(private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    fun ensureChannels(highProminence: Boolean) {
        val countdown = NotificationChannel(
            CHANNEL_COUNTDOWN,
            context.getString(R.string.channel_countdown_name),
            if (highProminence) NotificationManager.IMPORTANCE_DEFAULT else NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_countdown_desc)
            setShowBadge(false)
        }
        val outcomes = NotificationChannel(
            CHANNEL_OUTCOMES,
            context.getString(R.string.channel_outcomes_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_outcomes_desc)
        }
        manager.createNotificationChannel(countdown)
        manager.createNotificationChannel(outcomes)
    }

    fun countdown(remainingText: String): Notification {
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelIntent = Intent(context, com.github.y3knik.connectwithkia.scheduler.CancelLockReceiver::class.java)
        val cancelPi = PendingIntent.getBroadcast(
            context, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_COUNTDOWN)
            .setContentTitle(context.getString(R.string.notif_countdown_title))
            .setContentText(context.getString(R.string.notif_countdown_text, remainingText))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, context.getString(R.string.notif_cancel_action), cancelPi)
            .build()
    }

    fun success() {
        val n = NotificationCompat.Builder(context, CHANNEL_OUTCOMES)
            .setContentTitle(context.getString(R.string.notif_success_title))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIF_OUTCOME, n)
    }

    fun failure(reason: String) {
        val n = NotificationCompat.Builder(context, CHANNEL_OUTCOMES)
            .setContentTitle(context.getString(R.string.notif_failure_title))
            .setContentText(reason)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIF_OUTCOME, n)
    }

    companion object {
        const val CHANNEL_COUNTDOWN = "countdown"
        const val CHANNEL_OUTCOMES = "outcomes"
        const val NOTIF_COUNTDOWN = 1001
        const val NOTIF_OUTCOME = 1002
    }
}
```

- [ ] **Step 3: Create CancelLockReceiver**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/scheduler/CancelLockReceiver.kt`:

```kotlin
package com.github.y3knik.connectwithkia.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.y3knik.connectwithkia.di.AppContainer

class CancelLockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppContainer.get(context).scheduler.onEvent(LockEvent.UserCancelled)
    }
}
```

- [ ] **Step 4: Register CancelLockReceiver in manifest**

Inside `<application>`:

```xml
        <receiver
            android:name=".scheduler.CancelLockReceiver"
            android:exported="false" />
```

- [ ] **Step 5: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/
git commit -m "feat(service): add NotificationHelper, channels, and CancelLockReceiver"
```

---

### Task 22: LockForegroundService — owns the countdown notification

**Files:**
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/service/LockForegroundService.kt`
- Modify: `app/src/main/kotlin/com/github/y3knik/connectwithkia/scheduler/LockScheduler.kt`
- Modify: `app/src/main/kotlin/com/github/y3knik/connectwithkia/di/AppContainer.kt`
- Modify: `app/src/main/AndroidManifest.xml`

The service runs only while the scheduler is in `PendingLock` or `Locking`. It updates the countdown notification each second and observes outcomes for the success/failure notification.

- [ ] **Step 1: Create the service**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/service/LockForegroundService.kt`:

```kotlin
package com.github.y3knik.connectwithkia.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.github.y3knik.connectwithkia.di.AppContainer
import com.github.y3knik.connectwithkia.scheduler.LockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LockForegroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var tickerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val container = AppContainer.get(this)
        val notifications = NotificationHelper(this)
        notifications.ensureChannels(container.settings.highProminenceCountdown)

        startForeground(NotificationHelper.NOTIF_COUNTDOWN, notifications.countdown("..."))

        scope.launch {
            container.scheduler.state.collectLatest { state ->
                when (state) {
                    is LockState.PendingLock -> startTicker(notifications, container.settings.pendingLockTargetMs ?: 0L)
                    is LockState.Locking -> {
                        tickerJob?.cancel()
                    }
                    is LockState.Done -> {
                        if (state.success && container.settings.successNotification) notifications.success()
                        if (!state.success) notifications.failure(state.reason ?: "unknown error")
                        stopSelf()
                    }
                    else -> stopSelf()
                }
            }
        }
    }

    private fun startTicker(notifications: NotificationHelper, targetEpochMs: Long) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                val remaining = targetEpochMs - System.currentTimeMillis()
                if (remaining <= 0) break
                val mins = TimeUnit.MILLISECONDS.toMinutes(remaining)
                val secs = TimeUnit.MILLISECONDS.toSeconds(remaining) - mins * 60
                val text = "%d:%02d".format(mins, secs)
                val n = notifications.countdown(text)
                getSystemService(android.app.NotificationManager::class.java)
                    .notify(NotificationHelper.NOTIF_COUNTDOWN, n)
                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
```

- [ ] **Step 2: Make scheduler start/stop the service**

Add to AppContainer initialization an observer on the scheduler's state to launch the service when PendingLock is entered. Add to `AppContainer.kt`, after the `scheduler` property:

```kotlin
    private val serviceController = SchedulerServiceController(context.applicationContext, scheduler)

    init { serviceController.start() }
```

`app/src/main/kotlin/com/github/y3knik/connectwithkia/service/SchedulerServiceController.kt`:

```kotlin
package com.github.y3knik.connectwithkia.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.github.y3knik.connectwithkia.scheduler.LockScheduler
import com.github.y3knik.connectwithkia.scheduler.LockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class SchedulerServiceController(
    private val context: Context,
    private val scheduler: LockScheduler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            scheduler.state.distinctUntilChanged().collect { state ->
                when (state) {
                    is LockState.PendingLock -> startService()
                    else -> Unit // service self-stops on Done or non-pending state
                }
            }
        }
    }

    private fun startService() {
        val intent = Intent(context, LockForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
```

- [ ] **Step 3: Register the service in the manifest**

Inside `<application>`:

```xml
        <service
            android:name=".service.LockForegroundService"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="Walk-away lock for personal Kia Connect vehicle" />
        </service>
```

- [ ] **Step 4: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(service): add LockForegroundService and SchedulerServiceController"
```

---

## Phase 7 — UI (Tasks 23–26)

### Task 23: Compose navigation + Material 3 scaffold

**Files:**
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/AppNavigation.kt`
- Modify: `app/src/main/kotlin/com/github/y3knik/connectwithkia/MainActivity.kt`

- [ ] **Step 1: Create AppNavigation**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/AppNavigation.kt`:

```kotlin
package com.github.y3knik.connectwithkia.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.y3knik.connectwithkia.ui.credentials.CredentialsScreen
import com.github.y3knik.connectwithkia.ui.settings.SettingsScreen
import com.github.y3knik.connectwithkia.ui.status.StatusScreen

sealed class Route(val path: String, val label: String) {
    data object Status : Route("status", "Status")
    data object Credentials : Route("credentials", "Credentials")
    data object Settings : Route("settings", "Settings")
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = route == Route.Status.path,
                    onClick = { nav.navigate(Route.Status.path) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(Route.Status.label) },
                )
                NavigationBarItem(
                    selected = route == Route.Credentials.path,
                    onClick = { nav.navigate(Route.Credentials.path) },
                    icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    label = { Text(Route.Credentials.label) },
                )
                NavigationBarItem(
                    selected = route == Route.Settings.path,
                    onClick = { nav.navigate(Route.Settings.path) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(Route.Settings.label) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Route.Status.path,
            modifier = Modifier.padding(padding),
        ) {
            composable(Route.Status.path) { StatusScreen() }
            composable(Route.Credentials.path) { CredentialsScreen() }
            composable(Route.Settings.path) { SettingsScreen() }
        }
    }
}
```

- [ ] **Step 2: Update MainActivity**

```kotlin
package com.github.y3knik.connectwithkia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.github.y3knik.connectwithkia.ui.AppNavigation
import com.github.y3knik.connectwithkia.ui.theme.ConnectWithKiaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConnectWithKiaTheme {
                AppNavigation()
            }
        }
    }
}
```

- [ ] **Step 3: Add placeholder screen composables (so the build compiles)**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/status/StatusScreen.kt`:

```kotlin
package com.github.y3knik.connectwithkia.ui.status

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun StatusScreen() { Text("Status") }
```

`app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/credentials/CredentialsScreen.kt`:

```kotlin
package com.github.y3knik.connectwithkia.ui.credentials

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun CredentialsScreen() { Text("Credentials") }
```

`app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/settings/SettingsScreen.kt`:

```kotlin
package com.github.y3knik.connectwithkia.ui.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SettingsScreen() { Text("Settings") }
```

- [ ] **Step 4: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/
git commit -m "feat(ui): add navigation scaffold and placeholder screens"
```

---

### Task 24: Credentials screen + ViewModel

**Files:**
- Replace: `app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/credentials/CredentialsScreen.kt`
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/credentials/CredentialsViewModel.kt`
- Create: `app/src/test/kotlin/com/github/y3knik/connectwithkia/ui/credentials/CredentialsViewModelTest.kt`

- [ ] **Step 1: Write the failing ViewModel test**

`app/src/test/kotlin/com/github/y3knik/connectwithkia/ui/credentials/CredentialsViewModelTest.kt`:

```kotlin
package com.github.y3knik.connectwithkia.ui.credentials

import app.cash.turbine.test
import com.github.y3knik.connectwithkia.data.CredentialsRepository
import com.github.y3knik.connectwithkia.data.InMemoryPreferences
import com.github.y3knik.connectwithkia.kia.KiaClient
import com.github.y3knik.connectwithkia.kia.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CredentialsViewModelTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private class FakeKia(var loginOk: Boolean, var vehicles: List<Vehicle>) : KiaClient {
        override suspend fun login(email: String, password: String) =
            if (loginOk) Result.success(Unit) else Result.failure(RuntimeException("bad creds"))
        override suspend fun vehicles() = Result.success(vehicles)
        override suspend fun lock(vehicleId: String, pin: String) = Result.success(Unit)
    }

    @Test
    fun `testConnection succeeds and persists email password pin vehicleId`() = runTest {
        val repo = CredentialsRepository(InMemoryPreferences())
        val kia = FakeKia(loginOk = true, vehicles = listOf(Vehicle("VID-1", "EV9", "VIN")))
        val vm = CredentialsViewModel(repo, kia)
        vm.onEmailChange("a@b.com")
        vm.onPasswordChange("pw")
        vm.onPinChange("1234")

        vm.testAndSave()

        vm.state.test {
            val first = awaitItem()
            assertTrue(first is CredentialsUiState.SavedSuccessfully || first is CredentialsUiState.Verifying)
            // Drain until success
            var state = first
            while (state !is CredentialsUiState.SavedSuccessfully) state = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val stored = repo.read()!!
        assertEquals("a@b.com", stored.email)
        assertEquals("pw", stored.password)
        assertEquals("1234", stored.pin)
        assertEquals("VID-1", repo.vehicleId)
    }

    @Test
    fun `testConnection emits Error on bad credentials`() = runTest {
        val repo = CredentialsRepository(InMemoryPreferences())
        val kia = FakeKia(loginOk = false, vehicles = emptyList())
        val vm = CredentialsViewModel(repo, kia)
        vm.onEmailChange("a@b.com")
        vm.onPasswordChange("wrong")
        vm.onPinChange("1234")

        vm.testAndSave()

        vm.state.test {
            var state = awaitItem()
            while (state !is CredentialsUiState.Error) state = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: Run the test**

```bash
./gradlew :app:testDebugUnitTest --tests "*CredentialsViewModel*"
```

Expected: FAIL — ViewModel unresolved.

- [ ] **Step 3: Create the ViewModel**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/credentials/CredentialsViewModel.kt`:

```kotlin
package com.github.y3knik.connectwithkia.ui.credentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.y3knik.connectwithkia.data.CredentialsRepository
import com.github.y3knik.connectwithkia.kia.KiaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CredentialsUiState {
    data class Editing(val email: String = "", val password: String = "", val pin: String = "") : CredentialsUiState
    data object Verifying : CredentialsUiState
    data object SavedSuccessfully : CredentialsUiState
    data class Error(val message: String) : CredentialsUiState
}

class CredentialsViewModel(
    private val repository: CredentialsRepository,
    private val kia: KiaClient,
) : ViewModel() {

    private var email: String = repository.read()?.email.orEmpty()
    private var password: String = repository.read()?.password.orEmpty()
    private var pin: String = repository.read()?.pin.orEmpty()

    private val _state = MutableStateFlow<CredentialsUiState>(CredentialsUiState.Editing(email, password, pin))
    val state: StateFlow<CredentialsUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) { email = value; emitEditing() }
    fun onPasswordChange(value: String) { password = value; emitEditing() }
    fun onPinChange(value: String) {
        pin = value.filter { it.isDigit() }.take(4)
        emitEditing()
    }
    private fun emitEditing() { _state.value = CredentialsUiState.Editing(email, password, pin) }

    fun testAndSave() {
        if (email.isBlank() || password.isBlank() || pin.length != 4) {
            _state.value = CredentialsUiState.Error("Fill all three fields. PIN must be 4 digits.")
            return
        }
        _state.value = CredentialsUiState.Verifying
        viewModelScope.launch {
            kia.login(email, password)
                .onFailure {
                    _state.value = CredentialsUiState.Error(it.message ?: "Login failed")
                    return@launch
                }
            val vehicles = kia.vehicles().getOrNull()
            if (vehicles.isNullOrEmpty()) {
                _state.value = CredentialsUiState.Error("No vehicles found on the account")
                return@launch
            }
            repository.write(email, password, pin)
            repository.vehicleId = vehicles.first().id
            _state.value = CredentialsUiState.SavedSuccessfully
        }
    }
}
```

- [ ] **Step 4: Replace placeholder CredentialsScreen**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/credentials/CredentialsScreen.kt`:

```kotlin
package com.github.y3knik.connectwithkia.ui.credentials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.KeyboardOptions
import com.github.y3knik.connectwithkia.di.AppContainer

@Composable
fun CredentialsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = AppContainer.get(context)
    val vm: CredentialsViewModel = viewModel(
        factory = androidx.lifecycle.viewmodel.initializer {
            CredentialsViewModel(container.credentials, container.kiaClient)
        }.let { init ->
            androidx.lifecycle.ViewModelProvider.Factory.from(init)
        },
    )
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val editing = (state as? CredentialsUiState.Editing) ?: CredentialsUiState.Editing()
        OutlinedTextField(
            value = editing.email, onValueChange = vm::onEmailChange,
            label = { Text("Kia Connect email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        OutlinedTextField(
            value = editing.password, onValueChange = vm::onPasswordChange,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
        )
        OutlinedTextField(
            value = editing.pin, onValueChange = vm::onPinChange,
            label = { Text("4-digit PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        Button(onClick = { vm.testAndSave() }, enabled = state !is CredentialsUiState.Verifying) {
            Text("Test and save")
        }
        when (val s = state) {
            is CredentialsUiState.Verifying -> CircularProgressIndicator()
            is CredentialsUiState.SavedSuccessfully -> Text("Saved.")
            is CredentialsUiState.Error -> Text(s.message)
            else -> Unit
        }
    }
}
```

- [ ] **Step 5: Run tests**

```bash
./gradlew :app:testDebugUnitTest --tests "*Credentials*"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/
git commit -m "feat(ui): add credentials screen and viewmodel with test-and-save"
```

---

### Task 25: Status screen + ViewModel

**Files:**
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/status/StatusViewModel.kt`
- Replace: `app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/status/StatusScreen.kt`
- Create: `app/src/test/kotlin/com/github/y3knik/connectwithkia/ui/status/StatusViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

`app/src/test/kotlin/com/github/y3knik/connectwithkia/ui/status/StatusViewModelTest.kt`:

```kotlin
package com.github.y3knik.connectwithkia.ui.status

import com.github.y3knik.connectwithkia.scheduler.LockState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import kotlin.test.assertEquals

class StatusViewModelTest {
    @Test
    fun `display text reflects state`() {
        val state = MutableStateFlow<LockState>(LockState.Idle)
        val vm = StatusViewModel(state, lockNow = {}, toggleEnabled = {}, isEnabled = { true })
        assertEquals("Idle — waiting for next drive", vm.displayText(LockState.Idle))
        assertEquals("Watching — Android Auto connected", vm.displayText(LockState.Connected))
        assertEquals("Watching — Android Auto disconnected", vm.displayText(LockState.PendingLock))
        assertEquals("Locking now…", vm.displayText(LockState.Locking))
        assertEquals("Last attempt: locked", vm.displayText(LockState.Done(success = true)))
        assertEquals("Last attempt failed: nope", vm.displayText(LockState.Done(success = false, reason = "nope")))
        assertEquals("Not configured — add credentials", vm.displayText(LockState.Disabled(configured = false)))
        assertEquals("Disabled (master toggle off)", vm.displayText(LockState.Disabled(configured = true)))
    }
}
```

- [ ] **Step 2: Run the test**

```bash
./gradlew :app:testDebugUnitTest --tests "*StatusViewModel*"
```

Expected: FAIL.

- [ ] **Step 3: Create StatusViewModel**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/status/StatusViewModel.kt`:

```kotlin
package com.github.y3knik.connectwithkia.ui.status

import androidx.lifecycle.ViewModel
import com.github.y3knik.connectwithkia.scheduler.LockState
import kotlinx.coroutines.flow.StateFlow

class StatusViewModel(
    val state: StateFlow<LockState>,
    val lockNow: () -> Unit,
    val toggleEnabled: () -> Unit,
    val isEnabled: () -> Boolean,
) : ViewModel() {

    fun displayText(state: LockState): String = when (state) {
        is LockState.Disabled -> if (state.configured) "Disabled (master toggle off)" else "Not configured — add credentials"
        LockState.Idle -> "Idle — waiting for next drive"
        LockState.Connected -> "Watching — Android Auto connected"
        LockState.PendingLock -> "Watching — Android Auto disconnected"
        LockState.Locking -> "Locking now…"
        is LockState.Done -> if (state.success) "Last attempt: locked" else "Last attempt failed: ${state.reason ?: "unknown"}"
    }
}
```

- [ ] **Step 4: Replace StatusScreen**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/status/StatusScreen.kt`:

```kotlin
package com.github.y3knik.connectwithkia.ui.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.y3knik.connectwithkia.di.AppContainer
import com.github.y3knik.connectwithkia.scheduler.LockEvent

@Composable
fun StatusScreen() {
    val container = AppContainer.get(LocalContext.current)
    var enabled by remember { mutableStateOf(container.settings.enabled) }
    val state by container.scheduler.state.collectAsState()
    val vm = remember {
        StatusViewModel(
            state = container.scheduler.state,
            lockNow = { container.scheduler.onEvent(LockEvent.AlarmFired) },
            toggleEnabled = {
                container.settings.enabled = !container.settings.enabled
                if (container.settings.enabled) container.scheduler.onEvent(LockEvent.MasterEnabled)
                else container.scheduler.onEvent(LockEvent.MasterDisabled)
            },
            isEnabled = { container.settings.enabled },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Enabled")
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    vm.toggleEnabled()
                },
            )
        }
        Text(vm.displayText(state))
        OutlinedButton(onClick = { vm.lockNow() }) { Text("Lock now (test)") }
    }
}
```

- [ ] **Step 5: Run tests + build**

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Expected: PASS + BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/
git commit -m "feat(ui): add status screen with master toggle and lock-now button"
```

---

### Task 26: Settings screen + ViewModel

**Files:**
- Create: `app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/settings/SettingsViewModel.kt`
- Replace: `app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: Create SettingsViewModel**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/settings/SettingsViewModel.kt`:

```kotlin
package com.github.y3knik.connectwithkia.ui.settings

import androidx.lifecycle.ViewModel
import com.github.y3knik.connectwithkia.data.AppSettings
import com.github.y3knik.connectwithkia.data.CredentialsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val highProminence: Boolean,
    val successNotification: Boolean,
    val lockDelayMinutes: Int,
)

class SettingsViewModel(
    private val settings: AppSettings,
    private val credentials: CredentialsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            highProminence = settings.highProminenceCountdown,
            successNotification = settings.successNotification,
            lockDelayMinutes = settings.lockDelayMinutes,
        ),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun setHighProminence(value: Boolean) {
        settings.highProminenceCountdown = value
        _state.value = _state.value.copy(highProminence = value)
    }

    fun setSuccessNotification(value: Boolean) {
        settings.successNotification = value
        _state.value = _state.value.copy(successNotification = value)
    }

    fun setDelay(value: Int) {
        settings.lockDelayMinutes = value
        _state.value = _state.value.copy(lockDelayMinutes = settings.lockDelayMinutes)
    }

    fun signOut() {
        credentials.clear()
    }
}
```

- [ ] **Step 2: Replace SettingsScreen**

`app/src/main/kotlin/com/github/y3knik/connectwithkia/ui/settings/SettingsScreen.kt`:

```kotlin
package com.github.y3knik.connectwithkia.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.y3knik.connectwithkia.di.AppContainer

@Composable
fun SettingsScreen() {
    val container = AppContainer.get(LocalContext.current)
    val vm = remember { SettingsViewModel(container.settings, container.credentials) }
    val s by vm.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Status-bar countdown")
            Switch(checked = s.highProminence, onCheckedChange = vm::setHighProminence)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Success notification")
            Switch(checked = s.successNotification, onCheckedChange = vm::setSuccessNotification)
        }
        Text("Lock delay: ${s.lockDelayMinutes} min")
        Slider(
            value = s.lockDelayMinutes.toFloat(),
            onValueChange = { vm.setDelay(it.toInt()) },
            valueRange = 1f..15f,
            steps = 13,
        )
        OutlinedButton(onClick = vm::signOut) { Text("Sign out") }
    }
}
```

- [ ] **Step 3: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/
git commit -m "feat(ui): add settings screen with toggles, delay slider, sign-out"
```

---

## Phase 8 — Wire detection to scheduler (Task 27)

### Task 27: Observe CarConnection from app process and feed scheduler

**Files:**
- Modify: `app/src/main/kotlin/com/github/y3knik/connectwithkia/ConnectWithKiaApp.kt`
- Modify: `app/src/main/kotlin/com/github/y3knik/connectwithkia/MainActivity.kt`

- [ ] **Step 0: Add runtime permission requests in MainActivity**

The manifest already declares `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, etc. (Task 4), but the user still needs to grant them at runtime on Android 12+. The simplest first-launch UX: request `POST_NOTIFICATIONS` via the activity result API and deep-link to system settings for `SCHEDULE_EXACT_ALARM` and battery optimization when they're missing.

Replace `MainActivity.kt`:

```kotlin
package com.github.y3knik.connectwithkia

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.github.y3knik.connectwithkia.ui.AppNavigation
import com.github.y3knik.connectwithkia.ui.theme.ConnectWithKiaTheme

class MainActivity : ComponentActivity() {

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result ignored; UI just rechecks on next resume */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        ensureExactAlarm()
        ensureBatteryOptExempt()
        setContent {
            ConnectWithKiaTheme {
                AppNavigation()
            }
        }
    }

    private fun ensureExactAlarm() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (am.canScheduleExactAlarms()) return
        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
    }

    private fun ensureBatteryOptExempt() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:$packageName")),
        )
    }
}
```

These prompts only appear when needed and are a no-op afterwards. They run on every launch as a cheap way to keep state correct; a future polish task can move them behind a banner on the Status screen.

- [ ] **Step 1: Wire CarConnection observation into Application onCreate**

Replace `ConnectWithKiaApp.kt`:

```kotlin
package com.github.y3knik.connectwithkia

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.github.y3knik.connectwithkia.detect.CarConnectionObserver
import com.github.y3knik.connectwithkia.detect.CarConnectionState
import com.github.y3knik.connectwithkia.di.AppContainer
import com.github.y3knik.connectwithkia.scheduler.LockEvent

class ConnectWithKiaApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.get(this)

        val observer = CarConnectionObserver(this)
        var lastState: CarConnectionState? = null
        observer.observe(ProcessLifecycleOwner.get()) { state ->
            if (state == lastState) return@observe
            lastState = state
            val event = when (state) {
                CarConnectionState.NOT_CONNECTED -> LockEvent.AaDisconnected
                CarConnectionState.PROJECTION, CarConnectionState.NATIVE -> LockEvent.AaConnected
            }
            container.scheduler.onEvent(event)
        }
    }
}
```

- [ ] **Step 2: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual smoke test on a real device**

Build, install, sign in:

```bash
./gradlew :app:installDebug
```

1. Open the app. Status screen says "Not configured — add credentials".
2. Tap **Credentials**. Enter email/password/PIN. Tap **Test and save**. Verify it succeeds (returns to Editing with a "Saved." message).
3. Tap **Status**. State should now reflect AA state (likely "Idle — waiting for next drive" if not in the car).
4. Connect phone to EV9 via Android Auto. Status should change to "Watching — Android Auto connected" within a few seconds.
5. Park and disconnect AA. Status should change to "Watching — Android Auto disconnected". The countdown notification should appear.
6. Wait the full delay (or tap **Lock now (test)** to bypass) and observe a success notification + the car physically locking.

If any step fails, capture `adb logcat -s ConnectWithKia:* AndroidRuntime:E` output and debug before continuing.

- [ ] **Step 4: Commit**

```bash
git add app/
git commit -m "feat(app): wire CarConnection observer to scheduler in Application"
```

---

## Phase 9 — Polish (Tasks 28–30)

### Task 28: Release pipeline (signed APK on tag)

**Files:**
- Create: `.github/workflows/release.yml`

- [ ] **Step 1: Create release workflow**

`.github/workflows/release.yml`:

```yaml
name: release

on:
  push:
    tags:
      - 'v*.*.*'

permissions:
  contents: write

jobs:
  build-signed-apk:
    runs-on: ubuntu-latest
    timeout-minutes: 25
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - uses: gradle/actions/setup-gradle@v4

      - name: Decode keystore
        env:
          KEYSTORE_B64: ${{ secrets.SIGNING_KEYSTORE_BASE64 }}
        run: |
          echo "$KEYSTORE_B64" | base64 -d > app/release.keystore

      - name: Build release APK
        env:
          SIGNING_KEY_ALIAS: ${{ secrets.SIGNING_KEY_ALIAS }}
          SIGNING_KEY_PASSWORD: ${{ secrets.SIGNING_KEY_PASSWORD }}
          SIGNING_STORE_PASSWORD: ${{ secrets.SIGNING_STORE_PASSWORD }}
        run: |
          chmod +x ./gradlew
          ./gradlew :app:assembleRelease --stacktrace

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          files: app/build/outputs/apk/release/app-release.apk
```

- [ ] **Step 2: Wire signing config into `app/build.gradle.kts`**

Add inside the `android { }` block, before `buildTypes`:

```kotlin
    signingConfigs {
        create("release") {
            val keystore = file("release.keystore")
            if (keystore.exists()) {
                storeFile = keystore
                keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: "release"
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: ""
                storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: ""
            }
        }
    }
```

- [ ] **Step 3: Document the secrets in README**

Append to `README.md`:

```markdown
## Signed releases

The `release` workflow signs the APK using these repository secrets (Settings → Secrets and variables → Actions):

- `SIGNING_KEYSTORE_BASE64` — `base64` of an Android signing keystore
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`
- `SIGNING_STORE_PASSWORD`

Create the keystore once locally:

```bash
keytool -genkey -v -keystore release.keystore -alias release \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w 0 release.keystore > release.keystore.b64
```

Copy the contents of `release.keystore.b64` into the `SIGNING_KEYSTORE_BASE64` secret. Tag a release with `git tag v0.1.0 && git push --tags` to trigger the build.
```

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/release.yml app/build.gradle.kts README.md
git commit -m "ci: add signed release workflow on tag push"
```

---

### Task 29: CodeRabbit + Dependabot + PR template

**Files:**
- Create: `.coderabbit.yaml`
- Create: `.github/dependabot.yml`
- Create: `.github/pull_request_template.md`

- [ ] **Step 1: CodeRabbit config**

`.coderabbit.yaml`:

```yaml
language: en
reviews:
  profile: chill
  auto_review:
    enabled: true
    drafts: false
  poem: false
  request_changes_workflow: false
  high_level_summary: true
  review_status: true
  path_filters:
    - "!**/build/**"
    - "!**/generated/**"
    - "!gradle/wrapper/**"
    - "!**/*.png"
    - "!**/*.jpg"
chat:
  auto_reply: true
```

- [ ] **Step 2: Dependabot config**

`.github/dependabot.yml`:

```yaml
version: 2
updates:
  - package-ecosystem: gradle
    directory: "/"
    schedule:
      interval: weekly
    groups:
      kotlin-and-compose:
        patterns:
          - "org.jetbrains.kotlin*"
          - "androidx.compose*"
      androidx:
        patterns:
          - "androidx.*"
      networking:
        patterns:
          - "com.squareup.*"
  - package-ecosystem: github-actions
    directory: "/"
    schedule:
      interval: weekly
```

- [ ] **Step 3: PR template**

`.github/pull_request_template.md`:

```markdown
## What

<!-- one-line summary -->

## Why

<!-- motivation and context -->

## Verification

- [ ] Unit tests added or updated
- [ ] Manually verified on device (state, screen, or flow):
- [ ] CI green
```

- [ ] **Step 4: Commit**

```bash
git add .coderabbit.yaml .github/dependabot.yml .github/pull_request_template.md
git commit -m "ci: add coderabbit, dependabot, pr template"
```

---

### Task 30: Update spec status, open PR

**Files:**
- Modify: `docs/superpowers/specs/2026-06-12-connectwithkia-design.md` (status line)

- [ ] **Step 1: Update spec status to "Implemented"**

In `docs/superpowers/specs/2026-06-12-connectwithkia-design.md`, change:

```markdown
**Status:** Draft (pending review)
```

to:

```markdown
**Status:** Implemented
```

- [ ] **Step 2: Push and open PR**

```bash
git add docs/
git commit -m "docs: mark design spec as implemented"
git push -u origin feat/walk-away-lock
gh pr create \
  --base main \
  --head feat/walk-away-lock \
  --title "feat: walk-away lock app v0.1" \
  --body "$(cat <<'EOF'
## Summary
- Implements the walk-away lock feature described in `docs/superpowers/specs/2026-06-12-connectwithkia-design.md`.
- Adds `:kia` (pure-Kotlin Kia Connect Canada client) and `:app` (UI + service + scheduler).
- Adds CI, signed release, Dependabot, CodeRabbit, and PR template.

## Test plan
- [ ] CI green on this PR
- [ ] Sideload the debug APK on phone, complete onboarding
- [ ] Verify state transitions on Android Auto connect/disconnect
- [ ] Verify lock fires after delay
- [ ] Verify cancel from notification works
- [ ] Verify cancel by AA reconnect works
EOF
)"
```

- [ ] **Step 3: Verify CI + CodeRabbit run on the PR**

Open the PR in the browser. Confirm:
- `ci / build-and-test` succeeds and uploads `app-debug-apk`.
- CodeRabbit posts a summary comment within a few minutes.

- [ ] **Step 4: Merge after addressing CodeRabbit feedback**

Address any actionable comments from CodeRabbit, push updates, then squash-merge to `main`.

---

## Done

After Task 30 merges, the repo on `main`:

- builds a debug APK on every PR (downloadable from Actions)
- builds a signed release APK on every `v*.*.*` tag (attached to a GitHub Release)
- is reviewed by CodeRabbit on every PR
- is kept current by Dependabot

The app on your phone:

- watches Android Auto state via `CarConnection`
- arms a 5-minute (configurable) alarm on AA disconnect
- shows a cancellable countdown notification while pending
- cancels automatically if AA reconnects
- locks the EV9 via `kiaconnect.ca` when the alarm fires
- shows success/failure notifications according to your settings
- survives phone reboot by re-arming the pending alarm
