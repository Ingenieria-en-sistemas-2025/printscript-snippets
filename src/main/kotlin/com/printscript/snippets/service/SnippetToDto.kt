package com.printscript.snippets.service

import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.domain.model.SnippetVersion
import com.printscript.snippets.dto.SnippetDetailDto
import com.printscript.snippets.dto.SnippetSummaryDto
import com.printscript.snippets.error.ApiDiagnostic
import io.printscript.contracts.DiagnosticDto

object SnippetToDto {
    fun toDetailDto(snippet: Snippet, version: SnippetVersion, content: String?): SnippetDetailDto =
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

    fun toSummaryDto(snippet: Snippet, authorEmail: String): SnippetSummaryDto =
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

    // mapper simple Execution.DiagnosticDto -> ApiDiagnostic
    fun toApiDiagnostics(list: List<DiagnosticDto>): List<ApiDiagnostic> =
        list.map { diagnostic ->
            ApiDiagnostic(diagnostic.ruleId, diagnostic.message, diagnostic.line, diagnostic.col)
        }
}
