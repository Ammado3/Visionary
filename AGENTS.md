# Visionary Developer & Agent Guide

## Module Structure
- **`:androidApp`**: Android application entry point.
- **`:composeApp`**: Compose Multiplatform application UI & DI wiring.
- **`:core:common`**: Core primitives, AppResult, AppError, AppDispatchers, VectorSearchEngine.
- **`:core:database`**: Room 3 database entities, DAOs, AppDatabase.
- **`:core:mvi`**: BaseMviViewModel, UiState, UiIntent, UiEvent.
- **`:core:testkit`**: Shared unit test support module (`runViewModelTest`, `collectEvents`, model factories, fakes, `AppResult` assertions).
- **`:domain`**: Pure Kotlin domain models, repository interfaces, ports, and use cases.
- **`:data`**: Data layer repository implementations.
- **`:features:*`**: Feature modules (`gallery`, `home`, `onboarding`, `scanner`, `search`, `settings`, `vault`, `teleport`).

## Commands That Work Today
- **Run all Android host unit tests**:
  ```bash
  ./gradlew testAndroidHostTest
  ```
- **Build Android debug app**:
  ```bash
  ./gradlew assembleDebug
  ```

## Testing Architecture & Guidelines
1. **Host Unit Testing**:
   - All KMP library modules use `com.android.kotlin.multiplatform.library` with host tests enabled (`withHostTest {}`).
   - Run tests using `./gradlew testAndroidHostTest`.
2. **Test Dependencies & `:core:testkit`**:
   - Test dependencies (`kotlin("test")`, `kotlinx-coroutines-test`, `turbine`, `:core:testkit`) are automatically injected into `commonTest` source sets via `KmpLibraryConventionPlugin`.
   - Use `:core:testkit` for shared fakes, factories, assertions, and test dispatchers. Never add `:core:testkit` as a production dependency (`commonMain` / `androidMain`).
3. **No Mocking Libraries / Handwritten Fakes**:
   - Use deterministic, stateful handwritten fakes from `:core:testkit` or feature-specific test doubles.
4. **ViewModel Tests**:
   - Wrap ViewModel tests with `runViewModelTest` and collect one-shot MVI events using `collectEvents(viewModel)`.
5. **Data Layer / Room Testing Note**:
   - In-memory Room 3 databases using `BundledSQLiteDriver` require native SQLite binary loading on the target platform. For host JVM unit testing, repository/use case unit tests use handwritten fakes (e.g. `FakeMediaRepository`), while full Room SQLite integration tests run as instrumented Android tests (`androidTest`).
