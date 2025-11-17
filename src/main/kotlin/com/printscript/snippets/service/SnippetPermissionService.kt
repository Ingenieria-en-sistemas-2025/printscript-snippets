package com.printscript.snippets.service

import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.dto.ShareSnippetReq
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.permission.SnippetPermission
import io.printscript.contracts.permissions.PermissionCreateSnippetInput
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SnippetPermissionService(
    private val snippetRepo: SnippetRepo,
    private val permissionClient: SnippetPermission,
) {
    private val authorization = SnippetAuthorizationScopeService(permissionClient)

    @Transactional
    fun shareSnippetOwnerAware(ownerId: String, req: ShareSnippetReq) {
        val snippet = snippetRepo.findById(UUID.fromString(req.snippetId))
            .orElseThrow { NotFound("Snippet not found") }
        authorization.requireOwner(ownerId, snippet)

        permissionClient.createAuthorization(
            PermissionCreateSnippetInput(
                snippetId = req.snippetId,
                userId = req.userId,
                scope = req.permissionType,
            ),
        )
    }

    fun checkPermissions(
        userId: String,
        snippetId: UUID,
        min: AccessLevel,
    ) {
        val snippet = snippetRepo.findById(snippetId)
            .orElseThrow { NotFound("Snippet not found") }

        when (min) {
            AccessLevel.READER -> authorization.requireReaderOrAbove(userId, snippet)
            AccessLevel.EDITOR -> authorization.requireEditorOrOwner(userId, snippet)
            AccessLevel.OWNER -> authorization.requireOwner(userId, snippet)
        }
    }
}
