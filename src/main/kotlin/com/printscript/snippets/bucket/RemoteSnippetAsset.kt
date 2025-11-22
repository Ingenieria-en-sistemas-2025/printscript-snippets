package com.printscript.snippets.bucket

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Component
class RemoteSnippetAsset(
    @Qualifier("m2mRestTemplate") private val rest: RestTemplate, // el token se agrega solo
    @Value("\${asset.service.base-url:http://asset-service:8080}")
    private val baseUrl: String, // URL base del asset-service
) : SnippetAsset {

    private fun buildUrl(container: String, key: String): String {
        val encodedKey = java.net.URLEncoder.encode(key, Charsets.UTF_8) // encodea key segura
        return UriComponentsBuilder
            .fromUriString(baseUrl)
            .pathSegment("v1", "asset", container, encodedKey)
            .build(false)
            .toUriString() // genera URL final
    }

    override fun upload(container: String, key: String, bytes: ByteArray) {
        // headers SOLO para poner el content-type
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_OCTET_STREAM
        }

        // PUT con body binario
        rest.exchange(
            buildUrl(container, key),
            HttpMethod.PUT,
            HttpEntity(bytes, headers), // body + content-type
            Void::class.java,
        )
    }

    override fun download(container: String, key: String): ByteArray {
        // GET que recibe un byte[]
        return rest.exchange(
            buildUrl(container, key),
            HttpMethod.GET,
            null, // no se necesita body ni headers
            ByteArray::class.java,
        ).body ?: error("empty body")
    }

    override fun delete(container: String, key: String) {
        // DELETE sin headers (token lo agrega el interceptor)
        rest.exchange(
            buildUrl(container, key),
            HttpMethod.DELETE,
            null,
            Void::class.java,
        )
    }
}
