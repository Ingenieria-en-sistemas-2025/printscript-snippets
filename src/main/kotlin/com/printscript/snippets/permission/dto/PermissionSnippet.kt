package com.printscript.snippets.permission.dto

data class PermissionSnippet(
    val id: String,
    val snippetId: String,
    val ownerId: String,
    val scope: String,
)
