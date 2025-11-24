package com.printscript.snippets.rules

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.printscript.snippets.domain.model.RulesState
import com.printscript.snippets.dto.RuleDto
import com.printscript.snippets.enums.RulesType
import com.printscript.snippets.service.rules.LintRuleStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

class LintRuleStrategyTest {

    private val strategy = LintRuleStrategy()

    @Test
    fun defaultEnabled_tiene2Reglas() {
        val enabled = strategy.defaultEnabled()
        assertEquals(
            setOf(
                "IdentifierStyleRuleStreaming",
                "PrintlnSimpleArgRuleStreaming",
            ),
            enabled,
        )
    }

    @Test
    fun defaultValues_esVacio() {
        assertTrue(strategy.defaultValues().isEmpty())
    }

    @Test
    fun allRuleIds_enOrdenCorrecto() {
        val ids = strategy.allRuleIds()
        assertEquals(
            listOf(
                "IdentifierStyleRuleStreaming",
                "PrintlnSimpleArgRuleStreaming",
                "ReadInputSimpleArgRuleStreaming",
            ),
            ids,
        )
    }

    @Test
    fun toRuleDtos_mapeaEnabledYCopiaValueString() {
        val enabled = setOf("IdentifierStyleRuleStreaming")
        val values = mapOf(
            "IdentifierStyleRuleStreaming" to "CAMEL_CASE",
            "PrintlnSimpleArgRuleStreaming" to "x",
            "ReadInputSimpleArgRuleStreaming" to null,
        )

        val dtos = strategy.toRuleDtos(enabled, values)

        val idRule = dtos.first { it.id == "IdentifierStyleRuleStreaming" }
        assertTrue(idRule.enabled)
        assertEquals("CAMEL_CASE", idRule.value)

        val printlnRule = dtos.first { it.id == "PrintlnSimpleArgRuleStreaming" }
        assertFalse(printlnRule.enabled)
        assertEquals("x", printlnRule.value)

        val readInput = dtos.first { it.id == "ReadInputSimpleArgRuleStreaming" }
        assertFalse(readInput.enabled)
        assertNull(readInput.value)
    }

    @Test
    fun toRuleDtos_convierteNumberAInt() {
        val enabled = emptySet<String>()
        val values = mapOf(
            "IdentifierStyleRuleStreaming" to 7,
        )

        val dtos = strategy.toRuleDtos(enabled, values)

        val idRule = dtos.first { it.id == "IdentifierStyleRuleStreaming" }
        assertEquals(7, idRule.value)
    }

    @Test
    fun buildStateFromDtos_extraeEnabledYOptions() {
        val rules = listOf(
            RuleDto("IdentifierStyleRuleStreaming", true, "CAMEL_CASE"),
            RuleDto("PrintlnSimpleArgRuleStreaming", false, null),
            RuleDto("ReadInputSimpleArgRuleStreaming", true, "hola"),
        )

        val state = strategy.buildStateFromDtos(
            rules,
            configText = "   { x:1 }   ",
            configFormat = "json",
        )

        assertEquals(
            setOf("IdentifierStyleRuleStreaming", "ReadInputSimpleArgRuleStreaming"),
            state.enabled,
        )

        assertEquals(
            mapOf(
                "IdentifierStyleRuleStreaming" to "CAMEL_CASE",
                "ReadInputSimpleArgRuleStreaming" to "hola",
            ),
            state.options,
        )

        assertEquals("{ x:1 }", state.configText)
        assertEquals("json", state.configFormat)
    }

    @Test
    fun buildStateFromDtos_descartaConfigTextInvalido() {
        val state = strategy.buildStateFromDtos(
            emptyList(),
            configText = "{}",
            configFormat = null,
        )
        assertNull(state.configText)
        assertEquals("json", state.configFormat) // default
    }

    @Test
    fun buildEffectiveConfig_siHayRowUsaEseConfig() {
        val row = RulesState(
            id = UUID.randomUUID(),
            type = RulesType.LINT,
            ownerId = "u1",
            enabledJson = listOf("IdentifierStyleRuleStreaming"),
            optionsJson = emptyMap(),
            configText = """{ "abc": 1 }""",
            configFormat = "json",
        )

        val result = strategy.buildEffectiveConfig(row, emptyList())

        assertEquals("""{ "abc": 1 }""", result.first)
        assertEquals("json", result.second)
    }

    @Test
    fun buildEffectiveConfig_sinRowGeneraJSON() {
        val rules = listOf(
            RuleDto("IdentifierStyleRuleStreaming", true, "SNAKE_CASE"),
            RuleDto("PrintlnSimpleArgRuleStreaming", false, null),
            RuleDto("ReadInputSimpleArgRuleStreaming", true, null),
        )

        val (cfg, fmt) = strategy.buildEffectiveConfig(null, rules)

        val json = jacksonObjectMapper().readTree(cfg)

        assertEquals("json", fmt)

        assertEquals(true, json["identifiers"]["enabled"].booleanValue())
        assertEquals("SNAKE_CASE", json["identifiers"]["style"].textValue())

        // usa defaultEnabled() porque row es null:
        assertEquals(true, json["printlnRule"]["enabled"].booleanValue()) // <- corregido
        assertEquals(false, json["readInputRule"]["enabled"].booleanValue())
    }

    @Test
    fun buildEffectiveConfig_identifierDefaultCamelCase() {
        val rules = listOf(
            RuleDto("IdentifierStyleRuleStreaming", true, null),
        )

        val (cfg, _) = strategy.buildEffectiveConfig(null, rules)

        val json = jacksonObjectMapper().readTree(cfg)
        assertEquals("CAMEL_CASE", json["identifiers"]["style"].textValue())
    }

    @Test
    fun buildEffectiveConfig_identifierUppercaseAndNormalized() {
        val rules = listOf(
            RuleDto("IdentifierStyleRuleStreaming", true, "snake_case"),
        )

        val (cfg, _) = strategy.buildEffectiveConfig(null, rules)

        val json = jacksonObjectMapper().readTree(cfg)
        assertEquals("SNAKE_CASE", json["identifiers"]["style"].textValue())
    }

    @Test
    fun buildEffectiveConfig_readInputOffSiNoEstaEnabled() {
        val rules = listOf(
            RuleDto("IdentifierStyleRuleStreaming", true, null),
        )

        val (cfg, _) = strategy.buildEffectiveConfig(null, rules)

        val json = jacksonObjectMapper().readTree(cfg)
        assertFalse(json["readInputRule"]["enabled"].booleanValue())
    }
}
