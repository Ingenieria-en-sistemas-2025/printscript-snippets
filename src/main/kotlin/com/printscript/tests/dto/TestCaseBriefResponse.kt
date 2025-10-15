package com.printscript.tests.dto

data class TestCaseBriefResponse(
    val id: Long,
    val name: String,
    val lastRunStatus: String?,
    val lastRunAt: String?,
)
