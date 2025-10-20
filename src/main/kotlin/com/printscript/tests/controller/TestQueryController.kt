package com.printscript.tests.controller

import com.printscript.tests.dto.SnippetTestsResponse
import com.printscript.tests.dto.TestCaseBriefResponse
import com.printscript.tests.service.TestService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// US6
@RestController
@RequestMapping("/snippets/{snippetId}/tests")
class TestQueryController(
    private val testService: TestService,
) {
    // Lista de tests (Brief) para un snippet
    @GetMapping
    fun listBrief(
        @PathVariable snippetId: Long,
        request: HttpServletRequest,
    ): List<TestCaseBriefResponse> {
        return testService.getTestsBriefBySnippet(snippetId)
    }

    // Resumen (totales/passed/failed/error)
    @GetMapping("/summary")
    fun summary(
        @PathVariable snippetId: Long,
        request: HttpServletRequest,
    ): SnippetTestsResponse {
        return testService.getTestsSummary(snippetId)
    }
}
