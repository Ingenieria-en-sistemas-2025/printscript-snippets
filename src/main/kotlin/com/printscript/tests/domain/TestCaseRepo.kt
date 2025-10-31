package com.printscript.tests.domain

import com.printscript.tests.domain.model.TestCase
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TestCaseRepo : JpaRepository<TestCase, UUID> {
    fun findAllBySnippetId(snippetId: UUID): List<TestCase>
    fun save(entity: TestCase): TestCase
    fun existsBySnippetIdAndName(snippetId: UUID, name: String): Boolean // ve si ya existe un test con ese nombre dentro del snippet
}
