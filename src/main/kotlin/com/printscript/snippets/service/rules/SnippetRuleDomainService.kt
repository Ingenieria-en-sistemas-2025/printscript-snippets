package com.printscript.snippets.service.rules

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.printscript.snippets.bucket.SnippetAsset
import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.domain.model.LintStatus
import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.domain.model.SnippetVersion
import com.printscript.snippets.dto.SnippetDetailDto
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.execution.SnippetExecution
import com.printscript.snippets.permission.SnippetPermission
import com.printscript.snippets.service.ComplianceCalculator
import com.printscript.snippets.service.SnippetAuthorizationScopeService
import io.printscript.contracts.DiagnosticDto
import io.printscript.contracts.formatter.FormatReq
import io.printscript.contracts.linting.LintReq
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.util.UUID

@Service
class SnippetRuleDomainService(
    private val snippetRepo: SnippetRepo,
    private val versionRepo: SnippetVersionRepo,
    private val assetClient: SnippetAsset,
    private val executionClient: SnippetExecution,
    private val permissionClient: SnippetPermission,
    private val rulesStateService: RulesStateService,
) {

    private val authorization = SnippetAuthorizationScopeService(permissionClient)
    private val containerName = "snippets"

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

    fun saveFormatted(snippetId: UUID, formatted: String) {
        val snippet = snippetRepo.findById(snippetId)
            .orElseThrow { NotFound("Snippet not found") }

        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet $snippetId has no versions")

        val baseKey = "${
            snippet.ownerId
        }/$snippetId/v${latest.versionNumber}.ps"
        val formattedKey = baseKey.replace(".ps", ".formatted.ps")

        assetClient.upload(containerName, formattedKey, formatted.toByteArray(Charsets.UTF_8))
        latest.formattedKey = formattedKey
        latest.isFormatted = true

        versionRepo.save(latest)
    }

    fun saveLint(
        snippetId: UUID,
        violations: List<DiagnosticDto>,
    ) {
        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet $snippetId has no versions")

        val json = jacksonObjectMapper().writeValueAsString(violations)
        latest.lintIssues = json
        latest.isValid = violations.isEmpty()
        latest.lintStatus = LintStatus.DONE
        versionRepo.save(latest)
        val s = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        s.lastLintCount = violations.size
        s.lastIsValid = latest.isValid
        s.compliance = ComplianceCalculator.compute(LintStatus.DONE, latest.isValid, violations.size)
        snippetRepo.save(s)
    }

    @Transactional
    fun formatOneOwnerAware(userId: String, snippetId: UUID): SnippetDetailDto {
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        authorization.requireOwner(userId, snippet)

        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet without versions")

        val original = String(assetClient.download(containerName, latest.contentKey), StandardCharsets.UTF_8)

        val rules = rulesStateService.getFormatAsRules(snippet.ownerId)
        val options = FormatterMapper.toFormatterOptionsDto(rules)
        val (cfgText, cfgFmt) = rulesStateService.currentFormatConfigEffective(snippet.ownerId)

        val req = FormatReq(
            language = snippet.language,
            version = snippet.languageVersion,
            content = original,
            configText = cfgText,
            configFormat = cfgFmt,
            options = options,
        )
        val res = executionClient.format(req)

        saveFormatted(snippetId, res.formattedContent)
        val updated = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId) ?: latest
        return toDetailDto(snippet, updated, res.formattedContent)
    }

    @Transactional
    fun lintOneOwnerAware(userId: String, snippetId: UUID): SnippetDetailDto {
        val snippet = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        authorization.requireOwner(userId, snippet)

        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet without versions")

        val original = String(assetClient.download(containerName, latest.contentKey), StandardCharsets.UTF_8)

        val (cfgText, cfgFmt) = rulesStateService.currentLintConfigEffective(snippet.ownerId)
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
