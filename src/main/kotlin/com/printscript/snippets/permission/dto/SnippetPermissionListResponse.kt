package com.printscript.snippets.permission.dto

data class SnippetPermissionListResponse(
    val permissions: List<PermissionSnippet>,
    val count: Long,
)
