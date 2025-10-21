package com.printscript.tests.service

import com.printscript.tests.clients.PermissionClient
import com.printscript.tests.domain.TestCaseEntity
import com.printscript.tests.domain.TestCaseRepository
import com.printscript.tests.dto.CreateTestRequest
import com.printscript.tests.dto.UpdateTestRequest
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional

class TestServiceTest {

    private val repo = mockk<TestCaseRepository>()
    private val snippetClient = mockk<PermissionClient>()
    private lateinit var service: TestService

    @BeforeEach
    fun setup() {
        service = TestService(repo, snippetClient)
    }

    @Test
    fun testCreateSavesAndReturnsResponse() {
        // true to u1 can write
        every { snippetClient.canWrite("u1", 42L) } returns true

        val saved = TestCaseEntity(
            id = 100L,
            snippetId = 42L,
            name = "suma",
            inputs = listOf("2", "3"),
            expectedOutputs = listOf("5"),
            targetVersionNumber = 3L,
            createdBy = "u1",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            lastRunStatus = "NEVER_RUN",
            lastRunOutput = null,
            lastRunAt = null,
        )
        every { repo.save(any<TestCaseEntity>()) } returns saved

        val req = CreateTestRequest("suma", listOf("2", "3"), listOf("5"), 3L)
        val resp = service.create(42L, req, "u1")

        assertEquals(100L, resp.id)
        assertEquals("suma", resp.name)
        assertEquals(listOf("2", "3"), resp.inputs)
        assertEquals(listOf("5"), resp.expectedOutputs)
        assertEquals(3L, resp.targetVersionNumber)
        assertEquals("NEVER_RUN", resp.lastRunStatus)
        verify { repo.save(match { it.name == "suma" && it.snippetId == 42L }) }
        verify { snippetClient.canWrite("u1", 42L) }
        confirmVerified(repo, snippetClient)
    }

    @Test
    fun testUpdate() {
        every { snippetClient.canWrite("u1", 42L) } returns true
        val current = TestCaseEntity(
            id = 10L, snippetId = 42L, name = "old",
            inputs = listOf("1"), expectedOutputs = listOf("1"),
            targetVersionNumber = null,
            createdBy = "u1", createdAt = Instant.now(), updatedAt = Instant.now(),
        )
        every { repo.findById(10L) } returns Optional.of(current)
        every { repo.save(any<TestCaseEntity>()) } answers { firstArg() }

        val req = UpdateTestRequest(
            name = "nuevo",
            inputs = listOf("2", "3"),
            expectedOutputs = listOf("5"),
            targetVersionNumber = 4L,
        )
        val resp = service.update(10L, 42L, req, "u1")

        assertEquals("nuevo", resp.name)
        assertEquals(listOf("2", "3"), resp.inputs)
        assertEquals(listOf("5"), resp.expectedOutputs)
        assertEquals(4L, resp.targetVersionNumber)
    }

    @Test
    fun testDelete() {
        every { snippetClient.canWrite("u1", 42L) } returns true
        val entity = TestCaseEntity(
            id = 10L, snippetId = 42L, name = "x",
            inputs = emptyList(), expectedOutputs = emptyList(),
            targetVersionNumber = null, createdBy = "u1",
            createdAt = Instant.now(), updatedAt = Instant.now(),
        )
        every { repo.findById(10L) } returns Optional.of(entity)
        every { repo.delete(entity) } just runs

        service.delete(10L, 42L, "u1")
        verify { repo.delete(entity) }
    }

    @Test
    fun testGetTestsSummaryCalculatesTotals() {
        every { snippetClient.canRead("u1", 42L) } returns true
        val e1 = TestCaseEntity(
            1L,
            42L,
            "a",
            emptyList(),
            emptyList(),
            null,
            "u1",
            Instant.now(),
            Instant.now(),
            "PASSED",
            null,
            null,
        )
        val e2 = TestCaseEntity(
            2L,
            42L,
            "b",
            emptyList(),
            emptyList(),
            null,
            "u1",
            Instant.now(),
            Instant.now(),
            "FAILED",
            null,
            null,
        )
        val e3 = TestCaseEntity(
            3L,
            42L,
            "c",
            emptyList(),
            emptyList(),
            null,
            "u1",
            Instant.now(),
            Instant.now(),
            "ERROR",
            null,
            null,
        )
        every { repo.findBySnippetId(42L) } returns listOf(e1, e2, e3)

        val summary = service.getTestsSummary(42L, "u1")
        assertEquals(3, summary.total)
        assertEquals(1, summary.passed)
        assertEquals(1, summary.failed)
        assertEquals(1, summary.error)
    }
}
