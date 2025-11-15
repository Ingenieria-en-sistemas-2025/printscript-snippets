package com.printscript.snippets.permission

import io.printscript.contracts.permissions.PermissionCreateSnippetInput
import io.printscript.contracts.permissions.SnippetPermissionListResponse
import org.springframework.http.ResponseEntity

interface SnippetPermission {
    fun createAuthorization(input: PermissionCreateSnippetInput): ResponseEntity<String>
    fun getAuthorBySnippetId(snippetId: String): ResponseEntity<String>
    fun getAllSnippetsPermission(userId: String, pageNum: Int, pageSize: Int): ResponseEntity<SnippetPermissionListResponse>
    fun deleteSnippetPermissions(snippetId: String): ResponseEntity<Unit>
    fun getUserScopeForSnippet(userId: String, snippetId: String): String?
}
