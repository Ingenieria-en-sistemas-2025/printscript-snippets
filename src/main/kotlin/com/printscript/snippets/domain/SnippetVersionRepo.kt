package com.printscript.snippets.domain

import com.printscript.snippets.domain.model.SnippetVersion
import org.springframework.data.jpa.repository.JpaRepository

interface SnippetVersionRepo : JpaRepository<SnippetVersion, Long> {
    // busca la ult version de un snippet osoea la de nro mas alto
    fun findTopBySnippetIdOrderByVersionNumberDesc(snippetId: Long): SnippetVersion?
    fun findMaxVersionBySnippetId(snippetId: Long): Int?
    fun findAllBySnippetId(snippetId: Long): List<SnippetVersion>
    fun save(entity: SnippetVersion): SnippetVersion
}
