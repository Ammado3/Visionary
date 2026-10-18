I cannot generate a direct file download link in this chat interface, but you can click the **Copy** icon in the top-right corner of the code block below and save it directly as **`ARCHITECTURE.md`** in your repository root.

This updated version includes **Section 3.3 & Section 3.4**, which explicitly map out every single `expect`/`actual` file, its exact module and source-set file path (`commonMain`, `androidMain`, `iosMain`), and its complete implementation code, alongside the Domain Port Interfaces vs. `expect/actual` boundary rules.

```markdown
# ARCHITECTURE.md — Visionary KMP/CMP Base Architecture Specification

> **Instructions for the AI Agent:** Read this specification completely before creating or modifying any files. Your objective is to scaffold and implement the complete, compiling Kotlin Multiplatform (KMP) and Compose Multiplatform (CMP) base architecture for **Visionary**, including `build-logic` convention plugins, multi-module structure, explicit `expect`/`actual` placements, Room 3 KMP database with FTS5, Koin DI, MVI base classes, Navigation 3 routing, Domain contracts, Fake repositories, and feature skeletons.

---

## 1. Project Identity & Core Technical Constraints

- **App Name:** Visionary
- **Root Package:** `com.o3.vanguard.visionary`
- **Architecture:** Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP) using Clean Architecture (Ports & Adapters) and strict MVI (`UiState`, `UiIntent`, `UiEvent`).
- **Target Strategy:**
  - Every KMP module must configure and compile for both **Android** (`androidLibrary`) and **iOS** (`iosArm64`, `iosSimulatorArm64`).
  - **Android** (`minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`) is the primary runtime target for the MVP.
  - **iOS** adapters in `:platform` (`iosMain`) must provide compile-safe, non-crashing stubs that return `AppResult.Err(AppError.Unsupported("OS_NOT_SUPPORTED_YET"))` or safe no-ops. **Never** use `TODO()` in `iosMain` code.
- **Zero-Network Policy:** The application must never request network access. Strip `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE` from `:androidApp` using `tools:node="remove"`. Do not add any HTTP client, analytics, or crash-reporting dependencies.
- **Strict Code Hygiene:**
  - No `!!`, no `GlobalScope`, no hardcoded `Dispatchers.IO` or `Dispatchers.Default` (always inject `AppDispatchers`).
  - No user content, OCR text, file names, or URIs in logs.
  - All user-facing strings must reside in Compose Multiplatform resources (`composeResources/values/strings.xml`).

---

## 2. Version Catalog (`gradle/libs.versions.toml`)

Use the single source of truth in `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.4.21"
composeMultiplatform = "1.12.2"
agp = "9.4.0"
ksp = "2.3.11"
coroutines = "1.11.0"
serialization = "1.8.0"
koin = "4.2.2"
room3 = "3.0.3"
sqlite = "2.7.1"
coil = "3.6.3"
navigation3 = "1.1.1"
lifecycle = "2.10.0"
mlkitTextRecognition = "16.0.1"
mlkitImageLabeling = "17.0.9"
mlkitDocScanner = "16.0.0-beta1"
biometric = "1.2.0-alpha05"
workManager = "2.10.0"

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }

koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }

room3-runtime = { module = "androidx.room3:room3-runtime", version.ref = "room3" }
room3-compiler = { module = "androidx.room3:room3-compiler", version.ref = "room3" }
sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }

nav3-ui = { module = "org.jetbrains.androidx.navigation3:navigation3-ui", version.ref = "navigation3" }
lifecycle-viewmodel = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel", version.ref = "lifecycle" }
lifecycle-runtime-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-nav3 = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycle" }

coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }

mlkit-text-recognition = { module = "com.google.mlkit:text-recognition", version.ref = "mlkitTextRecognition" }
mlkit-image-labeling = { module = "com.google.mlkit:image-labeling", version.ref = "mlkitImageLabeling" }
mlkit-document-scanner = { module = "com.google.android.gms:play-services-mlkit-document-scanner", version.ref = "mlkitDocScanner" }
androidx-biometric = { module = "androidx.biometric:biometric", version.ref = "biometric" }
androidx-work-runtime = { module = "androidx.work:work-runtime-ktx", version.ref = "workManager" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-kmp-library = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
room3 = { id = "androidx.room3", version.ref = "room3" }

```

---

## 3. Multi-Module Graph, Dependency Rules & Exact `expect`/`actual` Placement

### 3.1 Directory & Module Structure (`settings.gradle.kts`)

```text
visionary/
├── build-logic/                 # Included build for convention plugins
├── gradle/libs.versions.toml
├── androidApp/                  # Android Application module (MainActivity, VisionaryApp, Manifest)
├── iosApp/                      # iOS entry point wrapper
├── composeApp/                  # Shared KMP root library: App(), AppNavHost, Koin aggregation
├── core/
│   ├── common/                  # Pure Kotlin: AppResult, AppError, AppDispatchers, AppConfig, expect/actual primitives
│   ├── mvi/                     # BaseMviViewModel, UiState, UiIntent, UiEvent, CollectEvents
│   ├── navigation/              # @Serializable AppRoute (NavKey), Navigator, SavedStateConfiguration
│   ├── designsystem/            # CMP Theme, Tokens, Reusable UI Components, Fake Coil Fetcher
│   ├── database/                # Room 3 Database, Entities, DAOs, FTS5 Triggers, expect/actual Database Builder
│   └── testing/                 # Fake* Repositories & Ports, FakeData, FakeControls
├── domain/                      # Pure Kotlin: Models, Repository & Port Interfaces, UseCases
├── data/                        # Real Repository Implementations & Entity-Domain Mappers
├── platform/                    # AndroidMain & iosMain implementations of Domain Ports + expect/actual platformModule
└── features/
    ├── onboarding/
    ├── home/
    ├── gallery/
    ├── search/
    ├── scanner/
    ├── vault/
    └── settings/

```

```kotlin
// settings.gradle.kts
pluginManagement {
    includeBuild("build-logic")
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "visionary"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":androidApp", ":composeApp")
include(":core:common", ":core:mvi", ":core:navigation", ":core:designsystem", ":core:database", ":core:testing")
include(":domain", ":data", ":platform")
include(
    ":features:onboarding", ":features:home", ":features:gallery",
    ":features:search", ":features:scanner", ":features:vault", ":features:settings",
)

```

### 3.2 Allowed Dependency Matrix

| Module | Allowed Module Dependencies | Allowed External Libraries |
| --- | --- | --- |
| `:core:common` | None | `kotlinx-coroutines-core` (Pure Kotlin only — no Compose, no Lifecycle, no Android) |
| `:core:mvi` | `:core:common` | `lifecycle-viewmodel`, `lifecycle-runtime-compose`, `kotlinx-coroutines-core` |
| `:core:navigation` | None | `nav3-ui`, `kotlinx-serialization-json` (No `:domain` or `:features:*` dependency) |
| `:core:designsystem` | `:core:common` | Compose Multiplatform, `coil-compose` (No `:domain` dependency) |
| `:core:database` | `:core:common` | `room3-runtime`, `sqlite-bundled`, `kotlinx-serialization-json`, `koin-core` |
| `:domain` | `:core:common` | `kotlinx-coroutines-core`, `koin-core` (Pure Kotlin only — zero Android/iOS/Room/Compose imports) |
| `:data` | `:domain`, `:core:common`, `:core:database` | `kotlinx-coroutines-core`, `koin-core`, `kotlinx-serialization-json` |
| `:platform` | `:domain`, `:core:common` | `koin-core`, Android SDK, ML Kit, Biometric, WorkManager (`androidMain` only) |
| `:core:testing` | `:domain`, `:core:common` | `kotlinx-coroutines-core`, `koin-core` |
| `:features:*` | `:domain`, `:core:common`, `:core:mvi`, `:core:navigation`, `:core:designsystem` | Compose Multiplatform, `koin-compose`, `koin-compose-viewmodel`, `lifecycle-runtime-compose` |
| `:composeApp` | All modules (wiring only) | Compose Multiplatform, Koin, `lifecycle-viewmodel-nav3` |
| `:androidApp` | `:composeApp`, `:core:common`, `:platform` | `koin-android`, AndroidX Activity Compose |

**Forbidden Dependencies:**

* `:features:*` must **never** depend on another `:features:*` module, `:data`, `:platform`, `:core:database`, or `:core:testing`.
* `:domain` must **never** import platform classes (`android.*`, `platform.UIKit.*`), Room annotations, or Compose annotations.

---

### 3.3 Strict `expect`/`actual` Policy & Complete File Placement Map

**Architectural Rule:**

1. **Stateful / Asynchronous / Lifecycle-Bound Platform Capabilities MUST NOT use `expect class`.** Instead, declare a pure Kotlin **Interface (Port)** in `:domain/src/commonMain/.../port/`, implement the Android version in `:platform/src/androidMain/`, implement the safe iOS stub in `:platform/src/iosMain/`, and bind them inside `:platform`'s `expect val platformModule: Module`.
2. **`expect`/`actual` is strictly restricted to the 5 stateless/wiring locations listed in the table below.** Never create `expect`/`actual` declarations in `:domain`, `:data`, or `:features:*`.

| # | `expect` Declaration | Module | `commonMain` File Path | `androidMain` File Path | `iosMain` File Path |
| --- | --- | --- | --- | --- | --- |
| **1** | `expect fun randomUuid(): String`<br>

<br>`expect fun currentTimeMillis(): Long` | `:core:common` | `core/common/src/commonMain/kotlin/com/o3/vanguard/visionary/core/common/PlatformPrimitives.kt` | `core/common/src/androidMain/kotlin/com/o3/vanguard/visionary/core/common/PlatformPrimitives.android.kt` | `core/common/src/iosMain/kotlin/com/o3/vanguard/visionary/core/common/PlatformPrimitives.ios.kt` |
| **2** | `expect object AppDatabaseConstructor`<br>

<br>`expect val databasePlatformModule: Module` | `:core:database` | `core/database/src/commonMain/kotlin/com/o3/vanguard/visionary/core/database/DatabaseBuilder.kt` | `core/database/src/androidMain/kotlin/com/o3/vanguard/visionary/core/database/DatabaseBuilder.android.kt` | `core/database/src/iosMain/kotlin/com/o3/vanguard/visionary/core/database/DatabaseBuilder.ios.kt` |
| **3** | `expect class AppDirectories` | `:platform` | `platform/src/commonMain/kotlin/com/o3/vanguard/visionary/platform/AppDirectories.kt` | `platform/src/androidMain/kotlin/com/o3/vanguard/visionary/platform/AppDirectories.android.kt` | `platform/src/iosMain/kotlin/com/o3/vanguard/visionary/platform/AppDirectories.ios.kt` |
| **4** | `expect val platformModule: Module` | `:platform` | `platform/src/commonMain/kotlin/com/o3/vanguard/visionary/platform/di/PlatformModule.kt` | `platform/src/androidMain/kotlin/com/o3/vanguard/visionary/platform/di/PlatformModule.android.kt` | `platform/src/iosMain/kotlin/com/o3/vanguard/visionary/platform/di/PlatformModule.ios.kt` |

---

### 3.4 Exact Code for All `expect`/`actual` Files

#### Placement 1: `:core:common` — Stateless Primitives

```kotlin
// FILE: core/common/src/commonMain/kotlin/com/o3/vanguard/visionary/core/common/PlatformPrimitives.kt
package com.o3.vanguard.visionary.core.common

expect fun randomUuid(): String
expect fun currentTimeMillis(): Long

```

```kotlin
// FILE: core/common/src/androidMain/kotlin/com/o3/vanguard/visionary/core/common/PlatformPrimitives.android.kt
package com.o3.vanguard.visionary.core.common

import java.util.UUID

actual fun randomUuid(): String = UUID.randomUUID().toString()
actual fun currentTimeMillis(): Long = System.currentTimeMillis()

```

```kotlin
// FILE: core/common/src/iosMain/kotlin/com/o3/vanguard/visionary/core/common/PlatformPrimitives.ios.kt
package com.o3.vanguard.visionary.core.common

import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970

actual fun randomUuid(): String = NSUUID().UUIDString()
actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

```

#### Placement 2: `:core:database` — Room 3 Database Constructor & Platform Builder Module

```kotlin
// FILE: core/database/src/commonMain/kotlin/com/o3/vanguard/visionary/core/database/DatabaseBuilder.kt
package com.o3.vanguard.visionary.core.database

import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.o3.vanguard.visionary.core.common.AppDispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

expect val databasePlatformModule: Module

fun buildAppDatabase(
    builder: RoomDatabase.Builder<AppDatabase>,
    dispatchers: AppDispatchers,
): AppDatabase = builder
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(dispatchers.io)
    .addCallback(FtsCallback)
    .build()

val databaseModule = module {
    includes(databasePlatformModule)
    single<AppDatabase> { buildAppDatabase(get(), get()) }
    single { get<AppDatabase>().mediaDao() }
    single { get<AppDatabase>().searchDao() }
    single { get<AppDatabase>().indexJobDao() }
    single { get<AppDatabase>().vaultDao() }
}

```

```kotlin
// FILE: core/database/src/androidMain/kotlin/com/o3/vanguard/visionary/core/database/DatabaseBuilder.android.kt
package com.o3.vanguard.visionary.core.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

fun getAndroidDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("visionary.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}

actual val databasePlatformModule: Module = module {
    single<RoomDatabase.Builder<AppDatabase>> { getAndroidDatabaseBuilder(get()) }
}

```

```kotlin
// FILE: core/database/src/iosMain/kotlin/com/o3/vanguard/visionary/core/database/DatabaseBuilder.ios.kt
package com.o3.vanguard.visionary.core.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun getIosDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val docDir = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    val dbPath = requireNotNull(docDir?.path) + "/visionary.db"
    return Room.databaseBuilder<AppDatabase>(name = dbPath)
}

actual val databasePlatformModule: Module = module {
    single<RoomDatabase.Builder<AppDatabase>> { getIosDatabaseBuilder() }
}

```

#### Placement 3: `:platform` — App-Private Directories (`scans` & `vault_blobs`)

```kotlin
// FILE: platform/src/commonMain/kotlin/com/o3/vanguard/visionary/platform/AppDirectories.kt
package com.o3.vanguard.visionary.platform

expect class AppDirectories {
    fun vaultBlobsDirPath(): String
    fun scansDirPath(): String
    fun cacheDirPath(): String
}

```

```kotlin
// FILE: platform/src/androidMain/kotlin/com/o3/vanguard/visionary/platform/AppDirectories.android.kt
package com.o3.vanguard.visionary.platform

import android.content.Context
import java.io.File

actual class AppDirectories(private val context: Context) {
    actual fun vaultBlobsDirPath(): String =
        File(context.filesDir, "vault_blobs").apply { mkdirs() }.absolutePath

    actual fun scansDirPath(): String =
        File(context.filesDir, "scans").apply { mkdirs() }.absolutePath

    actual fun cacheDirPath(): String =
        context.cacheDir.absolutePath
}

```

```kotlin
// FILE: platform/src/iosMain/kotlin/com/o3/vanguard/visionary/platform/AppDirectories.ios.kt
package com.o3.vanguard.visionary.platform

import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class AppDirectories {
    private fun docRoot(): String {
        val url = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        return requireNotNull(url?.path)
    }

    actual fun vaultBlobsDirPath(): String = "${docRoot()}/vault_blobs"
    actual fun scansDirPath(): String = "${docRoot()}/scans"
    actual fun cacheDirPath(): String {
        val url = NSFileManager.defaultManager.URLForDirectory(
            directory = NSCachesDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        return requireNotNull(url?.path)
    }
}

```

#### Placement 4: `:platform` — Koin `platformModule` Binding All Domain Ports

```kotlin
// FILE: platform/src/commonMain/kotlin/com/o3/vanguard/visionary/platform/di/PlatformModule.kt
package com.o3.vanguard.visionary.platform.di

import org.koin.core.module.Module

expect val platformModule: Module

```

```kotlin
// FILE: platform/src/androidMain/kotlin/com/o3/vanguard/visionary/platform/di/PlatformModule.android.kt
package com.o3.vanguard.visionary.platform.di

import com.o3.vanguard.visionary.domain.port.*
import com.o3.vanguard.visionary.platform.AppDirectories
import com.o3.vanguard.visionary.platform.android.*
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { AppDirectories(get()) }
    single<PermissionGateway> { AndroidPermissionGateway(get()) }
    single<MediaSourceEngine> { AndroidMediaStoreSource(get()) }
    single<MediaDeleteGateway> { AndroidMediaDeleteGateway(get()) }
    single<OcrEngine> { MlKitOcrEngine(get()) }
    single<ImageLabelEngine> { MlKitImageLabelEngine(get()) }
    single<ScannerPort> { MlKitDocumentScannerAdapter(get(), get()) }
    single<BiometricAuthenticator> { AndroidBiometricAuthenticator(get()) }
    single<VaultCryptoEngine> { AndroidVaultCryptoEngine(get()) }
    single<SecureScreenPort> { AndroidSecureScreenPort(get()) }
    single<BackgroundSyncScheduler> { WorkManagerSyncScheduler(get()) }
}

```

```kotlin
// FILE: platform/src/iosMain/kotlin/com/o3/vanguard/visionary/platform/di/PlatformModule.ios.kt
package com.o3.vanguard.visionary.platform.di

import com.o3.vanguard.visionary.domain.port.*
import com.o3.vanguard.visionary.platform.AppDirectories
import com.o3.vanguard.visionary.platform.ios.*
import org.koin.core.module.Module
import org.koin.dsl.module

// Non-crashing iOS stubs returning AppResult.Err(AppError.Unsupported("OS_NOT_SUPPORTED_YET"))
actual val platformModule: Module = module {
    single { AppDirectories() }
    single<PermissionGateway> { IosStubPermissionGateway() }
    single<MediaSourceEngine> { IosStubMediaSourceEngine() }
    single<MediaDeleteGateway> { IosStubMediaDeleteGateway() }
    single<OcrEngine> { IosStubOcrEngine() }
    single<ImageLabelEngine> { IosStubImageLabelEngine() }
    single<ScannerPort> { IosStubScannerPort() }
    single<BiometricAuthenticator> { IosStubBiometricAuthenticator() }
    single<VaultCryptoEngine> { IosStubVaultCryptoEngine() }
    single<SecureScreenPort> { IosStubSecureScreenPort() }
    single<BackgroundSyncScheduler> { IosStubBackgroundSyncScheduler() }
}

```

---

## 4. Convention Plugins (`build-logic/`)

Create four convention plugins inside `build-logic/`:

1. **`visionary.kmp.library`**:
* Applies `org.jetbrains.kotlin.multiplatform` and `com.android.kotlin.multiplatform.library`.
* Configures `androidLibrary { compileSdk = 36; minSdk = 26 }`, `iosArm64()`, and `iosSimulatorArm64()`.
* Enables `allWarningsAsErrors.set(true)`.


2. **`visionary.kmp.compose`**:
* Applies `visionary.kmp.library`, `org.jetbrains.compose`, and `org.jetbrains.kotlin.plugin.compose`.


3. **`visionary.kmp.feature`**:
* Applies `visionary.kmp.compose`.
* Automatically adds `commonMain.dependencies` for `:domain`, `:core:common`, `:core:mvi`, `:core:navigation`, `:core:designsystem`, `koin-compose`, `koin-compose-viewmodel`, and `lifecycle-runtime-compose`.
* Every feature module's `build.gradle.kts` requires only:
```kotlin
plugins { id("visionary.kmp.feature") }
kotlin { androidLibrary { namespace = "com.o3.vanguard.visionary.features.<name>" } }

```




4. **`visionary.android.application`**:
* Applies `com.android.application`, `org.jetbrains.compose`, and `org.jetbrains.kotlin.plugin.compose` for `:androidApp`.



---

## 5. Core Foundations (`:core:common` & `:core:mvi`)

### 5.1 `:core:common` Contracts

```kotlin
package com.o3.vanguard.visionary.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module

sealed interface AppError {
    data class Unsupported(val reason: String) : AppError
    data object PermissionDenied : AppError
    data object NotFound : AppError
    data object Cancelled : AppError
    data class Io(val kind: String) : AppError
    data class Crypto(val kind: String) : AppError
    data class Unknown(val kind: String) : AppError
}

sealed interface AppResult<out T> {
    data class Ok<T>(val value: T) : AppResult<T>
    data class Err(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Ok -> AppResult.Ok(transform(value))
    is AppResult.Err -> this
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Ok)?.value

interface AppDispatchers {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}

class DefaultAppDispatchers : AppDispatchers {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main
}

data class AppConfig(
    val versionName: String,
    val isDebug: Boolean,
    val useFakeData: Boolean,
)

val coreCommonModule = module {
    single<AppDispatchers> { DefaultAppDispatchers() }
}

```

### 5.2 `:core:mvi` Base ViewModel & Event Collector

```kotlin
package com.o3.vanguard.visionary.core.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

interface UiState
interface UiIntent
interface UiEvent

abstract class BaseMviViewModel<S : E I UiEvent UiIntent, UiState,>(
    initialState: S,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _events = Channel<E>(Channel.UNLIMITED)
    val events: Flow<E> = _events.receiveAsFlow()

    protected fun updateState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    protected fun sendEvent(event: E) {
        _events.trySend(event)
    }

    abstract fun onIntent(intent: I)
}

@Composable
fun <E : UiEvent> CollectEvents(events: Flow<E>, onEvent: (E) -> Unit) {
    val current by rememberUpdatedState(onEvent)
    LaunchedEffect(events) {
        events.collect { current(it) }
    }
}

```

---

## 6. Domain Layer Contracts (`:domain`)

All classes in `:domain` are pure Kotlin in `commonMain`.

### 6.1 Domain Models (`:domain/model`)

```kotlin
package com.o3.vanguard.visionary.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class MediaId(val value: Long)

enum class MediaOrigin { LIBRARY, APP_SCAN }

enum class MediaCategory { PHOTO, SCREENSHOT, DOCUMENT, RECEIPT, SCAN }

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class OcrTextLine(
    val text: String,
    val boundingBox: NormalizedRect,
    val confidence: Float,
)

data class OcrResult(
    val fullText: String,
    val lines: List<OcrTextLine>,
)

enum class SmartEntityType(val flagBit: Int) {
    PHONE(1 shl 0),
    EMAIL(1 shl 1),
    URL(1 shl 2),
    AMOUNT(1 shl 3),
    WIFI_PASSWORD(1 shl 4),
    TRACKING_CODE(1 shl 5),
    OTP(1 shl 6),
}

data class SmartEntity(
    val type: SmartEntityType,
    val rawValue: String,
    val displayLabel: String,
)

data class MediaItem(
    val id: MediaId,
    val uri: String,
    val origin: MediaOrigin,
    val category: MediaCategory,
    val takenAtMs: Long,
    val width: Int,
    val height: Int,
    val textPreview: String?,
    val entityFlags: Int = 0,
)

enum class CollectionKind(val id: String) {
    ALL("all"),
    SCREENSHOTS("screenshots"),
    DOCUMENTS("documents"),
    RECEIPTS("receipts"),
    SCANS("scans");

    companion object {
        fun fromId(id: String): CollectionKind = entries.firstOrNull { it.id == id } ?: ALL
    }
}

data class CollectionSummary(
    val kind: CollectionKind,
    val count: Int,
    val coverUri: String?,
)

enum class SearchFilter { ALL, SCREENSHOTS, RECEIPTS, DOCUMENTS, SCANS, HAS_PHONE_OR_URL }

data class SearchHit(
    val item: MediaItem,
    val snippet: String,
    val matchedTerms: List<String>,
    val entities: List<SmartEntity> = emptyList(),
)

enum class IndexState { IDLE, RUNNING, PAUSED_BATTERY, NEEDS_ACCESS }

data class IndexProgress(
    val total: Int,
    val indexed: Int,
    val pending: Int,
    val failed: Int,
    val state: IndexState,
)

sealed interface MediaAccess {
    data object Full : MediaAccess
    data class Partial(val count: Int) : MediaAccess
    data object Denied : MediaAccess
}

sealed interface DeleteConsentResult {
    data object Deleted : DeleteConsentResult
    data object DeclinedByUser : DeleteConsentResult
    data class NotPossible(val reason: String) : DeleteConsentResult
}

data class VaultItem(
    val id: String,
    val createdAtMs: Long,
    val width: Int,
    val height: Int,
)

data class ScannedDocument(
    val pageUris: List<String>,
    val pdfUri: String?,
)

data class AppSettings(
    val onboardingDone: Boolean = false,
    val indexOnlyWhileCharging: Boolean = false,
    val vaultLockTimeoutSeconds: Int = 30,
)

```

### 6.2 Repository Interfaces (`:domain/repository`)

```kotlin
package com.o3.vanguard.visionary.domain.repository

import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MediaRepository {
    fun observeCollections(): Flow<List<CollectionSummary>>
    fun observeMedia(kind: CollectionKind): Flow<List<MediaItem>>
    fun observeItem(id: MediaId): Flow<MediaItem?>
    fun observeOcrLines(id: MediaId): Flow<List<OcrTextLine>>
    fun observeRecentFinds(limit: Int): Flow<List<MediaItem>>
}

interface SearchRepository {
    fun search(query: String, filter: SearchFilter): Flow<List<SearchHit>>
    fun observeRecentSearches(): Flow<List<String>>
    suspend fun saveRecentSearch(query: String)
}

interface IndexRepository {
    fun observeProgress(): Flow<IndexProgress>
    suspend fun retryFailed(): AppResult<Unit>
    suspend fun rebuild(): AppResult<Unit>
}

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

interface VaultRepository {
    val isUnlocked: StateFlow<Boolean>
    fun observeItems(): Flow<List<VaultItem>>
    suspend fun unlock(): AppResult<Unit>
    fun lock()
    suspend fun moveToVault(id: MediaId): AppResult<Unit>
    suspend fun restore(itemId: String): AppResult<Unit>
    suspend fun delete(itemId: String): AppResult<Unit>
}

```

### 6.3 Platform Port Interfaces (`:domain/port`)

```kotlin
package com.o3.vanguard.visionary.domain.port

import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.*
import kotlinx.coroutines.flow.Flow

interface PermissionGateway {
    fun observeAccess(): Flow<MediaAccess>
    suspend fun requestAccess(): MediaAccess
}

interface MediaSourceEngine {
    suspend fun queryDelta(sinceCheckpoint: Long): AppResult<List<MediaItem>>
    suspend fun queryAllLibraryIds(): AppResult<Set<Long>>
}

interface MediaDeleteGateway {
    suspend fun requestDelete(uris: List<String>): DeleteConsentResult
}

interface OcrEngine {
    suspend fun recognizeText(uri: String): AppResult<OcrResult>
}

interface ImageLabelEngine {
    suspend fun labelImage(uri: String): AppResult<List<String>>
}

interface ScannerPort {
    suspend fun isAvailable(): Boolean
    suspend fun scan(): AppResult<ScannedDocument>
}

interface BiometricAuthenticator {
    suspend fun authenticate(): AppResult<Unit>
}

interface VaultCryptoEngine {
    suspend fun unlockSession(): AppResult<Unit>
    fun lockSession()
    suspend fun encryptToVault(sourceUri: String, itemId: String): AppResult<Unit>
    suspend fun decryptFromVault(itemId: String, thumbnail: Boolean): AppResult<ByteArray>
    suspend fun deleteVaultBlobs(itemId: String): AppResult<Unit>
}

interface SecureScreenPort {
    fun setSecureScreen(enabled: Boolean)
}

interface BackgroundSyncScheduler {
    fun scheduleIncrementalIndexing(chargingOnly: Boolean)
}

```

### 6.4 Domain UseCases & DI (`:domain/usecase` & `:domain/di`)

Create single-responsibility use cases (`ObserveCollectionsUseCase`, `ObserveRecentFindsUseCase`, `ObserveIndexProgressUseCase`, `ObserveMediaUseCase`, `SearchMediaUseCase`, `ResolveStartRouteUseCase`, `MoveToVaultUseCase`, `StartScanUseCase`, `SmartMediaClassifier`, `EntityExtractor`) and register them in `val domainModule = module { ... }`.

---

## 7. Database Layer Setup (`:core:database`)

### 7.1 Entities (`:core:database/entity`)

```kotlin
package com.o3.vanguard.visionary.core.database.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "media_item",
    indices = [Index("sortKey"), Index("category"), Index("ref", unique = true)]
)
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ref: String,
    val origin: String,
    val category: String,
    val sortKey: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val isScreenshot: Boolean,
    val entityFlags: Int,
    val fingerprint: String,
)

@Entity(tableName = "media_text")
data class MediaTextEntity(
    @PrimaryKey val mediaId: Long,
    val fullText: String,
    val searchText: String,
    val labelsText: String,
    val blocksJson: String,
    val wordCount: Int,
    val textCoverage: Float,
)

@Entity(tableName = "index_job", indices = [Index("state")])
data class IndexJobEntity(
    @PrimaryKey val mediaId: Long,
    val state: String, // PENDING, PROCESSING, INDEXED, FAILED, FAILED_CORRUPT
    val attempts: Int,
    val nextAttemptAtMs: Long,
    val lastErrorKind: String?,
)

@Entity(tableName = "index_exclusion")
data class IndexExclusionEntity(
    @PrimaryKey val ref: String,
    val excludedAtMs: Long,
)

@Entity(tableName = "vault_item")
data class VaultItemEntity(
    @PrimaryKey val id: String,
    val createdAtMs: Long,
    val mime: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val originalTakenAtMs: Long,
)

@Entity(tableName = "recent_search")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAtMs: Long,
)

@Entity(tableName = "sync_checkpoint")
data class SyncCheckpointEntity(
    @PrimaryKey val key: String = "DEFAULT",
    val lastTimestampMs: Long,
    val lastGeneration: Long,
)

```

### 7.2 External-Content FTS5 Table & Triggers (`FtsCallback.kt`)

```kotlin
package com.o3.vanguard.visionary.core.database

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object FtsCallback : RoomDatabase.Callback() {
    override fun onCreate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE VIRTUAL TABLE IF NOT EXISTS media_fts USING fts5(
                 search_text, labels_text,
                 content='media_text', content_rowid='mediaId',
                 tokenize='unicode61 remove_diacritics 2', prefix='2 3')"""
        )
        connection.execSQL(
            """CREATE TRIGGER IF NOT EXISTS media_text_ai AFTER INSERT ON media_text BEGIN
                 INSERT INTO media_fts(rowid, search_text, labels_text)
                 VALUES (new.mediaId, new.searchText, new.labelsText); END"""
        )
        connection.execSQL(
            """CREATE TRIGGER IF NOT EXISTS media_text_ad AFTER DELETE ON media_text BEGIN
                 INSERT INTO media_fts(media_fts, rowid, search_text, labels_text)
                 VALUES ('delete', old.mediaId, old.searchText, old.labelsText); END"""
        )
        connection.execSQL(
            """CREATE TRIGGER IF NOT EXISTS media_text_au AFTER UPDATE ON media_text BEGIN
                 INSERT INTO media_fts(media_fts, rowid, search_text, labels_text)
                 VALUES ('delete', old.mediaId, old.searchText, old.labelsText);
                 INSERT INTO media_fts(rowid, search_text, labels_text)
                 VALUES (new.mediaId, new.searchText, new.labelsText); END"""
        )
    }
}

```

---

## 8. Fake Data Layer (`:core:testing`)

All UI screens and ViewModel tests work off deterministic fakes in `:core:testing` without waiting for `:data` or `:platform`.

* Implement `FakeControls` holding `val scenario = MutableStateFlow(FakeScenario.Normal)` (`Normal`, `Empty`, `Error`, `Loading`).
* Implement `FakeData` providing 60 synthetic `MediaItem` entries spanning `PHOTO`, `SCREENSHOT`, `DOCUMENT`, `RECEIPT`, and `SCAN` with sample OCR lines and bounding boxes.
* Implement `FakeMediaRepository`, `FakeSearchRepository`, `FakeIndexRepository`, `FakeSettingsRepository`, `FakeVaultRepository`, `FakePermissionGateway`, `FakeScannerPort`, and `FakeBiometricAuthenticator`.
* Export `val fakeDataModule = module { ... }` binding all interfaces from Section 6.2 and 6.3.

---

## 9. Navigation 3 Setup (`:core:navigation` & `:composeApp`)

```kotlin
package com.o3.vanguard.visionary.core.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable data object Onboarding : AppRoute
    @Serializable data object Home : AppRoute
    @Serializable data class Gallery(val collectionId: String) : AppRoute
    @Serializable data class Viewer(val mediaId: Long, val highlight: String? = null) : AppRoute
    @Serializable data class Search(val initialQuery: String = "") : AppRoute
    @Serializable data object Scanner : AppRoute
    @Serializable data object VaultLock : AppRoute
    @Serializable data object VaultGrid : AppRoute
    @Serializable data class VaultViewer(val itemId: String) : AppRoute
    @Serializable data object Settings : AppRoute
    @Serializable data object IndexHealth : AppRoute
}

val AppNavSavedStateConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AppRoute.Onboarding::class, AppRoute.Onboarding.serializer())
            subclass(AppRoute.Home::class, AppRoute.Home.serializer())
            subclass(AppRoute.Gallery::class, AppRoute.Gallery.serializer())
            subclass(AppRoute.Viewer::class, AppRoute.Viewer.serializer())
            subclass(AppRoute.Search::class, AppRoute.Search.serializer())
            subclass(AppRoute.Scanner::class, AppRoute.Scanner.serializer())
            subclass(AppRoute.VaultLock::class, AppRoute.VaultLock.serializer())
            subclass(AppRoute.VaultGrid::class, AppRoute.VaultGrid.serializer())
            subclass(AppRoute.VaultViewer::class, AppRoute.VaultViewer.serializer())
            subclass(AppRoute.Settings::class, AppRoute.Settings.serializer())
            subclass(AppRoute.IndexHealth::class, AppRoute.IndexHealth.serializer())
        }
    }
}

class Navigator(private val backStack: MutableList<NavKey>) {
    fun navigate(route: AppRoute) {
        if (backStack.lastOrNull() != route) backStack.add(route)
    }
    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }
    fun replaceTop(route: AppRoute) {
        if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
        backStack.add(route)
    }
    fun replaceAll(route: AppRoute) {
        backStack.clear()
        backStack.add(route)
    }
    fun popTo(route: AppRoute, inclusive: Boolean = false) {
        val index = backStack.indexOfLast { it == route }
        if (index < 0) return
        val keep = if (inclusive) index else index + 1
        while (backStack.size > keep) backStack.removeAt(backStack.lastIndex)
    }
}

```

---

## 10. Feature Module Standard Pattern (`:features:*`)

Scaffold all 7 feature modules (`onboarding`, `home`, `gallery`, `search`, `scanner`, `vault`, `settings`) using this exact 5-file layout:

```text
features/<name>/src/commonMain/kotlin/com/o3/vanguard/visionary/features/<name>/
├── <Name>Contract.kt     # @Immutable State : UiState, sealed Intent : UiIntent, sealed Event : UiEvent
├── <Name>ViewModel.kt    # Extends BaseMviViewModel<State, Event Intent,>
├── <Name>Screen.kt       # <Name>Route (stateful Koin wiring) + <Name>Screen (stateless UI with Loading/Empty/Error/Content)
├── <Name>Entries.kt      # fun EntryProviderScope<NavKey>.<name>Entries(navigator: Navigator)
└── <Name>Module.kt       # val <name>Module = module { viewModelOf(::<Name>ViewModel) }

```

### Feature Routing Map to Wire in `AppNavHost`:

1. **`onboarding`**: Owns `AppRoute.Onboarding` → Navigates via `navigator.replaceAll(AppRoute.Home)`.
2. **`home`**: Owns `AppRoute.Home` → Navigates to `Gallery(kind.id)`, `Search()`, `Viewer(id)`, `Scanner`, `VaultLock`, `Settings`.
3. **`gallery`**: Owns `AppRoute.Gallery(collectionId)` & `AppRoute.Viewer(mediaId, highlight)` → Navigates to `Viewer`, `back()`.
4. **`search`**: Owns `AppRoute.Search(initialQuery)` → Navigates to `Viewer(id, highlight = query)`, `back()`.
5. **`scanner`**: Owns `AppRoute.Scanner` → Navigates via `navigator.replaceTop(AppRoute.Gallery("scans"))`, `back()`.
6. **`vault`**: Owns `AppRoute.VaultLock`, `AppRoute.VaultGrid`, `AppRoute.VaultViewer(itemId)` → Unlock calls `replaceTop(AppRoute.VaultGrid)`, Lock calls `popTo(AppRoute.Home)`.
7. **`settings`**: Owns `AppRoute.Settings` & `AppRoute.IndexHealth` → Navigates to `IndexHealth`, `back()`.

---

## 11. Root Koin Aggregation (`:composeApp`) & Entry Points

```kotlin
// FILE: composeApp/src/commonMain/kotlin/com/o3/vanguard/visionary/di/AppModules.kt
val featureModules = listOf(
    onboardingModule,
    homeModule,
    galleryModule,
    searchModule,
    scannerModule,
    vaultModule,
    settingsModule,
)

fun appModules(config: AppConfig): List<Module> = buildList {
    add(module { single { config } })
    add(coreCommonModule)
    add(domainModule)
    addAll(featureModules)
    if (config.useFakeData) {
        add(fakeDataModule)
    } else {
        add(databaseModule)
        add(dataModule)
        add(platformModule)
    }
}

```

---

## 12. Step-by-Step Execution Order for the Agent

Execute the architecture setup in these 5 sequential stages and verify compilation after each stage:

1. **Stage 1 (Gradle & Build-Logic):** Create `settings.gradle.kts`, `gradle/libs.versions.toml`, `build-logic/` convention plugins, and `build.gradle.kts` files for all modules. Verify `./gradlew help` succeeds.
2. **Stage 2 (Core, `expect`/`actual` Primitives & Domain):** Implement `:core:common` (with `PlatformPrimitives.kt` in `commonMain`/`androidMain`/`iosMain`), `:core:mvi`, `:core:navigation`, `:core:designsystem`, and `:domain` (models, repositories, ports, use cases, `domainModule`).
3. **Stage 3 (Database & Testing Fakes):** Implement `:core:database` (Room 3 entities, DAOs, `FtsCallback`, and `DatabaseBuilder.kt` in `commonMain`/`androidMain`/`iosMain`) and `:core:testing` (all `Fake*` implementations and `fakeDataModule`).
4. **Stage 4 (Data, Platform `expect`/`actual` & Feature Skeletons):** Create repository implementations in `:data`, `AppDirectories` and `PlatformModule` (`commonMain`/`androidMain`/`iosMain`) with safe Android/iOS port stubs in `:platform`, and the 5-file MVI skeletons for all 7 modules in `:features:*`.
5. **Stage 5 (Root Wiring & Verification):** Wire `AppNavHost` and `appModules` in `:composeApp`, configure `MainActivity` and `VisionaryApp` in `:androidApp` (with `INTERNET` permission removed in `AndroidManifest.xml`), and run `./gradlew assembleDebug` and unit tests to confirm a clean build.

```

```