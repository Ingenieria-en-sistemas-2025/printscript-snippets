package com.printscript.tests.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TestRunRepository : JpaRepository<TestRunEntity, Long> {
    fun findByTestId(testId: Long): List<TestRunEntity>
}
