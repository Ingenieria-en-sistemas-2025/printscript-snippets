package com.printscript.snippets.dto

data class SnippetSummaryDto(
    val id: String,
    val name: String,
    val description: String?,
    val language: String,
    val version: String,
    val ownerId: String,
    val ownerEmail: String,
    val lastIsValid: Boolean,
    val lastLintCount: Int,
)
// GET /snippets -> PageDto<SnippetSummaryDto>
