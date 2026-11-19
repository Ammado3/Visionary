# Repository Guidelines

## Project Structure & Module Organization

Visionary is a Kotlin Multiplatform project targeting Android and iOS. Application entry points live in `androidApp/` and `iosApp/`; shared Compose UI and dependency wiring live in `composeApp/`. Reusable layers are split into `core/` (`common`, `mvi`, `navigation`, `designsystem`, `database`, and `testing`), `domain/`, `data/`, and `platform/`. Product areas are isolated under `features/` (for example, `features/home` and `features/vault`). Put shared Kotlin in `src/commonMain`; use `src/androidMain` or `src/iosMain` only for platform-specific implementations. Build conventions are maintained in `build-logic/`.

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

## Testing Guidelines

Tests are not currently checked in. New shared tests should go under a module's `src/commonTest`, with platform-specific tests in `src/androidHostTest` or the relevant iOS test source set. Name test classes after the subject and behavior, such as `VaultViewModelTest`. Run module tests with `.\gradlew.bat :<module>:allTests` or the platform-specific task exposed by Gradle.

## Commit & Pull Request Guidelines

Recent commits use concise imperative summaries, for example `Restructure project into modular Kotlin Multiplatform architecture`. Keep commits focused and describe the affected module. Pull requests should explain the change and validation performed, link the relevant issue, and include screenshots or a short recording for UI changes. Call out Android/iOS behavior differences and any migration or configuration steps.

## Security & Configuration Tips

Do not commit secrets, signing files, or machine-specific values. Keep local SDK paths in `local.properties`; review dependency and database-schema changes carefully before merging.
