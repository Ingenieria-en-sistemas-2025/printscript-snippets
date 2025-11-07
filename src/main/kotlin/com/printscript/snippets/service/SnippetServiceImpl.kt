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
import com.printscript.snippets.dto.RelationFilter
import com.printscript.snippets.dto.ShareSnippetReq
import com.printscript.snippets.dto.SingleTestRunResult
import com.printscript.snippets.dto.SnippetDetailDto
import com.printscript.snippets.dto.SnippetSource
import com.printscript.snippets.dto.SnippetSummaryDto
import com.printscript.snippets.dto.TestCaseDto
import com.printscript.snippets.dto.UpdateSnippetReq
import com.printscript.snippets.error.ApiDiagnostic
import com.printscript.snippets.error.InvalidRequest
import com.printscript.snippets.error.InvalidSnippet
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.error.UnsupportedOperation
import com.printscript.snippets.execution.SnippetExecution
import com.printscript.snippets.execution.dto.DiagnosticDto
import com.printscript.snippets.execution.dto.FormatReq
import com.printscript.snippets.execution.dto.FormatRes
import com.printscript.snippets.execution.dto.LintReq
import com.printscript.snippets.execution.dto.LintRes
import com.printscript.snippets.execution.dto.ParseReq
import com.printscript.snippets.execution.dto.ParseRes
import com.printscript.snippets.execution.dto.RunSingleTestReq
import com.printscript.snippets.permission.SnippetPermission
import com.printscript.snippets.permission.dto.PermissionCreateSnippetInput
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

@Service
class SnippetServiceImpl(
    private val snippetRepo: SnippetRepo,
    private val versionRepo: SnippetVersionRepo,
    private val testCaseRepo: TestCaseRepo,
    private val assetClient: SnippetAsset,
    private val executionClient: SnippetExecution,
    private val permissionClient: SnippetPermission,
) : SnippetService {
    private val logger = LoggerFactory.getLogger(SnippetServiceImpl::class.java)
    private val containerName = "snippets"
    private val objectMapper = jacksonObjectMapper()

    // key del archivo en el bucket
    private fun buildVersionKey(ownerId: String, snippetId: UUID, versionNumber: Long): String =
        "$ownerId/$snippetId/v$versionNumber.ps"

    // ult nro de version persistido para ese snippet
    private fun getLatestVersionNumber(snippetId: UUID): Long =
        versionRepo.findMaxVersionBySnippetId(snippetId) ?: 0L

    // mapper simple Execution.DiagnosticDto -> ApiDiagnostic
    private fun toApiDiagnostics(list: List<DiagnosticDto>): List<ApiDiagnostic> =
        list.map { diagnostic -> ApiDiagnostic(diagnostic.ruleId, diagnostic.message, diagnostic.line, diagnostic.col) }

    private fun assertOwner(userId: String, snippet: Snippet) {
        if (snippet.ownerId != userId) throw UnsupportedOperation("Only owner can modify snippet")
    }

    private fun createAndPersistVersion(
        snippet: Snippet,
        content: String,
    ): SnippetVersion {
        val nextVersion = getLatestVersionNumber(snippet.id!!) + 1
        val contentKey = buildVersionKey(snippet.ownerId, snippet.id!!, nextVersion)

        logger.info("Starting version creation for snippet ${snippet.id}, next version: $nextVersion")

        // valido sintaxis antes de subir al bucket
        val parseRes = executionClient.parse(ParseReq(snippet.language, snippet.languageVersion, content))
        if (!parseRes.valid) {
            logger.error("Snippet version creation failed due to syntax errors. Count: ${parseRes.diagnostics.size}")
            throw InvalidSnippet(toApiDiagnostics(parseRes.diagnostics))
        }

        logger.debug("Uploading content to asset client with key: $contentKey")
        assetClient.upload(containerName, contentKey, content.toByteArray(StandardCharsets.UTF_8))

        logger.debug("Calling execution service for linting...")
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
                createdAt = Instant.now(),
            ),
        )

        snippet.currentVersionId = version.id
        snippet.lastIsValid = version.isValid
        snippet.lastLintCount = lintRes.violations.size
        snippetRepo.save(snippet)
        logger.info("Snippet version $nextVersion created successfully")
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
            lintCount = snippet.lastLintCount,
        )

    override fun createSnippet(ownerId: String, req: CreateSnippetReq): SnippetDetailDto {
        logger.info("Request to create snippet '${req.name}' by user $ownerId")
        val content = (req.content ?: "").also {
            if (it.isBlank()) throw InvalidRequest("El contenido del snippet no puede estar vacío")
        }
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

        val version = createAndPersistVersion(snippet, content)

//        logger.debug("Calling permission client to set OWNER for snippet ${snippet.id}")
//        permissionClient.createAuthorization(
//            PermissionCreateSnippetInput(snippet.id!!.toString(), ownerId, scope = "OWNER"),
//            token = "",
//        )
//        logger.info("Snippet ${snippet.id} created and OWNER permission granted.")

        return toDetailDto(snippet, version, content)
    }

    @Transactional(readOnly = true)
    override fun getSnippet(snippetId: UUID): SnippetDetailDto {
        val snippet = snippetRepo.findById(snippetId)
            .orElseThrow { NotFound("Snippet with ID $snippetId not found") }

        val latestVersion = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet $snippetId has no versions")

        val content = String(
            assetClient.download(containerName, latestVersion.contentKey),
            StandardCharsets.UTF_8,
        )
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

        val content = String(
            assetClient.download(containerName, latestVersion.contentKey),
            StandardCharsets.UTF_8,
        )

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
                "El enum SnippetSource no contiene el contenido.",
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
        val content = String(fileBytes, StandardCharsets.UTF_8)
        val version = createAndPersistVersion(snippet, content)
        return toDetailDto(snippet, version, content)
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

        val snippetIds: List<String> = (response?.authorizations ?: emptyList())
            .map { it.snippetId.toString() }

        if (snippetIds.isEmpty()) {
            return PageDto(emptyList(), 0, page, size)
        }

        val uuids = snippetIds.map(UUID::fromString)
        val snippets = snippetRepo.findAllById(uuids)

        val summaries = snippets.map { s ->
            SnippetSummaryDto(
                id = s.id!!.toString(),
                name = s.name,
                description = s.description,
                language = s.language,
                version = s.languageVersion,
                ownerId = s.ownerId,
                lastIsValid = s.lastIsValid,
                lastLintCount = s.lastLintCount,
            )
        }

        return PageDto(
            items = summaries,
            // CORRECCIÓN: Usar 'total' en lugar de 'count'
            count = response?.total?.toLong() ?: summaries.size.toLong(),
            page = page,
            pageSize = size,
        )
    }

    @Transactional
    override fun shareSnippet(req: ShareSnippetReq) {
        permissionClient.createAuthorization(
            PermissionCreateSnippetInput(
                snippetId = req.snippetId,
                userId = req.userId,
                scope = req.permissionType,
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
                id = null,
                snippetId = UUID.fromString(req.snippetId), // si viene como String
                name = req.name,
                inputs = inputsJson,
                expectedOutputs = expectedOutputsJson,
                targetVersionNumber = req.targetVersionNumber,
                createdBy = "system",
            ),
        )

        val inputs = objectMapper.readValue(inputsJson, Array<String>::class.java).toList()
        val expectedOutputs = objectMapper.readValue(expectedOutputsJson, Array<String>::class.java).toList()

        return TestCaseDto(
            id = testCase.id!!.toString(),
            snippetId = testCase.snippetId.toString(),
            name = testCase.name,
            inputs = inputs,
            expectedOutputs = expectedOutputs,
            targetVersionNumber = testCase.targetVersionNumber,
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
                targetVersionNumber = it.targetVersionNumber,
            )
        }

    @Transactional
    override fun deleteTestCase(testCaseId: UUID) {
        testCaseRepo.deleteById(testCaseId)
    }

    override fun runParse(req: ParseReq): ParseRes = executionClient.parse(req)
    override fun runLint(req: LintReq): LintRes = executionClient.lint(req)
    override fun runFormat(req: FormatReq): FormatRes = executionClient.format(req)

    override fun listAccessibleSnippets(userId: String, page: Int, size: Int, name: String?, language: String?, valid: Boolean?, relation: RelationFilter, sort: String): PageDto<SnippetSummaryDto> {
        // pregunta a permissions q snippets puede ver el user
        val perms = permissionClient.getAllSnippetsPermission(userId, token = "", pageNum = page, pageSize = size).body

        // CORRECCIÓN: Usar 'authorizations' en lugar de 'permissions'
        val ids = (perms?.authorizations ?: emptyList())
            .map { it.snippetId }

        if (ids.isEmpty()) return PageDto(emptyList(), 0, page, size)

        val uuidIds = ids.map(UUID::fromString)

        val all = snippetRepo.findAllById(uuidIds) // trae todos los snippets q tiene permiso

        // filtro x relacion
        val base = when (relation) {
            RelationFilter.OWNER -> all.filter { it.ownerId == userId }
            RelationFilter.SHARED -> all.filter { it.ownerId != userId }
            RelationFilter.BOTH -> all
        }

        val filtered = base
            .filter { name == null || it.name.contains(name, ignoreCase = true) }
            .filter { language == null || it.language.equals(language, ignoreCase = true) }
            .filter { valid == null || it.lastIsValid == valid }

        // ordeno
        val (field, dir) = sort.split(",", limit = 2).let { it.getOrNull(0) to (it.getOrNull(1) ?: "ASC") }
        val sorted = when (field) {
            "name" -> filtered.sortedBy { it.name.lowercase() }
            "language" -> filtered.sortedBy { it.language.lowercase() }
            "valid" -> filtered.sortedBy { it.lastIsValid }
            "updatedAt" -> filtered.sortedBy { it.updatedAt }
            else -> filtered.sortedBy { it.name.lowercase() }
        }.let { if (dir.equals("DESC", true)) it.reversed() else it }

        val from = (page * size).coerceAtMost(sorted.size)
        val to = (from + size).coerceAtMost(sorted.size)

        val pageItems = sorted.subList(from, to).map { snippet ->
            SnippetSummaryDto(
                id = snippet.id!!.toString(),
                name = snippet.name,
                description = snippet.description,
                language = snippet.language,
                version = snippet.languageVersion,
                ownerId = snippet.ownerId,
                lastIsValid = snippet.lastIsValid,
                lastLintCount = snippet.lastLintCount,
            )
        }
        return PageDto(pageItems, perms?.total?.toLong() ?: sorted.size.toLong(), page, size)
    }

    override fun createSnippetFromFile(ownerId: String, meta: CreateSnippetReq, bytes: ByteArray): SnippetDetailDto {
        if (bytes.isEmpty()) throw InvalidRequest("El archivo subido está vacío")
        val content = String(bytes, StandardCharsets.UTF_8)
        if (content.isBlank()) throw InvalidRequest("El contenido del snippet no puede estar vacío")
        return createSnippet(ownerId, meta.copy(content = content, source = SnippetSource.FILE_UPLOAD))
    }

    override fun updateSnippetOwnerAware(userId: String, snippetId: UUID, req: UpdateSnippetReq): SnippetDetailDto {
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        assertOwner(userId, snippet) // para verificar owner

        req.name?.let { snippet.name = it } // si el req trae nuevo name actualiza
        req.description?.let { snippet.description = it } // lo mismo con descr

        val langProvided = req.language != null
        val verProvided = req.version != null
        if (langProvided.xor(verProvided)) {
            throw InvalidRequest("Para cambiar el lenguaje, debés enviar language y version juntos")
        }

        var langChanged = false
        req.language?.let {
            snippet.language = it
            langChanged = true
        }
        req.version?.let {
            snippet.languageVersion = it
            langChanged = true
        }

        snippetRepo.save(snippet)

        // si mando nuveo content o cambio leng o version creo nueva version
        if (req.content != null || langChanged) { // si req.content tiene contenido lo uso
            val content = req.content ?: run {
                val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId) // sino uso el ultimo
                    ?: throw NotFound("Snippet without versions")
                String(assetClient.download(containerName, latest.contentKey), StandardCharsets.UTF_8)
            }
            if (content.isBlank()) throw InvalidRequest("El contenido del snippet no puede estar vacío")
            val version = createAndPersistVersion(snippet, content)
            return toDetailDto(snippet, version, content)
        }

        // si no hay cambios de content ni de leng/version devuelvo  SnippetDetailDto actualizado pero con mismo content
        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet without versions")
        val content = String(assetClient.download(containerName, latest.contentKey), StandardCharsets.UTF_8)
        return toDetailDto(snippet, latest, content)
    }

    @Transactional
    override fun deleteSnippetOwnerAware(userId: String, snippetId: UUID) {
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        assertOwner(userId, snippet)
        deleteSnippet(snippetId)
    }

    @Transactional
    override fun shareSnippetOwnerAware(ownerId: String, req: ShareSnippetReq) {
        val snippet = snippetRepo.findById(UUID.fromString(req.snippetId))
            .orElseThrow { NotFound("Snippet not found") }
        assertOwner(ownerId, snippet)
        shareSnippet(req)
    }

    @Transactional(readOnly = true)
    override fun download(snippetId: UUID, formatted: Boolean): ByteArray {
        val version = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet without versions")
        val key = if (formatted && version.isFormatted && version.formattedKey != null) version.formattedKey!! else version.contentKey
        return assetClient.download(containerName, key)
    }

    @Transactional(readOnly = true)
    override fun filename(snippetId: UUID, formatted: Boolean): String {
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        val version = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet without versions")
        val base = "${snippet.name}-v${version.versionNumber}"
        return if (formatted && version.isFormatted && version.formattedKey != null) "$base.formatted.ps" else "$base.ps"
    }

    @Transactional(readOnly = true)
    override fun runOneTestOwnerAware(
        userId: String,
        snippetId: UUID,
        testCaseId: UUID,
    ): SingleTestRunResult {
        // Autorización básica: owner o con permiso
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        val perms = permissionClient.getAllSnippetsPermission(userId, token = "", pageNum = 0, pageSize = 1).body

        val canAccess = snippet.ownerId == userId ||
            (perms?.authorizations ?: emptyList()).any { it.snippetId == snippetId.toString() }

        if (!canAccess) throw UnsupportedOperation("You don't have permission to run tests for this snippet")

        // latest content
        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet without versions")
        val content = String(assetClient.download(containerName, latest.contentKey), StandardCharsets.UTF_8)

        // test case
        val tc = testCaseRepo.findById(testCaseId).orElseThrow { NotFound("Test case not found") }
        if (tc.snippetId != snippetId) throw InvalidRequest("El test no pertenece a este snippet")

        val inputs = objectMapper.readValue(tc.inputs, Array<String>::class.java).toList()
        val expected = objectMapper.readValue(tc.expectedOutputs, Array<String>::class.java).toList()

        // armar request execution /run-test
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
}
