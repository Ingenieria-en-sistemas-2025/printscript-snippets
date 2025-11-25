package com.printscript.snippets.rules

import com.printscript.snippets.domain.RulesStateRepo
import com.printscript.snippets.domain.model.RulesState
import com.printscript.snippets.dto.RuleDto
import com.printscript.snippets.enums.RulesType
import com.printscript.snippets.service.rules.FormatRuleStrategy
import com.printscript.snippets.service.rules.LintRuleStrategy
import com.printscript.snippets.service.rules.RulesStateService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RulesStateServiceTest {

    @Mock
    lateinit var rulesStateRepo: RulesStateRepo

    lateinit var service: RulesStateService

    @BeforeEach
    fun setUp() {
        service = RulesStateService(rulesStateRepo, listOf(LintRuleStrategy(), FormatRuleStrategy()))
    }

    private fun row(
        type: RulesType,
        owner: String?,
        enabled: List<String> = emptyList(),
        options: Map<String, Any?>? = null,
        text: String? = null,
        fmt: String? = null,
    ) = RulesState(
        id = UUID.randomUUID(),
        type = type,
        ownerId = owner,
        enabledJson = enabled,
        optionsJson = options,
        configText = text,
        configFormat = fmt,
    )

    @Test
    fun `findRow devuelve fila de usuario si existe`() {
        val owner = "auth0|user"
        val userRow = row(RulesType.FORMAT, owner)

        whenever(rulesStateRepo.findByTypeAndOwnerId(RulesType.FORMAT, owner))
            .thenReturn(Optional.of(userRow))

        val method = RulesStateService::class.java
            .getDeclaredMethod("findRowForOwnerOrDefault", RulesType::class.java, String::class.java)
        method.isAccessible = true

        val result = method.invoke(service, RulesType.FORMAT, owner) as RulesState?

        assertSame(userRow, result)
        verify(rulesStateRepo).findByTypeAndOwnerId(RulesType.FORMAT, owner)
        verify(rulesStateRepo, never()).findByTypeAndOwnerId(RulesType.FORMAT, null)
    }

    @Test
    fun `findRow hace fallback a global si no hay fila de usuario`() {
        val owner = "auth0|user2"

        whenever(rulesStateRepo.findByTypeAndOwnerId(RulesType.FORMAT, owner))
            .thenReturn(Optional.empty())

        val globalRow = row(RulesType.FORMAT, null)
        whenever(rulesStateRepo.findByTypeAndOwnerId(RulesType.FORMAT, null))
            .thenReturn(Optional.of(globalRow))

        val method = RulesStateService::class.java
            .getDeclaredMethod("findRowForOwnerOrDefault", RulesType::class.java, String::class.java)
        method.isAccessible = true

        val result = method.invoke(service, RulesType.FORMAT, owner) as RulesState?

        assertSame(globalRow, result)
        verify(rulesStateRepo).findByTypeAndOwnerId(RulesType.FORMAT, owner)
        verify(rulesStateRepo).findByTypeAndOwnerId(RulesType.FORMAT, null)
    }

    @Test
    fun `readOptions devuelve solo valores no nulos`() {
        val owner = "auth0|opt"
        val r = row(
            type = RulesType.FORMAT,
            owner = owner,
            options = mapOf("indentSize" to 4, "foo" to null),
        )

        whenever(rulesStateRepo.findByTypeAndOwnerId(RulesType.FORMAT, owner))
            .thenReturn(Optional.of(r))

        val method = RulesStateService::class.java
            .getDeclaredMethod("readOptions", RulesType::class.java, String::class.java)
        method.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(service, RulesType.FORMAT, owner) as Map<String, Any?>

        assertEquals(1, result.size)
        assertEquals(4, result["indentSize"])
        assertFalse(result.containsKey("foo"))
    }

    @Test
    fun `readEnabled devuelve enabledJson de la fila si existe`() {
        val owner = "auth0|en1"
        val r = row(
            type = RulesType.LINT,
            owner = owner,
            enabled = listOf("R1", "R2"),
        )

        whenever(rulesStateRepo.findByTypeAndOwnerId(RulesType.LINT, owner))
            .thenReturn(Optional.of(r))

        val method = RulesStateService::class.java
            .getDeclaredMethod("readEnabled", RulesType::class.java, String::class.java)
        method.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(service, RulesType.LINT, owner) as Set<String>

        assertEquals(setOf("R1", "R2"), result)
    }

    @Test
    fun `readEnabled usa defaults del strategy si no hay filas`() {
        val owner = "auth0|en2"

        whenever(rulesStateRepo.findByTypeAndOwnerId(RulesType.LINT, owner))
            .thenReturn(Optional.empty())
        whenever(rulesStateRepo.findByTypeAndOwnerId(RulesType.LINT, null))
            .thenReturn(Optional.empty())

        val method = RulesStateService::class.java
            .getDeclaredMethod("readEnabled", RulesType::class.java, String::class.java)
        method.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(service, RulesType.LINT, owner) as Set<String>

        // No asumimos qué reglas son, solo que hay alguna default
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `normalizeFormat mapea variaciones a json o yaml`() {
        val method = RulesStateService::class.java
            .getDeclaredMethod("normalizeFormat", String::class.java)
        method.isAccessible = true

        assertEquals("yaml", method.invoke(service, "yMl"))
        assertEquals("json", method.invoke(service, "jsOn"))
        assertEquals("json", method.invoke(service, null))
        assertEquals("json", method.invoke(service, "cualquiercosa"))
    }

    @Test
    fun `saveState crea fila nueva si no existe y la guarda`() {
        val owner = "auth0|owner"

        // no existe fila para el owner
        whenever(rulesStateRepo.findByTypeAndOwnerId(RulesType.FORMAT, owner))
            .thenReturn(Optional.empty())

        val rules = listOf(
            mock<RuleDto>(),
            mock<RuleDto>(),
        )

        service.saveState(
            type = RulesType.FORMAT,
            ownerId = owner,
            rules = rules,
            configText = "cfg-text",
            configFormat = "yml",
        )

        val captor = argumentCaptor<RulesState>()
        verify(rulesStateRepo).save(captor.capture())

        val saved = captor.firstValue
        assertEquals(RulesType.FORMAT, saved.type)
        assertEquals(owner, saved.ownerId)
        assertEquals("cfg-text", saved.configText)
        // normalizado por normalizeFormat
        assertEquals("yaml", saved.configFormat)
    }

    @Test
    fun `saveState actualiza fila existente`() {
        val owner = "auth0|existing"
        val existing = row(
            type = RulesType.LINT,
            owner = owner,
            enabled = listOf("old"),
            options = mapOf("old" to 1),
            text = "old",
            fmt = "json",
        )

        whenever(rulesStateRepo.findByTypeAndOwnerId(RulesType.LINT, owner))
            .thenReturn(Optional.of(existing))

        val rules = listOf(mock<RuleDto>())

        service.saveState(
            type = RulesType.LINT,
            ownerId = owner,
            rules = rules,
            configText = "new-text",
            configFormat = "yaml",
        )

        verify(rulesStateRepo).save(existing)
        assertEquals("new-text", existing.configText)
        assertEquals("yaml", existing.configFormat)
    }

    @Test
    fun `currentConfigEffective siempre devuelve texto y formato no nulos`() {
        val owner = "auth0|cfg"

        // sin filas (usa defaults del strategy)
        whenever(rulesStateRepo.findByTypeAndOwnerId(RulesType.LINT, owner))
            .thenReturn(Optional.empty())
        whenever(rulesStateRepo.findByTypeAndOwnerId(RulesType.LINT, null))
            .thenReturn(Optional.empty())

        val (text, fmt) = service.currentLintConfigEffective(owner)

        assertNotNull(text)
        assertTrue(text.isNotBlank())
        assertEquals("json", fmt) // por currentConfigEffective (cfgFmt ?: "json")
    }

    @Test
    fun `alias methods delegan a los genericos`() {
        val owner = "auth0|alias"
        val spyService = spy(service)

        spyService.getFormatAsRules(owner)
        verify(spyService).getRules(RulesType.FORMAT, owner)

        spyService.getLintAsRules(owner)
        verify(spyService).getRules(RulesType.LINT, owner)

        spyService.currentFormatConfig(owner)
        verify(spyService).currentConfig(RulesType.FORMAT, owner)

        spyService.currentLintConfig(owner)
        verify(spyService).currentConfig(RulesType.LINT, owner)

        spyService.currentFormatConfigEffective(owner)
        verify(spyService).currentConfigEffective(RulesType.FORMAT, owner)

        spyService.currentLintConfigEffective(owner)
        verify(spyService).currentConfigEffective(RulesType.LINT, owner)
    }
}
