package com.printscript.snippets.bucket

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

@Component
class RemoteSnippetAsset(
    @Qualifier("plainRestClient") private val rest: RestClient,
    @Value("\${asset.service.base-url:http://asset-service:8080}")
    private val baseUrl: String,
) : SnippetAsset {

    private fun buildUrl(container: String, key: String): String {
        val url = UriComponentsBuilder
            .fromUriString(baseUrl)
            .pathSegment("v1", "asset", container)
            .path("/$key")
            .build(false)
            .toUriString()
        return url
    }

    @PutMapping("/v1/asset/{container}/{key:.+}")
    override fun upload(
        @PathVariable container: String,
        @PathVariable("key") key: String,
        @RequestBody body: ByteArray,
    ) {
        rest.put()
            .uri(buildUrl(container, key))
            .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
            .body(body)
            .retrieve()
            .toBodilessEntity()
    }

    @GetMapping("/v1/asset/{container}/{key:.+}")
    override fun download(
        @PathVariable container: String,
        @PathVariable("key") key: String,
    ): ByteArray =
        rest.get()
            .uri(buildUrl(container, key))
            .retrieve()
            .body(ByteArray::class.java) ?: error("empty body")

    @DeleteMapping("/v1/asset/{container}/{key:.+}")
    override fun delete(
        @PathVariable container: String,
        @PathVariable("key") key: String,
    ) {
        rest.delete()
            .uri(buildUrl(container, key))
            .retrieve()
            .toBodilessEntity()
    }
}
