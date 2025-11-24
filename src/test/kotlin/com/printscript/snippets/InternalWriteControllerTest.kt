package com.printscript.snippets

import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.domain.model.SnippetVersion
import com.printscript.snippets.enums.Compliance
import com.printscript.snippets.enums.LintStatus
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.redis.controllers.InternalWriteController
import com.printscript.snippets.service.rules.SnippetRuleDomainService
import io.printscript.contracts.DiagnosticDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InternalWriteControllerTest {

    @Mock lateinit var results: SnippetRuleDomainService

    @Mock lateinit var versionRepo: SnippetVersionRepo

    @Mock lateinit var snippetRepo: SnippetRepo

    private fun controller() =
        InternalWriteController(results, versionRepo, snippetRepo)

    // =========================================================================
    // saveFmt
    // =========================================================================
    @Test
    fun `saveFmt llama a saveFormatted con el contenido correcto`() {
        val id = UUID.randomUUID()

        controller().saveFmt(id, mapOf("content" to "formatted-content"))

        verify(results).saveFormatted(id, "formatted-content")
    }

    @Test
    fun `saveFmt lanza IllegalArgumentException si falta content`() {
        val id = UUID.randomUUID()

        assertThrows(IllegalArgumentException::class.java) {
            controller().saveFmt(id, emptyMap())
        }
    }

    // =========================================================================
    // saveLint
    // =========================================================================
    @Test
    fun `saveLint llama a saveLint con los diagnostics`() {
        val id = UUID.randomUUID()
        val diags = listOf(
            DiagnosticDto("R1", "msg", 1, 2),
        )

        controller().saveLint(id, diags)

        verify(results).saveLint(id, diags)
    }

    // =========================================================================
    // markLintFailed - caso feliz
    // =========================================================================
    @Test
    fun `markLintFailed actualiza lintStatus y compliance y guarda ambos`() {
        val id = UUID.randomUUID()

        val version = SnippetVersion(
            id = UUID.randomUUID(),
            snippetId = id,
            versionNumber = 1,
            contentKey = "key",
        )

        val snippet = Snippet(
            id = id,
            ownerId = "auth0|x",
            name = "s",
            description = null,
            language = "printscript",
            languageVersion = "1.1",
        )

        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(id))
            .thenReturn(version)

        whenever(snippetRepo.findById(id))
            .thenReturn(Optional.of(snippet))

        controller().markLintFailed(id)

        assertEquals(LintStatus.FAILED, version.lintStatus)
        assertEquals(Compliance.FAILED, snippet.compliance)

        verify(versionRepo).save(version)
        verify(snippetRepo).save(snippet)
    }

    // =========================================================================
    // markLintFailed - sin versiones
    // =========================================================================
    @Test
    fun `markLintFailed lanza NotFound si no hay versiones`() {
        val id = UUID.randomUUID()

        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(id))
            .thenReturn(null)

        assertThrows(NotFound::class.java) {
            controller().markLintFailed(id)
        }
    }

    // =========================================================================
    // markLintFailed - snippet inexistente
    // =========================================================================
    @Test
    fun `markLintFailed lanza NotFound si el snippet no existe`() {
        val id = UUID.randomUUID()

        val version = SnippetVersion(
            id = UUID.randomUUID(),
            snippetId = id,
            versionNumber = 1,
            contentKey = "key",
        )

        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(id))
            .thenReturn(version)

        whenever(snippetRepo.findById(id))
            .thenReturn(Optional.empty())

        assertThrows(NotFound::class.java) {
            controller().markLintFailed(id)
        }
    }
}
