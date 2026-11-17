package com.o3.vanguard.visionary.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.o3.vanguard.visionary.domain.model.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataStoreSettingsRepositoryTest {

    private fun tempFile(): okio.Path {
        val name = "settings-test-${Random.nextLong().toString().replace("-", "0")}.preferences_pb"
        return (FileSystem.SYSTEM_TEMPORARY_DIRECTORY.toString() + "/" + name).toPath()
    }

    @Test
    fun observe_emptyStore_returnsDefaultAppSettings() = runTest {
        val file = tempFile()
        try {
            val dataStore = PreferenceDataStoreFactory.createWithPath(
                scope = backgroundScope,
                produceFile = { file },
            )
            val repository = DataStoreSettingsRepository(dataStore)

            assertEquals(AppSettings(), repository.observe().first())
        } finally {
            FileSystem.SYSTEM.delete(file, mustExist = false)
        }
    }

    @Test
    fun update_onboardingDone_updatesField() = runTest {
        val file = tempFile()
        try {
            val dataStore = PreferenceDataStoreFactory.createWithPath(
                scope = backgroundScope,
                produceFile = { file },
            )
            val repository = DataStoreSettingsRepository(dataStore)

            repository.update { it.copy(onboardingDone = true) }

            assertTrue(repository.observe().first().onboardingDone)
        } finally {
            FileSystem.SYSTEM.delete(file, mustExist = false)
        }
    }

    @Test
    fun persistenceAcrossInstances_secondInstanceReadsSavedValues() = runTest {
        val file = tempFile()
        try {
            val job1 = Job()
            val scope1 = CoroutineScope(coroutineContext + job1)
            val dataStore1 = PreferenceDataStoreFactory.createWithPath(
                scope = scope1,
                produceFile = { file },
            )
            val repo1 = DataStoreSettingsRepository(dataStore1)

            repo1.update { it.copy(onboardingDone = true, vaultLockTimeoutSeconds = 120) }
            job1.cancel()

            val job2 = Job()
            val scope2 = CoroutineScope(coroutineContext + job2)
            val dataStore2 = PreferenceDataStoreFactory.createWithPath(
                scope = scope2,
                produceFile = { file },
            )
            val repo2 = DataStoreSettingsRepository(dataStore2)

            val settings = repo2.observe().first()
            assertTrue(settings.onboardingDone)
            assertEquals(120, settings.vaultLockTimeoutSeconds)

            job2.cancel()
        } finally {
            FileSystem.SYSTEM.delete(file, mustExist = false)
        }
    }

    @Test
    fun twoUpdates_changingDifferentFields_bothValuesPresent() = runTest {
        val file = tempFile()
        try {
            val dataStore = PreferenceDataStoreFactory.createWithPath(
                scope = backgroundScope,
                produceFile = { file },
            )
            val repository = DataStoreSettingsRepository(dataStore)

            repository.update { it.copy(onboardingDone = true) }
            repository.update { it.copy(indexOnlyWhileCharging = true) }

            val settings = repository.observe().first()
            assertTrue(settings.onboardingDone)
            assertTrue(settings.indexOnlyWhileCharging)
        } finally {
            FileSystem.SYSTEM.delete(file, mustExist = false)
        }
    }

    @Test
    fun vaultLockTimeoutSeconds_roundTripsNonDefaultValue() = runTest {
        val file = tempFile()
        try {
            val dataStore = PreferenceDataStoreFactory.createWithPath(
                scope = backgroundScope,
                produceFile = { file },
            )
            val repository = DataStoreSettingsRepository(dataStore)

            repository.update { it.copy(vaultLockTimeoutSeconds = 120) }

            assertEquals(120, repository.observe().first().vaultLockTimeoutSeconds)
        } finally {
            FileSystem.SYSTEM.delete(file, mustExist = false)
        }
    }
}
