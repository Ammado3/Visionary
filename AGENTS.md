# Repository Guidelines

## Project Structure & Module Organization

Visionary is a Kotlin Multiplatform project targeting Android and iOS. Application entry points live in `androidApp/` and `iosApp/`; shared Compose UI and dependency wiring live in `composeApp/`. Reusable layers are split into `core/` (`common`, `mvi`, `navigation`, `designsystem`, `database`, and `testkit`), `domain/`, `data/`, and `platform/`. Product areas are isolated under `features/` (for example, `features/home` and `features/vault`). Put shared Kotlin in `src/commonMain`; use `src/androidMain` or `src/iosMain` only for platform-specific implementations. Build conventions are maintained in `build-logic/`.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper from the repository root:

```powershell
.\gradlew.bat build
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :androidApp:installDebug
```

`build` compiles and validates all configured modules, `assembleDebug` creates the Android debug APK, and `installDebug` installs it on a connected emulator/device. Open `iosApp/` in Xcode to build and run the iOS application. Run targeted Gradle tasks for a module while iterating, such as `.\gradlew.bat :features:home:build`.

## Coding Style & Naming Conventions

Follow Kotlin conventions with four-space indentation, trailing commas in multiline declarations, and explicit meaningful names. Use `PascalCase` for classes, composables, and modules; `camelCase` for functions, properties, and parameters; and `UPPER_SNAKE_CASE` for constants. Keep feature files grouped by responsibility (`*Screen`, `*ViewModel`, `*Contract`, `*Module`). Preserve the existing package root `com.o3.vanguard.visionary`. The build enables all compiler warnings as errors, so resolve warnings before committing.

## Architecture Rules

- **Module dependencies:** features depend on `:domain` and `:core:*` only, never on `:data`, `:core:database` or `:platform`. (Known exception: `:features:onboarding` imports `AppPermissionGate` from `:platform`; to be removed.) `:domain` depends only on `:core:common`.
- **Use cases:** add a use case only when it combines several repositories or ports, or contains business logic (for example `DeleteMediaUseCase`). For a single read or write, a ViewModel may call the repository directly. Don't add pass-through use cases.
- **Koin:** use `viewModelOf` / `singleOf` / `factoryOf` with constructor references and `bind` for interfaces. Use a lambda only for runtime parameters, with named arguments.
- **Navigation:** every route is an `AppRoute` in `:core:navigation`. When you add one, register it in `AppRouteSerializersModule` and add a sample to `AppRouteSerializationTest`. Tab ↔ route mapping lives only in `TabNavigation.kt`; switch tabs with `navigator.navigateToTab(tab)`.
- **Packages:** a module's package is `com.o3.vanguard.visionary.<layer>.<module>`, matching its folder name (for example `features/gallery` → `com.o3.vanguard.visionary.features.gallery`). Use correct English spelling in package and file names. Known exceptions, to be renamed in a dedicated PR: `feature.teleport` (should be `features.teleport`), `features.search` in `features/aisearch`, `features.vault.valutscreen`, `core.designsystem.component.customekeyboard`.
- **Screen files:** each screen has `<Name>Screen.kt` (Route + stateless Screen + Preview), `<Name>ViewModel.kt`, and its State, Intent and Event types. For new screens, put those three types in one `<Name>Contract.kt`.

## Testing Guidelines

Shared test fakes, factories and MVI helpers live in `:core:testkit`, which the build adds to every module's `commonTest` automatically. Production code must never depend on `:core:testkit`. New shared tests should go under a module's `src/commonTest`, with platform-specific tests in `src/androidHostTest` or the relevant iOS test source set. Name test classes after the subject and behavior, such as `VaultViewModelTest`. Run module tests with `.\gradlew.bat :<module>:allTests` or the platform-specific task exposed by Gradle.

## Commit & Pull Request Guidelines

Recent commits use concise imperative summaries, for example `Restructure project into modular Kotlin Multiplatform architecture`. Keep commits focused and describe the affected module. Pull requests should explain the change and validation performed, link the relevant issue, and include screenshots or a short recording for UI changes. Call out Android/iOS behavior differences and any migration or configuration steps.

## Security & Configuration Tips

Do not commit secrets, signing files, or machine-specific values. Keep local SDK paths in `local.properties`; review dependency and database-schema changes carefully before merging.
