package com.printscript.tests.service

import com.printscript.tests.clients.ExecutionClient
import com.printscript.tests.clients.SnippetClient
import com.printscript.tests.domain.TestCaseRepository
import com.printscript.tests.domain.TestRunEntity
import com.printscript.tests.domain.TestRunRepository
import com.printscript.tests.dto.TestRunResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class TestRunnerService(
    private val testCaseRepo: TestCaseRepository,
    private val testRunRepo: TestRunRepository,
    private val executionClient: ExecutionClient,
    private val snippetClient: SnippetClient,
) : TestRunner {

    @Transactional // US9
    override fun runSingle(testId: Long, userId: String): TestRunResponse {
        val test = testCaseRepo.findById(testId)
            .orElseThrow { IllegalArgumentException("TestCase $testId no existe") }
        if (!snippetClient.canRead()) {
            throw SecurityException("Sin acceso al snippet ${test.snippetId}")
        }

        val startedAt = Instant.now()
        var outputs: List<String>? = null
        var error: String? = null
        var status = "ERROR"

        val duration = kotlin.system.measureTimeMillis {
            runCatching { executionClient.execute(test.snippetId, test.inputs) }
                .onSuccess {
                    outputs = it
                    status = if (it == test.expectedOutputs) "PASSED" else "FAILED"
                }
                .onFailure { ex ->
                    error = ex.message ?: ex.javaClass.simpleName
                    status = "ERROR"
                }
        }

        val run = testRunRepo.save(
            TestRunEntity(
                id = null,
                testId = test.id!!,
                snippetId = test.snippetId,
                snippetVersionNumber = test.targetVersionNumber ?: 1L,
                status = status,
                inputs = test.inputs,
                expectedOutputs = test.expectedOutputs,
                outputs = outputs,
                errorMessage = error,
                durationMs = duration,
                executedBy = userId,
                executedAt = startedAt,
            ),
        )

        val updated = test.copy(
            lastRunStatus = status,
            lastRunOutput = outputs,
            lastRunAt = startedAt,
            updatedAt = Instant.now(),
        )
        testCaseRepo.save(updated)

        return run.toResponse()
    }

    @Transactional // US16
    override fun runAllForSnippet(snippetId: Long, userId: String): List<TestRunResponse> {
        if (!snippetClient.canRead()) {
            throw SecurityException("Sin acceso al snippet $snippetId")
        }
        return testCaseRepo.findBySnippetId(snippetId).map { runSingle(it.id!!, userId) }
    }

    // Read only porque son de consulta (historial y detalle)
    @Transactional(readOnly = true)
    override fun getTestRunHistory(testId: Long, userId: String): List<TestRunResponse> {
        val test = testCaseRepo.findById(testId)
            .orElseThrow { IllegalArgumentException("TestCase $testId no existe") }
        if (!snippetClient.canRead()) {
            throw SecurityException("Sin acceso al snippet ${test.snippetId}")
        }

        return testRunRepo.findByTestId(testId)
            .sortedByDescending { it.executedAt }
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    override fun getTestRun(runId: Long, userId: String): TestRunResponse {
        val run = testRunRepo.findById(runId)
            .orElseThrow { IllegalArgumentException("TestRun $runId no existe") }
        val test = testCaseRepo.findById(run.testId)
            .orElseThrow { IllegalArgumentException("TestCase ${run.testId} no existe") }
        if (!snippetClient.canRead()) {
            throw SecurityException("Sin acceso al snippet ${test.snippetId}")
        }

        return run.toResponse()
    }

    // mapper
    private fun TestRunEntity.toResponse() = TestRunResponse(
        testId = testId,
        snippetId = snippetId,
        snippetVersionNumber = snippetVersionNumber,
        status = status,
        inputs = inputs,
        expectedOutputs = expectedOutputs,
        outputs = outputs,
        errorMessage = errorMessage,
        durationMs = durationMs,
        executedBy = executedBy,
        executedAt = executedAt,
    )
}
