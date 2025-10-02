package com.printscript.tests.api

import com.printscript.tests.dto.CreateTestRequest
import com.printscript.tests.dto.TestCaseResponse
import com.printscript.tests.service.TestService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/snippets/{snippetId}/tests")
class TestCrudController(
    private val testService: TestService
) {
    @PostMapping
    fun create(
        @PathVariable snippetId: Long,
        @RequestBody req: CreateTestRequest,
        request: HttpServletRequest
    ): ResponseEntity<TestCaseResponse> {
        val userId = RequestUserResolver.resolveUserId(request)
        val created = testService.create(snippetId, req, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
}
