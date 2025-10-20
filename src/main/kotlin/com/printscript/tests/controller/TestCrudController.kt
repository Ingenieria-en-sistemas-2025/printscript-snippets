package com.printscript.tests.controller

import com.printscript.tests.dto.CreateTestRequest
import com.printscript.tests.dto.TestCaseResponse
import com.printscript.tests.dto.UpdateTestRequest
import com.printscript.tests.service.TestService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/snippets/{snippetId}/tests")
class TestCrudController(
    private val testService: TestService,
) {

    // CREATE
    @PostMapping
    fun create(
        @PathVariable snippetId: Long,
        @Valid @RequestBody req: CreateTestRequest,
        request: HttpServletRequest,
    ): ResponseEntity<TestCaseResponse> {
        val userId = RequestUserResolver.resolveUserId(request)
        val created = testService.create(snippetId, req, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    // READ ALL
    @GetMapping
    fun list(
        @PathVariable snippetId: Long,
        request: HttpServletRequest,
    ): List<TestCaseResponse> {
        val userId = RequestUserResolver.resolveUserId(request)
        return testService.getTestsBySnippet(snippetId)
    }

    // READ ONE
    @GetMapping("/{testId}")
    fun get(
        @PathVariable snippetId: Long,
        @PathVariable testId: Long,
        request: HttpServletRequest,
    ): TestCaseResponse {
        val userId = RequestUserResolver.resolveUserId(request)
        return testService.getTestForSnippet(testId, snippetId)
    }

    // UPDATE
    @PutMapping("/{testId}")
    fun update(
        @PathVariable snippetId: Long,
        @PathVariable testId: Long,
        @Valid @RequestBody req: UpdateTestRequest,
        request: HttpServletRequest,
    ): TestCaseResponse {
        val userId = RequestUserResolver.resolveUserId(request)
        return testService.update(testId, snippetId, req)
    }

    // DELETE
    @DeleteMapping("/{testId}")
    fun delete(
        @PathVariable snippetId: Long,
        @PathVariable testId: Long,
        request: HttpServletRequest,
    ): ResponseEntity<Void> {
        val userId = RequestUserResolver.resolveUserId(request)
        testService.delete(testId, snippetId)
        return ResponseEntity.noContent().build()
    }
}
