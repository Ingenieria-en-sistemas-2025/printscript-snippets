package com.printscript.snippets

import com.printscript.snippets.bucket.SnippetAsset
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.domain.model.SnippetVersion
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.redis.controllers.InternalReadController
import com.printscript.snippets.redis.dto.ContentDto
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class InternalReadControllerTest {

    @Mock
    lateinit var versionRepo: SnippetVersionRepo

    @Mock
    lateinit var asset: SnippetAsset

    private val controller: InternalReadController by lazy {
        InternalReadController(versionRepo, asset)
    }

    @Test
    fun `content devuelve el contenido si existe la version`() {
        // given
        val id = UUID.randomUUID()
        val version = SnippetVersion(
            id = UUID.randomUUID(),
            snippetId = id,
            versionNumber = 1,
            contentKey = "key123"
        )

        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(id))
            .thenReturn(version)

        whenever(asset.download("snippets", "key123"))
            .thenReturn("hello world".toByteArray())

        // when
        val dto: ContentDto = controller.content(id)

        // then
        assertEquals("hello world", dto.content)

        verify(versionRepo).findTopBySnippetIdOrderByVersionNumberDesc(id)
        verify(asset).download("snippets", "key123")
    }

    @Test
    fun `content lanza NotFound cuando no hay versiones`() {
        // given
        val id = UUID.randomUUID()

        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(id))
            .thenReturn(null)

        // then
        assertThrows(NotFound::class.java) {
            controller.content(id)
        }

        verify(versionRepo).findTopBySnippetIdOrderByVersionNumberDesc(id)
    }
}