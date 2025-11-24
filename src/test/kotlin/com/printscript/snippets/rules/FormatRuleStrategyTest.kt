package com.printscript.snippets.rules


import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.printscript.snippets.domain.model.RulesState
import com.printscript.snippets.dto.RuleDto
import com.printscript.snippets.enums.RulesType
import com.printscript.snippets.service.rules.FormatRuleStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FormatRuleStrategyTest {

    private val strategy = FormatRuleStrategy()
    private val mapper = jacksonObjectMapper()

    private fun row(
        enabled: List<String> = emptyList(),
        options: Map<String, Any?>? = null,
        text: String? = null,
        fmt: String? = null,
    ) = RulesState(
        type = RulesType.FORMAT,
        ownerId = "auth0|x",
        enabledJson = enabled,
        optionsJson = options,
        configText = text,
        configFormat = fmt,
    )

    private fun r(id: String, enabled: Boolean, value: Any?) =
        RuleDto(id = id, enabled = enabled, value = value)


    @Test
    fun `defaultEnabled tiene reglas conocidas`() {
        val d = strategy.defaultEnabled()
        assertTrue("indent-spaces" in d)
        assertTrue("mandatory-single-space-separation" in d)
        assertTrue("if-brace-same-line" in d)
    }

    @Test
    fun `defaultValues contiene numeric defaults correctos`() {
        val dv = strategy.defaultValues()
        assertEquals(3, dv["indent_size"])
        assertEquals(3, dv["indent-spaces"])
        assertEquals(3, dv["tabsize"])
        assertEquals(0, dv["line-breaks-after-println"])
    }

    @Test
    fun `allRuleIds incluye bools y numericos y está ordenado`() {
        val ids = strategy.allRuleIds()
        assertTrue(ids.size > 5)
        assertEquals(ids.sorted(), ids) // ordenados
    }


    @Test
    fun `toRuleDtos mapea enabled y numeric defaults`() {
        val enabled = setOf("indent-spaces", "if-brace-below-line")
        val values = mapOf("indent-spaces" to 7)

        val dtos = strategy.toRuleDtos(enabled, values)

        val indent = dtos.find { it.id == "indent-spaces" }!!
        assertTrue(indent.enabled)
        assertEquals(7, indent.value)

        val below = dtos.find { it.id == "if-brace-below-line" }!!
        assertTrue(below.enabled)
        assertNull(below.value) // no numeric default

        val tab = dtos.find { it.id == "tabsize" }!!
        assertFalse(tab.enabled)
        assertEquals(3, tab.value) // default numeric
    }

    @Test
    fun `toRuleDtos convierte string numerico a int y descarta string invalido`() {
        val enabled = emptySet<String>()
        val values = mapOf(
            "indent_size" to "8",
            "tabsize" to "hola",
        )

        val dtos = strategy.toRuleDtos(enabled, values)

        val indent = dtos.find { it.id == "indent_size" }!!
        assertEquals(8, indent.value)          // "8" -> 8

        val tab = dtos.find { it.id == "tabsize" }!!
        assertNull(tab.value)                  // "hola" -> null, se descarta
    }


    @Test
    fun `buildStateFromDtos arma enabled + options + config normalizado`() {
        val rules = listOf(
            r("indent-spaces", true, 5),
            r("tabsize", false, 2),
            r("if-brace-below-line", true, null)
        )

        val pieces = strategy.buildStateFromDtos(
            rules = rules,
            configText = "   {hello}   ",
            configFormat = "yaml"
        )

        assertEquals(setOf("indent-spaces", "if-brace-below-line"), pieces.enabled)

        assertEquals(mapOf(
            "indent-spaces" to 5,
            "tabsize" to 2
        ), pieces.options)

        assertEquals("{hello}", pieces.configText)
        assertEquals("yaml", pieces.configFormat)
    }

    @Test
    fun `buildStateFromDtos ignora config vacia o {}`() {
        val rules = listOf(r("indent_size", true, 4))

        val p1 = strategy.buildStateFromDtos(rules, "", "json")
        assertNull(p1.configText)

        val p2 = strategy.buildStateFromDtos(rules, "{}", "json")
        assertNull(p2.configText)

        val p3 = strategy.buildStateFromDtos(rules, "   {}   ", "json")
        assertNull(p3.configText)
    }

    @Test
    fun `buildStateFromDtos normaliza configFormat nulo a json`() {
        val rules = emptyList<RuleDto>()
        val pieces = strategy.buildStateFromDtos(rules, null, null)

        assertEquals("json", pieces.configFormat)
    }


    @Test
    fun `buildEffectiveConfig usa config del row si existe`() {
        val row = row(
            enabled = listOf("indent-spaces"),
            options = mapOf("indent-spaces" to 2),
            text = "{mycfg}",
            fmt = "yaml"
        )

        val rules = listOf(r("indent-spaces", true, 2))

        val (text, fmt) = strategy.buildEffectiveConfig(row, rules)

        assertEquals("{mycfg}", text)
        assertEquals("yaml", fmt)
    }

    @Test
    fun `buildEffectiveConfig genera JSON si no hay configText`() {
        val row = row(
            text = null,
            options = null,
            fmt = null
        )

        val rules = listOf(
            r("indent-spaces", true, 3),
            r("mandatory-single-space-separation", true, null)
        )

        val (text, fmt) = strategy.buildEffectiveConfig(row, rules)

        assertEquals("json", fmt)

        val parsed = mapper.readValue(text, Map::class.java)

        // ensure keys exist & are ints/bools
        assertTrue(parsed.containsKey("indentSpaces"))
        assertTrue(parsed.containsKey("mandatorySingleSpaceSeparation"))
    }

    @Test
    fun `buildEffectiveConfig ignora configText vacío o {}`() {
        val row1 = row(text = "")
        val row2 = row(text = "{}")
        val row3 = row(text = "   {}   ")

        val rules = listOf(r("indent-spaces", true, 3))

        val r1 = strategy.buildEffectiveConfig(row1, rules)
        val r2 = strategy.buildEffectiveConfig(row2, rules)
        val r3 = strategy.buildEffectiveConfig(row3, rules)

        assertFalse(r1.first.isBlank())
        assertFalse(r2.first.isBlank())
        assertFalse(r3.first.isBlank())
    }
}