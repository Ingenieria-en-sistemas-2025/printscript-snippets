package com.printscript.tests.api

import com.printscript.tests.service.TestRunnerService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
class TestRunController(private val runner: TestRunnerService) {
    @PostMapping("/tests/{id}/run")
    fun run(@PathVariable id: Long, request: HttpServletRequest) =
        runner.runSingle(id, RequestUserResolver.resolveUserId(request))

    @PostMapping("/snippets/{snippetId}/tests/run-all")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun runAll(@PathVariable snippetId: Long, request: HttpServletRequest) =
        runner.runAllForSnippet(snippetId, RequestUserResolver.resolveUserId(request))
}
