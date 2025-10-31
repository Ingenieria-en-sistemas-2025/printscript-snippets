package com.printscript.snippets.permission.dto

data class PermissionSnippet(
    val snippetId: String,
    val authorId: String,
    val permissionType: String, // "OWNER", "READER", "EDITOR"
)
