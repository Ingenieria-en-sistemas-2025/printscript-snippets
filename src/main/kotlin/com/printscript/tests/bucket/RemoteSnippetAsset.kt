package com.printscript.tests.bucket

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class RemoteSnippetAsset(
    private val rest: RestClient,
    @Value("\${asset.service.base-url}") private val baseUrl: String
) : SnippetAsset {

    override fun upload(container: String, key: String, bytes: ByteArray) {
        rest.put()
            .uri("$baseUrl/v1/asset/$container/$key")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(bytes)
            .retrieve()
            .toBodilessEntity()
    }

    override fun download(container: String, key: String): ByteArray =
        rest.get()
            .uri("$baseUrl/v1/asset/$container/$key")
            .retrieve()
            .body(ByteArray::class.java) ?: error("empty body")

    override fun delete(container: String, key: String) {
        rest.delete()
            .uri("$baseUrl/v1/asset/$container/$key")
            .retrieve()
            .toBodilessEntity()
    }
}