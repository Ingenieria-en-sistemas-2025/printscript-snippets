package com.printscript.snippets.permission.dto

data class SnippetPermissionListResponse(
    val authorizations: List<PermissionSnippet>,
    val total: Int,
)
