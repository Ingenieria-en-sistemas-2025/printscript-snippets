package com.printscript.tests.domain

import com.printscript.tests.domain.model.TestCase
import org.springframework.data.jpa.repository.JpaRepository

interface TestCaseRepo: JpaRepository<TestCase, Long> {
    fun findBySnippetId(snippetId: Long): List<TestCase>
    fun existsBySnippetIdAndName(snippetId: Long, name: String): Boolean //ve si ya existe un test con ese nombre dentro del snippet
}