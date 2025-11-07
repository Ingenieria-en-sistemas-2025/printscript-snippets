package com.printscript.snippets.permission.dto

import java.util.UUID

data class PermissionSnippet(
    val id: UUID,
    val snippetId: UUID,
    val ownerId: String,
    val scope: String,
)
