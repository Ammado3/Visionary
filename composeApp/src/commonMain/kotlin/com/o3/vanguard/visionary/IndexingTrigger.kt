package com.o3.vanguard.visionary

/**
 * Whether the shared UI starts indexing once the start route is known.
 *
 * Each platform has exactly one trigger:
 * - Android: `MainActivity` starts `IndexingForegroundService`, so this is `false`;
 * - iOS: there is no background service yet, so `App` starts it.
 */
internal expect val startsIndexingFromApp: Boolean
