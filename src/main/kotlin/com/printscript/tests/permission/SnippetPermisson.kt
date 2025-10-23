package com.printscript.tests.permission

import com.printscript.tests.permission.dto.PermissionCreateSnippetInput
import com.printscript.tests.permission.dto.SnippetPermissionListResponse
import org.springframework.http.ResponseEntity

interface SnippetPermisson {
    fun createAuthorization(input: PermissionCreateSnippetInput, token: String): ResponseEntity<String>
    fun getAuthorBySnippetId(snippetId: String, token: String): ResponseEntity<String>
    fun getAllSnippetsPermission(userId: String, token: String, pageNum: Int, pageSize: Int): ResponseEntity<SnippetPermissionListResponse>
    fun deleteSnippetPermissions(snippetId: String, token: String): ResponseEntity<Unit>
}
