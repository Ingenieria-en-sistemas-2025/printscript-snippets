package com.printscript.snippets.permission.dto

import java.util.UUID

data class PermissionCreateSnippetInput(
    val snippetId: UUID,
    val userId: String,
    val scope: String, // OWNER, READER o EDITOR
)
