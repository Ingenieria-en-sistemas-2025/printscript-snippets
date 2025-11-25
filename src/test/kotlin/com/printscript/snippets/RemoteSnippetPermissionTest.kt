package com.printscript.snippets

import com.printscript.snippets.permission.RemoteSnippetPermission
import io.printscript.contracts.permissions.PermissionCreateSnippetInput
import io.printscript.contracts.permissions.PermissionSnippet
import io.printscript.contracts.permissions.SnippetPermissionListResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

class RemoteSnippetPermissionTest {

    @Mock
    lateinit var restTemplate: RestTemplate

    private lateinit var service: RemoteSnippetPermission

    private val baseUrl = "http://permissions-ms"

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        service = RemoteSnippetPermission(restTemplate, baseUrl)
    }

    @Test
    fun `createAuthorization devuelve OK`() {
        val input = PermissionCreateSnippetInput(
            snippetId = "s1",
            userId = "u1",
            scope = "writer",
        )

        val expected = ResponseEntity.ok("done")

        `when`(
            restTemplate.exchange(
                eq("$baseUrl/authorization"),
                eq(HttpMethod.POST),
                any(HttpEntity::class.java),
                eq(String::class.java),
            ),
        ).thenReturn(expected)

        val res = service.createAuthorization(input)

        assertEquals("done", res.body)
    }

    @Test
    fun `createAuthorization lanza excepcion y loggea`() {
        val input = PermissionCreateSnippetInput("s1", "u1", "reader")

        `when`(
            restTemplate.exchange(
                eq("$baseUrl/authorization"),
                eq(HttpMethod.POST),
                any(HttpEntity::class.java),
                eq(String::class.java),
            ),
        ).thenThrow(RestClientException("boom"))

        assertThrows<RestClientException> {
            service.createAuthorization(input)
        }
    }

    @Test
    fun `getAllSnippetsPermission devuelve lista`() {
        val snippet = PermissionSnippet("p1", "s1", "writer")
        val list = SnippetPermissionListResponse(listOf(snippet), 1)

        `when`(
            restTemplate.exchange(
                eq("$baseUrl/authorization/me?userId=u1&pageNum=0&pageSize=5"),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                eq(SnippetPermissionListResponse::class.java),
            ),
        ).thenReturn(ResponseEntity.ok(list))

        val res = service.getAllSnippetsPermission("u1", 0, 5)

        assertEquals(1, res.body!!.total)
        assertEquals("writer", res.body!!.authorizations.first().scope)
    }

    @Test
    fun `getAllSnippetsPermission lanza error`() {
        `when`(
            restTemplate.exchange(
                any(String::class.java),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                eq(SnippetPermissionListResponse::class.java),
            ),
        ).thenThrow(RestClientException("boom"))

        assertThrows<RestClientException> {
            service.getAllSnippetsPermission("u1", 0, 10)
        }
    }

    @Test
    fun `deleteSnippetPermissions devuelve OK`() {
        `when`(
            restTemplate.exchange(
                eq("$baseUrl/authorization/snippet/s1"),
                eq(HttpMethod.DELETE),
                eq(HttpEntity.EMPTY),
                eq(Void::class.java),
            ),
        ).thenReturn(ResponseEntity(HttpStatus.NO_CONTENT))

        val res = service.deleteSnippetPermissions("s1")
        assertEquals(200, res.statusCode.value())
    }

    @Test
    fun `deleteSnippetPermissions excepcion`() {
        `when`(
            restTemplate.exchange(
                eq("$baseUrl/authorization/snippet/s1"),
                eq(HttpMethod.DELETE),
                eq(HttpEntity.EMPTY),
                eq(Void::class.java),
            ),
        ).thenThrow(RestClientException("fail"))

        assertThrows<RestClientException> {
            service.deleteSnippetPermissions("s1")
        }
    }

    @Test
    fun `getUserScopeForSnippet devuelve scope si existe`() {
        val list = SnippetPermissionListResponse(
            listOf(
                PermissionSnippet("1", "ABC", "writer"),
                PermissionSnippet("2", "DEF", "reader"),
            ),
            2,
        )

        `when`(
            restTemplate.exchange(
                eq("$baseUrl/authorization/me?userId=u1&pageNum=0&pageSize=${Int.MAX_VALUE}"),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                eq(SnippetPermissionListResponse::class.java),
            ),
        ).thenReturn(ResponseEntity.ok(list))

        val scope = service.getUserScopeForSnippet("u1", "def")

        assertEquals("reader", scope)
    }

    @Test
    fun `getUserScopeForSnippet devuelve null si no existe`() {
        val list = SnippetPermissionListResponse(emptyList(), 0)

        `when`(
            restTemplate.exchange(
                any(String::class.java),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                eq(SnippetPermissionListResponse::class.java),
            ),
        ).thenReturn(ResponseEntity.ok(list))

        val scope = service.getUserScopeForSnippet("u1", "zzz")

        assertNull(scope)
    }

    @Test
    fun `getUserScopeForSnippet lanza error`() {
        `when`(
            restTemplate.exchange(
                any(String::class.java),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                eq(SnippetPermissionListResponse::class.java),
            ),
        ).thenThrow(RestClientException("boom"))

        assertThrows<RestClientException> {
            service.getUserScopeForSnippet("u1", "abc")
        }
    }
}
