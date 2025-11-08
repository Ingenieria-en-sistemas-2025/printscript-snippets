package com.printscript.snippets.permission

import com.printscript.snippets.permission.dto.PermissionCreateSnippetInput
import com.printscript.snippets.permission.dto.SnippetPermissionListResponse
import org.springframework.http.ResponseEntity

interface SnippetPermission {
    fun createAuthorization(input: PermissionCreateSnippetInput): ResponseEntity<String>
    fun getAuthorBySnippetId(snippetId: String): ResponseEntity<String>
    fun getAllSnippetsPermission(userId: String, pageNum: Int, pageSize: Int): ResponseEntity<SnippetPermissionListResponse>
    fun deleteSnippetPermissions(snippetId: String): ResponseEntity<Unit>
    fun getUserScopeForSnippet(userId: String, snippetId: String): String?
}
