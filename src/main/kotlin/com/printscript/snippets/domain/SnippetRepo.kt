package com.printscript.snippets.domain

import com.printscript.snippets.domain.model.Snippet
import io.lettuce.core.dynamic.annotation.Param
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SnippetRepo : JpaRepository<Snippet, Long> {
    fun findByIdIn(ids: List<Long>, pageable: Pageable): Page<Snippet>
    fun existsByOwnerIdAndName(ownerId: String, name: String): Boolean

    @Query("select s.id from Snippet s")
    fun findAllIds(): List<Long>

    @Query("select s.language as language, s.languageVersion as languageVersion from Snippet s where s.id = :id")
    fun getLangAndVersion(@Param("id") id: Long): LangVerProjection
}
