package com.printscript.tests.permission.dto

data class SnippetPermissionListResponse(
    val permissions: List<PermissionSnippet>,
    val count: Long,
)
