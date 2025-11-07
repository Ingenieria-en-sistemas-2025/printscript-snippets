package com.printscript.snippets.permission.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class PermissionCreateSnippetInput(
    @field:NotBlank(message = "Snippet ID cannot be blank")
    val snippetId: String,

    @field:NotBlank(message = "User ID cannot be blank")
    val userId: String,

    @field:NotBlank(message = "Scope cannot be blank")
    @field:Pattern(
        regexp = "^(OWNER|READER|EDITOR)$",
        message = "Scope must be OWNER, READER, or EDITOR",
    )
    val scope: String,
)
