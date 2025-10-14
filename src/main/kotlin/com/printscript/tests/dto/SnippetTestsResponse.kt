package com.printscript.tests.dto

data class SnippetTestsResponse(
    val snippetId: Long,
    val tests: List<TestCaseBriefResponse>,
    val total: Int,
    val passed: Int,
    val failed: Int,
    val error: Int
)