package com.printscript.snippets.redis.controllers

import com.printscript.snippets.bucket.SnippetAsset
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.redis.dto.ContentDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/internal/snippets")
class InternalReadController(private val versionRepo: SnippetVersionRepo, private val asset: SnippetAsset) {
    private val container = "snippets"

    @GetMapping("/{id}/content")
    fun content(@PathVariable id: UUID): ContentDto {
        val latest = versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(id)
            ?: throw NotFound("Snippet $id has no versions")
        val bytes = asset.download(container, latest.contentKey)
        return ContentDto(bytes.toString(Charsets.UTF_8))
    }
}
