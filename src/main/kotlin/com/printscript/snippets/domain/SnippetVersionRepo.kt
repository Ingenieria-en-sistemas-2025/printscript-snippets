package com.printscript.snippets.domain

import com.printscript.snippets.domain.model.SnippetVersion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface SnippetVersionRepo : JpaRepository<SnippetVersion, UUID> {
    // busca la ult version de un snippet osoea la de nro mas alto
    fun findTopBySnippetIdOrderByVersionNumberDesc(snippetId: UUID): SnippetVersion?

    @Query(
        """
        select max(sv.versionNumber)
        from SnippetVersion sv
        where sv.snippetId = :snippetId
    """,
    )
    fun findMaxVersionBySnippetId(snippetId: UUID): Long?
    fun findAllBySnippetId(snippetId: UUID): List<SnippetVersion>
    fun save(entity: SnippetVersion): SnippetVersion
}
