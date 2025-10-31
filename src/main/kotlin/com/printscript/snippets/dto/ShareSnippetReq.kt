package com.printscript.snippets.dto

// para pedir que Permissions le de acceso a otro user sobre un snippet
data class ShareSnippetReq(
    val snippetId: String,
    val userId: String,
    val permissionType: String,
)
