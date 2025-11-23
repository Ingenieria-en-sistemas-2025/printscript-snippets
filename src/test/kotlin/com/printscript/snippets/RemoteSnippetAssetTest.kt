package com.printscript.snippets

import com.printscript.snippets.bucket.RemoteSnippetAsset
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.http.*
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class RemoteSnippetAssetTest {

    private val rest: RestTemplate = Mockito.mock(RestTemplate::class.java)
    private val baseUrl = "http://asset-service:8080"
    private val client = RemoteSnippetAsset(rest, baseUrl)

    @Test
    fun `upload hace PUT al asset-service con content-type binario`() {
        val container = "snippets"
        val key = "owner/snippet-1/v1.ps"
        val bytes = "code".toByteArray(StandardCharsets.UTF_8)

        @Suppress("UNCHECKED_CAST")
        val entityCaptor =
            ArgumentCaptor.forClass(HttpEntity::class.java) as ArgumentCaptor<HttpEntity<ByteArray>>

        client.upload(container, key, bytes)

        val expectedUrl =
            "$baseUrl/v1/asset/$container/${URLEncoder.encode(key, StandardCharsets.UTF_8)}"

        Mockito.verify(rest).exchange(
            Mockito.eq(expectedUrl),
            Mockito.eq(HttpMethod.PUT),
            entityCaptor.capture(),
            Mockito.eq(Void::class.java),
        )

        val sentEntity = entityCaptor.value
        assertArrayEquals(bytes, sentEntity.body)
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, sentEntity.headers.contentType)
    }

    @Test
    fun `download hace GET y devuelve el body`() {
        val container = "snippets"
        val key = "owner/snippet-2/v1.ps"
        val bytes = "hello".toByteArray(StandardCharsets.UTF_8)

        val expectedUrl =
            "$baseUrl/v1/asset/$container/${URLEncoder.encode(key, StandardCharsets.UTF_8)}"

        Mockito.`when`(
            rest.exchange(
                Mockito.eq(expectedUrl),
                Mockito.eq(HttpMethod.GET),
                Mockito.isNull(),
                Mockito.eq(ByteArray::class.java),
            ),
        ).thenReturn(ResponseEntity.ok(bytes))

        val result = client.download(container, key)

        assertArrayEquals(bytes, result)
    }

    @Test
    fun `delete hace DELETE al asset-service`() {
        val container = "snippets"
        val key = "owner/snippet-3/v1.ps"

        val expectedUrl =
            "$baseUrl/v1/asset/$container/${URLEncoder.encode(key, StandardCharsets.UTF_8)}"

        client.delete(container, key)

        Mockito.verify(rest).exchange(
            Mockito.eq(expectedUrl),
            Mockito.eq(HttpMethod.DELETE),
            Mockito.isNull(),
            Mockito.eq(Void::class.java),
        )
    }
}