package com.printscript.snippets.service

import com.printscript.snippets.bucket.SnippetAsset
import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.dto.SingleTestRunResult
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.execution.SnippetExecution
import com.printscript.snippets.permission.SnippetAuthorizationScopeHelper
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
    private val testService: SnippetTestService,
    private val assetClient: SnippetAsset,
    private val executionClient: SnippetExecution,
    permissionClient: SnippetPermission,
) {
    private val authorization = SnippetAuthorizationScopeHelper(permissionClient)
    private val containerName = "snippets"

    @Transactional(readOnly = true)
    fun runOneTestWithPermissions(
        userId: String,
        snippetId: UUID,
        testCaseId: UUID,
    ): SingleTestRunResult {
        val snippet = requireSnippetWithReadAccess(userId, snippetId)
        val testCase = testService.getTestCaseForSnippet(userId, snippetId, testCaseId)
        val content = loadLatestContent(snippetId)

        return doRunSingleTest(snippet, content, testCase.expectedOutputs, testCase.inputs)
    }

    fun runSnippetOwnerAware(
        userId: String,
        snippetId: UUID,
        inputs: List<String>?,
    ): RunRes {
        val snippet = requireSnippetWithReadAccess(userId, snippetId)
        val content = loadLatestContent(snippetId)

        return doRun(snippet, content, inputs)
    }

    private fun requireSnippetWithReadAccess(userId: String, snippetId: UUID): Snippet {
        val snippet = snippetRepo.findById(snippetId)
            .orElseThrow { NotFound("Snippet not found") }

        authorization.requireReaderOrAbove(userId, snippet)
        return snippet
    }

    private fun loadLatestContent(snippetId: UUID): String {
        val version = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet without versions")

        val bytes = assetClient.download(containerName, version.contentKey)
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun doRunSingleTest(
        snippet: Snippet,
        content: String,
        expectedOutputs: List<String>,
        inputs: List<String>,
    ): SingleTestRunResult {
        val execReq = RunSingleTestReq(
            language = snippet.language,
            version = snippet.languageVersion,
            content = content,
            inputs = inputs,
            expectedOutputs = expectedOutputs,
            options = null,
        )

        val execRes = executionClient.runSingleTest(execReq)

        return SingleTestRunResult(
            status = execRes.status,
            actual = execRes.actual,
            expected = expectedOutputs,
            mismatchAt = execRes.mismatchAt,
            diagnostic = execRes.diagnostic,
        )
    }

    private fun doRun(
        snippet: Snippet,
        content: String,
        inputs: List<String>?,
    ): RunRes {
        val req = RunReq(
            language = snippet.language,
            version = snippet.languageVersion,
            content = content,
            inputs = inputs,
        )
        return executionClient.run(req)
    }
}
