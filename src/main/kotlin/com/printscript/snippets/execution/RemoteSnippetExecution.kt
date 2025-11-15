package com.printscript.snippets.execution

import com.printscript.snippets.auth.Auth0TokenService
import io.printscript.contracts.formatter.FormatReq
import io.printscript.contracts.formatter.FormatRes
import io.printscript.contracts.linting.LintReq
import io.printscript.contracts.linting.LintRes
import io.printscript.contracts.parse.ParseReq
import io.printscript.contracts.parse.ParseRes
import io.printscript.contracts.run.RunReq
import io.printscript.contracts.run.RunRes
import io.printscript.contracts.tests.RunSingleTestReq
import io.printscript.contracts.tests.RunSingleTestRes
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class RemoteSnippetExecution(
    @param:Qualifier("restClient") private val rest: RestClient,
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

    override fun run(req: RunReq): RunRes =
        rest.post()
            .uri("$baseUrl/run")
            .headers(::auth)
            .body(req)
            .retrieve()
            .body(RunRes::class.java) ?: error("empty run")

    override fun runSingleTest(req: RunSingleTestReq): RunSingleTestRes =
        rest.post().uri("$baseUrl/run-test")
            .headers(::auth)
            .body(req)
            .retrieve()
            .body(RunSingleTestRes::class.java) ?: error("empty run-single-test")
}
