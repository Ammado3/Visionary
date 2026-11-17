package com.o3.vanguard.visionary.domain

import com.o3.vanguard.visionary.core.testkit.factory.testMediaItem
import com.o3.vanguard.visionary.core.testkit.fake.FakeMediaDeleteGateway
import com.o3.vanguard.visionary.core.testkit.fake.FakeMediaRepository
import com.o3.vanguard.visionary.core.testkit.fake.FakeTrashRepository
import com.o3.vanguard.visionary.domain.model.DeleteConsentResult
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.MediaItem
import com.o3.vanguard.visionary.domain.model.TrashItem
import com.o3.vanguard.visionary.domain.repository.TrashRepository
import com.o3.vanguard.visionary.domain.usecase.DeleteMediaUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DeleteMediaUseCaseTest {

    @Test
    fun invoke_withDeletedResult_deletesMediaAndLeavesInTrash() = runTest {
        val mediaDeleteGateway = FakeMediaDeleteGateway()
        val mediaRepository = FakeMediaRepository()
        val trashRepository = FakeTrashRepository()
        mediaDeleteGateway.nextResult = DeleteConsentResult.Deleted

        val useCase = DeleteMediaUseCase(mediaDeleteGateway, mediaRepository, trashRepository)
        val item1 = testMediaItem(id = MediaId(1L), uri = "content://1")
        val item2 = testMediaItem(id = MediaId(2L), uri = "content://2")

        val result = useCase(listOf(item1, item2))

        assertEquals(DeleteConsentResult.Deleted, result)
        assertEquals(listOf(MediaId(1L), MediaId(2L)), mediaRepository.deleteMediaCalls)
        assertEquals(2, trashRepository.trashItems.value.size)
        assertEquals(MediaId(1L), trashRepository.trashItems.value[0].id)
        assertEquals(MediaId(2L), trashRepository.trashItems.value[1].id)
    }

    @Test
    fun invoke_withDeclinedByUserResult_rollsBackTrashAndDoesNotDeleteMedia() = runTest {
        val mediaDeleteGateway = FakeMediaDeleteGateway()
        val mediaRepository = FakeMediaRepository()
        val trashRepository = FakeTrashRepository()
        mediaDeleteGateway.nextResult = DeleteConsentResult.DeclinedByUser

        val useCase = DeleteMediaUseCase(mediaDeleteGateway, mediaRepository, trashRepository)
        val item1 = testMediaItem(id = MediaId(1L), uri = "content://1")

        val result = useCase(listOf(item1))

        assertEquals(DeleteConsentResult.DeclinedByUser, result)
        assertTrue(mediaRepository.deleteMediaCalls.isEmpty())
        assertTrue(trashRepository.trashItems.value.isEmpty())
    }

    @Test
    fun invoke_withEmptyList_returnsDeletedAndMakesNoGatewayCall() = runTest {
        val mediaDeleteGateway = FakeMediaDeleteGateway()
        val mediaRepository = FakeMediaRepository()
        val trashRepository = FakeTrashRepository()

        val useCase = DeleteMediaUseCase(mediaDeleteGateway, mediaRepository, trashRepository)

        val result = useCase(emptyList())

        assertEquals(DeleteConsentResult.Deleted, result)
        assertTrue(mediaDeleteGateway.deleteRequests.isEmpty())
    }

    @Test
    fun invoke_whenCancelledInMoveToTrash_rethrowsCancellationException() = runTest {
        val mediaDeleteGateway = FakeMediaDeleteGateway()
        val mediaRepository = FakeMediaRepository()
        val cancellingTrashRepository = object : TrashRepository {
            override fun observeTrashItems(): Flow<List<TrashItem>> = emptyFlow()
            override suspend fun moveToTrash(item: MediaItem) {
                throw CancellationException("Cancelled")
            }
            override suspend fun deleteFromTrash(id: MediaId) {}
            override suspend fun restoreFromTrash(id: MediaId) {}
        }

        val useCase = DeleteMediaUseCase(mediaDeleteGateway, mediaRepository, cancellingTrashRepository)
        val item = testMediaItem(id = MediaId(1L))

        assertFailsWith<CancellationException> {
            useCase(listOf(item))
        }
    }
}
