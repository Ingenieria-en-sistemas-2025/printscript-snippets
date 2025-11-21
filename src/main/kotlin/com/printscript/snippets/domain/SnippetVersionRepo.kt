package com.printscript.snippets.domain

import com.printscript.snippets.domain.model.SnippetVersion
import com.printscript.snippets.enums.LintStatus
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(
        """
        update SnippetVersion sv
           set sv.lintStatus = :status
         where sv.snippetId = :snippetId
           and sv.versionNumber = (
             select max(sv2.versionNumber)
               from SnippetVersion sv2
              where sv2.snippetId = :snippetId
           )
        """,
    )
    fun updateLatestLintStatus(snippetId: UUID, status: LintStatus): Int
}
