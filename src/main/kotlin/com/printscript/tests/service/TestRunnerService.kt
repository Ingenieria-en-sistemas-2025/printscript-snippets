package com.printscript.tests.service

import com.printscript.tests.clients.ExecutionClient
import com.printscript.tests.clients.SnippetClient
import com.printscript.tests.domain.JsonUtils
import com.printscript.tests.domain.TestCaseRepository
import com.printscript.tests.domain.TestRunEntity
import com.printscript.tests.domain.TestRunRepository
import com.printscript.tests.dto.TestRunResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.system.measureTimeMillis

@Service
class TestRunnerService(
    private val testCaseRepo: TestCaseRepository,
    private val testRunRepo: TestRunRepository,
    private val json: JsonUtils,
    private val executionClient: ExecutionClient,
    private val snippetClient: SnippetClient
) {
    //Ejecuta un test por ID y devuelve el resultado
    @Transactional
    fun runSingle(testId: Long, userId: String): TestRunResponse {
        val test = testCaseRepo.findById(testId)
            .orElseThrow { IllegalArgumentException("TestCase $testId no existe") }

        // FALSO cuando tenga Snippet Service real lo activo
        if (!snippetClient.canRead(userId, test.snippetId)) {
            throw IllegalStateException("Usuario $userId no tiene acceso de lectura al snippet ${test.snippetId}")
        }

        val inputs = json.jsonToList(test.inputs)
        val expected = json.jsonToList(test.expectedOutputs)
        val startedAt = Instant.now()
        var outputs: List<String>? = null
        var error: String? = null
        var status = "ERROR"

        val duration = measureTimeMillis {
            try {
                // FALSO: ExecutionClient devuelve los mismos inputs (cambiar por la llamada real al Exec Service)
                outputs = executionClient.execute(test.snippetId, inputs)
                status = if (outputs == expected) "PASSED" else "FAILED"
            } catch (ex: Exception) {
                error = ex.message ?: ex.javaClass.simpleName
                status = "ERROR"
            }
        }

        // Persistimos el historico de ejecución (test_run)
        val runEntity = TestRunEntity(
            testId = test.id!!,
            snippetId = test.snippetId,
            snippetVersionNumber = test.targetVersionNumber ?: 1L,
            status = status,
            inputsJson = json.listToJson(inputs),
            expectedOutputsJson = json.listToJson(expected),
            outputsJson = outputs?.let { json.listToJson(it) },
            errorMessage = error,
            durationMs = duration,
            executedBy = userId,
            executedAt = startedAt
        )
        testRunRepo.save(runEntity)

        // Actualizar campos de last run en test_case
        test.lastRunStatus = status
        test.lastRunOutput = outputs?.let { json.listToJson(it) }
        test.lastRunAt = startedAt
        // updated_at se auto-setea con tu trigger V3
        testCaseRepo.save(test)

        return TestRunResponse(
            testId = test.id!!,
            snippetId = test.snippetId,
            snippetVersionNumber = runEntity.snippetVersionNumber,
            status = status,
            inputs = inputs,
            expectedOutputs = expected,
            outputs = outputs,
            errorMessage = error,
            durationMs = duration,
            executedBy = userId,
            executedAt = startedAt
        )
    }

    // Ejecuta todos los tests de un snippet. Devuelve la lista de resultados
    @Transactional
    fun runAllForSnippet(snippetId: Long, userId: String): List<TestRunResponse> {
        // autorizacion
        if (!snippetClient.canRead(userId, snippetId)) {
            throw IllegalStateException("Usuario $userId no tiene acceso de lectura al snippet $snippetId")
        }
        val tests = testCaseRepo.findBySnippetId(snippetId)
        return tests.map { runSingle(it.id!!, userId) }
    }
}
