package com.printscript.snippets.domain

import com.printscript.snippets.domain.model.Snippet
import io.lettuce.core.dynamic.annotation.Param
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface SnippetRepo : JpaRepository<Snippet, UUID> {
    @Query("select s.id from Snippet s")
    fun findAllIds(): List<UUID>

    @Query(
        """
    select s.language as language, s.languageVersion as languageVersion
    from Snippet s
    where s.id = :id
""",
    )
    fun getLangAndVersion(@Param("id") id: UUID): LangVerProjection

    @Query("select s.id from Snippet s where s.ownerId = :ownerId")
    fun findAllIdsByOwner(ownerId: String): List<UUID>
}
