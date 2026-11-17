package com.o3.vanguard.visionary.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.o3.vanguard.visionary.domain.model.AppSettings
import com.o3.vanguard.visionary.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import okio.IOException

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private companion object {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val INDEX_ONLY_WHILE_CHARGING = booleanPreferencesKey("index_only_while_charging")
        val VAULT_LOCK_TIMEOUT_SECONDS = intPreferencesKey("vault_lock_timeout_seconds")

        val defaults = AppSettings()
    }

    private fun Preferences.toAppSettings(): AppSettings = AppSettings(
        onboardingDone = this[ONBOARDING_DONE] ?: defaults.onboardingDone,
        indexOnlyWhileCharging = this[INDEX_ONLY_WHILE_CHARGING] ?: defaults.indexOnlyWhileCharging,
        vaultLockTimeoutSeconds = this[VAULT_LOCK_TIMEOUT_SECONDS] ?: defaults.vaultLockTimeoutSeconds,
    )

    override fun observe(): Flow<AppSettings> = dataStore.data
        .catch { e ->
            if (e is IOException) {
                emit(emptyPreferences())
            } else {
                throw e
            }
        }
        .map { prefs -> prefs.toAppSettings() }
        .distinctUntilChanged()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toAppSettings())
            prefs[ONBOARDING_DONE] = updated.onboardingDone
            prefs[INDEX_ONLY_WHILE_CHARGING] = updated.indexOnlyWhileCharging
            prefs[VAULT_LOCK_TIMEOUT_SECONDS] = updated.vaultLockTimeoutSeconds
        }
    }
}
