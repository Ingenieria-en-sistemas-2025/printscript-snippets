package com.printscript.snippets.redis.controllers

import com.printscript.snippets.execution.dto.DiagnosticDto
import com.printscript.snippets.redis.service.SnippetResultsService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/internal/snippets")
class InternalWriteController(private val results: SnippetResultsService) {
    @PostMapping("/{id}/format")
    fun saveFmt(@PathVariable id: UUID, @RequestBody body: Map<String, String>) {
        val formatted = body["content"] ?: throw IllegalArgumentException("content is required")
        results.saveFormatted(id, formatted)
    }

    @PostMapping("/{id}/lint")
    fun saveLint(@PathVariable id: UUID, @RequestBody v: List<DiagnosticDto>) {
        results.saveLint(id, v)
    }
}
