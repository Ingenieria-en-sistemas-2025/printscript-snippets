package com.printscript.snippets.service

import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.enums.Compliance
import com.printscript.snippets.enums.LintStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LintReevaluationService(
    private val snippetRepo: SnippetRepo,
    private val versionRepo: SnippetVersionRepo,
) {

    fun markOwnerSnippetsPending(ownerId: String): List<UUID> {
        val ids: List<UUID> = snippetRepo.findAllIdsByOwner(ownerId)

        ids.forEach { id ->
            versionRepo.updateLatestLintStatus(id, LintStatus.PENDING)
            val snippet = snippetRepo.findById(id).orElseThrow()
            snippet.compliance = Compliance.PENDING
            snippetRepo.save(snippet)
        }

        return ids
    }
}
