package com.printscript.tests.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TestCaseRepository : JpaRepository<TestCaseEntity, Long> {
    fun findBySnippetId(snippetId: Long): List<TestCaseEntity>
}
