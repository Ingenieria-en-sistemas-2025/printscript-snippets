package com.printscript.tests.domain

import com.printscript.tests.domain.model.SnippetVersion
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SnippetVersionRepo : JpaRepository<SnippetVersion, UUID> {
    // busca la ult version de un snippet osoea la de nro mas alto
    fun findTopBySnippetIdOrderByVersionNumberDesc(snippetId: UUID): SnippetVersion?
    fun findMaxVersionBySnippetId(snippetId: UUID): Long?
    fun findAllBySnippetId(snippetId: UUID): List<SnippetVersion>
    fun save(entity: SnippetVersion): SnippetVersion
}
