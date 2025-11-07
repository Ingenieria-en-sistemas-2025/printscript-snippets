package com.printscript.snippets.permission

import com.printscript.snippets.permission.dto.PermissionCreateSnippetInput
import com.printscript.snippets.permission.dto.SnippetPermissionListResponse
import org.springframework.http.ResponseEntity

interface SnippetPermission {
    fun createAuthorization(input: PermissionCreateSnippetInput, token: String): ResponseEntity<String>
    fun getAuthorBySnippetId(snippetId: String, token: String): ResponseEntity<String>
    fun getAllSnippetsPermission(userId: String, token: String, pageNum: Int, pageSize: Int): ResponseEntity<SnippetPermissionListResponse>
    fun deleteSnippetPermissions(snippetId: String, token: String): ResponseEntity<Unit>
}
