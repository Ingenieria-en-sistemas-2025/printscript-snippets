package com.printscript.tests.dto

data class SnippetDetailDto(
    val id: Long,
    val name: String,
    val description: String?,
    val language: String,
    val version: String,
    val ownerId: String,
    val content: String?,
    val isValid: Boolean,
    val lintCount: Int
)

//GET /snippets/{id} -> SnippetDetailDto