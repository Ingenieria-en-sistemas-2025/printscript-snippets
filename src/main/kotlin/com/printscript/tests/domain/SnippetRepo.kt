package com.printscript.tests.domain

import com.printscript.tests.domain.model.Snippet
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface SnippetRepo: JpaRepository<Snippet, Long> {
    fun findByIdIn(ids: List<Long>, pageable: Pageable): Page<Snippet>
    fun existsByOwnerIdAndName(ownerId: String, name: String): Boolean
}