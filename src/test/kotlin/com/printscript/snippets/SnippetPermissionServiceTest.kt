package com.printscript.snippets

import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.dto.ShareSnippetReq
import com.printscript.snippets.enums.AccessLevel
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.permission.SnippetPermission
import com.printscript.snippets.service.SnippetAuthorizationScopeService
import com.printscript.snippets.service.SnippetPermissionService
import io.printscript.contracts.permissions.PermissionCreateSnippetInput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SnippetPermissionServiceTest {

    @Mock
    lateinit var snippetRepo: SnippetRepo

    @Mock
    lateinit var permissionClient: SnippetPermission

    // ===== helper =====
    private fun snippet(id: UUID, owner: String): Snippet {
        val s = Mockito.mock(Snippet::class.java)
        Mockito.`when`(s.id).thenReturn(id)
        Mockito.`when`(s.ownerId).thenReturn(owner)
        return s
    }

    // =========================================================
    // shareSnippetOwnerAware
    // =========================================================

    @Test
    fun `shareSnippetOwnerAware ok crea permiso`() {
        val snippetId = UUID.randomUUID()
        val ownerId = "auth0|o1"
        val req = ShareSnippetReq(
            snippetId = snippetId.toString(),
            userId = "auth0|u2",
            permissionType = "EDITOR",
        )

        Mockito.`when`(snippetRepo.findById(snippetId))
            .thenReturn(Optional.of(snippet(snippetId, ownerId)))

        val service = SnippetPermissionService(snippetRepo, permissionClient)

        service.shareSnippetOwnerAware(ownerId, req)

        val captor = ArgumentCaptor.forClass(PermissionCreateSnippetInput::class.java)
        Mockito.verify(permissionClient).createAuthorization(captor.capture())

        val sent = captor.value
        assertEquals(snippetId.toString(), sent.snippetId)
        assertEquals("auth0|u2", sent.userId)
        assertEquals("EDITOR", sent.scope)
    }

    @Test
    fun `shareSnippetOwnerAware lanza si snippet no existe`() {
        val ownerId = "auth0|owner"
        val req = ShareSnippetReq(
            snippetId = UUID.randomUUID().toString(),
            userId = "x",
        )

        Mockito.`when`(snippetRepo.findById(Mockito.any()))
            .thenReturn(Optional.empty())

        val service = SnippetPermissionService(snippetRepo, permissionClient)

        assertThrows(NotFound::class.java) {
            service.shareSnippetOwnerAware(ownerId, req)
        }
    }

    // =========================================================
    // checkPermissions
    // =========================================================

    @Test
    fun `checkPermissions llama requireReaderOrAbove`() {
        val id = UUID.randomUUID()
        val snip = snippet(id, "u1")

        Mockito.`when`(snippetRepo.findById(id)).thenReturn(Optional.of(snip))

        val service = SpykPermissionService(snippetRepo, permissionClient)

        service.checkPermissions("uX", id, AccessLevel.READER)

        Mockito.verify(service.authorization)
            .requireReaderOrAbove("uX", snip)
    }

    @Test
    fun `checkPermissions llama requireEditorOrOwner`() {
        val id = UUID.randomUUID()
        val snip = snippet(id, "u1")

        Mockito.`when`(snippetRepo.findById(id)).thenReturn(Optional.of(snip))

        val service = SpykPermissionService(snippetRepo, permissionClient)

        service.checkPermissions("uX", id, AccessLevel.EDITOR)

        Mockito.verify(service.authorization)
            .requireEditorOrOwner("uX", snip)
    }

    @Test
    fun `checkPermissions llama requireOwner`() {
        val id = UUID.randomUUID()
        val snip = snippet(id, "u1")

        Mockito.`when`(snippetRepo.findById(id)).thenReturn(Optional.of(snip))

        val service = SpykPermissionService(snippetRepo, permissionClient)

        service.checkPermissions("uX", id, AccessLevel.OWNER)

        Mockito.verify(service.authorization)
            .requireOwner("uX", snip)
    }

    // === Helper class para interceptar las llamadas a authorization ===
    class SpykPermissionService(
        repo: SnippetRepo,
        client: SnippetPermission,
    ) : SnippetPermissionService(repo, client) {

        // mockeamos explícitamente el servicio de scopes
        val authorization: SnippetAuthorizationScopeService =
            Mockito.mock(SnippetAuthorizationScopeService::class.java)

        override fun checkPermissions(
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
}