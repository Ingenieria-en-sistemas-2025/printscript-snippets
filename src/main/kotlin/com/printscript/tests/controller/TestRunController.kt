package com.printscript.tests.controller

import com.printscript.tests.dto.TestRunResponse
import com.printscript.tests.service.TestRunner
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class TestRunController(
    private val runner: TestRunner,
) {

    // (US9) Correr un test individual
    @PostMapping("/tests/{id}/run")
    fun runSingleTest(
        @PathVariable id: Long,
        request: HttpServletRequest,
    ): TestRunResponse {
        val userId = RequestUserResolver.resolveUserId(request)
        return runner.runSingle(id, userId)
    }

    // (US16) Correr todos los tests de un snippet
    @PostMapping("/snippets/{snippetId}/tests/run-all")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun runAllTests(
        @PathVariable snippetId: Long,
        request: HttpServletRequest,
    ): List<TestRunResponse> {
        val userId = RequestUserResolver.resolveUserId(request)
        return runner.runAllForSnippet(snippetId, userId)
    }

    // Historial de ejecuciones de un test
    @GetMapping("/tests/{testId}/runs")
    fun getTestRunHistory(
        @PathVariable testId: Long,
        request: HttpServletRequest,
    ): List<TestRunResponse> {
        val userId = RequestUserResolver.resolveUserId(request)
        return runner.getTestRunHistory(testId, userId)
    }

    // ejecucion especifica
    @GetMapping("/test-runs/{runId}")
    fun getTestRun(
        @PathVariable runId: Long,
        request: HttpServletRequest,
    ): TestRunResponse {
        val userId = RequestUserResolver.resolveUserId(request)
        return runner.getTestRun(runId, userId)
    }
}
