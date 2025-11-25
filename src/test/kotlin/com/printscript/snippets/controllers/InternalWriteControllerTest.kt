package com.printscript.snippets.controllers

import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.redis.controllers.InternalWriteController
import com.printscript.snippets.service.rules.SnippetRuleDomainService
import io.printscript.contracts.DiagnosticDto
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InternalWriteControllerTest {

    @Mock lateinit var results: SnippetRuleDomainService

    @Mock lateinit var versionRepo: SnippetVersionRepo

    @Mock lateinit var snippetRepo: SnippetRepo

    private fun controller() =
        InternalWriteController(results)

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
}
