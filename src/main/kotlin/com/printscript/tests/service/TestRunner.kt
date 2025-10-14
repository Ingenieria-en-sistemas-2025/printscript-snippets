package com.printscript.tests.service

import com.printscript.tests.dto.TestRunResponse

interface TestRunner {
    fun runSingle(testId: Long, userId: String): TestRunResponse
    fun runAllForSnippet(snippetId: Long, userId: String): List<TestRunResponse>
    fun getTestRunHistory(testId: Long, userId: String): List<TestRunResponse>
    fun getTestRun(runId: Long, userId: String): TestRunResponse
}