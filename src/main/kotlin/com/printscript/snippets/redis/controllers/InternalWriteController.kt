package com.printscript.snippets.redis.controllers

import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.domain.model.enums.Compliance
import com.printscript.snippets.domain.model.enums.LintStatus
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.service.rules.SnippetRuleDomainService
import io.printscript.contracts.DiagnosticDto
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/internal/snippets")
class InternalWriteController(private val results: SnippetRuleDomainService, private val versionRepo: SnippetVersionRepo, private val snippetRepo: SnippetRepo) {

    @PostMapping("/{id}/format")
    fun saveFmt(@PathVariable id: UUID, @RequestBody body: Map<String, String>) {
        val formatted = body["content"] ?: throw IllegalArgumentException("content is required")
        results.saveFormatted(id, formatted)
    }

    @PostMapping("/{id}/lint")
    fun saveLint(@PathVariable id: UUID, @RequestBody v: List<DiagnosticDto>) {
        results.saveLint(id, v)
    }

    @PostMapping("/{id}/lint-failed")
    fun markLintFailed(@PathVariable id: UUID) {
        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(id)
            ?: throw NotFound("Snippet $id has no versions")

        latest.lintStatus = LintStatus.FAILED
        versionRepo.save(latest)
        val s = snippetRepo.findById(id).orElseThrow { NotFound("Snippet not found") }
        s.compliance = Compliance.FAILED
        snippetRepo.save(s)
    }
}
