package com.printscript.snippets.permission

import com.printscript.snippets.permission.dto.PermissionCreateSnippetInput
import com.printscript.snippets.permission.dto.SnippetPermissionListResponse
import org.springframework.http.ResponseEntity
import java.util.UUID

interface SnippetPermission {
    fun createAuthorization(input: PermissionCreateSnippetInput, token: String): ResponseEntity<String>
    fun getAuthorBySnippetId(snippetId: UUID, token: String): ResponseEntity<String>
    fun getAllSnippetsPermission(userId: String, token: String, pageNum: Int, pageSize: Int): ResponseEntity<SnippetPermissionListResponse>
    fun deleteSnippetPermissions(snippetId: UUID, token: String): ResponseEntity<Unit>
}
