package com.printscript.snippets.execution

import com.printscript.snippets.auth.Auth0TokenService
import com.printscript.snippets.execution.dto.FormatReq
import com.printscript.snippets.execution.dto.FormatRes
import com.printscript.snippets.execution.dto.LintReq
import com.printscript.snippets.execution.dto.LintRes
import com.printscript.snippets.execution.dto.ParseReq
import com.printscript.snippets.execution.dto.ParseRes
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class RemoteSnippetExecution(
    private val rest: RestClient,
    private val m2m: Auth0TokenService,
    @Value("\${execution.base-url}") private val baseUrl: String,
) : SnippetExecution {

    private fun auth(h: HttpHeaders) {
        h.set(HttpHeaders.AUTHORIZATION, "Bearer ${m2m.getAccessToken()}")
        h.contentType = MediaType.APPLICATION_JSON
        h.accept = listOf(MediaType.APPLICATION_JSON)
    }

    override fun parse(req: ParseReq): ParseRes =
        rest.post().uri("$baseUrl/parse").headers(::auth).body(req).retrieve()
            .body(ParseRes::class.java) ?: error("empty parse")

    override fun lint(req: LintReq): LintRes =
        rest.post().uri("$baseUrl/lint").headers(::auth).body(req).retrieve()
            .body(LintRes::class.java) ?: error("empty lint")

    override fun format(req: FormatReq): FormatRes =
        rest.post().uri("$baseUrl/format").headers(::auth).body(req).retrieve()
            .body(FormatRes::class.java) ?: error("empty format")
}
