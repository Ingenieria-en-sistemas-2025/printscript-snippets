package com.printscript.tests.service

import com.printscript.tests.clients.SnippetClient
import com.printscript.tests.domain.TestCaseEntity
import com.printscript.tests.domain.TestCaseRepository
import com.printscript.tests.domain.toBrief
import com.printscript.tests.domain.toResponse
import com.printscript.tests.dto.CreateTestRequest
import com.printscript.tests.dto.SnippetTestsResponse
import com.printscript.tests.dto.TestCaseBriefResponse
import com.printscript.tests.dto.TestCaseResponse
import com.printscript.tests.dto.UpdateTestRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class TestService(
    private val repo: TestCaseRepository,
    private val snippetClient: SnippetClient,
) {

    // CREATE (US8)
    @Transactional
    fun create(snippetId: Long, req: CreateTestRequest, userId: String): TestCaseResponse {
        ensureCanWrite()
        val entity = TestCaseEntity(
            snippetId = snippetId,
            name = req.name,
            inputs = req.inputs,
            expectedOutputs = req.expectedOutputs,
            targetVersionNumber = req.targetVersionNumber,
            createdBy = userId,
        )
        return repo.save(entity).toResponse()
    }

    // READ ALL (US6)
    @Transactional(readOnly = true)
    fun getTestsBySnippet(snippetId: Long): List<TestCaseResponse> {
        ensureCanRead()
        return repo.findBySnippetId(snippetId).map { it.toResponse() }
    }

    // READ ONE
    @Transactional(readOnly = true)
    fun getTestForSnippet(testId: Long, snippetId: Long): TestCaseResponse {
        val test = repo.findById(testId).orElseThrow { IllegalArgumentException("Test $testId no existe") }
        ensureCanRead()
        require(test.snippetId == snippetId) { "El test $testId no pertenece al snippet $snippetId" }
        return test.toResponse()
    }

    // UPDATE entidad inmutable: usar copy(...)
    @Transactional
    fun update(testId: Long, snippetId: Long, req: UpdateTestRequest): TestCaseResponse {
        val current = repo.findById(testId).orElseThrow { IllegalArgumentException("Test $testId no existe") }
        require(current.snippetId == snippetId) { "El test $testId no pertenece al snippet $snippetId" }
        ensureCanWrite()

        val updated = current.copy(
            name = req.name ?: current.name,
            inputs = req.inputs ?: current.inputs,
            expectedOutputs = req.expectedOutputs ?: current.expectedOutputs,
            targetVersionNumber = req.targetVersionNumber ?: current.targetVersionNumber,
            updatedAt = Instant.now(),
        )
        return repo.save(updated).toResponse()
    }

    // DELETE
    @Transactional
    fun delete(testId: Long, snippetId: Long) {
        val test = repo.findById(testId).orElseThrow { IllegalArgumentException("Test $testId no existe") }
        require(test.snippetId == snippetId) { "El test $testId no pertenece al snippet $snippetId" }
        ensureCanWrite()
        repo.delete(test)
    }

    @Transactional(readOnly = true)
    fun getTestsBriefBySnippet(snippetId: Long): List<TestCaseBriefResponse> {
        ensureCanRead()
        return repo.findBySnippetId(snippetId).map { it.toBrief() }
    }

    @Transactional(readOnly = true)
    fun getTestsSummary(snippetId: Long, userId: String): SnippetTestsResponse {
        val tests = getTestsBriefBySnippet(snippetId)
        return SnippetTestsResponse(
            snippetId = snippetId,
            tests = tests,
            total = tests.size,
            passed = tests.count { it.lastRunStatus == "PASSED" },
            failed = tests.count { it.lastRunStatus == "FAILED" },
            error = tests.count { it.lastRunStatus == "ERROR" },
        )
    }

    // helpers
    private fun ensureCanRead() {
        if (!snippetClient.canRead()) throw SecurityException("Sin permisos de lectura")
    }
    private fun ensureCanWrite() {
        if (!snippetClient.canWrite()) throw SecurityException("Sin permisos de escritura")
    }
}
