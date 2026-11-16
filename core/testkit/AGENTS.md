# `:core:testkit` Module Guide

## Purpose
`:core:testkit` is the single shared test-support module for unit testing across all Visionary modules (`:domain`, `:data`, `:core:*`, `:features:*`). It centralizes test dispatchers, ViewModel test helpers, domain model factories, assertion extensions, and handwritten fakes.

## Contents
- **`dispatcher/`**: `TestAppDispatchers` and `runViewModelTest`.
- **`mvi/`**: `collectEvents` extension for `BaseMviViewModel`.
- **`factory/`**: Defaulted builders for domain model objects (`testMediaItem`, `testCollectionSummary`, `testVaultItem`, `testAppSettings`, `testScannedDocument`).
- **`fake/`**: Deterministic, stateful fakes for all domain repositories and ports.
- **`assert/`**: `assertOk()` and `assertErr()` extensions for `AppResult`.

## Rules for Adding Fakes
1. **Never a production dependency**: `:core:testkit` must ONLY be included in `commonTest` or `androidHostTest` source sets. Never add `implementation(project(":core:testkit"))` to `commonMain` or `androidMain`.
2. **Deterministic & stateful**: Fakes must use `MutableStateFlow` or in-memory lists. No `delay()`, no randomness, and no canned scenario flows.
3. **No mocking libraries**: All fakes must be handwritten.
4. **Shared threshold**: Only place fakes in `:core:testkit` if 2 or more modules require them. Feature-specific doubles belong in that feature's `commonTest` source set.
