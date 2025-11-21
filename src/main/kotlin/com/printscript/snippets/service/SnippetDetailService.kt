package com.printscript.snippets.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.printscript.snippets.bucket.SnippetAsset
import com.printscript.snippets.domain.LanguageConfigRepo
import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.domain.model.LintStatus
import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.domain.model.SnippetVersion
import com.printscript.snippets.dto.CreateSnippetReq
import com.printscript.snippets.dto.FileTypeDto
import com.printscript.snippets.dto.PageDto
import com.printscript.snippets.dto.RelationFilter
import com.printscript.snippets.dto.SnippetDetailDto
import com.printscript.snippets.dto.SnippetSource
import com.printscript.snippets.dto.SnippetSummaryDto
import com.printscript.snippets.dto.UpdateSnippetReq
import com.printscript.snippets.error.ApiDiagnostic
import com.printscript.snippets.error.InvalidRequest
import com.printscript.snippets.error.InvalidSnippet
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.execution.SnippetExecution
import com.printscript.snippets.permission.SnippetPermission
import com.printscript.snippets.service.rules.RulesStateService
import com.printscript.snippets.user.UserService
import io.printscript.contracts.DiagnosticDto
import io.printscript.contracts.linting.LintReq
import io.printscript.contracts.parse.ParseReq
import io.printscript.contracts.permissions.PermissionCreateSnippetInput
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.util.UUID

@Service
class SnippetDetailService(
    private val snippetRepo: SnippetRepo,
    private val versionRepo: SnippetVersionRepo,
    private val assetClient: SnippetAsset,
    private val executionClient: SnippetExecution,
    private val permissionClient: SnippetPermission,
    private val userService: UserService,
    private val rulesStateService: RulesStateService,
    private val languageConfigRepo: LanguageConfigRepo,
) {

    private val authorization = SnippetAuthorizationScopeService(permissionClient)
    private val logger = LoggerFactory.getLogger(SnippetDetailService::class.java)
    private val containerName = "snippets"
    private val objectMapper = jacksonObjectMapper()

    private fun requireSnippet(snippetId: UUID): Snippet =
        snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet with ID $snippetId not found") }

    private fun requireLatestVersion(snippetId: UUID): SnippetVersion =
        versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet $snippetId has no versions")

    private fun validateNonBlankContent(content: String) {
        if (content.isBlank()) {
            throw InvalidRequest("El contenido del snippet no puede estar vacío")
        }
    }

    private fun readContent(key: String): String =
        String(assetClient.download(containerName, key), StandardCharsets.UTF_8)

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

        val (configText, configFormat) = rulesStateService.currentLintConfigEffective(snippet.ownerId)
        logger.debug("Calling execution service for linting with owner rules...")
        val lintRes = executionClient.lint(
            LintReq(
                language = snippet.language,
                version = snippet.languageVersion,
                content = content,
                configText = configText,
                configFormat = configFormat,
            ),
        )

        val version = versionRepo.save(
            SnippetVersion(
                id = null,
                snippetId = snippet.id!!,
                versionNumber = nextVersion,
                contentKey = contentKey,
                formattedKey = null,
                isFormatted = false,
                isValid = lintRes.violations.isEmpty(),
                lintIssues = objectMapper.writeValueAsString(lintRes.violations),
                lintStatus = LintStatus.DONE,
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

    private fun toSummaryDto(snippet: Snippet): SnippetSummaryDto {
        val authorEmail = userService.getEmailById(snippet.ownerId)
        return SnippetSummaryDto(
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

    @Transactional
    fun createSnippet(ownerId: String, req: CreateSnippetReq): SnippetDetailDto {
        logger.info("Request to create snippet '${req.name}' by user $ownerId")
        val content = (req.content ?: "").also { validateNonBlankContent(it) }

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
    fun getSnippet(snippetId: UUID): SnippetDetailDto {
        val snippet = requireSnippet(snippetId)

        val latest = requireLatestVersion(snippetId)

        val key = if (latest.isFormatted && latest.formattedKey != null) {
            latest.formattedKey!!
        } else {
            latest.contentKey
        }

        val content = readContent(key)
        return toDetailDto(snippet, latest, content)
    }

    fun deleteSnippet(snippetId: UUID) {
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

    @Transactional(readOnly = true)
    fun listAccessibleSnippets(
        userId: String,
        page: Int,
        size: Int,
        name: String?,
        relation: RelationFilter,
    ): PageDto<SnippetSummaryDto> {
        val base = findSnippetsWithPermissions(userId, relation)

        val filtered = base.filter { snippet ->
            name == null || snippet.name.contains(name, ignoreCase = true)
        }

        val sorted = filtered.sortedByDescending { it.createdAt }

        val total = sorted.size
        val (from, to) = pageBounds(total, page, size)

        val pageItems = if (from < total) {
            sorted.subList(from, to).map(::toSummaryDto)
        } else {
            emptyList()
        }

        return PageDto(pageItems, total.toLong(), page, size)
    }

    fun createSnippetFromFile(ownerId: String, meta: CreateSnippetReq, bytes: ByteArray): SnippetDetailDto {
        if (bytes.isEmpty()) throw InvalidRequest("El archivo subido está vacío")
        val content = String(bytes, StandardCharsets.UTF_8)
        validateNonBlankContent(content)
        return createSnippet(ownerId, meta.copy(content = content, source = SnippetSource.FILE_UPLOAD))
    }

    @Transactional
    fun updateSnippetOwnerAware(userId: String, snippetId: UUID, req: UpdateSnippetReq): SnippetDetailDto {
        val snippet = requireSnippet(snippetId)
        authorization.requireEditorOrOwner(userId, snippet)

        applyBasicUpdates(snippet, req)
        snippetRepo.save(snippet)

        val needsNewVersion = req.content != null || req.language != null || req.version != null

        if (needsNewVersion) {
            val content = resolveUpdatedContent(snippetId, req)
            validateNonBlankContent(content)
            val version = createAndPersistVersion(snippet, content)
            return toDetailDto(snippet, version, content)
        }

        val latest = requireLatestVersion(snippetId)
        val content = readContent(latest.contentKey)
        return toDetailDto(snippet, latest, content)
    }

    @Transactional
    fun deleteSnippetOwnerAware(userId: String, snippetId: UUID) {
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        authorization.requireOwner(userId, snippet)
        deleteSnippet(snippetId)
    }

    @Transactional(readOnly = true)
    fun download(snippetId: UUID, formatted: Boolean): ByteArray {
        val version = requireLatestVersion(snippetId)
        val key = if (formatted && version.isFormatted && version.formattedKey != null) version.formattedKey!! else version.contentKey
        return assetClient.download(containerName, key)
    }

    @Transactional(readOnly = true)
    fun filename(snippetId: UUID, formatted: Boolean): String {
        val snippet = requireSnippet(snippetId)
        val version = requireLatestVersion(snippetId)
        val base = "${snippet.name}-v${version.versionNumber}"
        return if (formatted && version.isFormatted && version.formattedKey != null) {
            "$base.formatted.prs"
        } else {
            "$base.prs"
        }
    }

    fun getFileTypes(): List<FileTypeDto> {
        val rows = languageConfigRepo.findAll()

        return rows
            .groupBy { it.language }
            .map { (lang, entries) ->
                FileTypeDto(
                    language = lang,
                    extension = entries.first().extension,
                    versions = entries.map { it.version }.sortedDescending(),
                )
            }
    }

    private fun findSnippetsWithPermissions(
        userId: String,
        relation: RelationFilter,
    ): List<Snippet> {
        val perms = permissionClient.getAllSnippetsPermission(userId, pageNum = 0, pageSize = 1000).body
        val ids = (perms?.authorizations ?: emptyList()).mapNotNull { authView ->
            runCatching { UUID.fromString(authView.snippetId) }
                .onFailure {
                    logger.warn("Invalid UUID format found in authorization service: ${authView.snippetId}", it)
                }
                .getOrNull()
        }

        val all = snippetRepo.findAllById(ids)

        return when (relation) {
            RelationFilter.OWNER -> all.filter { it.ownerId == userId }
            RelationFilter.SHARED -> all.filter { it.ownerId != userId }
            RelationFilter.BOTH -> all
        }
    }

    private fun pageBounds(total: Int, page: Int, size: Int): Pair<Int, Int> {
        val from = (page * size).coerceAtMost(total) // calcular indice iniciall
        val to = (from + size).coerceAtMost(total)
        return from to to
    }

    private fun applyBasicUpdates(snippet: Snippet, req: UpdateSnippetReq) {
        req.name?.let { snippet.name = it }
        req.description?.let { snippet.description = it }

        val langProvided = req.language != null
        val verProvided = req.version != null
        if (langProvided.xor(verProvided)) {
            throw InvalidRequest("Para cambiar el lenguaje, debés enviar language y version juntos")
        }

        req.language?.let { snippet.language = it }
        req.version?.let { snippet.languageVersion = it }
    }

    private fun resolveUpdatedContent(snippetId: UUID, req: UpdateSnippetReq): String =
        req.content ?: run {
            val latest = requireLatestVersion(snippetId)
            readContent(latest.contentKey)
        }
}
