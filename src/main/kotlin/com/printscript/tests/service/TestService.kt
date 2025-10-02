package com.printscript.tests.service

import TestCaseEntity
import com.printscript.tests.domain.JsonUtils
import com.printscript.tests.domain.TestCaseRepository
import com.printscript.tests.domain.toResponse
import com.printscript.tests.dto.CreateTestRequest
import com.printscript.tests.dto.TestCaseResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class TestService(
    private val repo: TestCaseRepository,
    private val json: JsonUtils
) {

    @Transactional
    fun create(snippetId: Long, req: CreateTestRequest, userId: String): TestCaseResponse {
        // chequear permisos contra Snippet Service (canWrite)
        val entity = TestCaseEntity(
            snippetId = snippetId,
            name = req.name,
            inputs = json.listToJson(req.inputs),
            expectedOutputs = json.listToJson(req.expectedOutputs),
            targetVersionNumber = req.targetVersionNumber,
            createdBy = userId,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        return repo.save(entity).toResponse(json)
    }
}
