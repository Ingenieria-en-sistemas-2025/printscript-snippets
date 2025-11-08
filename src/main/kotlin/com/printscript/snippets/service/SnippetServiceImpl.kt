package com.printscript.snippets.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.printscript.snippets.bucket.SnippetAsset
import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.domain.TestCaseRepo
import com.printscript.snippets.domain.model.LintStatus
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
import com.printscript.snippets.service.rules.FormatterMapper
import com.printscript.snippets.service.rules.RulesStateService
import com.printscript.snippets.user.UserService
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
    private val userService: UserService,
    private val rulesStateService: RulesStateService,
) : SnippetService {

    private val authorization = SnippetAuthorization(permissionClient)
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
                lintStatus = LintStatus.DONE,
                parseErrors = "[]",
                createdAt = Instant.now(),
            ),
        )

        snippet.currentVersionId = version.id
        snippet.lastIsValid = version.isValid
        snippet.lastLintCount = lintRes.violations.size
        snippet.compliance = ComplianceCalculator.compute(
            LintStatus.DONE, version.isValid, lintRes.violations.size,
        )
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

        logger.debug("Calling permission client to set OWNER for snippet ${snippet.id}")
        permissionClient.createAuthorization(
            PermissionCreateSnippetInput(snippet.id!!.toString(), ownerId, scope = "OWNER"),
        )
        logger.info("Snippet ${snippet.id} created and OWNER permission granted.")

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
        permissionClient.deleteSnippetPermissions(snippetId.toString())

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
        val decodedUserId = java.net.URLDecoder.decode(userId, "UTF-8")

        val response = permissionClient
            .getAllSnippetsPermission(decodedUserId, pageNum = 0, pageSize = Int.MAX_VALUE)
            .body

        logger.info("Authorization service response received")
        logger.info("Response is null: ${response == null}")
        logger.info("Authorizations list: ${response?.authorizations}")
        logger.info("Authorizations count: ${response?.authorizations?.size ?: 0}")
        logger.info("Total from response: ${response?.total}")

        val snippetIds: List<UUID> = (response?.authorizations ?: emptyList())
            .mapNotNull { authView ->
                try {
                    UUID.fromString(authView.snippetId)
                } catch (e: IllegalArgumentException) {
                    logger.warn("Invalid UUID format for snippetId: ${authView.snippetId}", e)
                    null
                }
            }

        logger.info("Found ${snippetIds.size} snippet IDs with permissions for user $userId")

        if (snippetIds.isEmpty()) {
            logger.info("No snippets found for user $userId, returning empty page")
            return PageDto(emptyList(), 0, page, size)
        }

        val snippets = snippetRepo.findAllById(snippetIds)
        logger.info("Retrieved ${snippets.size} snippets from database")

        val sorted = snippets.sortedByDescending { it.updatedAt }

        val total = sorted.size
        val from = (page * size).coerceAtMost(total)
        val to = (from + size).coerceAtMost(total)

        val pageItems = if (from < total) {
            sorted.subList(from, to).map { s ->
                val authorEmail = userService.getEmailById(s.ownerId)

                SnippetSummaryDto(
                    id = s.id!!.toString(),
                    name = s.name,
                    description = s.description,
                    language = s.language,
                    version = s.languageVersion,
                    ownerId = s.ownerId,
                    ownerEmail = authorEmail,
                    lastIsValid = s.lastIsValid,
                    lastLintCount = s.lastLintCount,
                    compliance = s.compliance.name,
                )
            }
        } else {
            emptyList()
        }

        logger.info("Returning page $page with ${pageItems.size} items (total: $total)")

        return PageDto(
            items = pageItems,
            count = total.toLong(),
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
        )
    }

    @Transactional
    override fun createTestCase(req: CreateTestReq): TestCaseDto {
        val testCase = testCaseRepo.save(
            TestCase(
                snippetId = UUID.fromString(req.snippetId!!),
                name = req.name,
                inputs = req.inputs,
                expectedOutputs = req.expectedOutputs,
                targetVersionNumber = req.targetVersionNumber,
                createdBy = "system",
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
    override fun listTestCases(snippetId: UUID): List<TestCaseDto> =
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
    override fun deleteTestCase(testCaseId: UUID) {
        testCaseRepo.deleteById(testCaseId)
    }

    override fun runParse(req: ParseReq): ParseRes = executionClient.parse(req)
    override fun runLint(req: LintReq): LintRes = executionClient.lint(req)
    override fun runFormat(req: FormatReq): FormatRes = executionClient.format(req)

    @Transactional(readOnly = true)
    override fun listAccessibleSnippets(
        userId: String,
        page: Int,
        size: Int,
        name: String?,
        language: String?,
        valid: Boolean?,
        relation: RelationFilter,
        sort: String,
    ): PageDto<SnippetSummaryDto> {
        val perms = permissionClient.getAllSnippetsPermission(userId, pageNum = 0, pageSize = Int.MAX_VALUE).body
        val ids = (perms?.authorizations ?: emptyList()).mapNotNull { authView ->
            runCatching { UUID.fromString(authView.snippetId) }
                .onFailure {
                    logger.warn("Invalid UUID format found in authorization service: ${authView.snippetId}", it)
                }
                .getOrNull()
        }

        val all = snippetRepo.findAllById(ids)

        val base = when (relation) {
            RelationFilter.OWNER -> all.filter { it.ownerId == userId }
            RelationFilter.SHARED -> all.filter { it.ownerId != userId }
            RelationFilter.BOTH -> all
        }

        val filtered = base
            .filter { name == null || it.name.contains(name, ignoreCase = true) }
            .filter { language == null || it.language.equals(language, ignoreCase = true) }
            .filter { valid == null || it.lastIsValid == valid }

        val (field, dir) = sort.split(",", limit = 2).let {
            it.getOrNull(0) to (it.getOrNull(1) ?: "ASC")
        }

        val sorted = when (field) {
            "name" -> filtered.sortedBy { it.name.lowercase() }
            "language" -> filtered.sortedBy { it.language.lowercase() }
            "valid" -> filtered.sortedBy { it.lastIsValid }
            "updatedAt" -> filtered.sortedBy { it.updatedAt }
            else -> filtered.sortedBy { it.name.lowercase() }
        }.let { if (dir.equals("DESC", true)) it.reversed() else it }

        val totalFiltered = sorted.size
        val from = (page * size).coerceAtMost(totalFiltered)
        val to = (from + size).coerceAtMost(totalFiltered)

        val pageItems = if (from < totalFiltered) {
            sorted.subList(from, to).map { snippet ->
                val authorEmail = userService.getEmailById(snippet.ownerId)

                SnippetSummaryDto(
                    id = snippet.id!!.toString(),
                    name = snippet.name,
                    description = snippet.description,
                    language = snippet.language,
                    version = snippet.languageVersion,
                    ownerId = snippet.ownerId,
                    ownerEmail = authorEmail,
                    lastIsValid = snippet.lastIsValid,
                    lastLintCount = snippet.lastLintCount,
                    compliance = snippet.compliance.name,
                )
            }
        } else {
            emptyList()
        }

        return PageDto(pageItems, totalFiltered.toLong(), page, size)
    }

    override fun createSnippetFromFile(ownerId: String, meta: CreateSnippetReq, bytes: ByteArray): SnippetDetailDto {
        if (bytes.isEmpty()) throw InvalidRequest("El archivo subido está vacío")
        val content = String(bytes, StandardCharsets.UTF_8)
        if (content.isBlank()) throw InvalidRequest("El contenido del snippet no puede estar vacío")
        return createSnippet(ownerId, meta.copy(content = content, source = SnippetSource.FILE_UPLOAD))
    }

    @Transactional
    override fun updateSnippetOwnerAware(userId: String, snippetId: UUID, req: UpdateSnippetReq): SnippetDetailDto {
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        authorization.requireEditorOrOwner(userId, snippet)

        req.name?.let { snippet.name = it }
        req.description?.let { snippet.description = it }

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

        if (req.content != null || langChanged) {
            val content = req.content ?: run {
                val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
                    ?: throw NotFound("Snippet without versions")
                String(assetClient.download(containerName, latest.contentKey), StandardCharsets.UTF_8)
            }
            if (content.isBlank()) throw InvalidRequest("El contenido del snippet no puede estar vacío")
            val version = createAndPersistVersion(snippet, content)
            return toDetailDto(snippet, version, content)
        }

        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet without versions")
        val content = String(assetClient.download(containerName, latest.contentKey), StandardCharsets.UTF_8)
        return toDetailDto(snippet, latest, content)
    }

    @Transactional
    override fun deleteSnippetOwnerAware(userId: String, snippetId: UUID) {
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        authorization.requireOwner(userId, snippet)
        deleteSnippet(snippetId)
    }

    @Transactional
    override fun shareSnippetOwnerAware(ownerId: String, req: ShareSnippetReq) {
        val snippet = snippetRepo.findById(UUID.fromString(req.snippetId))
            .orElseThrow { NotFound("Snippet not found") }
        authorization.requireOwner(ownerId, snippet)
        shareSnippet(req)
    }

    @Transactional(readOnly = true)
    override fun download(snippetId: UUID, formatted: Boolean): ByteArray {
        // val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }

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

    override fun saveFormatted(snippetId: UUID, formatted: String) {
        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet $snippetId has no versions")

        val formattedKey = "snippets/$snippetId/v${latest.versionNumber}.formatted.ps"
        assetClient.upload(containerName, formattedKey, formatted.toByteArray(Charsets.UTF_8))
        latest.formattedKey = formattedKey
        latest.isFormatted = true
        versionRepo.save(latest)
    }

    override fun saveLint(
        snippetId: UUID,
        violations: List<DiagnosticDto>,
    ) {
        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet $snippetId has no versions")

        val json = jacksonObjectMapper().writeValueAsString(violations)
        latest.lintIssues = json
        latest.isValid = latest.isValid && violations.isEmpty()
        latest.lintStatus = LintStatus.DONE
        versionRepo.save(latest)
        val s = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        s.lastLintCount = violations.size
        s.lastIsValid = latest.isValid
        s.compliance = ComplianceCalculator.compute(LintStatus.DONE, latest.isValid, violations.size)
        snippetRepo.save(s)
    }

    @Transactional
    override fun formatOne(snippetId: UUID): SnippetDetailDto {
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet without versions")

        val original = String(assetClient.download(containerName, latest.contentKey), StandardCharsets.UTF_8)

        // Rules -> FormatterOptionsDto
        val rules = rulesStateService.getFormatAsRules(snippet.ownerId)
        val options = FormatterMapper.toFormatterOptionsDto(rules)

        val (cfgText, cfgFmt) = rulesStateService.currentFormatConfig(snippet.ownerId)

        val req = FormatReq(
            language = snippet.language,
            version = snippet.languageVersion,
            content = original,
            options = options,
            configText = cfgText,
            configFormat = cfgFmt,
        )
        val res = executionClient.format(req)

        saveFormatted(snippetId, res.formattedContent)

        val updatedVersion = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId) ?: latest
        return toDetailDto(snippet, updatedVersion, res.formattedContent)
    }

    @Transactional
    override fun lintOne(snippetId: UUID): SnippetDetailDto {
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet without versions")

        val original = String(assetClient.download(containerName, latest.contentKey), StandardCharsets.UTF_8)

        val (cfgText, cfgFmt) = rulesStateService.currentLintConfig(snippet.ownerId)

        val req = LintReq(
            language = snippet.language,
            version = snippet.languageVersion,
            content = original,
            configText = cfgText,
            configFormat = cfgFmt,
        )
        val res = executionClient.lint(req)

        saveLint(snippetId, res.violations)

        val updatedVersion = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId) ?: latest
        return toDetailDto(snippet, updatedVersion, content = original)
    }

    @Transactional
    override fun formatOneOwnerAware(userId: String, snippetId: UUID): SnippetDetailDto {
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        authorization.requireOwner(userId, snippet)

        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet without versions")

        val original = String(assetClient.download(containerName, latest.contentKey), StandardCharsets.UTF_8)

        val rules = rulesStateService.getFormatAsRules(snippet.ownerId)
        val options = FormatterMapper.toFormatterOptionsDto(rules)
        val (cfgText, cfgFmt) = rulesStateService.currentFormatConfig(snippet.ownerId)

        val req = FormatReq(
            language = snippet.language,
            version = snippet.languageVersion,
            content = original,
            options = options,
            configText = cfgText,
            configFormat = cfgFmt,
        )
        val res = executionClient.format(req)

        saveFormatted(snippetId, res.formattedContent)
        val updated = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId) ?: latest
        return toDetailDto(snippet, updated, res.formattedContent)
    }

    @Transactional
    override fun lintOneOwnerAware(userId: String, snippetId: UUID): SnippetDetailDto {
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        authorization.requireOwner(userId, snippet)

        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet without versions")

        val original = String(assetClient.download(containerName, latest.contentKey), StandardCharsets.UTF_8)

        val (cfgText, cfgFmt) = rulesStateService.currentLintConfig(snippet.ownerId)

        val req = LintReq(
            language = snippet.language,
            version = snippet.languageVersion,
            content = original,
            configText = cfgText,
            configFormat = cfgFmt,
        )
        val res = executionClient.lint(req)

        saveLint(snippetId, res.violations)

        val updated = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId) ?: latest
        return toDetailDto(snippet, updated, original)
    }
}
