package com.printscript.snippets.service

import com.printscript.snippets.bucket.SnippetAsset
import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.domain.TestCaseRepo
import com.printscript.snippets.dto.SingleTestRunResult
import com.printscript.snippets.error.InvalidRequest
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.execution.SnippetExecution
import com.printscript.snippets.permission.SnippetPermission
import io.printscript.contracts.run.RunReq
import io.printscript.contracts.run.RunRes
import io.printscript.contracts.tests.RunSingleTestReq
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.util.UUID

@Service
class SnippetsExecuteService(
    private val snippetRepo: SnippetRepo,
    private val versionRepo: SnippetVersionRepo,
    private val testCaseRepo: TestCaseRepo,
    private val assetClient: SnippetAsset,
    private val executionClient: SnippetExecution,
    private val permissionClient: SnippetPermission,
) {
    private val authorization = SnippetAuthorizationScopeService(permissionClient)
    private val containerName = "snippets"

    @Transactional(readOnly = true)
    fun runOneTestOwnerAware(
        userId: String,
        snippetId: UUID,
        testCaseId: UUID,
    ): SingleTestRunResult {
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        authorization.requireReaderOrAbove(userId, snippet)

        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet without versions")
        val content = String(assetClient.download(containerName, latest.contentKey), StandardCharsets.UTF_8)

        val tc = testCaseRepo.findById(testCaseId).orElseThrow { NotFound("Test case not found") }
        if (tc.snippetId != snippetId) throw InvalidRequest("El test no pertenece a este snippet")

        val inputs = tc.inputs
        val expected = tc.expectedOutputs

        val execReq = RunSingleTestReq(
            language = snippet.language,
            version = snippet.languageVersion,
            content = content,
            inputs = inputs,
            expectedOutputs = expected,
            options = null,
        )

        val execRes = executionClient.runSingleTest(execReq)

        return SingleTestRunResult(
            status = execRes.status,
            actual = execRes.actual,
            expected = expected,
            mismatchAt = execRes.mismatchAt,
            diagnostic = execRes.diagnostic,
        )
    }

    fun runSnippetOwnerAware(
        userId: String,
        snippetId: UUID,
        inputs: List<String>?,
    ): RunRes {
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        authorization.requireReaderOrAbove(userId, snippet)

        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet without versions")

        val contentBytes = assetClient.download(containerName, latest.contentKey)
        val content = String(contentBytes, StandardCharsets.UTF_8)

        val req = RunReq(
            language = snippet.language, // ej. "printscript"
            version = snippet.languageVersion, // ej. "1.1"
            content = content,
            inputs = inputs,
        )

        val response = executionClient.run(req)
        return response
    }
}
