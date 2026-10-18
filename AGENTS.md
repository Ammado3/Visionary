# AGENTS.md — Visionary (Vanguard squad, "ada at o3")

> Read this file fully before writing any code. It is the contract for every human and AI agent working in this repo.
> Nothing here has been compiled yet. Items tagged **[VERIFY]** are best-known facts that the Day-1 task **S0** must confirm and then correct in this file. Items tagged **[DECIDE]** wait for the Tech Lead.
> Versions last checked: 2026-10-17.

---

## 0. What we are building

**Visionary** is an offline, privacy-first smart gallery for Android and iOS (Kotlin Multiplatform + Compose Multiplatform).

MVP scope: on-device OCR search, auto collections (All, Screenshots, Documents, Receipts, Scans), document scanner, biometric vault, home hub, settings, onboarding.
Out of scope until the Tech Lead says so: Magic Eraser, AR, semantic search, cloud anything, non-English OCR.

- Package: `com.o3.vanguard.visionary`
- Targets: Android 8.0+ (minSdk 26), iOS 16+. Every module compiles for both. Android is the first runtime target for the MVP **[DECIDE: confirm iOS runtime scope]**. iOS adapters never crash: they return `AppResult.Err(AppError.Unsupported(...))`, never `TODO()`.
- Privacy promise (use this wording only): *"The release app requests no network permission."* Never write "proves", "guarantees" or "mathematically".

---

## 1. Golden rules (non-negotiable)

1. **Build must be green with warnings as errors.** Run `./gradlew verifyAll` before every push.
2. **Dependency direction is law** (section 3). Never add a module dependency that is not in the allowed table.
3. **UI sprints never wait for data.** UI is built against fakes (section 6). Real implementations replace fakes without UI changes.
4. **No new dependency without Tech Lead approval.** Versions live only in `gradle/libs.versions.toml`.
5. **No content in logs.** Never log OCR text, file names, URIs, query text, vault anything.
6. **No `!!`, no `GlobalScope`, no hard-coded dispatchers, no hard-coded user-visible strings, no `TODO()` in shipped code.**
7. **No network code.** No `INTERNET`, no HTTP client, no analytics, no crash-reporting SDKs.
8. **Do not touch Phase 2 work** (eraser, AR, semantic search).
9. **Stop and ask** (section 14) instead of guessing when something is unclear.

---

## 2. Version baseline

| Tool | Version | Note |
| --- | --- | --- |
| Kotlin | 2.4.21 | |
| Compose Multiplatform | 1.12.2 | |
| Android Gradle Plugin | 9.4.0 | needs Gradle 9.6.0, JDK 17 |
| Gradle | 9.6.0 | |
| compileSdk / targetSdk / minSdk | 36 / 36 / 26 | **[VERIFY]** compileSdk 36 vs 37 at S0 |
| KSP | 2.3.11 | |
| Room | 3.0.3 (`androidx.room3`) | KSP only; `sqlite-bundled` 2.7.1 **[VERIFY]** |
| Koin | 4.2.2 | |
| Coil | 3.6.3 | |
| kotlinx.coroutines | 1.11.0 | |
| Navigation 3 (CMP) | `org.jetbrains.androidx.navigation3:navigation3-ui` 1.1.1 | needs CMP 1.10+ |
| Lifecycle ViewModel for Nav3 | `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3` 2.10.0 | **[VERIFY]** pairing with CMP 1.12.2 |
| detekt | 2.0.0-alpha.x (supports Kotlin 2.4) | stable 1.23.8 supports Kotlin 2.0 only **[VERIFY]** at S0, fallback: ktlint + Android Lint + `verify-architecture.sh` |
| ML Kit text recognition (bundled) | `com.google.mlkit:text-recognition` latest 16.x | never the unbundled variant |

---

## 2.1 Navigation choice

We use **Navigation 3** (user-owned back stack, `NavDisplay`, typed `NavKey` routes). This replaces the earlier "Navigation Compose" default. Do not add `navigation-compose`.

---

## 3. Module map and placement guide

```
visionary/
├── AGENTS.md
├── docs/                       # HANDBOOK.md, ADRs (docs/adr/NNNN-title.md), BENCHMARKS.md
├── build-logic/                # convention plugins (included build)
├── gradle/libs.versions.toml
├── scripts/verify-architecture.sh
├── androidApp/                 # Android application: MainActivity, Application, manifest, signing, build types
├── iosApp/                     # Xcode project (Swift entry point, CryptoKit/VisionKit bridges later)
├── composeApp/                 # shared library: App(), AppNavHost, Koin module aggregation, iOS entry points
├── core/
│   ├── common/                 # AppResult, AppError, AppDispatchers, AppConfig, Logger (pure Kotlin)
│   ├── mvi/                    # BaseMviViewModel, UiState/UiIntent/UiEvent
│   ├── navigation/             # AppRoute (NavKey), Navigator, saved-state config
│   ├── designsystem/           # theme, tokens, shared components, previews helpers
│   ├── database/               # Room 3: entities, DAOs, database, migrations, schemas/
│   └── testing/                # Fake* repositories/ports, FakeData, test helpers
├── domain/                     # models, repository + port interfaces, use cases (pure Kotlin)
├── data/                       # repository implementations (Room + ports)
├── platform/                   # androidMain / iosMain adapters (MediaStore, ML Kit, biometrics, scanner, workers)
└── features/
    ├── onboarding/  ├── home/  ├── gallery/  ├── search/
    ├── scanner/     ├── vault/ └── settings/
```

### 3.1 Allowed dependencies (enforced by `verifyModuleGraph` and `scripts/verify-architecture.sh`)

| Module | May depend on |
| --- | --- |
| `:core:common` | nothing (no Compose, no lifecycle, no Android) |
| `:core:mvi` | `:core:common`, androidx lifecycle-viewmodel, coroutines |
| `:core:navigation` | navigation3, kotlinx-serialization (no domain, no features) |
| `:core:designsystem` | Compose, Coil (no domain) |
| `:core:database` | `:core:common`, Room 3, sqlite-bundled |
| `:domain` | `:core:common`, coroutines (pure Kotlin) |
| `:data` | `:domain`, `:core:common`, `:core:database` |
| `:platform` | `:domain`, `:core:common`, Android/iOS APIs, ML Kit |
| `:core:testing` | `:domain`, `:core:common` (fakes, fixtures) |
| `:features:*` | `:domain`, `:core:common`, `:core:mvi`, `:core:navigation`, `:core:designsystem`, Koin, Compose |
| `:composeApp` | everything, but only to wire DI and navigation |
| `:androidApp` | `:composeApp` |

Forbidden, no exceptions: feature → feature, feature → `:data` / `:platform` / `:core:database` / `:core:testing`, `:domain` → anything Android/iOS/Room/Compose/Koin.

### 3.2 "Where do I put it?" cheat sheet

| I am writing… | Put it in |
| --- | --- |
| A data class used by UI and logic (`MediaItem`) | `:domain/model` |
| An interface for storage (`MediaRepository`) | `:domain/repository` |
| An interface for an OS/ML capability (`TextRecognizer`) | `:domain/port` |
| A use case | `:domain/usecase` |
| Room entity/DAO | `:core:database` |
| Entity ↔ domain mapper, repository impl | `:data` |
| ML Kit / MediaStore / WorkManager / Keystore code | `:platform` (`androidMain`; stub in `iosMain`) |
| A screen | `:features:<name>` |
| A composable reused by 2+ features | `:core:designsystem` |
| A route | `:core:navigation` |
| A fake or test fixture | `:core:testing` |
| A build fact (debug flag, version) | `AppConfig` (injected), never `BuildConfig` |

### 3.3 `expect`/`actual` — allowed only for

1. `RoomDatabaseConstructor` (`AppDatabaseConstructor`) and the per-platform `getDatabaseBuilder()`.
2. `platformModule: Module` in `:platform`.
3. `AppDirectories` and tiny stateless primitives (UUID, path type) in `:core:common`.

Anything stateful, async or lifecycle-bound is a **domain interface + Koin binding**, not `expect class`.

---

## 4. Base architecture

```
UI (Compose screen) ──intent──▶ ViewModel (MVI) ──▶ UseCase ──▶ Repository/Port (interface in :domain)
        ▲                           │                                   ▲
        └────── state/event ────────┘                      impl in :data / :platform (or Fake in :core:testing)
```

- **Ports and adapters.** `:domain` declares interfaces; `:data` / `:platform` / fakes implement them. Koin binds them.
- **MVI-lite.** One immutable `UiState` per screen (`StateFlow`), sealed `UiIntent` via `onIntent()`, sealed `UiEvent` for one-time effects (navigation requests, snackbars).
- **Platform actions are ports, not events.** Permissions, scanner, biometric prompt and system delete consent are `suspend` functions on ports (`PermissionGateway`, `ScannerPort`, `BiometricAuthenticator`, `MediaDeleteGateway`). Common UI code never touches Android APIs.
- **Errors are values.** Adapters and repositories return `AppResult<T>`; they do not throw across layer boundaries (except `CancellationException`, which is always rethrown).
- **Dispatchers are injected** (`AppDispatchers`). No `Dispatchers.IO` in feature or domain code.
- **Four screen states** (Loading, Empty, Error, Content) exist for every screen and have previews.

### 4.1 `:core:common`

```kotlin
package com.o3.vanguard.visionary.core.common

sealed interface AppError {
    data class Unsupported(val reason: String) : AppError
    data object PermissionDenied : AppError
    data object NotFound : AppError
    data object Cancelled : AppError
    data class Io(val kind: String) : AppError          // kind only, never a message with user data
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

data class AppConfig(
    val versionName: String,
    val isDebug: Boolean,
    val useFakeData: Boolean,   // must be false in release; a release test asserts it
)
```

### 4.2 `:core:mvi`

```kotlin
package com.o3.vanguard.visionary.core.mvi

interface UiState
interface UiIntent
interface UiEvent

abstract class BaseMviViewModel<S : UiState, I : UiIntent, E : UiEvent>(
    initialState: S,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _events = Channel<E>(Channel.UNLIMITED)   // trySend can never drop an event
    val events: Flow<E> = _events.receiveAsFlow()

    protected fun updateState(reducer: S.() -> S) = _state.update { it.reducer() }
    protected fun sendEvent(event: E) { _events.trySend(event) }

    abstract fun onIntent(intent: I)
}
```

Screen-level helper (put in `:core:mvi`, requires Compose runtime):

```kotlin
@Composable
fun <E : UiEvent> CollectEvents(events: Flow<E>, onEvent: (E) -> Unit) {
    val current by rememberUpdatedState(onEvent)
    LaunchedEffect(events) { events.collect { current(it) } }
}
```

---

## 5. Domain layer (pure Kotlin) — the frozen contracts

These are frozen after Day 1. Changing them needs a PR approved by the Tech Lead because every feature and every fake depends on them.

### 5.1 Models (`:domain/model`)

```kotlin
@JvmInline value class MediaId(val value: Long)

enum class MediaOrigin { LIBRARY, APP_SCAN }

enum class MediaCategory { PHOTO, SCREENSHOT, DOCUMENT, RECEIPT, SCAN }

data class MediaItem(
    val id: MediaId,
    val uri: String,                 // content:// URI or app-private path
    val origin: MediaOrigin,
    val category: MediaCategory,
    val takenAtMs: Long,
    val width: Int,
    val height: Int,
    val textPreview: String?,        // first line of OCR text, null until indexed
)

enum class CollectionKind(val id: String) {
    ALL("all"), SCREENSHOTS("screenshots"), DOCUMENTS("documents"),
    RECEIPTS("receipts"), SCANS("scans");
    companion object { fun fromId(id: String): CollectionKind = entries.firstOrNull { it.id == id } ?: ALL }
}

data class CollectionSummary(val kind: CollectionKind, val count: Int, val coverUri: String?)

enum class SearchFilter { ALL, SCREENSHOTS, RECEIPTS, DOCUMENTS, SCANS, HAS_PHONE_OR_URL }

data class SearchHit(val item: MediaItem, val snippet: String, val matchedTerms: List<String>)

enum class IndexState { IDLE, RUNNING, PAUSED_BATTERY, NEEDS_ACCESS }

data class IndexProgress(val total: Int, val indexed: Int, val pending: Int, val failed: Int, val state: IndexState)

sealed interface MediaAccess {
    data object Full : MediaAccess
    data class Partial(val count: Int) : MediaAccess
    data object Denied : MediaAccess
}

data class VaultItem(val id: String, val createdAtMs: Long, val width: Int, val height: Int) // never a filename

data class ScannedDocument(val pageUris: List<String>, val pdfUri: String?)

data class AppSettings(
    val onboardingDone: Boolean,
    val indexOnlyWhileCharging: Boolean,
    val vaultLockTimeoutSeconds: Int,
)
```

### 5.2 Repositories and ports (`:domain/repository`, `:domain/port`)

```kotlin
interface MediaRepository {
    fun observeCollections(): Flow<List<CollectionSummary>>
    fun observeMedia(kind: CollectionKind): Flow<List<MediaItem>>   // list for now; paging decision in task S3
    fun observeItem(id: MediaId): Flow<MediaItem?>
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
    fun observeItems(): Flow<List<VaultItem>>          // empty while locked
    suspend fun unlock(): AppResult<Unit>               // shows biometric prompt via BiometricAuthenticator
    fun lock()
    suspend fun moveToVault(id: MediaId): AppResult<Unit>
    suspend fun restore(itemId: String): AppResult<Unit>
    suspend fun delete(itemId: String): AppResult<Unit>
}

// ports (OS / ML capabilities)
interface PermissionGateway {
    fun observeAccess(): Flow<MediaAccess>
    suspend fun requestAccess(): MediaAccess
}
interface ScannerPort {
    suspend fun isAvailable(): Boolean
    suspend fun scan(): AppResult<ScannedDocument>
}
interface BiometricAuthenticator {
    suspend fun authenticate(): AppResult<Unit>
}
```

### 5.3 Use cases (`:domain/usecase`)

One class per use case, `operator fun invoke`. ViewModels depend on use cases, not on repositories, unless the use case would be a pure pass-through (then inject the repository and say so in a comment).

```kotlin
class ObserveCollectionsUseCase(private val media: MediaRepository) {
    operator fun invoke(): Flow<List<CollectionSummary>> = media.observeCollections()
}

class SearchMediaUseCase(private val search: SearchRepository) {
    operator fun invoke(query: String, filter: SearchFilter): Flow<List<SearchHit>> =
        if (query.isBlank()) flowOf(emptyList()) else search.search(query.trim(), filter)
}

class ResolveStartRouteUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(): StartDestination =
        if (settings.observe().first().onboardingDone) StartDestination.HOME else StartDestination.ONBOARDING
}
enum class StartDestination { ONBOARDING, HOME }
```

Other use cases follow the same pattern (`ObserveMediaUseCase`, `ObserveIndexProgressUseCase`, `MoveToVaultUseCase`, `StartScanUseCase`, …).

### 5.4 Domain DI

```kotlin
val domainModule = module {
    factoryOf(::ObserveCollectionsUseCase)
    factoryOf(::SearchMediaUseCase)
    factoryOf(::ResolveStartRouteUseCase)
    // add every use case here
}
```

---

## 6. Fake data layer — how UI sprints run uninterrupted

**Rule:** every repository and port in section 5.2 has a `Fake*` implementation in `:core:testing` from Day 1 (Tech Lead delivers them as task **S2**). The app runs end-to-end on fakes. Feature developers never wait for `:data` or `:platform`.

### 6.1 Rules for fakes

- Deterministic, synthetic data only (no real photos, no real personal text).
- Backed by `MutableStateFlow`, so mutations show up in the UI like real data.
- Simulated latency (`delay(300)`) so loading states are visible.
- A scenario switch: `Normal`, `Empty`, `Error`, `Loading`, so every screen's four states can be exercised without code changes.
- Fakes are also used in unit tests and `@Preview` functions.
- Fakes are present in the compiled binary (iOS cannot strip them) but **bound only when `AppConfig.useFakeData` is true**; a release-config unit test asserts it is false.

### 6.2 Fake infrastructure

```kotlin
package com.o3.vanguard.visionary.core.testing

enum class FakeScenario { Normal, Empty, Error, Loading }

class FakeControls {
    val scenario = MutableStateFlow(FakeScenario.Normal)
}

object FakeData {
    val media: List<MediaItem> = List(60) { i ->
        val category = when (i % 6) {
            0 -> MediaCategory.SCREENSHOT
            1 -> MediaCategory.DOCUMENT
            2 -> MediaCategory.RECEIPT
            3 -> MediaCategory.SCAN
            else -> MediaCategory.PHOTO
        }
        MediaItem(
            id = MediaId(i.toLong() + 1),
            uri = "fake://media/${i + 1}",
            origin = if (category == MediaCategory.SCAN) MediaOrigin.APP_SCAN else MediaOrigin.LIBRARY,
            category = category,
            takenAtMs = 1_760_000_000_000L - i * 3_600_000L,
            width = 1080,
            height = if (category == MediaCategory.SCREENSHOT) 2400 else 1440,
            textPreview = if (i % 2 == 0) "Sample text line ${i + 1}" else null,
        )
    }
}
```

### 6.3 Example fake repository

```kotlin
class FakeMediaRepository(private val controls: FakeControls) : MediaRepository {
    private val items = MutableStateFlow(FakeData.media)

    private fun <T> scenarioFlow(empty: T, content: Flow<T>): Flow<T> =
        controls.scenario.flatMapLatest { scenario ->
            when (scenario) {
                FakeScenario.Normal -> content.onStart { delay(300) }
                FakeScenario.Empty -> flowOf(empty)
                FakeScenario.Error -> flow { throw FakeFailure() }
                FakeScenario.Loading -> flow { awaitCancellation() }
            }
        }

    override fun observeCollections() = scenarioFlow(emptyList(), items.map { list ->
        CollectionKind.entries.map { kind ->
            val matching = list.filter { it.matches(kind) }
            CollectionSummary(kind, matching.size, matching.firstOrNull()?.uri)
        }
    })

    override fun observeMedia(kind: CollectionKind) =
        scenarioFlow(emptyList(), items.map { list -> list.filter { it.matches(kind) } })

    override fun observeItem(id: MediaId) = items.map { list -> list.firstOrNull { it.id == id } }

    override fun observeRecentFinds(limit: Int) =
        scenarioFlow(emptyList(), items.map { list -> list.filter { it.textPreview != null }.take(limit) })
}

class FakeFailure : RuntimeException("fake failure")   // ViewModels map thrown errors to the Error state via .catch

internal fun MediaItem.matches(kind: CollectionKind): Boolean = when (kind) {
    CollectionKind.ALL -> true
    CollectionKind.SCREENSHOTS -> category == MediaCategory.SCREENSHOT
    CollectionKind.DOCUMENTS -> category == MediaCategory.DOCUMENT
    CollectionKind.RECEIPTS -> category == MediaCategory.RECEIPT
    CollectionKind.SCANS -> origin == MediaOrigin.APP_SCAN
}
```

`FakeSearchRepository` filters `FakeData.media` by `textPreview.contains(query, ignoreCase = true)`. `FakeVaultRepository` keeps `isUnlocked` in a `MutableStateFlow` and unlocks after `delay(500)`. `FakeScannerPort.scan()` returns two fake page URIs. `FakePermissionGateway` starts as `Full`. `FakeIndexRepository` emits a progress value that counts up every second.

> Fake image URIs (`fake://…`) are rendered by a Coil fetcher in `:core:designsystem` that draws a solid-color placeholder derived from the id. The real fetcher (task A-thumb) loads from `ThumbnailLoader`. Features only ever pass a `MediaRef`/URI string to `VisionaryImage(...)`.

### 6.4 DI modules

```kotlin
// :core:testing
val fakeDataModule = module {
    single { FakeControls() }
    single<MediaRepository> { FakeMediaRepository(get()) }
    single<SearchRepository> { FakeSearchRepository(get()) }
    single<IndexRepository> { FakeIndexRepository() }
    single<SettingsRepository> { FakeSettingsRepository() }
    single<VaultRepository> { FakeVaultRepository() }
    single<PermissionGateway> { FakePermissionGateway() }
    single<ScannerPort> { FakeScannerPort() }
    single<BiometricAuthenticator> { FakeBiometricAuthenticator() }
}

// :data  (real; filled in progressively, one binding at a time)
val dataModule = module {
    single<MediaRepository> { RoomMediaRepository(get()) }
    // …
}

// :composeApp
fun appModules(config: AppConfig): List<Module> = buildList {
    add(module { single { config } })
    add(coreModule)                     // AppDispatchers etc.
    add(domainModule)
    addAll(featureModules)              // see section 8
    if (config.useFakeData) {
        add(fakeDataModule)
    } else {
        add(dataModule)
        add(platformModule)
        add(databaseModule)
    }
}
```

**Switching a repository from fake to real** (the migration path): bind the real class in `dataModule`; keep the fake in `fakeDataModule`; the switch is `AppConfig.useFakeData`. When partially real is needed during integration, a temporary mixed module is allowed but must be removed before the PR merges to trunk.

Turn fakes on: Gradle property `-Pvisionary.fakes=true` (default **true** in the `debug` build type until milestone M2, then default false). `androidApp` reads it into `AppConfig`.

---

## 7. Navigation 3 setup

### 7.1 Dependencies

```toml
# gradle/libs.versions.toml
[versions]
navigation3 = "1.1.1"
lifecycleNav3 = "2.10.0"
[libraries]
nav3-ui = { module = "org.jetbrains.androidx.navigation3:navigation3-ui", version.ref = "navigation3" }
lifecycle-viewmodel-nav3 = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycleNav3" }
```

`:core:navigation` exposes `api(libs.nav3.ui)` so features get `NavKey` and `EntryProviderScope` transitively. `:composeApp` adds `lifecycle-viewmodel-nav3`.

### 7.2 Routes (`:core:navigation`) — the single source of truth

```kotlin
package com.o3.vanguard.visionary.core.navigation

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable data object Onboarding : AppRoute
    @Serializable data object Home : AppRoute
    @Serializable data class Gallery(val collectionId: String) : AppRoute      // CollectionKind.id
    @Serializable data class Viewer(val mediaId: Long, val highlight: String? = null) : AppRoute
    @Serializable data class Search(val initialQuery: String = "") : AppRoute
    @Serializable data object Scanner : AppRoute
    @Serializable data object VaultLock : AppRoute
    @Serializable data object VaultGrid : AppRoute
    @Serializable data class VaultViewer(val itemId: String) : AppRoute
    @Serializable data object Settings : AppRoute
    @Serializable data object IndexHealth : AppRoute
}
```

Routes carry primitives and strings only (ids, never objects), so `:core:navigation` never depends on `:domain`.

Non-Android targets cannot use reflection, so every route is registered explicitly:

```kotlin
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
```

**Adding a route = 3 edits:** the sealed interface, this registration list, and the feature's entry function. A JVM unit test (`AppRouteRegistrationTest`) uses `AppRoute::class.sealedSubclasses` and fails if one is missing from the module.

### 7.3 Navigator

```kotlin
class Navigator(private val backStack: MutableList<NavKey>) {
    fun navigate(route: AppRoute) { if (backStack.lastOrNull() != route) backStack.add(route) }
    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }
    fun replaceTop(route: AppRoute) { if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex); backStack.add(route) }
    fun replaceAll(route: AppRoute) { backStack.clear(); backStack.add(route) }
    fun popTo(route: AppRoute, inclusive: Boolean = false) {
        val index = backStack.indexOfLast { it == route }
        if (index < 0) return
        val keep = if (inclusive) index else index + 1
        while (backStack.size > keep) backStack.removeAt(backStack.lastIndex)
    }
}
```

Unit-tested in `commonTest` with a plain `mutableListOf<NavKey>()`.

### 7.4 Each feature exposes one entry function

A feature never imports another feature. It receives the `Navigator` and navigates only to `AppRoute`s.

```kotlin
// :features:home
fun EntryProviderScope<NavKey>.homeEntries(navigator: Navigator) {
    entry<AppRoute.Home> {
        HomeRoute(
            onOpenCollection = { navigator.navigate(AppRoute.Gallery(it.id)) },
            onOpenSearch = { navigator.navigate(AppRoute.Search()) },
            onOpenViewer = { navigator.navigate(AppRoute.Viewer(it.value)) },
            onOpenScanner = { navigator.navigate(AppRoute.Scanner) },
            onOpenVault = { navigator.navigate(AppRoute.VaultLock) },
            onOpenSettings = { navigator.navigate(AppRoute.Settings) },
        )
    }
}
```

Routes with arguments read them from the entry key and pass them to the ViewModel with Koin `parametersOf`:

```kotlin
entry<AppRoute.Viewer> { key ->
    ViewerRoute(
        viewModel = koinViewModel { parametersOf(MediaId(key.mediaId), key.highlight) },
        onBack = { navigator.back() },
    )
}
```

### 7.5 The host (`:composeApp`)

```kotlin
@Composable
fun App() {
    VisionaryTheme {
        val resolveStart = koinInject<ResolveStartRouteUseCase>()
        var start by remember { mutableStateOf<AppRoute?>(null) }
        LaunchedEffect(Unit) {
            start = when (resolveStart()) {
                StartDestination.ONBOARDING -> AppRoute.Onboarding
                StartDestination.HOME -> AppRoute.Home
            }
        }
        start?.let { AppNavHost(it) } ?: SplashScreen()
    }
}

@Composable
private fun AppNavHost(start: AppRoute) {
    val backStack = rememberNavBackStack(AppNavSavedStateConfig, start)
    val navigator = remember(backStack) { Navigator(backStack) }

    NavDisplay(
        backStack = backStack,
        onBack = { navigator.back() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),   // scopes ViewModels to the entry
        ),
        entryProvider = entryProvider {
            onboardingEntries(navigator)
            homeEntries(navigator)
            galleryEntries(navigator)
            searchEntries(navigator)
            scannerEntries(navigator)
            vaultEntries(navigator)
            settingsEntries(navigator)
        },
    )
}
```

**[VERIFY at S0]** exact names and signatures of `rememberNavBackStack`, `NavDisplay` (`onBack`), the two decorators and `entryProvider` against navigation3-ui 1.1.1 on compile. Fix this file, not the code, if they differ.

### 7.6 Navigation rules

1. Navigate only through `Navigator` and `AppRoute`. No feature-to-feature imports, no string routes.
2. Callbacks come from `HomeRoute(onX = …)` parameters. ViewModels emit `UiEvent`s such as `NavigateToViewer(id)`; the route composable converts them to callbacks.
3. Cross-feature flows ("move to vault" from the viewer) call a domain use case, then navigate.
4. The vault: `VaultLock` → `navigator.replaceTop(AppRoute.VaultGrid)` after unlock; on lock, `popTo(AppRoute.Home)`.
5. Onboarding finishing calls `navigator.replaceAll(AppRoute.Home)`.
6. Every route has a preview-friendly stateless screen (section 8.2).

---

## 8. Feature modules

### 8.1 Layout inside every feature

```
features/home/src/commonMain/kotlin/com/o3/vanguard/visionary/features/home/
├── HomeContract.kt     # HomeState, HomeIntent, HomeEvent
├── HomeViewModel.kt
├── HomeScreen.kt       # HomeRoute (wiring) + HomeScreen (stateless) + previews
├── HomeEntries.kt      # EntryProviderScope<NavKey>.homeEntries(navigator)
└── HomeModule.kt       # Koin module
```

### 8.2 Complete worked example: Home (copy it for every feature)

```kotlin
// HomeContract.kt
@Immutable
data class HomeState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val collections: List<CollectionSummary> = emptyList(),
    val recentFinds: List<MediaItem> = emptyList(),
    val index: IndexProgress? = null,
) : UiState {
    val isEmpty get() = !loading && !error && collections.all { it.count == 0 }
}

sealed interface HomeIntent : UiIntent {
    data class OnCollectionClicked(val kind: CollectionKind) : HomeIntent
    data class OnRecentClicked(val id: MediaId) : HomeIntent
    data object OnSearchClicked : HomeIntent
    data object OnScanClicked : HomeIntent
    data object OnVaultClicked : HomeIntent
    data object OnSettingsClicked : HomeIntent
    data object OnRetryClicked : HomeIntent
}

sealed interface HomeEvent : UiEvent {
    data class OpenCollection(val kind: CollectionKind) : HomeEvent
    data class OpenViewer(val id: MediaId) : HomeEvent
    data object OpenSearch : HomeEvent
    data object OpenScanner : HomeEvent
    data object OpenVault : HomeEvent
    data object OpenSettings : HomeEvent
}
```

```kotlin
// HomeViewModel.kt
class HomeViewModel(
    private val observeCollections: ObserveCollectionsUseCase,
    private val observeRecent: ObserveRecentFindsUseCase,
    private val observeIndex: ObserveIndexProgressUseCase,
) : BaseMviViewModel<HomeState, HomeIntent, HomeEvent>(HomeState()) {

    init { load() }

    private fun load() {
        updateState { copy(loading = true, error = false) }
        viewModelScope.launch {
            combine(observeCollections(), observeRecent(limit = 12), observeIndex()) { c, r, i ->
                { s: HomeState -> s.copy(loading = false, error = false, collections = c, recentFinds = r, index = i) }
            }
                .catch { emit { s: HomeState -> s.copy(loading = false, error = true) } }
                .collect { reducer -> updateState { reducer(this) } }
        }
    }

    override fun onIntent(intent: HomeIntent) = when (intent) {
        is HomeIntent.OnCollectionClicked -> sendEvent(HomeEvent.OpenCollection(intent.kind))
        is HomeIntent.OnRecentClicked -> sendEvent(HomeEvent.OpenViewer(intent.id))
        HomeIntent.OnSearchClicked -> sendEvent(HomeEvent.OpenSearch)
        HomeIntent.OnScanClicked -> sendEvent(HomeEvent.OpenScanner)
        HomeIntent.OnVaultClicked -> sendEvent(HomeEvent.OpenVault)
        HomeIntent.OnSettingsClicked -> sendEvent(HomeEvent.OpenSettings)
        HomeIntent.OnRetryClicked -> load()
    }
}
```

```kotlin
// HomeScreen.kt
@Composable
fun HomeRoute(
    onOpenCollection: (CollectionKind) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenViewer: (MediaId) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is HomeEvent.OpenCollection -> onOpenCollection(event.kind)
            is HomeEvent.OpenViewer -> onOpenViewer(event.id)
            HomeEvent.OpenSearch -> onOpenSearch()
            HomeEvent.OpenScanner -> onOpenScanner()
            HomeEvent.OpenVault -> onOpenVault()
            HomeEvent.OpenSettings -> onOpenSettings()
        }
    }
    HomeScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun HomeScreen(state: HomeState, onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier) {
    when {
        state.loading -> ShimmerSkeletonGrid(modifier)
        state.error -> ErrorState(onRetry = { onIntent(HomeIntent.OnRetryClicked) }, modifier = modifier)
        state.isEmpty -> EmptyState(message = stringResource(Res.string.home_empty), modifier = modifier)
        else -> HomeContent(state, onIntent, modifier)
    }
}

@Preview @Composable private fun HomeLoadingPreview() = VisionaryTheme { HomeScreen(HomeState(loading = true), {}) }
@Preview @Composable private fun HomeErrorPreview() = VisionaryTheme { HomeScreen(HomeState(loading = false, error = true), {}) }
@Preview @Composable private fun HomeEmptyPreview() = VisionaryTheme { HomeScreen(HomeState(loading = false), {}) }
@Preview @Composable private fun HomeContentPreview() = VisionaryTheme { HomeScreen(previewHomeState(), {}) }
```

```kotlin
// HomeModule.kt
val homeModule = module {
    viewModelOf(::HomeViewModel)
}
```

All feature modules are collected in `:composeApp`: `val featureModules = listOf(onboardingModule, homeModule, galleryModule, searchModule, scannerModule, vaultModule, settingsModule)`.

### 8.3 Initial navigation per feature (what the Tech Lead scaffolds on Day 1)

Every feature ships on Day 1 with: contract, ViewModel on fakes, a skeleton screen showing its title and fake data, four-state previews, a Koin module, an entry function, and navigation wired in `AppNavHost`. Developers then fill in the real UI.

| Feature | Routes it owns | ViewModel (fake source) | Navigates to | Skeleton shows |
| --- | --- | --- | --- | --- |
| `onboarding` | `Onboarding` | `OnboardingViewModel` (`SettingsRepository`, `PermissionGateway`) | `replaceAll(Home)` | 3-page pager, permission button |
| `home` | `Home` | `HomeViewModel` (collections, recents, index) | Gallery, Search, Viewer, Scanner, VaultLock, Settings | 5 collection cards + vault card (lock only), search bar |
| `gallery` | `Gallery(collectionId)`, `Viewer(mediaId, highlight)` | `GalleryViewModel(kind)`, `ViewerViewModel(id, highlight)` (`MediaRepository`) | Viewer, back, (vault via use case) | grid of fake tiles; full-screen placeholder |
| `search` | `Search(initialQuery)` | `SearchViewModel` (`SearchRepository`) | `Viewer(id, highlight = query)` | search box, filter chips, fake hits |
| `scanner` | `Scanner` | `ScannerViewModel` (`ScannerPort`) | `Gallery("scans")` (replaceTop) | "Scan document" button, fake result |
| `vault` | `VaultLock`, `VaultGrid`, `VaultViewer(itemId)` | `VaultViewModel` (`VaultRepository`, `BiometricAuthenticator`) | VaultGrid, VaultViewer, popTo(Home) | unlock button, fake item grid |
| `settings` | `Settings`, `IndexHealth` | `SettingsViewModel`, `IndexHealthViewModel` | IndexHealth, back | toggles, index counters |

Feature entries (all in `:features:<name>`): `onboardingEntries`, `homeEntries`, `galleryEntries`, `searchEntries`, `scannerEntries`, `vaultEntries`, `settingsEntries`.

### 8.4 Compose rules

- Stateless screens take `state` and `onIntent`; no ViewModel access below the Route composable.
- State classes are `@Immutable`; lists use `ImmutableList`-style stable wrappers if recomposition problems are measured (not by default).
- Strings live in Compose resources (`composeResources/values/strings.xml`); none are hard-coded.
- Every interactive element has a content description or a semantic role.
- Hoist state, no `remember` for business state, keys in lazy lists (`key = { it.id.value }`).
- Colors, spacing and typography come from the design system tokens only.

---

## 9. Room implementation guide (`:core:database`)

> Source: Android's Room KMP docs (checked 2026-10-17). **[VERIFY]** items must be settled in task **S1 (database spike)**.

### 9.1 Gradle

```toml
[versions]
room3 = "3.0.3"
sqlite = "2.7.1"
[libraries]
room3-runtime = { module = "androidx.room3:room3-runtime", version.ref = "room3" }
room3-compiler = { module = "androidx.room3:room3-compiler", version.ref = "room3" }
sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }
[plugins]
ksp = { id = "com.google.devtools.ksp", version = "2.3.11" }
room3 = { id = "androidx.room3", version.ref = "room3" }
```

```kotlin
// core/database/build.gradle.kts
plugins {
    id("visionary.kmp.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}
kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.room3.runtime)
        implementation(libs.sqlite.bundled)
        api(projects.core.common)
    }
}
dependencies {
    add("kspAndroid", libs.room3.compiler)
    add("kspIosArm64", libs.room3.compiler)
    add("kspIosSimulatorArm64", libs.room3.compiler)
}
room { schemaDirectory("$projectDir/schemas") }   // schemas/ is committed to git
```

R8 rule for Android: `-keep class * extends androidx.room3.RoomDatabase { <init>(); }`.

### 9.2 Tables

| Table | Purpose |
| --- | --- |
| `media_item` | one row per library image or scan: ref, origin, dates, size, category, `sortKey`, `entityFlags`, fingerprint |
| `media_text` | 1:1 with `media_item`: `fullText`, `searchText`, `labelsText`, `blocksJson` (line boxes, stored once per image) |
| `index_job` | indexing state per item: `PENDING / PROCESSING / INDEXED / FAILED / FAILED_CORRUPT`, attempts, `nextAttemptAt` |
| `index_exclusion` | references that must never be indexed (vaulted originals whose deletion was declined) |
| `vault_item` | id, createdAt, mime, size, width, height; **never filename or text** |
| `recent_search`, `app_setting` | small support tables |
| `media_fts` | FTS5 external-content virtual table over `media_text` (below) |

### 9.3 Entities, DAO, database

```kotlin
@Entity(tableName = "media_item", indices = [Index("sortKey"), Index("category")])
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ref: String,
    val origin: String,                 // MediaOrigin.name
    val category: String,               // MediaCategory.name
    val sortKey: Long,                  // COALESCE(takenAt, addedAt)
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val entityFlags: Int,
    val fingerprint: String,            // "id:dateModified:size"
)

@Entity(tableName = "media_text")
data class MediaTextEntity(
    @PrimaryKey val mediaId: Long,
    val fullText: String,
    val searchText: String,
    val labelsText: String,
    val blocksJson: String,
)

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_item ORDER BY sortKey DESC")
    fun observeAll(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_item WHERE category = :category ORDER BY sortKey DESC")
    fun observeByCategory(category: String): Flow<List<MediaEntity>>

    @Query("SELECT category AS category, COUNT(*) AS count FROM media_item GROUP BY category")
    fun observeCategoryCounts(): Flow<List<CategoryCount>>

    @Upsert suspend fun upsert(items: List<MediaEntity>)
    @Query("DELETE FROM media_item WHERE id IN (:ids)") suspend fun delete(ids: List<Long>)
}

data class CategoryCount(val category: String, val count: Int)

@Database(
    entities = [MediaEntity::class, MediaTextEntity::class /* , IndexJobEntity, IndexExclusionEntity, VaultItemEntity … */],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
```

### 9.4 Platform builders and construction

```kotlin
// androidMain
fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = appContext.getDatabasePath("visionary.db").absolutePath,
    )
}

// iosMain
fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dir = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory, inDomain = NSUserDomainMask,
        appropriateForURL = null, create = false, error = null,
    )
    return Room.databaseBuilder<AppDatabase>(name = requireNotNull(dir?.path) + "/visionary.db")
}

// commonMain
fun buildDatabase(builder: RoomDatabase.Builder<AppDatabase>, dispatchers: AppDispatchers): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(dispatchers.io)   // [VERIFY] exact name in Room 3.0.3
        .addCallback(FtsCallback)
        .build()
```

Android only: set `android:allowBackup="false"` and exclude the database and vault directories from backup rules.

### 9.5 Full-text search

Baseline (Fallback A from the review): create an external-content FTS5 table in `Callback.onCreate`, kept in sync by triggers, so deletes and updates leave no ghost results.

```kotlin
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

- Query with `@RawQuery` taking `RoomRawQuery` (KMP). The FTS table is not a Room entity, so `Flow` results do not refresh by themselves: re-run the search when an "index changed" signal fires **[VERIFY observedEntities]**.
- Query builder: lowercase, split on non-alphanumerics, quote every token as a prefix term (`"tok"*`), never concatenate raw user input into SQL.
- The index side stores letter-digit splits (`ABC123` also indexes `abc 123`) so searching `123` finds it.
- **S1 spike order:** A) Room 3 `@Fts5` entity (platform support undocumented), B) this raw approach, C) a token table with range queries (`token >= :p AND token < :pEnd`, not `LIKE`). Pick the first that passes the acceptance test, record an ADR.
- **S1 acceptance test:** on a synthetic 10,000-item dataset, `"wifi pass"`, `"cafe muller"` vs `Café Müller` and `"123"` vs `ABC123` return the right items on Android and the iOS simulator; two-word prefix search latency is measured on the reference device and written into `docs/BENCHMARKS.md`; randomized insert/update/delete leaves the index consistent.

### 9.6 Room rules

1. All DAO functions are `suspend` or return `Flow`.
2. Writes that touch several tables use one transaction: `database.useWriterConnection { it.immediateTransaction { … } }`. Moving an item to the vault is one transaction.
3. Never `fallbackToDestructiveMigration`. Every schema change = version bump + `Migration` + migration test + committed `schemas/N.json`.
4. Entities and DAOs stay inside `:core:database`/`:data`. Mappers convert to `:domain` models at the repository boundary.
5. Do not store the original file name, text or any user content in the vault tables.
6. DAO tests use an in-memory or temp-file database through the common test harness **[VERIFY]** whether `Room.inMemoryDatabaseBuilder` exists in common Room 3; otherwise use a temp file path.
7. Keyset pagination for large lists (no `OFFSET`).

### 9.7 Real repository sketch (`:data`)

```kotlin
class RoomMediaRepository(private val db: AppDatabase) : MediaRepository {
    override fun observeMedia(kind: CollectionKind): Flow<List<MediaItem>> =
        (if (kind == CollectionKind.ALL) db.mediaDao().observeAll()
         else db.mediaDao().observeByCategory(kind.toCategory().name))
            .map { rows -> rows.map(MediaEntity::toDomain) }
    // …
}
```

---

## 10. Platform adapters (`:platform`) — summary

- One adapter class per port; Android implementation in `androidMain`, a non-crashing stub in `iosMain` until the iOS task is scheduled.
- Adapters return `AppResult` and never leak platform types.
- Android specifics you must respect:
  - MediaStore: add `RELATIVE_PATH` to the projection only on API 29+; page by keyset `(DATE_MODIFIED, _ID)`; sync with generation numbers on API 30+ and `DATE_ADDED`/`DATE_MODIFIED` with a 2-second overlap below; **do not reconcile deletions when access is denied or shrunk**.
  - Permissions: API 26–32 `READ_EXTERNAL_STORAGE` (`maxSdkVersion=32`); API 33 `READ_MEDIA_IMAGES`; API 34+ add `READ_MEDIA_VISUAL_USER_SELECTED` (partial access).
  - OCR: bundled ML Kit, one client per scope; cap total pixels (about 4 MP) instead of the long edge; split tall images (aspect over 2.5:1) into overlapping strips.
  - Scanner: ML Kit Document Scanner is delivered by Google Play services; check availability and show a message instead of crashing; copy results into `filesDir/scans/` (never MediaStore).
  - Vault: wrapped-master-key design (Keystore KEK wraps a 32-byte VMK; one biometric prompt unlocks the VMK; AES-256-GCM files with magic/version as AAD; no filenames; `FLAG_SECURE` via a `SecureScreen` port; vault images must not use the shared Coil loader).
  - Move to vault order: write temp files → verify → rename → one DB transaction (insert vault row, delete index rows, insert exclusion) → then request deletion of the original. A declined deletion keeps the item out of search through the exclusion table.
- Manifest: `tools:node="remove"` for `INTERNET` and `ACCESS_NETWORK_STATE`; CI fails if either appears in the merged release manifest.

---

## 11. Gradle and build setup (Tech Lead task S0)

### 11.1 `settings.gradle.kts`

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "visionary"

include(":androidApp", ":composeApp")
include(":core:common", ":core:mvi", ":core:navigation", ":core:designsystem", ":core:database", ":core:testing")
include(":domain", ":data", ":platform")
include(
    ":features:onboarding", ":features:home", ":features:gallery",
    ":features:search", ":features:scanner", ":features:vault", ":features:settings",
)
```

### 11.2 Convention plugins (`build-logic/`)

| Plugin id | Applies | Used by |
| --- | --- | --- |
| `visionary.kmp.library` | KMP + `com.android.kotlin.multiplatform.library`, targets `androidLibrary`, `iosArm64`, `iosSimulatorArm64`, warnings-as-errors, explicit API, ktlint, detekt, Kover | all library modules |
| `visionary.kmp.compose` | `visionary.kmp.library` + Compose Multiplatform + Compose compiler | designsystem, features, composeApp |
| `visionary.kmp.feature` | `visionary.kmp.compose` + standard feature dependencies (domain, common, mvi, navigation, designsystem, Koin, lifecycle) | features |
| `visionary.android.application` | AGP application settings, signing, release R8 | androidApp |

```kotlin
// KMP library module DSL under AGP 9 (built-in Kotlin: do NOT apply org.jetbrains.kotlin.android)
kotlin {
    androidLibrary {
        namespace = "com.o3.vanguard.visionary.core.common"   // must differ per module
        compileSdk = 36
        minSdk = 26
    }
    iosArm64(); iosSimulatorArm64()
    compilerOptions { allWarningsAsErrors.set(true) }
}
```

A feature module's `build.gradle.kts` must be this short (nothing else, no versions, no extra module deps):

```kotlin
plugins { id("visionary.kmp.feature") }
kotlin { androidLibrary { namespace = "com.o3.vanguard.visionary.features.home" } }
```

AGP 9 notes: no `BuildConfig` in KMP libraries; Compose resources need `androidResources { enable = true }` in modules that own resources; variants and signing live only in `:androidApp`.

### 11.3 Entry points

```kotlin
// :androidApp
class VisionaryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = AppConfig(BuildConfig.VERSION_NAME, BuildConfig.DEBUG, useFakeData = BuildConfig.USE_FAKES)
        startKoin { androidContext(this@VisionaryApp); modules(appModules(config)) }
    }
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}

// :composeApp iosMain
fun initKoin(useFakes: Boolean) = startKoin { modules(appModules(AppConfig("1.0", true, useFakes))) }
fun MainViewController() = ComposeUIViewController { App() }
```

`BuildConfig` is allowed in `:androidApp` only (it is an application module).

### 11.4 Quality gates

`./gradlew verifyAll` runs: ktlintCheck, detekt (or its fallback), Android Lint (warnings as errors), unit tests (Android and iOS simulator), `verifyModuleGraph`, `scripts/verify-architecture.sh` (forbidden imports), manifest check (no `INTERNET` in merged release manifest), Kover verification (80% on `:domain` and `:data`).

---

## 12. Team workflow

### 12.1 Order of work

| Phase | Owner | Output |
| --- | --- | --- |
| **S0** Skeleton | Tech Lead | repo, build-logic, version catalog, all modules empty but compiling for Android and iOS, `verifyAll`, "Hello pipeline" app launches on both platforms, **[VERIFY] items resolved in this file** |
| **S1** DB spike | Tech Lead | Room 3 + FTS option chosen, ADR, benchmark |
| **S2** Domain + fakes | Tech Lead | section 5 contracts, all `Fake*`, `FakeData`, `fakeDataModule` |
| **S3** Nav + feature skeletons | Tech Lead | section 7, 8.3: every feature wired, navigable end to end on fakes |
| **UI sprints** | Developers | each developer owns features (below), builds only against fakes |
| **Integration** | each owner | swap fake → real one binding at a time; UI untouched |

Developers start only after S3 is merged. Until then, read this file and prepare.

### 12.2 Ownership **[DECIDE: final split]**

| Owner | Features / modules |
| --- | --- |
| Dev 1 | `:core:designsystem`, `onboarding`, `home`, `gallery` |
| Dev 2 | OCR / labeling / scanner adapters, classifier, entity extractor, worker (`:platform`, `:domain`), `scanner` |
| Dev 3 | MediaStore + permissions, sync (`:platform`, `:data`), `search`, `settings` |
| Dev 4 | vault crypto/biometrics (`:platform`), `VaultRepository` (`:data`), `vault` |
| Tech Lead | `:domain` contracts, `:core:*`, `:core:database`, `IndexingCoordinator`, build-logic, CI, reviews |

Two people never edit the same module in the same PR. Cross-module changes go through a PR the owner of the other module reviews.

### 12.3 Git

- Trunk-based: short-lived branches `feat/<area>-<topic>`, `fix/…`, `chore/…`, merged to `main` by PR within a day or two. No long-lived branches.
- Conventional commits: `feat(home): add collection cards`.
- One PR = one concern, under about 400 changed lines where possible.
- PR must: pass CI, include tests for logic, include previews for UI, link the task, update docs if behavior or contracts changed.
- Squash merge. No force-push to `main`.

### 12.4 Definition of done (every PR)

1. `./gradlew verifyAll` passes locally and in CI.
2. New logic has unit tests; ViewModels are tested with fakes (Turbine for flows); coverage floor still met on `:domain` and `:data`.
3. UI changes include previews for Loading / Empty / Error / Content, light and dark.
4. Strings externalized; content descriptions present.
5. No new dependency, no suppressed warning without a comment explaining why and the Tech Lead's approval.
6. Docs or ADR updated if an architecture decision changed.

---

## 13. Testing guide

| Layer | How |
| --- | --- |
| `:domain` | pure `commonTest`, no mocks (use fakes), runs on Android and iOS simulator |
| ViewModels | `commonTest` with fakes + `kotlinx-coroutines-test` + Turbine; test every intent and the four states |
| DAOs / repositories | database tests against a real (in-memory or temp-file) Room database, including migrations |
| Adapters | contract tests per port: same test suite run against the Android adapter on device/emulator |
| Navigator | `commonTest` with a plain list; route registration test on JVM |
| UI | previews are mandatory; screenshot/UI tests are optional and added by the Tech Lead later |
| Search quality | golden dataset of synthetic images with expected categories and expected hits |

Test names: ``fun `given X when Y then Z`()``. Test data is synthetic. Never commit real photos.

---

## 14. Stop and ask the Tech Lead when…

- A frozen contract (section 5) needs a change.
- You need a new dependency, module, permission, or a new `expect`/`actual`.
- You are about to suppress a warning, lint rule or detekt rule.
- A **[VERIFY]** item contradicts reality.
- A requirement seems to conflict with the privacy promise.
- You would touch another owner's module.
- The task seems to need Phase 2 features.

When stopping, post: what you tried, what you found, the 1–2 options you see, your recommendation.

---

## 15. Instructions for AI coding assistants

1. Read this file first. Re-read the relevant section before each task.
2. Check the task card; do exactly that task, no drive-by refactors.
3. Never invent library APIs. If an API name in this file or in your memory might be wrong, compile and fix it, then **update this file** if the fix reveals a wrong statement.
4. Write the failing test first when adding logic.
5. Prefer the smallest change; keep modules within section 3.1.
6. Do not generate or commit secrets, keystores, real media, or user data.
7. Do not add logging of content, analytics, network code or TODO stubs.
8. Before saying "done": run `./gradlew verifyAll`, list what you ran and what you could not run.
9. When a fact is marked **[VERIFY]**, do not rely on it; verify it or ask.

---

## 16. Initial task list (copy into the tracker)

| ID | Task | Owner | Depends on |
| --- | --- | --- | --- |
| S0 | Repo skeleton, build-logic, catalog, `verifyAll`, Hello pipeline on both platforms, resolve all [VERIFY] | Tech Lead | — |
| S1 | Room 3 + FTS spike, ADR, benchmark | Tech Lead | S0 |
| S2 | Domain contracts + fakes + `fakeDataModule` | Tech Lead | S0 |
| S3 | `AppRoute`, `Navigator`, `AppNavHost`, seven feature skeletons wired | Tech Lead | S2 |
| D1 | Design system tokens + components + fake image fetcher | Dev 1 | S3 |
| D1a | Onboarding UI | Dev 1 | D1 |
| D1b | Home UI | Dev 1 | D1 |
| D1c | Gallery grid + viewer UI | Dev 1 | D1 |
| D3a | Search UI | Dev 3 | S3 |
| D3b | Settings + Index health UI | Dev 3 | S3 |
| D2a | Scanner UI | Dev 2 | S3 |
| D4a | Vault lock + grid + viewer UI | Dev 4 | S3 |
| R-a | Real Room repositories (`:data`) | Dev 3 | S1 |
| R-b | MediaStore + permissions adapters | Dev 3 | S0 |
| R-c | OCR + labeling + classifier + entity extractor | Dev 2 | S0 |
| R-d | Indexing worker + coordinator | Dev 2 / Tech Lead | R-a, R-c |
| R-e | Vault crypto + repository | Dev 4 | S1 |
| R-f | Switch default `useFakeData=false`, integration pass | everyone | R-a..R-e |

---

## 17. Open items

1. **[DECIDE]** iOS runtime scope for the MVP (compile-only stubs vs full iOS delivery).
2. **[DECIDE]** Delete/Restore inside the MVP.
3. **[DECIDE]** Final developer ownership split (section 12.2).
4. **[VERIFY]** Items tagged in sections 2, 7.5, 9.4, 9.6, plus: bundled ML Kit and the `INTERNET` permission; Document Scanner behavior offline after first download; `BIOMETRIC_STRONG | DEVICE_CREDENTIAL` behavior per API level.

When an item is resolved, edit this file in the same PR and remove the tag.
