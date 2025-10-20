package com.printscript.tests.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class TestCaseMappersTest {
    @Test
    fun testMethodToResponse() {
        val e = TestCaseEntity(
            id = 1L, snippetId = 42L, name = "n",
            inputs = listOf("a"), expectedOutputs = listOf("b"),
            targetVersionNumber = null, createdBy = "u",
            createdAt = Instant.now(), updatedAt = Instant.now(),
            lastRunStatus = "NEVER_RUN", lastRunOutput = null, lastRunAt = null,
        )
        // convierte entidad (TestCaseEntity) al dto (TestCaseResponse)
        val dto = e.toResponse()
        assertEquals(1L, dto.id)
        assertEquals("n", dto.name)
        assertEquals(listOf("a"), dto.inputs)
        assertEquals("NEVER_RUN", dto.lastRunStatus)
    }

    @Test
    fun testToBriefMapper() {
        val e = TestCaseEntity(
            id = 1L, snippetId = 42L, name = "n",
            inputs = emptyList(), expectedOutputs = emptyList(),
            targetVersionNumber = null, createdBy = "u",
            createdAt = Instant.now(), updatedAt = Instant.now(),
            lastRunStatus = "PASSED", lastRunOutput = null, lastRunAt = Instant.parse("2025-10-11T12:00:00Z"),
        )
        val brief = e.toBrief()
        assertEquals("n", brief.name)
        assertEquals("PASSED", brief.lastRunStatus)
        assertEquals("2025-10-11T12:00:00Z", brief.lastRunAt)
    }
}
