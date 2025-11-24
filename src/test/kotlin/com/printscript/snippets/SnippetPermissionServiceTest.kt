package com.printscript.snippets

import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.dto.ShareSnippetReq
import com.printscript.snippets.enums.AccessLevel
import com.printscript.snippets.enums.Compliance
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.error.UnsupportedOperation
import com.printscript.snippets.permission.SnippetPermission
import com.printscript.snippets.service.SnippetAuthorizationScopeService
import com.printscript.snippets.service.SnippetPermissionService
import io.printscript.contracts.permissions.PermissionCreateSnippetInput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class SnippetPermissionServiceTest {

    @Mock
    lateinit var snippetRepo: SnippetRepo

    @Mock
    lateinit var permissionClient: SnippetPermission

    // ==== SNIPPET REAL (NO MOCK!) ====
    private fun snippet(id: UUID, owner: String): Snippet =
        Snippet(
            id = id,
            ownerId = owner,
            name = "test",
            description = "d",
            language = "printscript",
            languageVersion = "1.1",
            currentVersionId = null,
            lastIsValid = true,
            lastLintCount = 0,
            compliance = Compliance.PENDING,
            createdAt = Instant.now(),
        )

    // =========================================================
    // shareSnippetOwnerAware
    // =========================================================

    @Test
    fun `shareSnippetOwnerAware ok crea permiso`() {
        val snippetId = UUID.randomUUID()
        val ownerId = "auth0|owner"

        val req = ShareSnippetReq(
            snippetId = snippetId.toString(),
            userId = "auth0|u2",
            permissionType = "EDITOR",
        )

        Mockito.`when`(snippetRepo.findById(snippetId))
            .thenReturn(Optional.of(snippet(snippetId, ownerId)))

        val service = SnippetPermissionService(snippetRepo, permissionClient)

        service.shareSnippetOwnerAware(ownerId, req)

        val captor = argumentCaptor<PermissionCreateSnippetInput>()
        verify(permissionClient).createAuthorization(captor.capture())

        val sent = captor.firstValue
        assertEquals(snippetId.toString(), sent.snippetId)
        assertEquals("auth0|u2", sent.userId)
        assertEquals("EDITOR", sent.scope)
    }

    @Test
    fun `shareSnippetOwnerAware lanza si snippet no existe`() {
        val req = ShareSnippetReq(
            snippetId = UUID.randomUUID().toString(),
            userId = "x",
        )

        Mockito.`when`(snippetRepo.findById(Mockito.any()))
            .thenReturn(Optional.empty())

        val service = SnippetPermissionService(snippetRepo, permissionClient)

        assertThrows(NotFound::class.java) {
            service.shareSnippetOwnerAware("owner", req)
        }
    }

    // =========================================================
    // checkPermissions
    // =========================================================

    @Test
    fun `checkPermissions llama requireReaderOrAbove`() {
        val id = UUID.randomUUID()
        val sn = snippet(id, "u1")

        Mockito.`when`(snippetRepo.findById(id)).thenReturn(Optional.of(sn))

        val service = SpykSvc(snippetRepo, permissionClient)

        service.checkPermissions("ux", id, AccessLevel.READER)

        verify(service.authorization).requireReaderOrAbove("ux", sn)
    }

    @Test
    fun `checkPermissions llama requireEditorOrOwner`() {
        val id = UUID.randomUUID()
        val sn = snippet(id, "u1")

        Mockito.`when`(snippetRepo.findById(id)).thenReturn(Optional.of(sn))

        val service = SpykSvc(snippetRepo, permissionClient)

        service.checkPermissions("ux", id, AccessLevel.EDITOR)

        verify(service.authorization).requireEditorOrOwner("ux", sn)
    }

    @Test
    fun `checkPermissions llama requireOwner`() {
        val id = UUID.randomUUID()
        val sn = snippet(id, "u1")

        Mockito.`when`(snippetRepo.findById(id)).thenReturn(Optional.of(sn))

        val service = SpykSvc(snippetRepo, permissionClient)

        service.checkPermissions("ux", id, AccessLevel.OWNER)

        verify(service.authorization).requireOwner("ux", sn)
    }

    class SpykSvc(
        repo: SnippetRepo,
        client: SnippetPermission,
    ) : SnippetPermissionService(repo, client) {

        val authorization: SnippetAuthorizationScopeService =
            Mockito.mock(SnippetAuthorizationScopeService::class.java)

        override fun checkPermissions(userId: String, snippetId: UUID, min: AccessLevel) {
            val sn = snippetRepo.findById(snippetId).orElseThrow()
            when (min) {
                AccessLevel.READER -> authorization.requireReaderOrAbove(userId, sn)
                AccessLevel.EDITOR -> authorization.requireEditorOrOwner(userId, sn)
                AccessLevel.OWNER -> authorization.requireOwner(userId, sn)
            }
        }
    }

    @Test
    fun `shareSnippetOwnerAware falla cuando no es owner`() {
        val snippetId = UUID.randomUUID()

        val sn = snippet(snippetId, "auth0|real-owner")

        Mockito.`when`(snippetRepo.findById(snippetId))
            .thenReturn(Optional.of(sn))

        val service = SnippetPermissionService(snippetRepo, permissionClient)

        val req = ShareSnippetReq(
            snippetId = snippetId.toString(),
            userId = "auth0|other",
            permissionType = "READER",
        )

        assertThrows(com.printscript.snippets.error.UnsupportedOperation::class.java) {
            service.shareSnippetOwnerAware("auth0|wrong-owner", req)
        }

        verify(permissionClient, Mockito.never())
            .createAuthorization(any())
    }

    @Test
    fun `checkPermissions lanza NotFound cuando el snippet no existe`() {
        val id = UUID.randomUUID()

        Mockito.`when`(snippetRepo.findById(id))
            .thenReturn(Optional.empty())

        val service = SnippetPermissionService(snippetRepo, permissionClient)

        assertThrows(NotFound::class.java) {
            service.checkPermissions("auth0|user", id, AccessLevel.READER)
        }
    }
}
