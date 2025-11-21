package com.printscript.snippets.service

import com.printscript.snippets.domain.TestCaseRepo
import com.printscript.snippets.domain.model.TestCase
import com.printscript.snippets.dto.CreateTestReq
import com.printscript.snippets.dto.TestCaseDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SnippetTestService(
    private val testCaseRepo: TestCaseRepo,
) {

    @Transactional
    fun createTestCase(req: CreateTestReq): TestCaseDto {
        val testCase = testCaseRepo.save(
            TestCase(
                snippetId = UUID.fromString(req.snippetId!!),
                name = req.name,
                inputs = req.inputs,
                expectedOutputs = req.expectedOutputs,
                targetVersionNumber = req.targetVersionNumber,
            ),
        )

        return TestCaseDto(
            id = testCase.id!!.toString(),
            snippetId = testCase.snippetId.toString(),
            name = testCase.name,
            inputs = testCase.inputs,
            expectedOutputs = testCase.expectedOutputs,
            targetVersionNumber = testCase.targetVersionNumber,
        )
    }

    @Transactional(readOnly = true)
    fun listTestCases(snippetId: UUID): List<TestCaseDto> =
        testCaseRepo.findAllBySnippetId(snippetId).map {
            TestCaseDto(
                id = it.id!!.toString(),
                snippetId = it.snippetId.toString(),
                name = it.name,
                inputs = it.inputs,
                expectedOutputs = it.expectedOutputs,
                targetVersionNumber = it.targetVersionNumber,
            )
        }

    @Transactional
    fun deleteTestCase(testCaseId: UUID) {
        testCaseRepo.deleteById(testCaseId)
    }
}
