package com.printscript.tests.service

import com.printscript.tests.clients.ExecutionClient
import com.printscript.tests.clients.PermissionClient
import com.printscript.tests.domain.TestCaseEntity
import com.printscript.tests.domain.TestCaseRepository
import com.printscript.tests.domain.TestRunEntity
import com.printscript.tests.domain.TestRunRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional

class TestRunnerServiceTest {
    // mock de todas excepto service que es la que se testea
    private val testCaseRepo = mockk<TestCaseRepository>()
    private val testRunRepo = mockk<TestRunRepository>()
    private val exec = mockk<ExecutionClient>()
    private val snippetClient = mockk<PermissionClient>()
    private lateinit var service: TestRunnerService

    @BeforeEach
    fun setup() {
        service = TestRunnerService(testCaseRepo, testRunRepo, exec, snippetClient)
    }

    private fun helperTestCaseEntityCreator() = TestCaseEntity(
        id = 10L, snippetId = 42L, name = "suma",
        inputs = listOf("2", "3"),
        expectedOutputs = listOf("5"),
        targetVersionNumber = 3L,
        createdBy = "u1", createdAt = Instant.now(), updatedAt = Instant.now(),
        lastRunStatus = "NEVER_RUN", lastRunOutput = null, lastRunAt = null,
    )

    @Test
    fun testRunSinglePassed() {
        // u1 si puede leer
        every { snippetClient.canRead("u1", 42L) } returns true
        every { testCaseRepo.findById(10L) } returns Optional.of(helperTestCaseEntityCreator())
        every { exec.execute(42L, listOf("2", "3")) } returns listOf("5")
        every { testRunRepo.save(any<TestRunEntity>()) } answers { firstArg() }
        every { testCaseRepo.save(any()) } answers { firstArg() }

        val resp = service.runSingle(10L, "u1")
        assertEquals("PASSED", resp.status)
        assertEquals(listOf("5"), resp.outputs)
        assertEquals(listOf("2", "3"), resp.inputs)
    }

    @Test
    fun testRunSingleFail() {
        every { snippetClient.canRead("u1", 42L) } returns true
        every { testCaseRepo.findById(10L) } returns Optional.of(helperTestCaseEntityCreator())
        every { exec.execute(42L, listOf("2", "3")) } returns listOf("7")
        every { testRunRepo.save(any<TestRunEntity>()) } answers { firstArg() }
        every { testCaseRepo.save(any()) } answers { firstArg() }

        val resp = service.runSingle(10L, "u1")
        assertEquals("FAILED", resp.status)
        assertEquals(listOf("7"), resp.outputs)
    }

    @Test
    fun testRunSingleError() {
        every { snippetClient.canRead("u1", 42L) } returns true
        every { testCaseRepo.findById(10L) } returns Optional.of(helperTestCaseEntityCreator())
        every { exec.execute(42L, any()) } throws IllegalStateException("error msg")
        every { testRunRepo.save(any<TestRunEntity>()) } answers { firstArg() }
        every { testCaseRepo.save(any()) } answers { firstArg() }
        // si el executor lanza, el servicio captura, marca ERROR,
        // propaga el mensaje y no devuelve outputs.
        val resp = service.runSingle(10L, "u1")
        assertEquals("ERROR", resp.status)
        assertEquals("error msg", resp.errorMessage)
        assertNull(resp.outputs)
    }

    @Test
    fun testRunAllForSnippetDelegatesRunSingle() {
        every { snippetClient.canRead("u1", 42L) } returns true
        val t1 = helperTestCaseEntityCreator().copy(id = 11L)
        val t2 = helperTestCaseEntityCreator().copy(id = 12L)
        every { testCaseRepo.findBySnippetId(42L) } returns listOf(t1, t2)
        every { testCaseRepo.findById(11L) } returns Optional.of(t1)
        every { testCaseRepo.findById(12L) } returns Optional.of(t2)
        every { exec.execute(42L, any()) } returnsMany listOf(listOf("5"), listOf("5"))
        every { testRunRepo.save(any<TestRunEntity>()) } answers { firstArg() }
        every { testCaseRepo.save(any()) } answers { firstArg() }
        // verifico que la lista de rtas tenga 2
        // y que el executor haya sido llamado 2 veces con runSingle
        val res = service.runAllForSnippet(42L, "u1")
        assertEquals(2, res.size)
        verify(exactly = 2) { exec.execute(42L, any()) }
    }

    @Test
    fun testGetTestRunHistoryMapsAndSorts() {
        // que devuelva el mas nuevo pimero y mappeado al dto
        val e = helperTestCaseEntityCreator()
        every { testCaseRepo.findById(10L) } returns Optional.of(e)
        every { snippetClient.canRead("u1", 42L) } returns true
        val r1 = TestRunEntity(
            null,
            10L,
            42L,
            3L,
            "PASSED",
            emptyList(),
            emptyList(),
            emptyList(),
            null,
            1,
            "u1",
            Instant.parse("2025-10-10T10:00:00Z"),
        )
        val r2 = TestRunEntity(
            null,
            10L,
            42L,
            3L,
            "FAILED",
            emptyList(),
            emptyList(),
            emptyList(),
            null,
            1,
            "u1",
            Instant.parse("2025-10-11T10:00:00Z"),
        )
        every { testRunRepo.findByTestId(10L) } returns listOf(r1, r2)

        val hist = service.getTestRunHistory(10L, "u1")
        assertEquals(listOf("FAILED", "PASSED"), hist.map { it.status })
    }
}
