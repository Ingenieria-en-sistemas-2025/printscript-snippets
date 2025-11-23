package com.printscript.snippets

import com.printscript.snippets.execution.RemoteSnippetExecution
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

@ExtendWith(MockitoExtension::class)
class RemoteSnippetExecutionTest {

    private val rest: RestTemplate = mock()
    private val baseUrl = "http://exec-service:8080"

    private val client = RemoteSnippetExecution(rest, baseUrl)

    @Test
    fun `parse delega en restTemplate y devuelve body`() {
        val req: ParseReq = mock()
        val expected: ParseRes = mock()

        whenever(
            rest.exchange(
                eq("$baseUrl/parse"),
                eq(HttpMethod.POST),
                any<HttpEntity<ParseReq>>(),
                eq(ParseRes::class.java),
            ),
        ).thenReturn(ResponseEntity(expected, HttpStatus.OK))

        val res = client.parse(req)

        assertSame(expected, res)

        verify(rest).exchange(
            eq("$baseUrl/parse"),
            eq(HttpMethod.POST),
            any<HttpEntity<ParseReq>>(),
            eq(ParseRes::class.java),
        )
    }

    @Test
    fun `parse lanza error cuando body es null`() {
        val req: ParseReq = mock()

        whenever(
            rest.exchange(
                eq("$baseUrl/parse"),
                eq(HttpMethod.POST),
                any<HttpEntity<ParseReq>>(),
                eq(ParseRes::class.java),
            ),
        ).thenReturn(ResponseEntity<ParseRes>(null, HttpStatus.OK))

        val ex = assertThrows<IllegalStateException> {
            client.parse(req)
        }
        assertEquals("empty parse", ex.message)
    }

    @Test
    fun `lint delega en restTemplate y devuelve body`() {
        val req: LintReq = mock()
        val expected: LintRes = mock()

        whenever(
            rest.exchange(
                eq("$baseUrl/lint"),
                eq(HttpMethod.POST),
                any<HttpEntity<LintReq>>(),
                eq(LintRes::class.java),
            ),
        ).thenReturn(ResponseEntity(expected, HttpStatus.OK))

        val res = client.lint(req)

        assertSame(expected, res)

        verify(rest).exchange(
            eq("$baseUrl/lint"),
            eq(HttpMethod.POST),
            any<HttpEntity<LintReq>>(),
            eq(LintRes::class.java),
        )
    }

    @Test
    fun `lint lanza error cuando body es null`() {
        val req: LintReq = mock()

        whenever(
            rest.exchange(
                eq("$baseUrl/lint"),
                eq(HttpMethod.POST),
                any<HttpEntity<LintReq>>(),
                eq(LintRes::class.java),
            ),
        ).thenReturn(ResponseEntity<LintRes>(null, HttpStatus.OK))

        val ex = assertThrows<IllegalStateException> {
            client.lint(req)
        }
        assertEquals("empty lint", ex.message)
    }


    @Test
    fun `format delega en restTemplate y devuelve body`() {
        val req: FormatReq = mock()
        val expected: FormatRes = mock()

        whenever(
            rest.exchange(
                eq("$baseUrl/format"),
                eq(HttpMethod.POST),
                any<HttpEntity<FormatReq>>(),
                eq(FormatRes::class.java),
            ),
        ).thenReturn(ResponseEntity(expected, HttpStatus.OK))

        val res = client.format(req)

        assertSame(expected, res)

        verify(rest).exchange(
            eq("$baseUrl/format"),
            eq(HttpMethod.POST),
            any<HttpEntity<FormatReq>>(),
            eq(FormatRes::class.java),
        )
    }

    @Test
    fun `format lanza error cuando body es null`() {
        val req: FormatReq = mock()

        whenever(
            rest.exchange(
                eq("$baseUrl/format"),
                eq(HttpMethod.POST),
                any<HttpEntity<FormatReq>>(),
                eq(FormatRes::class.java),
            ),
        ).thenReturn(ResponseEntity<FormatRes>(null, HttpStatus.OK))

        val ex = assertThrows<IllegalStateException> {
            client.format(req)
        }
        assertEquals("empty format", ex.message)
    }


    @Test
    fun `run delega en restTemplate y devuelve body`() {
        val req: RunReq = mock()
        val expected: RunRes = mock()

        whenever(
            rest.exchange(
                eq("$baseUrl/run"),
                eq(HttpMethod.POST),
                any<HttpEntity<RunReq>>(),
                eq(RunRes::class.java),
            ),
        ).thenReturn(ResponseEntity(expected, HttpStatus.OK))

        val res = client.run(req)

        assertSame(expected, res)

        verify(rest).exchange(
            eq("$baseUrl/run"),
            eq(HttpMethod.POST),
            any<HttpEntity<RunReq>>(),
            eq(RunRes::class.java),
        )
    }

    @Test
    fun `run lanza error cuando body es null`() {
        val req: RunReq = mock()

        whenever(
            rest.exchange(
                eq("$baseUrl/run"),
                eq(HttpMethod.POST),
                any<HttpEntity<RunReq>>(),
                eq(RunRes::class.java),
            ),
        ).thenReturn(ResponseEntity<RunRes>(null, HttpStatus.OK))

        val ex = assertThrows<IllegalStateException> {
            client.run(req)
        }
        assertEquals("empty run", ex.message)
    }


    @Test
    fun `runSingleTest delega en restTemplate y devuelve body`() {
        val req: RunSingleTestReq = mock()
        val expected: RunSingleTestRes = mock()

        whenever(
            rest.exchange(
                eq("$baseUrl/run-test"),
                eq(HttpMethod.POST),
                any<HttpEntity<RunSingleTestReq>>(),
                eq(RunSingleTestRes::class.java),
            ),
        ).thenReturn(ResponseEntity(expected, HttpStatus.OK))

        val res = client.runSingleTest(req)

        assertSame(expected, res)

        verify(rest).exchange(
            eq("$baseUrl/run-test"),
            eq(HttpMethod.POST),
            any<HttpEntity<RunSingleTestReq>>(),
            eq(RunSingleTestRes::class.java),
        )
    }

    @Test
    fun `runSingleTest lanza error cuando body es null`() {
        val req: RunSingleTestReq = mock()

        whenever(
            rest.exchange(
                eq("$baseUrl/run-test"),
                eq(HttpMethod.POST),
                any<HttpEntity<RunSingleTestReq>>(),
                eq(RunSingleTestRes::class.java),
            ),
        ).thenReturn(ResponseEntity<RunSingleTestRes>(null, HttpStatus.OK))

        val ex = assertThrows<IllegalStateException> {
            client.runSingleTest(req)
        }
        assertEquals("empty run-single-test", ex.message)
    }
}