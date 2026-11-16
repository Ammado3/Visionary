# Vault & Media Deletion Architecture Documentation

This document outlines the end-to-end technical implementation of the **Secure Private Vault** feature and **Android Scoped Storage Media Deletion** in the **Visionary** application.

---

## 1. Overview of "Move to Vault" Workflow

When a user selects an image/video in the gallery viewer and taps **"Add to" -> "Favorites" (Move to Vault)**, the following transactional sequence occurs:

1. **Item Lookup**: The media item is fetched from the Room database (`MediaDao`) using its unique `MediaId`.
2. **Secure Internal Copying (`VaultCryptoEngine`)**:
   - The original media stream is opened via `ContentResolver` and copied into the app's private internal secure storage directory (`vault_blobs` via `AppDirectories`).
   - This ensures the file is preserved securely inside app-specific storage before any public deletion occurs.
3. **Database Insertion (`VaultRepository`)**:
   - A `VaultItemEntity` record is created and inserted into the `vault_item` table with metadata (ID, dimensions, MIME type, timestamps).
4. **Public Gallery Deletion (`MediaDeleteGateway`)**:
   - The app requests deletion of the original file from the phone's public gallery/external storage.
5. **Database Cleanup**:
   - Upon successful public deletion, the item record and its OCR/text index are removed from the main `media_item` and `media_text` tables so it no longer appears in regular gallery collections or search results.
6. **Rollback Mechanism**:
   - If public deletion fails or the user declines the delete permission, the app automatically performs a rollback (deleting the temporary vault blob and aborting the database transaction) to maintain data consistency.

---

## 2. Android Scoped Storage & Permanent Deletion Logic

Deleting files owned by other apps (or captured via Camera/System Gallery) on modern Android (Android 10+ / API 29 through Android 16+) requires specific Scoped Storage handling.

### The Challenge
- On Android 11+ (API 30+), calling `ContentResolver.delete()` on media files created by other apps silently returns `0` or fails without throwing exceptions.
- Direct file path operations (`File.delete()`) fail due to Scoped Storage sandbox restrictions.

### The Solution: MediaStore Delete Request & Activity Result API
1. **Direct Attempt**: The gateway first attempts standard `ContentResolver.delete()`.
2. **System Delete Request (`MediaStore.createDeleteRequest`)**:
   - If direct deletion fails, the app uses `MediaStore.createDeleteRequest(contentResolver, uris)` to generate a system `PendingIntent`.
   - This intent prompts the user with an official Android system dialog asking for permission to delete the photo(s).
3. **Main Thread Enforcement (`Dispatchers.Main`)**:
   - Android Activity Result launchers (`launcher.launch()`) **must** be executed on the Main/UI thread. We explicitly wrap launcher calls in `CoroutineScope(Dispatchers.Main).launch` to prevent silent thread-dispatch failures.
4. **Activity Result Handling (`ActivityResultContracts.StartIntentSenderForResult`)**:
   - `MainActivity` registers an `ActivityResultLauncher` that listens for the user's response in the system delete dialog (`RESULT_OK` vs `RESULT_CANCELED`).
   - This result is passed back to `AndroidMediaDeleteGateway` via a callback continuation to resume the coroutine (either completing the deletion or aborting).

---

## 3. Permissions & Lifecycle Management

- **Required Permissions**:
  - `READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` (for reading and indexing gallery items).
  - No explicit `WRITE_EXTERNAL_STORAGE` permission is needed on Android 10+ because deletion is handled via `MediaStore` delete requests.
- **Activity Reference & Cleanup (`MainActivity`)**:
   - `AndroidMediaDeleteGateway.currentLauncher` and `AndroidBiometricAuthenticator.currentActivityProvider` are registered during `MainActivity.onCreate()`.
   - To prevent memory leaks during configuration changes or activity destruction (`onDestroy`), these references are cleared when `isFinishing` is true.

---

## 4. Room Database Migration (Version 1 to 2)

- **Schema Change**: Added `mime: String?` to media and vault item entities.
- **Migration Strategy**: Bumped Room database `version = 2` in `AppDatabase.kt` with auto-migration support to accommodate updated entity schemas without data loss.
