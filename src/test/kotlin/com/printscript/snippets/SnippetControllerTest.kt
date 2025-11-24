package com.printscript.snippets

import com.printscript.snippets.controller.SnippetController
import com.printscript.snippets.dto.CreateSnippetReq
import com.printscript.snippets.dto.CreateTestReq
import com.printscript.snippets.dto.FileTypeDto
import com.printscript.snippets.dto.PageDto
import com.printscript.snippets.dto.RunSnippetInputsReq
import com.printscript.snippets.dto.ShareSnippetReq
import com.printscript.snippets.dto.SingleTestRunResult
import com.printscript.snippets.dto.SnippetDetailDto
import com.printscript.snippets.dto.SnippetSummaryDto
import com.printscript.snippets.dto.TestCaseDto
import com.printscript.snippets.dto.UpdateSnippetReq
import com.printscript.snippets.enums.AccessLevel
import com.printscript.snippets.redis.service.BulkRulesService
import com.printscript.snippets.service.SnippetDetailService
import com.printscript.snippets.service.SnippetPermissionService
import com.printscript.snippets.service.SnippetTestService
import com.printscript.snippets.service.SnippetsExecuteService
import com.printscript.snippets.service.rules.RulesStateService
import com.printscript.snippets.service.rules.SnippetRuleDomainService
import io.printscript.contracts.run.RunRes
import org.junit.Assert.assertArrayEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertSame

@ExtendWith(MockitoExtension::class)
class SnippetControllerTest {

    @Mock
    lateinit var snippetDetailService: SnippetDetailService

    @Mock
    lateinit var snippetsExecuteService: SnippetsExecuteService

    @Mock
    lateinit var snippetTestService: SnippetTestService

    @Mock
    lateinit var snippetPermissionService: SnippetPermissionService

    @Mock
    lateinit var bulkRulesService: BulkRulesService

    @Mock
    lateinit var rulesStateService: RulesStateService

    @Mock
    lateinit var snippetRuleDomainService: SnippetRuleDomainService

    @InjectMocks
    lateinit var controller: SnippetController

    // === helper para crear un JwtAuthenticationToken falso ===
    private fun jwt(userId: String = "auth0|user-123"): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("token-value")
            .header("alg", "none")
            .claim("sub", userId)
            .build()

        return JwtAuthenticationToken(jwt, emptyList(), userId)
    }

    @Test
    fun `listAccessible delega al service con el user correcto`() {
        val principal = jwt("auth0|abc")
        val pageMock = mock(PageDto::class.java) as PageDto<SnippetSummaryDto>

        `when`(
            snippetDetailService.listAccessibleSnippets("auth0|abc", 0, 10, null),
        ).thenReturn(pageMock)

        val result = controller.listAccessible(principal, 0, 10, null)

        assertSame(pageMock, result)
        verify(snippetDetailService).listAccessibleSnippets("auth0|abc", 0, 10, null)
    }

    @Test
    fun `createFromEditor llama al service con el owner correcto`() {
        val principal = jwt("auth0|owner-1")
        val req = CreateSnippetReq(
            name = "test-snippet",
            description = "desc",
            language = "printscript",
            version = "1.1",
            extension = "prs",
            content = "println(1);",
        )

        val detailMock = mock(SnippetDetailDto::class.java)
        `when`(snippetDetailService.createSnippet("auth0|owner-1", req))
            .thenReturn(detailMock)

        val result = controller.createFromEditor(principal, req)

        assertSame(detailMock, result)
        verify(snippetDetailService).createSnippet("auth0|owner-1", req)
    }

    @Test
    fun `createFromFile envia meta y bytes al service`() {
        val principal = jwt("auth0|file-owner")
        val meta = CreateSnippetReq(
            name = "file-snippet",
            description = null,
            language = "printscript",
            version = "1.1",
            extension = "prs",
        )

        val fileContent = "println(42);"
        val file = MockMultipartFile(
            "file",
            "snippet.prs",
            MediaType.APPLICATION_OCTET_STREAM_VALUE,
            fileContent.toByteArray(),
        )

        val detailMock = mock(SnippetDetailDto::class.java)
        `when`(
            snippetDetailService.createSnippetFromFile("auth0|file-owner", meta, file.bytes),
        ).thenReturn(detailMock)

        val result = controller.createFromFile(principal, meta, file)

        assertSame(detailMock, result)
        verify(snippetDetailService).createSnippetFromFile("auth0|file-owner", meta, file.bytes)
    }

    @Test
    fun `updateSnippet delega a updateSnippetOwnerAware`() {
        val principal = jwt("auth0|editor")
        val snippetId = UUID.randomUUID()
        val req = UpdateSnippetReq(
            name = "nuevo nombre",
            description = "nueva desc",
            language = null,
            version = null,
            content = "println(999);",
        )

        val detailMock = mock(SnippetDetailDto::class.java)
        `when`(
            snippetDetailService.updateSnippetOwnerAware("auth0|editor", snippetId, req),
        ).thenReturn(detailMock)

        val result = controller.updateSnippet(principal, snippetId, req)

        assertSame(detailMock, result)
        verify(snippetDetailService).updateSnippetOwnerAware("auth0|editor", snippetId, req)
    }

    @Test
    fun `updateSnippetFromFile usa el service updateSnippetFromFile`() {
        val principal = jwt("auth0|editor")
        val snippetId = UUID.randomUUID()
        val meta = UpdateSnippetReq(
            name = "nuevo nombre",
            description = null,
            language = "printscript",
            version = "1.1",
            content = null, // se va a pisar en el service
        )

        val fileContent = "println(1+2);"
        val file = MockMultipartFile(
            "file",
            "snippet.prs",
            MediaType.APPLICATION_OCTET_STREAM_VALUE,
            fileContent.toByteArray(),
        )

        val detailMock = mock(SnippetDetailDto::class.java)
        `when`(
            snippetDetailService.updateSnippetFromFile(
                "auth0|editor",
                snippetId,
                meta,
                file.bytes,
            ),
        ).thenReturn(detailMock)

        val result = controller.updateSnippetFromFile(principal, snippetId, meta, file)

        assertSame(detailMock, result)
        verify(snippetDetailService).updateSnippetFromFile("auth0|editor", snippetId, meta, file.bytes)
    }

    @Test
    fun `deleteSnippet llama al service con user y snippet correctos`() {
        val principal = jwt("auth0|owner")
        val snippetId = UUID.randomUUID()

        controller.deleteSnippet(principal, snippetId)

        verify(snippetDetailService).deleteSnippetOwnerAware("auth0|owner", snippetId)
    }

    @Test
    fun `share delega en snippetPermissionService`() {
        val principal = jwt(userId = "auth0|owner")

        val req = ShareSnippetReq(
            snippetId = UUID.randomUUID().toString(),
            userId = "auth0|other",
            permissionType = "reader",
        )

        controller.share(principal, req)

        verify(snippetPermissionService).shareSnippetOwnerAware("auth0|owner", req)
    }

    @Test
    fun `download construye ResponseEntity con bytes del service`() {
        val principal = jwt("auth0|reader")
        val snippetId = UUID.randomUUID()
        val fileBytes = "content".toByteArray()

        `when`(
            snippetDetailService.download(snippetId, false),
        ).thenReturn(fileBytes)
        `when`(
            snippetDetailService.filename(snippetId, false),
        ).thenReturn("code.prs")

        val response = controller.download(principal, snippetId, false)

        // se chequea status, headers y body
        assertArrayEquals(fileBytes, response.body)
        assertEquals("attachment; filename=\"code.prs\"", response.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION))
        assertEquals(MediaType.TEXT_PLAIN, response.headers.contentType)

        verify(snippetPermissionService).checkPermissions("auth0|reader", snippetId, min = AccessLevel.READER)
        verify(snippetDetailService).download(snippetId, false)
        verify(snippetDetailService).filename(snippetId, false)
    }

    @Test
    fun `createTest pasa el snippetId al service`() {
        val snippetId = UUID.randomUUID()
        val req = CreateTestReq(
            snippetId = null,
            name = "test-1",
            inputs = emptyList(),
            expectedOutputs = emptyList(),
            targetVersionNumber = null,
        )

        val dtoMock = mock(TestCaseDto::class.java)
        `when`(
            snippetTestService.createTestCase(req.copy(snippetId = snippetId.toString())),
        ).thenReturn(dtoMock)

        val res = controller.createTest(snippetId, req)

        assertSame(dtoMock, res)
        verify(snippetTestService).createTestCase(req.copy(snippetId = snippetId.toString()))
    }

    @Test
    fun `listTests retorna lo del service`() {
        val snippetId = UUID.randomUUID()
        val listMock = listOf(mock(TestCaseDto::class.java))

        `when`(snippetTestService.listTestCases(snippetId)).thenReturn(listMock)

        val res = controller.listTests(snippetId)

        assertEquals(1, res.size)
        assertSame(listMock, res)
        verify(snippetTestService).listTestCases(snippetId)
    }

    @Test
    fun `deleteTest delega en service`() {
        val testId = UUID.randomUUID()

        controller.deleteTest(testId)

        verify(snippetTestService).deleteTestCase(testId)
    }

    @Test
    fun `runSingleTest llama a runOneTestOwnerAware`() {
        val principal = jwt("auth0|tester")
        val snippetId = UUID.randomUUID()
        val testId = UUID.randomUUID()

        val resultMock = mock(SingleTestRunResult::class.java)
        `when`(
            snippetsExecuteService.runOneTestOwnerAware("auth0|tester", snippetId, testId),
        ).thenReturn(resultMock)

        val res = controller.runSingleTest(principal, snippetId, testId)

        assertSame(resultMock, res)
        verify(snippetsExecuteService).runOneTestOwnerAware("auth0|tester", snippetId, testId)
    }

    @Test
    fun `getFileTypes devuelve lo del service`() {
        val listMock = listOf(mock(FileTypeDto::class.java))
        `when`(snippetDetailService.getFileTypes()).thenReturn(listMock)

        val res = controller.getFileTypes()

        assertSame(listMock, res)
        verify(snippetDetailService).getFileTypes()
    }

    @Test
    fun `runSnippet llama a runSnippetOwnerAware`() {
        val principal = jwt("auth0|runner")
        val snippetId = UUID.randomUUID()
        val body = RunSnippetInputsReq(inputs = listOf("1", "2"))
        val runResMock = mock(RunRes::class.java)

        `when`(
            snippetsExecuteService.runSnippetOwnerAware("auth0|runner", snippetId, body.inputs),
        ).thenReturn(runResMock)

        val response = controller.runSnippet(principal, snippetId, body)

        assertSame(runResMock, response.body)
        verify(snippetsExecuteService).runSnippetOwnerAware("auth0|runner", snippetId, body.inputs)
    }

    // kotlin
    @Test
    fun `ping devuelve no content`() {
        val response = controller.ping()
        assertEquals(org.springframework.http.HttpStatus.NO_CONTENT, response.statusCode)
    }

    @Test
    fun `getSnippet devuelve dto del service`() {
        val id = UUID.randomUUID()
        val dtoMock = mock(SnippetDetailDto::class.java)
        `when`(snippetDetailService.getSnippet(id)).thenReturn(dtoMock)

        val res = controller.getSnippet(id)

        assertSame(dtoMock, res)
        verify(snippetDetailService).getSnippet(id)
    }

    @Test
    fun `saveAndPublishFormat guarda y notifica`() {
        val owner = "auth0|owner-format"
        val principal = jwt(owner)
        val rules = listOf(mock(com.printscript.snippets.dto.RuleDto::class.java))
        val body = com.printscript.snippets.dto.SaveRulesReq(
            rules = rules,
            configText = "cfg",
            configFormat = "fmt"
        )

        val response = controller.saveAndPublishFormat(principal, body)

        assertEquals(org.springframework.http.HttpStatus.ACCEPTED, response.statusCode)
        verify(rulesStateService).saveFormatState(owner, body.rules, body.configText, body.configFormat)
        verify(bulkRulesService).onFormattingRulesChanged(owner)
    }

    @Test
    fun `saveAndPublishLint guarda y notifica`() {
        val owner = "auth0|owner-lint"
        val principal = jwt(owner)
        val rules = listOf(mock(com.printscript.snippets.dto.RuleDto::class.java))
        val body = com.printscript.snippets.dto.SaveRulesReq(
            rules = rules,
            configText = "cfg",
            configFormat = "fmt"
        )

        val response = controller.saveAndPublishLint(principal, body)

        assertEquals(org.springframework.http.HttpStatus.ACCEPTED, response.statusCode)
        verify(rulesStateService).saveLintState(owner, body.rules, body.configText, body.configFormat)
        verify(bulkRulesService).onLintingRulesChanged(owner)
    }

    @Test
    fun `getFmtRules devuelve lo del service`() {
        val owner = "auth0|owner-get-fmt"
        val principal = jwt(owner)
        val rules = listOf(mock(com.printscript.snippets.dto.RuleDto::class.java))
        `when`(rulesStateService.getFormatAsRules(owner)).thenReturn(rules)

        val response = controller.getFmtRules(principal)

        assertSame(rules, response.body)
        verify(rulesStateService).getFormatAsRules(owner)
    }

    @Test
    fun `getLintRules devuelve lo del service`() {
        val owner = "auth0|owner-get-lint"
        val principal = jwt(owner)
        val rules = listOf(mock(com.printscript.snippets.dto.RuleDto::class.java))
        `when`(rulesStateService.getLintAsRules(owner)).thenReturn(rules)

        val response = controller.getLintRules(principal)

        assertSame(rules, response.body)
        verify(rulesStateService).getLintAsRules(owner)
    }

    @Test
    fun `formatOne delega en snippetRuleDomainService`() {
        val owner = "auth0|formatter"
        val principal = jwt(owner)
        val id = UUID.randomUUID()
        val dtoMock = mock(SnippetDetailDto::class.java)
        `when`(snippetRuleDomainService.formatOneOwnerAware(owner, id)).thenReturn(dtoMock)

        val response = controller.formatOne(id, principal)

        assertEquals(org.springframework.http.HttpStatus.OK, response.statusCode)
        assertSame(dtoMock, response.body)
        verify(snippetRuleDomainService).formatOneOwnerAware(owner, id)
    }

    @Test
    fun `lintOne delega en snippetRuleDomainService`() {
        val owner = "auth0|linter"
        val principal = jwt(owner)
        val id = UUID.randomUUID()
        val dtoMock = mock(SnippetDetailDto::class.java)
        `when`(snippetRuleDomainService.lintOneOwnerAware(owner, id)).thenReturn(dtoMock)

        val response = controller.lintOne(id, principal)

        assertEquals(org.springframework.http.HttpStatus.OK, response.statusCode)
        assertSame(dtoMock, response.body)
        verify(snippetRuleDomainService).lintOneOwnerAware(owner, id)
    }

}
