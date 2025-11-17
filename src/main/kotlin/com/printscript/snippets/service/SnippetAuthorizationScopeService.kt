package com.printscript.snippets.service

import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.error.UnsupportedOperation
import com.printscript.snippets.permission.SnippetPermission
import java.util.UUID

class SnippetAuthorizationScopeService(
    private val permissionClient: SnippetPermission,
) {

    private fun String.toAccessLevel(): AccessLevel? = when (uppercase()) {
        "READER" -> AccessLevel.READER
        "EDITOR" -> AccessLevel.EDITOR
        "OWNER" -> AccessLevel.OWNER
        else -> null
    }

    private fun userExplicitScopeForSnippet(userId: String, snippetId: UUID): AccessLevel? =
        permissionClient.getUserScopeForSnippet(userId, snippetId.toString())?.toAccessLevel()

    fun requireScopeAtLeast(userId: String, snippet: Snippet, minRequired: AccessLevel) {
        if (snippet.ownerId == userId && minRequired <= AccessLevel.OWNER) {
            return
        }
        val maxScope = userExplicitScopeForSnippet(userId, snippet.id!!)
        if (maxScope == null || maxScope.ordinal < minRequired.ordinal) {
            throw UnsupportedOperation("Insufficient scope. Required: $minRequired")
        }
    }

    fun requireOwner(userId: String, snippet: Snippet) =
        requireScopeAtLeast(userId, snippet, AccessLevel.OWNER)

    fun requireEditorOrOwner(userId: String, snippet: Snippet) =
        requireScopeAtLeast(userId, snippet, AccessLevel.EDITOR)

    fun requireReaderOrAbove(userId: String, snippet: Snippet) =
        requireScopeAtLeast(userId, snippet, AccessLevel.READER)
}
