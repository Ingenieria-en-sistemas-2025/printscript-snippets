package com.printscript.tests.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.printscript.tests.bucket.SnippetAsset
import com.printscript.tests.domain.SnippetRepo
import com.printscript.tests.domain.SnippetVersionRepo
import com.printscript.tests.domain.TestCaseRepo
import com.printscript.tests.domain.model.Snippet
import com.printscript.tests.domain.model.SnippetVersion
import com.printscript.tests.domain.model.TestCase
import com.printscript.tests.dto.CreateSnippetReq
import com.printscript.tests.dto.CreateTestReq
import com.printscript.tests.dto.PageDto
import com.printscript.tests.dto.ShareSnippetReq
import com.printscript.tests.dto.SnippetDetailDto
import com.printscript.tests.dto.SnippetSource
import com.printscript.tests.dto.SnippetSummaryDto
import com.printscript.tests.dto.TestCaseDto
import com.printscript.tests.dto.UpdateSnippetReq
import com.printscript.tests.error.ApiDiagnostic
import com.printscript.tests.error.InvalidSnippet
import com.printscript.tests.error.NotFound
import com.printscript.tests.error.UnsupportedOperation
import com.printscript.tests.execution.SnippetExecution
import com.printscript.tests.execution.dto.DiagnosticDto
import com.printscript.tests.execution.dto.FormatReq
import com.printscript.tests.execution.dto.FormatRes
import com.printscript.tests.execution.dto.LintReq
import com.printscript.tests.execution.dto.LintRes
import com.printscript.tests.execution.dto.ParseReq
import com.printscript.tests.execution.dto.ParseRes
import com.printscript.tests.permission.SnippetPermission
import com.printscript.tests.permission.dto.PermissionCreateSnippetInput
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SnippetServiceImpl(
    private val snippetRepo: SnippetRepo,
    private val versionRepo: SnippetVersionRepo,
    private val testCaseRepo: TestCaseRepo,
    private val assetClient: SnippetAsset,
    private val executionClient: SnippetExecution,
    private val permissionClient: SnippetPermission,
): SnippetService {
    private val containerName = "snippets"
    private val objectMapper = jacksonObjectMapper()

    //key del archivo en el bucket
    private fun buildVersionKey(ownerId: String, snippetId: UUID, versionNumber: Long): String =
        "$ownerId/$snippetId/v$versionNumber.ps"

    //ult nro de version persistido para ese snippet
    private fun getLatestVersionNumber(snippetId: UUID): Long =
        versionRepo.findMaxVersionBySnippetId(snippetId) ?: 0L

    // mapper simple Execution.DiagnosticDto -> ApiDiagnostic
    private fun toApiDiagnostics(list: List<DiagnosticDto>): List<ApiDiagnostic> =
        list.map { diagnostic -> ApiDiagnostic(diagnostic.ruleId, diagnostic.message, diagnostic.line, diagnostic.col) }


    private fun createAndPersistVersion(
        snippet: Snippet,
        content: String
    ): SnippetVersion {
        val nextVersion = getLatestVersionNumber(snippet.id!!) + 1
        val contentKey = buildVersionKey(snippet.ownerId, snippet.id!!, nextVersion)

        //valido sintaxis antes de subir al bucket
        val parseRes = executionClient.parse(ParseReq(snippet.language, snippet.languageVersion, content))
        if (!parseRes.valid) {
            throw InvalidSnippet(toApiDiagnostics(parseRes.diagnostics))
        }

        assetClient.upload(containerName, contentKey, content.toByteArray(StandardCharsets.UTF_8))
        val lintRes = executionClient.lint(LintReq(snippet.language, snippet.languageVersion, content))

        val version = versionRepo.save(
            SnippetVersion(
                id = null,
                snippetId = snippet.id!!,
                versionNumber = nextVersion,
                contentKey = contentKey,
                formattedKey = null,
                isFormatted = false,
                isValid = parseRes.valid && lintRes.violations.isEmpty(),
                lintIssues = objectMapper.writeValueAsString(lintRes.violations),
                parseErrors = "[]",
                createdAt = Instant.now()
            )
        )

        snippet.currentVersionId = version.id
        snippet.lastIsValid = version.isValid
        snippet.lastLintCount = lintRes.violations.size
        snippetRepo.save(snippet)

        return version
    }

    private fun toDetailDto(snippet: Snippet, version: SnippetVersion, content: String?): SnippetDetailDto =
        SnippetDetailDto(
            id = snippet.id!!.toString(),
            name = snippet.name,
            description = snippet.description,
            language = snippet.language,
            version = snippet.languageVersion,
            ownerId = snippet.ownerId,
            content = content,
            isValid = version.isValid,
            lintCount = snippet.lastLintCount
        )


    override fun createSnippet(ownerId: String, req: CreateSnippetReq): SnippetDetailDto {
        val content = req.content ?: ""
        val snippet = snippetRepo.save(
            Snippet(
                ownerId = ownerId,
                name = req.name,
                description = req.description,
                language = req.language,
                languageVersion = req.version,
                currentVersionId = null,
                lastIsValid = false,
                lastLintCount = 0
            )
        )

        val version = createAndPersistVersion(snippet, content)

        permissionClient.createAuthorization(
            PermissionCreateSnippetInput(snippet.id!!.toString(), ownerId, "OWNER"),
            token = ""
        )

        return toDetailDto(snippet, version, content)
    }


    @Transactional(readOnly = true)
    override fun getSnippet(snippetId: UUID): SnippetDetailDto {
        val snippet = snippetRepo.findById(snippetId)
            .orElseThrow { NotFound("Snippet with ID $snippetId not found") }

        val latestVersion = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet $snippetId has no versions")

        val content = assetClient.download(containerName, latestVersion.contentKey)
            .toString(StandardCharsets.UTF_8)

        return toDetailDto(snippet, latestVersion, content)
    }


    override fun updateSnippet(snippetId: UUID, req: UpdateSnippetReq): SnippetDetailDto {
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
    override fun deleteSnippet(snippetId: UUID) {
        permissionClient.deleteSnippetPermissions(snippetId.toString(), token = "")

        // borrar files del bucket
        val versions = versionRepo.findAllBySnippetId(snippetId)
        versions.forEach { v ->
            assetClient.delete(containerName, v.contentKey)
            v.formattedKey?.let { assetClient.delete(containerName, it) }
        }

        // borrar datos en db
        versionRepo.deleteAll(versions)
        snippetRepo.deleteById(snippetId)
    }

    @Transactional
    override fun addVersion(snippetId: UUID, req: SnippetSource): SnippetDetailDto {
        throw UnsupportedOperation(
            "Use addVersionFromInlineContent(...) o addVersionFromUploadedFile(...). " +
                    "El enum SnippetSource no contiene el contenido."
        )
    }

    @Transactional
    fun addVersionFromInlineContent(snippetId: UUID, content: String): SnippetDetailDto {
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        val version = createAndPersistVersion(snippet, content)
        return toDetailDto(snippet, version, content)
    }

    @Transactional
    fun addVersionFromUploadedFile(snippetId: UUID, fileBytes: ByteArray): SnippetDetailDto {
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        val content = fileBytes.toString(StandardCharsets.UTF_8)
        val version = createAndPersistVersion(snippet, content)
        return toDetailDto(snippet, version, content)
    }


    @Transactional(readOnly = true)
    override fun listMySnippets(
        userId: String,
        page: Int,
        size: Int
    ): PageDto<SnippetSummaryDto> {
        val response = permissionClient
            .getAllSnippetsPermission(userId, token = "", pageNum = page, pageSize = size)
            .body

        val snippetIds: List<UUID> = (response?.permissions ?: emptyList())
            .mapNotNull { runCatching { UUID.fromString(it.snippetId) }.getOrNull() }

        if (snippetIds.isEmpty())
            return PageDto(emptyList(), 0, page, size)

        val snippets = snippetRepo.findAllById(snippetIds)
        val summaries = snippets.map { s ->
            SnippetSummaryDto(
                id = s.id!!.toString(),
                name = s.name,
                description = s.description,
                language = s.language,
                version = s.languageVersion,
                ownerId = s.ownerId,
                lastIsValid = s.lastIsValid,
                lastLintCount = s.lastLintCount
            )
        }

        return PageDto(
            items = summaries,
            count = response?.count ?: summaries.size.toLong(),
            page = page,
            pageSize = size
        )
    }

    @Transactional
    override fun shareSnippet(req: ShareSnippetReq) {
        permissionClient.createAuthorization(
            PermissionCreateSnippetInput(
                snippetId = req.snippetId,
                userId = req.userId,
                permissionType = req.permissionType
            ),
            token = ""
        )
    }
    @Transactional
    override fun createTestCase(req: CreateTestReq): TestCaseDto {
        val inputsJson = objectMapper.writeValueAsString(req.inputs)
        val expectedOutputsJson = objectMapper.writeValueAsString(req.expectedOutputs)

        val testCase = testCaseRepo.save(
            TestCase(
                id = null,
                snippetId = UUID.fromString(req.snippetId), // si viene como String
                name = req.name,
                inputs = inputsJson,
                expectedOutputs = expectedOutputsJson,
                targetVersionNumber = req.targetVersionNumber,
                createdBy = "system"
            )
        )

        val inputs = objectMapper.readValue(inputsJson, Array<String>::class.java).toList()
        val expectedOutputs = objectMapper.readValue(expectedOutputsJson, Array<String>::class.java).toList()

        return TestCaseDto(
            id = testCase.id!!.toString(),
            snippetId = testCase.snippetId.toString(),
            name = testCase.name,
            inputs = inputs,
            expectedOutputs = expectedOutputs,
            targetVersionNumber = testCase.targetVersionNumber
        )
    }


    @Transactional(readOnly = true)
    override fun listTestCases(snippetId: UUID): List<TestCaseDto> =
        testCaseRepo.findAllBySnippetId(snippetId).map {
            val inputs = objectMapper.readValue(it.inputs, Array<String>::class.java).toList()
            val expectedOutputs = objectMapper.readValue(it.expectedOutputs, Array<String>::class.java).toList()

            TestCaseDto(
                id = it.id!!.toString(),
                snippetId = it.snippetId.toString(),
                name = it.name,
                inputs = inputs,
                expectedOutputs = expectedOutputs,
                targetVersionNumber = it.targetVersionNumber
            )
        }

    @Transactional
    override fun deleteTestCase(testCaseId: UUID) {
        testCaseRepo.deleteById(testCaseId)
    }


    override fun runParse(req: ParseReq): ParseRes = executionClient.parse(req)
    override fun runLint(req: LintReq): LintRes = executionClient.lint(req)
    override fun runFormat(req: FormatReq): FormatRes = executionClient.format(req)

}
