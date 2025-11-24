package com.printscript.snippets

import com.printscript.snippets.bucket.SnippetAsset
import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.domain.model.Snippet
import com.printscript.snippets.domain.model.SnippetVersion
import com.printscript.snippets.dto.SnippetDetailDto
import com.printscript.snippets.enums.LintStatus
import com.printscript.snippets.error.NotFound
import com.printscript.snippets.execution.SnippetExecution
import com.printscript.snippets.permission.SnippetPermission
import com.printscript.snippets.service.rules.RulesStateService
import com.printscript.snippets.service.rules.SnippetRuleDomainService
import io.printscript.contracts.DiagnosticDto
import io.printscript.contracts.formatter.FormatReq
import io.printscript.contracts.formatter.FormatRes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.nio.charset.StandardCharsets
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SnippetRuleDomainServiceTest {

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
    lateinit var rulesStateService: RulesStateService

    lateinit var service: SnippetRuleDomainService

    @BeforeEach
    fun setUp() {
        service = SnippetRuleDomainService(
            snippetRepo = snippetRepo,
            versionRepo = versionRepo,
            assetClient = assetClient,
            executionClient = executionClient,
            permissionClient = permissionClient,
            rulesStateService = rulesStateService,
        )
    }

    private fun snippet(
        id: UUID = UUID.randomUUID(),
        ownerId: String = "auth0|owner",
        language: String = "printscript",
        version: String = "1.1",
        lastLintCount: Int = 0,
        lastIsValid: Boolean = true,
    ): Snippet {
        val s = org.mockito.Mockito.mock(Snippet::class.java)
        whenever(s.id).thenReturn(id)
        whenever(s.ownerId).thenReturn(ownerId)
        whenever(s.language).thenReturn(language)
        whenever(s.languageVersion).thenReturn(version)
        whenever(s.name).thenReturn("snippet-name")
        whenever(s.description).thenReturn("desc")
        whenever(s.lastLintCount).thenReturn(lastLintCount)
        whenever(s.lastIsValid).thenReturn(lastIsValid)
        return s
    }

    private fun version(
        id: UUID = UUID.randomUUID(),
        snippetId: UUID,
        number: Long = 1L,
        contentKey: String = "owner/$snippetId/v$number.ps",
        isValid: Boolean = true,
        lintStatus: LintStatus = LintStatus.PENDING,
    ): SnippetVersion {
        val v = org.mockito.Mockito.mock(SnippetVersion::class.java)
        whenever(v.id).thenReturn(id)
        whenever(v.snippetId).thenReturn(snippetId)
        whenever(v.versionNumber).thenReturn(number)
        whenever(v.contentKey).thenReturn(contentKey)
        whenever(v.isValid).thenReturn(isValid)
        whenever(v.lintStatus).thenReturn(lintStatus)
        return v
    }

    @Test
    fun `saveFormatted feliz sube archivo y actualiza version`() {
        val snippetId = UUID.randomUUID()
        val s = snippet(id = snippetId, ownerId = "auth0|owner")
        val v = version(
            snippetId = snippetId,
            number = 3L,
            contentKey = "auth0|owner/$snippetId/v3.ps",
        )

        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.of(s))
        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)).thenReturn(v)

        val formatted = "FORMATTED CODE"

        service.saveFormatted(snippetId, formatted)

        val expectedBase = "${s.ownerId}/$snippetId/v${v.versionNumber}.ps"
        val expectedFormattedKey = expectedBase.replace(".ps", ".formatted.ps")

        verify(assetClient).upload(
            eq("snippets"),
            eq(expectedFormattedKey),
            eq(formatted.toByteArray(Charsets.UTF_8)),
        )

        // No miramos campos internos de la versión porque es un mock,
        // pero sí verificamos que se persista.
        verify(versionRepo).save(v)
        verify(snippetRepo).save(s)
    }

    @Test
    fun `saveFormatted lanza NotFound si snippet no tiene versiones`() {
        val snippetId = UUID.randomUUID()
        val s = snippet(id = snippetId)

        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.of(s))
        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)).thenReturn(null)

        assertThrows<NotFound> {
            service.saveFormatted(snippetId, "x")
        }
    }

    @Test
    fun `saveFormatted lanza NotFound si snippet no existe`() {
        val snippetId = UUID.randomUUID()

        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.empty())

        assertThrows<NotFound> {
            service.saveFormatted(snippetId, "x")
        }
    }

    @Test
    fun `saveLint con violaciones actualiza version y snippet`() {
        val snippetId = UUID.randomUUID()
        val v = version(snippetId = snippetId)
        val s = snippet(id = snippetId)

        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)).thenReturn(v)
        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.of(s))

        val violations = listOf(
            DiagnosticDto("RULE_1", "msg 1", 1, 1),
            DiagnosticDto("RULE_2", "msg 2", 2, 3),
        )

        service.saveLint(snippetId, violations)

        // Verificamos que se haya persistido la versión y el snippet
        verify(versionRepo).save(v)
        verify(snippetRepo).save(s)
    }

    @Test
    fun `saveLint sin violaciones marca DONE y persiste`() {
        val snippetId = UUID.randomUUID()
        val v = version(snippetId = snippetId)
        val s = snippet(id = snippetId)

        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)).thenReturn(v)
        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.of(s))

        val violations = emptyList<DiagnosticDto>()

        service.saveLint(snippetId, violations)

        verify(versionRepo).save(v)
        verify(snippetRepo).save(s)
    }

    @Test
    fun `saveLint lanza NotFound si no hay versiones`() {
        val snippetId = UUID.randomUUID()
        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)).thenReturn(null)

        assertThrows<NotFound> {
            service.saveLint(snippetId, emptyList())
        }
    }

    @Test
    fun `formatOneOwnerAware feliz usa reglas, llama execution y devuelve dto con contenido formateado`() {
        val userId = "auth0|owner"
        val snippetId = UUID.randomUUID()
        val s = snippet(id = snippetId, ownerId = userId)
        val v = version(
            snippetId = snippetId,
            number = 2L,
            contentKey = "auth0|owner/$snippetId/v2.ps",
        )

        val original = "let x = 1;"
        val formatted = "let x = 1;\n"

        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.of(s))
        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)).thenReturn(v)
        whenever(assetClient.download("snippets", v.contentKey))
            .thenReturn(original.toByteArray(StandardCharsets.UTF_8))

        // reglas de formato
        whenever(rulesStateService.getFormatAsRules(s.ownerId))
            .thenReturn(emptyList())
        whenever(rulesStateService.currentFormatConfigEffective(s.ownerId))
            .thenReturn("CFG_TEXT" to "JSON")

        val formatRes = mock<FormatRes>()
        whenever(formatRes.formattedContent).thenReturn(formatted)

        val reqCaptor = argumentCaptor<FormatReq>()
        whenever(executionClient.format(reqCaptor.capture())).thenReturn(formatRes)

        // Cuando saveFormatted se ejecute, queremos que versionRepo devuelva una versión "actualizada"
        val updatedVersion = version(snippetId = snippetId, number = 2L)
        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId))
            .thenReturn(v) // primera vez (antes de format)
            .thenReturn(updatedVersion) // segunda vez (después de saveFormatted)

        val dto: SnippetDetailDto = service.formatOneOwnerAware(userId, snippetId)

        val sentReq = reqCaptor.firstValue
        assertEquals(s.language, sentReq.language)
        assertEquals(s.languageVersion, sentReq.version)
        assertEquals(original, sentReq.content)
        assertEquals("CFG_TEXT", sentReq.configText)
        assertEquals("JSON", sentReq.configFormat)

        assertEquals(s.id.toString(), dto.id)
        assertEquals(formatted, dto.content)

        verify(executionClient).format(any())
    }

    @Test
    fun `formatOneOwnerAware lanza NotFound si snippet no existe`() {
        val userId = "auth0|someone"
        val snippetId = UUID.randomUUID()

        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.empty())

        assertThrows<NotFound> {
            service.formatOneOwnerAware(userId, snippetId)
        }
    }

    @Test
    fun `formatOneOwnerAware lanza NotFound si snippet no tiene versiones`() {
        val userId = "auth0|owner"
        val snippetId = UUID.randomUUID()
        val s = snippet(id = snippetId, ownerId = userId)

        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.of(s))
        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)).thenReturn(null)

        assertThrows<NotFound> {
            service.formatOneOwnerAware(userId, snippetId)
        }
    }

    @Test
    fun `lintOneOwnerAware lanza NotFound si snippet no existe`() {
        val userId = "auth0|owner"
        val snippetId = UUID.randomUUID()

        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.empty())

        assertThrows<NotFound> {
            service.lintOneOwnerAware(userId, snippetId)
        }
    }

    @Test
    fun `lintOneOwnerAware lanza NotFound si snippet no tiene versiones`() {
        val userId = "auth0|owner"
        val snippetId = UUID.randomUUID()
        val s = snippet(id = snippetId, ownerId = userId)

        whenever(snippetRepo.findById(snippetId)).thenReturn(Optional.of(s))
        whenever(versionRepo.findTopBySnippetIdOrderByVersionNumberDesc(snippetId)).thenReturn(null)

        assertThrows<NotFound> {
            service.lintOneOwnerAware(userId, snippetId)
        }
    }
}
