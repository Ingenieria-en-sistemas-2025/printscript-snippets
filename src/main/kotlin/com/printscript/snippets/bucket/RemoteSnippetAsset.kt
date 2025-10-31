package com.printscript.snippets.bucket

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class RemoteSnippetAsset(
    @Qualifier("plainRestClient") private val rest: RestClient,
    @Value("\${asset.service.base-url}") private val baseUrl: String,
) : SnippetAsset {

    private fun buildUrl(container: String, key: String): String {
        val encContainer = java.net.URLEncoder.encode(container, Charsets.UTF_8)
        val encKey = java.net.URLEncoder.encode(key, Charsets.UTF_8) // incluye ownerId y '/'
        return "$baseUrl/v1/asset/$encContainer/$encKey"
    }

    override fun upload(container: String, key: String, bytes: ByteArray) {
        rest.put()
            .uri(buildUrl(container, key))
            .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
            .body(bytes)
            .retrieve()
            .toBodilessEntity()
    }

    override fun download(container: String, key: String): ByteArray =
        rest.get()
            .uri(buildUrl(container, key))
            .retrieve()
            .body(ByteArray::class.java) ?: error("empty body")

    override fun delete(container: String, key: String) {
        rest.delete()
            .uri(buildUrl(container, key))
            .retrieve()
            .toBodilessEntity()
    }
}
