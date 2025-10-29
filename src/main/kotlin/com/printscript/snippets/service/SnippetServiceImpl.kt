package com.printscript.snippets.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.printscript.snippets.bucket.SnippetAsset
import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.domain.TestCaseRepo
import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.domain.model.SnippetVersion
import com.printscript.snippets.domain.model.TestCase
import com.printscript.snippets.dto.CreateSnippetReq
import com.printscript.snippets.dto.CreateTestReq
import com.printscript.snippets.dto.PageDto
import com.printscript.snippets.dto.ShareSnippetReq
import com.printscript.snippets.dto.SnippetDetailDto
import com.printscript.snippets.dto.SnippetSource
import com.printscript.snippets.dto.SnippetSummaryDto
import com.printscript.snippets.dto.TestCaseDto
import com.printscript.snippets.dto.UpdateSnippetReq
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.error.UnsupportedOperation
import com.printscript.snippets.execution.SnippetExecution
import com.printscript.snippets.execution.dto.FormatReq
import com.printscript.snippets.execution.dto.FormatRes
import com.printscript.snippets.execution.dto.LintReq
import com.printscript.snippets.execution.dto.LintRes
import com.printscript.snippets.execution.dto.ParseReq
import com.printscript.snippets.execution.dto.ParseRes
import com.printscript.snippets.permission.SnippetPermission
import com.printscript.snippets.permission.dto.PermissionCreateSnippetInput
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
class SnippetServiceImpl(
    private val snippetRepo: SnippetRepo,
    private val versionRepo: SnippetVersionRepo,
    private val testCaseRepo: TestCaseRepo,
    private val assetClient: SnippetAsset,
    private val executionClient: SnippetExecution,
    private val permissionClient: SnippetPermission,
) : SnippetService {
    private val containerName = "snippets"
    private val objectMapper = jacksonObjectMapper()

    // key del archivo en el bucket
    private fun buildVersionKey(snippetId: Long, versionNumber: Long): String =
        "snippets/$snippetId/v$versionNumber.ps"

    // ult nro de version persistido para ese snippet
    private fun getLatestVersionNumber(snippetId: Long): Long =
        (versionRepo.findMaxVersionBySnippetId(snippetId) ?: 0L) as Long

    // dto q usa la ui
    private fun toDetailDto(snippet: Snippet, version: SnippetVersion, content: String?): SnippetDetailDto =
        SnippetDetailDto(
            id = snippet.id!!,
            name = snippet.name,
            description = snippet.description,
            language = snippet.language,
            version = snippet.languageVersion,
            ownerId = snippet.ownerId,
            content = content,
            isValid = version.isValid,
            lintCount = snippet.lastLintCount,
        )

    override fun createSnippet(
        ownerId: String,
        req: CreateSnippetReq,
    ): SnippetDetailDto {
        val content = req.content ?: ""
        // crear entidad base del snippet
        val snippet = snippetRepo.save(
            Snippet(
                ownerId = ownerId,
                name = req.name,
                description = req.description,
                language = req.language,
                languageVersion = req.version,
                currentVersionId = null,
                lastIsValid = false,
                lastLintCount = 0,
            ),
        )

        // subir el archivo al bucket
        val versionNumber = 1L
        val contentKey = buildVersionKey(snippet.id!!, versionNumber)
        assetClient.upload(containerName, contentKey, content.toByteArray(StandardCharsets.UTF_8))

        // execution se ocupa de parse y lint
        val parseResult = executionClient.parse(ParseReq(req.language, req.version, req.content))
        val lintResult = executionClient.lint(LintReq(req.language, req.version, req.content))

        // guardo version inicial
        val snippetVersion = versionRepo.save(
            SnippetVersion(
                snippetId = snippet.id!!,
                versionNumber = versionNumber,
                contentKey = contentKey,
                formattedKey = null,
                isFormatted = false,
                isValid = parseResult.valid && lintResult.violations.isEmpty(),
                lintIssues = objectMapper.writeValueAsString(lintResult.violations),
                parseErrors = objectMapper.writeValueAsString(parseResult.diagnostics),
                createdAt = Instant.now(),
            ),
        )
        // actualizo el snippet ppal con estado actual
        snippet.currentVersionId = snippetVersion.id
        snippet.lastIsValid = snippetVersion.isValid
        snippet.lastLintCount = lintResult.violations.size
        snippetRepo.save(snippet)

        // registro permisos owner en permissions
        permissionClient.createAuthorization(
            PermissionCreateSnippetInput(
                snippetId = snippet.id!!.toString(),
                userId = ownerId,
                permissionType = "OWNER",
            ),
            token = "",
        )

        return toDetailDto(snippet, snippetVersion, req.content)
    }

    @Transactional(readOnly = true)
    override fun getSnippet(snippetId: Long): SnippetDetailDto {
        val snippet = snippetRepo.findById(snippetId)
            .orElseThrow { NotFound("Snippet with ID $snippetId not found") }

        val latestVersion = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet $snippetId has no versions")

        val content = assetClient.download(containerName, latestVersion.contentKey)
            .toString(StandardCharsets.UTF_8)

        return toDetailDto(snippet, latestVersion, content)
    }

    override fun updateSnippet(
        snippetId: Long,
        req: UpdateSnippetReq,
    ): SnippetDetailDto {
        val snippet = snippetRepo.findById(snippetId)
            .orElseThrow { NotFound("Snippet not found") }

        req.name?.let { snippet.name = it }
        req.description?.let { snippet.description = it }

        val savedSnippet = snippetRepo.save(snippet)
        val latestVersion = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet without versions")

        val content = assetClient.download(containerName, latestVersion.contentKey)
            .toString(StandardCharsets.UTF_8)

        return toDetailDto(savedSnippet, latestVersion, content)
    }

    @Transactional
    override fun deleteSnippet(snippetId: Long) {
        // borro permisos
        permissionClient.deleteSnippetPermissions(snippetId.toString(), token = "")

        // borro files del bucket
        val versions = versionRepo.findAllBySnippetId(snippetId)
        versions.forEach { version -> assetClient.delete(containerName, version.contentKey) }

        // borro datos en db
        versionRepo.deleteAll(versions)
        snippetRepo.deleteById(snippetId)
    }

    @Transactional
    override fun addVersion(snippetId: Long, req: SnippetSource): SnippetDetailDto {
        // solo enruta
        throw UnsupportedOperation(
            "Use addVersionFromInlineContent(...) o addVersionFromUploadedFile(...). " +
                "El enum SnippetSource no contiene el contenido.",
        )
    }

    @Transactional
    fun addVersionFromInlineContent(snippetId: Long, content: String): SnippetDetailDto {
        val snippet = snippetRepo.findById(snippetId)
            .orElseThrow { NotFound("Snippet not found") }

        val nextVersionNumber = getLatestVersionNumber(snippetId) + 1
        val contentKey = buildVersionKey(snippetId, nextVersionNumber)
        // subir
        assetClient.upload(containerName, contentKey, content.toByteArray(StandardCharsets.UTF_8))

        // validar
        val parseResult = executionClient.parse(ParseReq(snippet.language, snippet.languageVersion, content))
        val lintResult = executionClient.lint(LintReq(snippet.language, snippet.languageVersion, content))

        // guardar version
        val newVersion = versionRepo.save(
            SnippetVersion(
                snippetId = snippet.id!!,
                versionNumber = nextVersionNumber,
                contentKey = contentKey,
                formattedKey = null,
                isFormatted = false,
                isValid = parseResult.valid && lintResult.violations.isEmpty(),
                lintIssues = objectMapper.writeValueAsString(lintResult.violations),
                parseErrors = objectMapper.writeValueAsString(parseResult.diagnostics),
                createdAt = Instant.now(),
            ),
        )

        snippet.currentVersionId = newVersion.id
        snippet.lastIsValid = newVersion.isValid
        snippet.lastLintCount = lintResult.violations.size
        snippetRepo.save(snippet)

        return toDetailDto(snippet, newVersion, content)
    }

    @Transactional
    fun addVersionFromUploadedFile(snippetId: Long, fileBytes: ByteArray): SnippetDetailDto {
        val snippet = snippetRepo.findById(snippetId)
            .orElseThrow { NotFound("Snippet not found") }

        val nextVersionNumber = getLatestVersionNumber(snippetId) + 1
        val contentKey = buildVersionKey(snippetId, nextVersionNumber)

        assetClient.upload(containerName, contentKey, fileBytes) // subir bytes
        val contentAsString = fileBytes.toString(StandardCharsets.UTF_8)

        // validar
        val parseResult = executionClient.parse(ParseReq(snippet.language, snippet.languageVersion, contentAsString))
        val lintResult = executionClient.lint(LintReq(snippet.language, snippet.languageVersion, contentAsString))

        val newVersion = versionRepo.save(
            SnippetVersion(
                snippetId = snippet.id!!,
                versionNumber = nextVersionNumber,
                contentKey = contentKey,
                formattedKey = null,
                isFormatted = false,
                isValid = parseResult.valid && lintResult.violations.isEmpty(),
                lintIssues = objectMapper.writeValueAsString(lintResult.violations),
                parseErrors = objectMapper.writeValueAsString(parseResult.diagnostics),
                createdAt = Instant.now(),
            ),
        )
        snippet.currentVersionId = newVersion.id
        snippet.lastIsValid = newVersion.isValid
        snippet.lastLintCount = lintResult.violations.size
        snippetRepo.save(snippet)

        return toDetailDto(snippet, newVersion, contentAsString)
    }

    @Transactional(readOnly = true)
    override fun listMySnippets(
        userId: String,
        page: Int,
        size: Int,
    ): PageDto<SnippetSummaryDto> {
        val response = permissionClient
            .getAllSnippetsPermission(userId, token = "", pageNum = page, pageSize = size)
            .body

        val snippetIds = (response?.permissions ?: emptyList())
            .mapNotNull { it.snippetId.toLongOrNull() }

        if (snippetIds.isEmpty()) {
            return PageDto(emptyList(), 0, page, size)
        }

        val snippets = snippetRepo.findAllById(snippetIds)
        val summaries = snippets.map { snippet ->
            SnippetSummaryDto(
                id = snippet.id!!,
                name = snippet.name,
                description = snippet.description,
                language = snippet.language,
                version = snippet.languageVersion,
                ownerId = snippet.ownerId,
                lastIsValid = snippet.lastIsValid,
                lastLintCount = snippet.lastLintCount,
            )
        }

        return PageDto(
            items = summaries,
            count = response?.count ?: summaries.size.toLong(),
            page = page,
            pageSize = size,
        )
    }

    @Transactional
    override fun shareSnippet(req: ShareSnippetReq) {
        permissionClient.createAuthorization(
            PermissionCreateSnippetInput(
                snippetId = req.snippetId.toString(),
                userId = req.userId,
                permissionType = req.permissionType,
            ),
            token = "",
        )
    }

    @Transactional
    override fun createTestCase(req: CreateTestReq): TestCaseDto {
        val inputsJson = objectMapper.writeValueAsString(req.inputs)
        val expectedOutputsJson = objectMapper.writeValueAsString(req.expectedOutputs)

        val testCase = testCaseRepo.save(
            TestCase(
                snippetId = req.snippetId,
                name = req.name,
                inputs = inputsJson,
                expectedOutputs = expectedOutputsJson,
                targetVersionNumber = req.targetVersionNumber,
                createdBy = "system", // o agarrar el user id
            ),
        )

        val inputs = objectMapper.readValue(inputsJson, Array<String>::class.java).toList()
        val expectedOutputs = objectMapper.readValue(expectedOutputsJson, Array<String>::class.java).toList()

        return TestCaseDto(
            id = testCase.id!!,
            snippetId = testCase.snippetId,
            name = testCase.name,
            inputs = inputs,
            expectedOutputs = expectedOutputs,
            targetVersionNumber = testCase.targetVersionNumber,
        )
    }

    @Transactional(readOnly = true)
    override fun listTestCases(snippetId: Long): List<TestCaseDto> =
        testCaseRepo.findAllBySnippetId(snippetId).map {
            val inputs = objectMapper.readValue(it.inputs, Array<String>::class.java).toList()
            val expectedOutputs = objectMapper.readValue(it.expectedOutputs, Array<String>::class.java).toList()

            TestCaseDto(
                id = it.id!!,
                snippetId = it.snippetId,
                name = it.name,
                inputs = inputs,
                expectedOutputs = expectedOutputs,
                targetVersionNumber = it.targetVersionNumber,
            )
        }

    @Transactional
    override fun deleteTestCase(testCaseId: Long) {
        testCaseRepo.deleteById(testCaseId)
    }

    override fun runParse(req: ParseReq): ParseRes = executionClient.parse(req)
    override fun runLint(req: LintReq): LintRes = executionClient.lint(req)
    override fun runFormat(req: FormatReq): FormatRes = executionClient.format(req)
}
