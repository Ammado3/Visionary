This is a Kotlin Multiplatform project targeting Android and iOS.

* [/iosApp](./iosApp) contains the iOS application entry point (SwiftUI wrapper around the shared Compose UI).
* [/composeApp](./composeApp) contains the shared Compose Multiplatform application UI and builds the `Shared` framework for iOS.
* [/core](./core) contains shared primitives, database (Room), navigation, MVI base classes, design system, and testkit.
* [/domain](./domain) contains pure Kotlin domain models, repository interfaces, and use cases.
* [/data](./data) contains repository implementations.
* [/platform](./platform) contains platform-specific implementations and bridges.
* [/features](./features) contains modularized feature sets (`gallery`, `home`, `onboarding`, `scanner`, `search`, `settings`, `teleport`, `vault`).

### Running the apps

- **Android app**: `./gradlew :androidApp:assembleDebug`
- **iOS app**: Open `iosApp/iosApp.xcodeproj` in Xcode or run via Xcode CLI targeting your connected iOS device or simulator.

### Running tests

- **Host Unit tests**: `./gradlew testAndroidHostTest`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
