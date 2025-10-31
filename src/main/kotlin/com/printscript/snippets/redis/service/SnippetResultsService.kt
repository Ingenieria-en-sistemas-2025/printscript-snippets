package com.printscript.snippets.redis.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.printscript.snippets.bucket.SnippetAsset
import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.execution.dto.DiagnosticDto
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SnippetResultsService(
    private val snippetRepo: SnippetRepo,
    private val versionRepo: SnippetVersionRepo,
    private val asset: SnippetAsset,
) {
    private val container = "snippets"

    fun saveFormatted(snippetId: UUID, formatted: String) {
        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet $snippetId has no versions")

        val formattedKey = "snippets/$snippetId/v${latest.versionNumber}.formatted.ps"
        asset.upload(container, formattedKey, formatted.toByteArray(Charsets.UTF_8))
        latest.formattedKey = formattedKey
        latest.isFormatted = true
        versionRepo.save(latest)
    }

    fun saveLint(snippetId: UUID, violations: List<DiagnosticDto>) {
        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)
            ?: throw NotFound("Snippet $snippetId has no versions")

        val json = jacksonObjectMapper().writeValueAsString(violations)
        latest.lintIssues = json
        latest.isValid = latest.isValid && violations.isEmpty()
        versionRepo.save(latest)
        val s = snippetRepo.findById(snippetId).orElseThrow { NotFound("Snippet not found") }
        s.lastLintCount = violations.size
        s.lastIsValid = latest.isValid
        snippetRepo.save(s)
    }
}
