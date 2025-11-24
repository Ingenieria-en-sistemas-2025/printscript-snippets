package com.printscript.snippets

import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.enums.LintStatus
import com.printscript.snippets.service.LintReevaluationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class LintReevaluationServiceTest {

    @Mock
    lateinit var snippetRepo: SnippetRepo

    @Mock
    lateinit var versionRepo: SnippetVersionRepo

    @Test
    fun `markOwnerSnippetsPending actualiza lint y compliance y devuelve ids`() {
        val ownerId = "auth0|user"
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()

        // el owner tiene 2 snippets
        whenever(snippetRepo.findAllIdsByOwner(ownerId)).thenReturn(listOf(id1, id2))

        // devolvemos mocks simples de Snippet (no stubeamos getters para evitar quilombos)
        val snip1: Snippet = mock()
        val snip2: Snippet = mock()
        whenever(snippetRepo.findById(id1)).thenReturn(Optional.of(snip1))
        whenever(snippetRepo.findById(id2)).thenReturn(Optional.of(snip2))

        val service = LintReevaluationService(snippetRepo, versionRepo)

        val result = service.markOwnerSnippetsPending(ownerId)

        // devuelve exactamente los ids que le dio el repo
        assertEquals(listOf(id1, id2), result)

        // actualiza el lint status de la última versión de cada snippet
        verify(versionRepo).updateLatestLintStatus(id1, LintStatus.PENDING)
        verify(versionRepo).updateLatestLintStatus(id2, LintStatus.PENDING)

        // guarda los snippets con compliance actualizado
        verify(snippetRepo).save(snip1)
        verify(snippetRepo).save(snip2)
    }

    @Test
    fun `markOwnerSnippetsPending con lista vacia no toca versiones ni snippets`() {
        val ownerId = "auth0|user"

        // el owner no tiene snippets
        whenever(snippetRepo.findAllIdsByOwner(ownerId)).thenReturn(emptyList())

        val service = LintReevaluationService(snippetRepo, versionRepo)

        val result = service.markOwnerSnippetsPending(ownerId)

        assertEquals(emptyList<UUID>(), result)

        // no debería llamar a versionRepo ni a findById/save
        verifyNoInteractions(versionRepo)
        verify(snippetRepo, never()).findById(any())
        verify(snippetRepo, never()).save(any())
    }
}