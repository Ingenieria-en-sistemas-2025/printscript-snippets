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
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class RemoteSnippetExecution(
    @Qualifier("m2mRestTemplate") private val rest: RestTemplate, // interceptor ya pone Authorization
    private val m2m: Auth0TokenService,
    @Value("\${execution.base-url}") private val baseUrl: String,
) : SnippetExecution {

    override fun parse(req: ParseReq): ParseRes {
        val entity = HttpEntity(req) // sin headers, los pone el interceptor

        val response = rest.exchange(
            "$baseUrl/parse",
            HttpMethod.POST,
            entity,
            ParseRes::class.java
        )

        return response.body ?: error("empty parse")
    }

    override fun lint(req: LintReq): LintRes {
        val entity = HttpEntity(req)

        val response = rest.exchange(
            "$baseUrl/lint",
            HttpMethod.POST,
            entity,
            LintRes::class.java
        )

        return response.body ?: error("empty lint")
    }

    override fun format(req: FormatReq): FormatRes {
        val entity = HttpEntity(req)

        val response = rest.exchange(
            "$baseUrl/format",
            HttpMethod.POST,
            entity,
            FormatRes::class.java
        )

        return response.body ?: error("empty format")
    }

    override fun run(req: RunReq): RunRes {
        val entity = HttpEntity(req)

        val response = rest.exchange(
            "$baseUrl/run",
            HttpMethod.POST,
            entity,
            RunRes::class.java
        )

        return response.body ?: error("empty run")
    }

    override fun runSingleTest(req: RunSingleTestReq): RunSingleTestRes {
        val entity = HttpEntity(req)

        val response = rest.exchange(
            "$baseUrl/run-test",
            HttpMethod.POST,
            entity,
            RunSingleTestRes::class.java
        )

        return response.body ?: error("empty run-single-test")
    }
}