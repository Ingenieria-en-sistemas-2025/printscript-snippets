package com.printscript.snippets.service

import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.TestCaseRepo
import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.domain.model.TestCase
import com.printscript.snippets.dto.CreateTestReq
import com.printscript.snippets.dto.TestCaseDto
import com.printscript.snippets.error.InvalidRequest
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.permission.SnippetAuthorizationScopeHelper
import com.printscript.snippets.permission.SnippetPermission
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SnippetTestService(
    private val snippetRepo: SnippetRepo,
    private val testCaseRepo: TestCaseRepo,
    private val permissionClient: SnippetPermission,
) {

    private val authorization = SnippetAuthorizationScopeHelper(permissionClient)

    private fun requireSnippet(snippetId: UUID): Snippet =
        snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet with ID $snippetId not found") }

    @Transactional
    fun createTestCase(userId: String, req: CreateTestReq): TestCaseDto {
        authorization.requireOwner(userId, requireSnippet(req.snippetId!!))
        val testCase = testCaseRepo.save(
            TestCase(
                snippetId = req.snippetId,
                name = req.name,
                inputs = req.inputs,
                expectedOutputs = req.expectedOutputs,
                targetVersionNumber = req.targetVersionNumber,
            ),
        )

        return SnippetAndSnippetTestsToDto.toDto(testCase)
    }

    @Transactional(readOnly = true)
    fun listTestCases(userId: String, snippetId: UUID): List<TestCaseDto> {
        authorization.requireReaderOrAbove(userId, requireSnippet(snippetId))
        return testCaseRepo.findAllBySnippetId(snippetId).map {
            SnippetAndSnippetTestsToDto.toDto(it)
        }
    }

    @Transactional
    fun deleteTestCase(userId: String, testCaseId: UUID) {
        val test = testCaseRepo.findById(testCaseId)
            .orElseThrow { NotFound("Test case not found") }
        val snippet = requireSnippet(test.snippetId)
        authorization.requireOwner(userId, snippet)
        testCaseRepo.delete(test)
    }

    @Transactional
    fun deleteAllTestsOfSnippet(snippetId: UUID) {
        testCaseRepo.deleteAllBySnippetId(snippetId)
    }

    @Transactional(readOnly = true)
    fun getTestCaseForSnippet(
        userId: String,
        snippetId: UUID,
        testCaseId: UUID,
    ): TestCase {
        val snippet = requireSnippet(snippetId)
        authorization.requireReaderOrAbove(userId, snippet)

        val test = testCaseRepo.findById(testCaseId)
            .orElseThrow { NotFound("Test case not found") }

        if (test.snippetId != snippetId) {
            throw InvalidRequest("El test no pertenece a este snippet")
        }

        return test
    }
}
