package com.printscript.snippets

import com.printscript.snippets.bucket.SnippetAsset
import com.printscript.snippets.domain.LanguageConfigRepo
import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.domain.model.LanguageConfig
import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.domain.model.SnippetVersion
import com.printscript.snippets.dto.CreateSnippetReq
import com.printscript.snippets.dto.UpdateSnippetReq
import com.printscript.snippets.enums.Compliance
import com.printscript.snippets.error.InvalidRequest
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.execution.SnippetExecution
import com.printscript.snippets.permission.SnippetPermission
import com.printscript.snippets.service.SnippetDetailService
import com.printscript.snippets.service.rules.RulesStateService
import com.printscript.snippets.user.UserService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.quality.Strictness
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SnippetDetailServiceTest {

    @Mock
    lateinit var snippetRepo: SnippetRepo

    @Mock
    lateinit var versionRepo: SnippetVersionRepo

    @Mock
    lateinit var assetClient: SnippetAsset

    @Mock
    lateinit var executionClient: SnippetExecution

    @Mock
    lateinit var permissionClient: SnippetPermission

    @Mock
    lateinit var userService: UserService

    @Mock
    lateinit var rulesStateService: RulesStateService

    @Mock
    lateinit var languageConfigRepo: LanguageConfigRepo

    @InjectMocks
    lateinit var service: SnippetDetailService

    // ===== helpers =====

    private fun snippet(
        id: UUID = UUID.randomUUID(),
        ownerId: String = "auth0|owner",
        name: String = "snippet-1",
        lang: String = "printscript",
        ver: String = "1.1",
        createdAt: Instant = Instant.parse("2024-01-01T00:00:00Z"),
        lastIsValid: Boolean = true,
        lastLint: Int = 0,
        compliance: Compliance = Compliance.COMPLIANT,
    ): Snippet {
        val s = mock(Snippet::class.java)
        `when`(s.id).thenReturn(id)
        `when`(s.ownerId).thenReturn(ownerId)
        `when`(s.name).thenReturn(name)
        `when`(s.description).thenReturn("desc")
        `when`(s.language).thenReturn(lang)
        `when`(s.languageVersion).thenReturn(ver)
        `when`(s.lastIsValid).thenReturn(lastIsValid)
        `when`(s.lastLintCount).thenReturn(lastLint)
        `when`(s.compliance).thenReturn(compliance)
        `when`(s.createdAt).thenReturn(createdAt)
        return s
    }

    private fun version(
        id: UUID = UUID.randomUUID(),
        snippetId: UUID,
        number: Long,
        contentKey: String,
        formattedKey: String? = null,
        isFormatted: Boolean = false,
        isValid: Boolean = true,
    ): SnippetVersion {
        val v = mock(SnippetVersion::class.java)
        `when`(v.id).thenReturn(id)
        `when`(v.snippetId).thenReturn(snippetId)
        `when`(v.versionNumber).thenReturn(number)
        `when`(v.contentKey).thenReturn(contentKey)
        `when`(v.formattedKey).thenReturn(formattedKey)
        `when`(v.isFormatted).thenReturn(isFormatted)
        `when`(v.isValid).thenReturn(isValid)
        return v
    }

    // =========================================================
    // getSnippet
    // =========================================================

//    @Test
//    fun `getSnippet usa contentKey cuando no esta formateado`() {
//        val id = UUID.randomUUID()
//        val s = snippet(id = id)
//        val v = version(
//            snippetId = id,
//            number = 1L,
//            contentKey = "owner/$id/v1.ps",
//            isFormatted = false,
//            formattedKey = null,
//        )
//        val code = "println(1);"
//
//        `when`(snippetRepo.findById(id)).thenReturn(Optional.of(s))
//        `when`(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(id)).thenReturn(v)
//        `when`(assetClient.download("snippets", "owner/$id/v1.ps"))
//            .thenReturn(code.toByteArray(StandardCharsets.UTF_8))
//
//        val dto = service.getSnippet(id)
//
//        assertEquals(id.toString(), dto.id)
//        assertEquals(code, dto.content)
//    }

    @Test
    fun `getSnippet usa formattedKey si esta formateado`() {
        val id = UUID.randomUUID()
        val s = snippet(id = id)
        val v = version(
            snippetId = id,
            number = 2L,
            contentKey = "owner/$id/v2.ps",
            formattedKey = "owner/$id/v2.formatted.ps",
            isFormatted = true,
        )
        val formatted = "formatted();"

        `when`(snippetRepo.findById(id)).thenReturn(Optional.of(s))
        `when`(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(id)).thenReturn(v)
        `when`(assetClient.download("snippets", "owner/$id/v2.formatted.ps"))
            .thenReturn(formatted.toByteArray(StandardCharsets.UTF_8))

        val dto = service.getSnippet(id)

        assertEquals(formatted, dto.content)
    }

    @Test
    fun `getSnippet lanza NotFound si no existe snippet`() {
        val id = UUID.randomUUID()
        `when`(snippetRepo.findById(id)).thenReturn(Optional.empty())

        assertThrows<NotFound> {
            service.getSnippet(id)
        }
    }

    // =========================================================
    // listAccessibleSnippets
    // =========================================================

//    @Test
//    fun `listAccessibleSnippets cuando no hay permisos devuelve pagina vacia`() {
//        val userId = "auth0|u1"
//
//        val emptyPermissions = SnippetPermissionListResponse(
//            authorizations = emptyList(),
//            total = 0,
//        )
//        val response = ResponseEntity.ok(emptyPermissions)
//
//        `when`(
//            permissionClient.getAllSnippetsPermission(userId, 0, 1000),
//        ).thenReturn(response)
//
//        `when`(
//            snippetRepo.findAllById(emptyList<UUID>()),
//        ).thenReturn(emptyList())
//
//        val page: PageDto<SnippetSummaryDto> =
//            service.listAccessibleSnippets(userId, page = 0, size = 10, name = null)
//
//        assertEquals(0, page.count)
//        assertTrue(page.items.isEmpty())
//
//        verify(permissionClient).getAllSnippetsPermission(userId, 0, 1000)
//    }

    // =========================================================
    // createSnippet
    // =========================================================

//    @Test
//    fun `createSnippet feliz crea version, sube al bucket y llama permiso OWNER`() {
//        val ownerId = "auth0|owner"
//        val snippetId = UUID.randomUUID()
//        val req = CreateSnippetReq(
//            name = "my-snippet",
//            description = "desc",
//            language = "printscript",
//            version = "1.1",
//            extension = "prs",
//            content = "println(1);",
//            source = SnippetSource.INLINE,
//        )
//
//        val savedSnippet = snippet(id = snippetId, ownerId = ownerId)
//
//        val parseRes = mock(ParseRes::class.java)
//        `when`(parseRes.valid).thenReturn(true)
//        `when`(parseRes.diagnostics).thenReturn(emptyList())
//
//        val lintRes = mock(LintRes::class.java)
//        `when`(lintRes.violations).thenReturn(emptyList())
//
//        `when`(snippetRepo.save(ArgumentMatchers.any(Snippet::class.java))).thenReturn(savedSnippet)
//        `when`(versionRepo.findMaxVersionBySnippetId(snippetId)).thenReturn(0L)
//
//        val v1 = version(
//            snippetId = snippetId,
//            number = 1L,
//            contentKey = "$ownerId/$snippetId/v1.ps",
//        )
//        `when`(versionRepo.save(ArgumentMatchers.any(SnippetVersion::class.java))).thenReturn(v1)
//
//        `when`(executionClient.parse(ArgumentMatchers.any(ParseReq::class.java))).thenReturn(parseRes)
//        `when`(rulesStateService.currentLintConfigEffective(ownerId))
//            .thenReturn("" to "")
//        `when`(executionClient.lint(ArgumentMatchers.any())).thenReturn(lintRes)
//
//        val dto: SnippetDetailDto = service.createSnippet(ownerId, req)
//
//        assertEquals(snippetId.toString(), dto.id)
//        assertEquals(ownerId, dto.ownerId)
//        assertEquals(req.content, dto.content)
//
//        verify(executionClient).parse(ArgumentMatchers.any(ParseReq::class.java))
//        verify(executionClient).lint(ArgumentMatchers.any())
//
//        verify(assetClient).upload(
//            "snippets",
//            "$ownerId/$snippetId/v1.ps",
//            "println(1);".toByteArray(StandardCharsets.UTF_8),
//        )
//
//        val captor = ArgumentCaptor.forClass(PermissionCreateSnippetInput::class.java)
//        verify(permissionClient).createAuthorization(captor.capture())
//        val authReq = captor.value
//        assertEquals(snippetId.toString(), authReq.snippetId)
//        assertEquals(ownerId, authReq.userId)
//        assertEquals("OWNER", authReq.scope)
//    }

    @Test
    fun `createSnippet con contenido en blanco lanza InvalidRequest`() {
        val ownerId = "auth0|owner"
        val req = CreateSnippetReq(
            name = "x",
            description = null,
            language = "printscript",
            version = "1.1",
            extension = "prs",
            content = "   ",
        )

        val ex = assertThrows<InvalidRequest> {
            service.createSnippet(ownerId, req)
        }
        assertTrue(ex.message!!.contains("no puede estar vacío"))
        verifyNoInteractions(executionClient)
    }

//    @Test
//    fun `createSnippet con parse invalido lanza InvalidSnippet y no sube al bucket`() {
//        val ownerId = "auth0|owner"
//        val snippetId = UUID.randomUUID()
//        val req = CreateSnippetReq(
//            name = "my-snippet",
//            description = null,
//            language = "printscript",
//            version = "1.1",
//            extension = "prs",
//            content = "println(1);",
//        )
//
//        val savedSnippet = snippet(id = snippetId, ownerId = ownerId)
//        `when`(snippetRepo.save(ArgumentMatchers.any(Snippet::class.java))).thenReturn(savedSnippet)
//
//        val parseRes = mock(ParseRes::class.java)
//        `when`(parseRes.valid).thenReturn(false)
//        `when`(parseRes.diagnostics).thenReturn(emptyList())
//
//        `when`(executionClient.parse(ArgumentMatchers.any(ParseReq::class.java))).thenReturn(parseRes)
//
//        assertThrows<InvalidSnippet> {
//            service.createSnippet(ownerId, req)
//        }
//
//        verify(executionClient).parse(ArgumentMatchers.any(ParseReq::class.java))
//        verifyNoInteractions(assetClient)
//    }

    // =========================================================
    // createSnippetFromFile
    // =========================================================

    @Test
    fun `createSnippetFromFile con archivo vacio lanza InvalidRequest`() {
        val ownerId = "auth0|owner"
        val meta = CreateSnippetReq(
            name = "file",
            description = null,
            language = "printscript",
            version = "1.1",
            extension = "prs",
        )

        val ex = assertThrows<InvalidRequest> {
            service.createSnippetFromFile(ownerId, meta, ByteArray(0))
        }
        assertTrue(ex.message!!.contains("archivo subido está vacío"))
    }

    // =========================================================
    // updateSnippetOwnerAware
    // =========================================================

//    @Test
//    fun `updateSnippetOwnerAware con content crea nueva version`() {
//        val userId = "auth0|editor"
//        val snippetId = UUID.randomUUID()
//        val s = snippet(id = snippetId, ownerId = userId)
//
//        `when`(snippetRepo.findById(snippetId)).thenReturn(Optional.of(s))
//        `when`(versionRepo.findMaxVersionBySnippetId(snippetId)).thenReturn(1L)
//
//        val parseRes = mock(ParseRes::class.java)
//        `when`(parseRes.valid).thenReturn(true)
//        `when`(parseRes.diagnostics).thenReturn(emptyList())
//        val lintRes = mock(LintRes::class.java)
//        `when`(lintRes.violations).thenReturn(emptyList())
//
//        `when`(executionClient.parse(ArgumentMatchers.any(ParseReq::class.java))).thenReturn(parseRes)
//        `when`(rulesStateService.currentLintConfigEffective(userId)).thenReturn("" to "")
//        `when`(executionClient.lint(ArgumentMatchers.any())).thenReturn(lintRes)
//
//        val v2Key = "$userId/$snippetId/v2.ps"
//        val v2 = version(
//            snippetId = snippetId,
//            number = 2L,
//            contentKey = v2Key,
//        )
//        `when`(versionRepo.save(ArgumentMatchers.any(SnippetVersion::class.java))).thenReturn(v2)
//
//        val req = UpdateSnippetReq(
//            name = "nuevo",
//            description = "desc",
//            language = null,
//            version = null,
//            content = "println(2);",
//        )
//
//        val dto = service.updateSnippetOwnerAware(userId, snippetId, req)
//
//        assertEquals(snippetId.toString(), dto.id)
//        assertEquals("println(2);", dto.content)
//
//        verify(assetClient).upload(
//            "snippets",
//            v2Key,
//            "println(2);".toByteArray(StandardCharsets.UTF_8),
//        )
//    }

    @Test
    fun `updateSnippetOwnerAware solo language o solo version lanza InvalidRequest`() {
        val userId = "auth0|editor"
        val snippetId = UUID.randomUUID()
        val s = snippet(id = snippetId, ownerId = userId)
        `when`(snippetRepo.findById(snippetId)).thenReturn(Optional.of(s))

        val reqOnlyLang = UpdateSnippetReq(
            name = null,
            description = null,
            language = "printscript",
            version = null,
            content = null,
        )

        assertThrows<InvalidRequest> {
            service.updateSnippetOwnerAware(userId, snippetId, reqOnlyLang)
        }

        val reqOnlyVer = UpdateSnippetReq(
            name = null,
            description = null,
            language = null,
            version = "1.1",
            content = null,
        )

        assertThrows<InvalidRequest> {
            service.updateSnippetOwnerAware(userId, snippetId, reqOnlyVer)
        }
    }

    // =========================================================
    // deleteSnippetOwnerAware
    // =========================================================

    @Test
    fun `deleteSnippetOwnerAware pide permisos y borra snippet y versiones`() {
        val userId = "auth0|owner"
        val snippetId = UUID.randomUUID()
        val s = snippet(id = snippetId, ownerId = userId)

        val v1 = version(snippetId = snippetId, number = 1L, contentKey = "k1")
        val v2 = version(snippetId = snippetId, number = 2L, contentKey = "k2", formattedKey = "kf2", isFormatted = true)

        `when`(snippetRepo.findById(snippetId)).thenReturn(Optional.of(s))
        `when`(versionRepo.findAllBySnippetId(snippetId)).thenReturn(listOf(v1, v2))

        service.deleteSnippetOwnerAware(userId, snippetId)

        verify(permissionClient).deleteSnippetPermissions(snippetId.toString())
        verify(assetClient).delete("snippets", "k1")
        verify(assetClient).delete("snippets", "k2")
        verify(assetClient).delete("snippets", "kf2")
        verify(versionRepo).deleteAll(listOf(v1, v2))
        verify(snippetRepo).deleteById(snippetId)
    }

    // =========================================================
    // download & filename
    // =========================================================

//    @Test
//    fun `download trae bytes de la ultima version sin formatear`() {
//        val id = UUID.randomUUID()
//        val v = version(
//            snippetId = id,
//            number = 3L,
//            contentKey = "owner/$id/v3.ps",
//        )
//        `when`(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(id)).thenReturn(v)
//        val bytes = "code".toByteArray()
//        `when`(assetClient.download("snippets", "owner/$id/v3.ps")).thenReturn(bytes)
//
//        val res = service.download(id, formatted = false)
//
//        assertArrayEquals(bytes, res)
//    }

    @Test
    fun `filename devuelve nombre correcto para version sin formatear`() {
        val id = UUID.randomUUID()
        val s = snippet(id = id, name = "my-snippet")
        val v = version(snippetId = id, number = 5L, contentKey = "k")

        `when`(snippetRepo.findById(id)).thenReturn(Optional.of(s))
        `when`(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(id)).thenReturn(v)

        val name = service.filename(id, formatted = false)

        assertEquals("my-snippet-v5.prs", name)
    }

    @Test
    fun `filename devuelve nombre formateado cuando corresponde`() {
        val id = UUID.randomUUID()
        val s = snippet(id = id, name = "my-snippet")
        val v = version(
            snippetId = id,
            number = 5L,
            contentKey = "k",
            formattedKey = "kf",
            isFormatted = true,
        )

        `when`(snippetRepo.findById(id)).thenReturn(Optional.of(s))
        `when`(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(id)).thenReturn(v)

        val name = service.filename(id, formatted = true)

        assertEquals("my-snippet-v5.formatted.prs", name)
    }

    // =========================================================
    // getFileTypes
    // =========================================================

    @Test
    fun `getFileTypes agrupa por lenguaje y ordena versiones desc`() {
        val cfg1 = LanguageConfig(
            id = UUID.randomUUID(),
            language = "printscript",
            extension = "prs",
            version = "1.0",
        )
        val cfg2 = LanguageConfig(
            id = UUID.randomUUID(),
            language = "printscript",
            extension = "prs",
            version = "1.1",
        )
        val cfg3 = LanguageConfig(
            id = UUID.randomUUID(),
            language = "js",
            extension = "js",
            version = "es6",
        )

        `when`(languageConfigRepo.findAll()).thenReturn(listOf(cfg1, cfg2, cfg3))

        val list = service.getFileTypes()

        assertEquals(2, list.size)

        val ps = list.first { it.language == "printscript" }
        assertEquals("prs", ps.extension)
        assertEquals(listOf("1.1", "1.0"), ps.versions)

        val js = list.first { it.language == "js" }
        assertEquals(listOf("es6"), js.versions)
    }
}
