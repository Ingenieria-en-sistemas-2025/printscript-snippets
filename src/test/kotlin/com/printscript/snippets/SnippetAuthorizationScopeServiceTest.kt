package com.printscript.snippets

import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.error.UnsupportedOperation
import com.printscript.snippets.permission.SnippetPermission
import com.printscript.snippets.service.SnippetAuthorizationScopeService
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class
SnippetAuthorizationScopeServiceTest {

    @Mock
    lateinit var permissionClient: SnippetPermission

    private fun snippet(id: UUID, owner: String): Snippet {
        val s = org.mockito.kotlin.mock<Snippet>()
        whenever(s.id).thenReturn(id)
        whenever(s.ownerId).thenReturn(owner)
        return s
    }

    @Test
    fun `requireOwner pasa si lo es`() {
        val id = UUID.randomUUID()
        val snip = snippet(id, "u1")
        val svc = SnippetAuthorizationScopeService(permissionClient)

        assertDoesNotThrow {
            svc.requireOwner("u1", snip)
        }
    }

    @Test
    fun `requireOwner lanza si user no es owner`() {
        val id = UUID.randomUUID()
        val snip = snippet(id, "u1")
        val svc = SnippetAuthorizationScopeService(permissionClient)

        assertThrows(UnsupportedOperation::class.java) {
            svc.requireOwner("other", snip)
        }
    }

    @Test
    fun `requireEditorOrOwner pasa si owner`() {
        val id = UUID.randomUUID()
        val snip = snippet(id, "u1")

        val svc = SnippetAuthorizationScopeService(permissionClient)
        assertDoesNotThrow { svc.requireEditorOrOwner("u1", snip) }
    }

    @Test
    fun `requireEditorOrOwner pasa si tiene scope EDITOR`() {
        val id = UUID.randomUUID()
        val snip = snippet(id, "u1")

        whenever(permissionClient.getUserScopeForSnippet("u2", id.toString()))
            .thenReturn("EDITOR")

        val svc = SnippetAuthorizationScopeService(permissionClient)

        assertDoesNotThrow {
            svc.requireEditorOrOwner("u2", snip)
        }
    }

    @Test
    fun `requireEditorOrOwner lanza si solo tiene READER`() {
        val id = UUID.randomUUID()
        val snip = snippet(id, "u1")

        whenever(permissionClient.getUserScopeForSnippet("u2", id.toString()))
            .thenReturn("READER")

        val svc = SnippetAuthorizationScopeService(permissionClient)

        assertThrows(UnsupportedOperation::class.java) {
            svc.requireEditorOrOwner("u2", snip)
        }
    }

    @Test
    fun `requireReaderOrAbove lanza si user no tiene ningun scope`() {
        val id = UUID.randomUUID()
        val snip = snippet(id, "u1")

        whenever(permissionClient.getUserScopeForSnippet("x", id.toString()))
            .thenReturn(null)

        val svc = SnippetAuthorizationScopeService(permissionClient)

        assertThrows(UnsupportedOperation::class.java) {
            svc.requireReaderOrAbove("x", snip)
        }
    }
}
