package com.printscript.snippets.permission.dto

data class PermissionCreateSnippetInput(
    val snippetId: String,
    val userId: String,
    val permissionType: String, // "OWNER", "READER", o "EDITOR"
)
