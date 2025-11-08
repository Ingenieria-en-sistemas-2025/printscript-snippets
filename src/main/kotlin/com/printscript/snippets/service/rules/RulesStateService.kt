package com.printscript.snippets.service.rules

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.printscript.snippets.domain.RulesStateRepo
import com.printscript.snippets.domain.model.RulesState
import com.printscript.snippets.domain.model.RulesType
import com.printscript.snippets.dto.RuleDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RulesStateService(
    private val rulesStateRepo: RulesStateRepo
) {
    private val om = jacksonObjectMapper()

    private val FORMAT_BOOL_RULES = setOf(
        "enforce-spacing-around-equals",
        "enforce-no-spacing-around-equals",
        "enforce-spacing-after-colon-in-declaration",
        "enforce-spacing-before-colon-in-declaration",
        "indent-inside-if",
        "mandatory-single-space-separation",
        "if-brace-below-line",
        "if-brace-same-line",
    )

    // Numéricas (con valor editable en UI)  los vals van en optionsJson
    private val FORMAT_NUMERIC_DEFAULTS = mapOf(
        "indent_size" to 3,
        "indent-spaces" to 3,
        "tabsize" to 3,
        "line-breaks-after-println" to 0,
        "line_breaks_after_println" to 0,
    )

    private val LINT_RULES = listOf(
        "IdentifierStyleRuleStreaming",
        "PrintlnSimpleArgRuleStreaming",
        "ReadInputSimpleArgRuleStreaming"
    )

    private val ALL_FORMAT_IDS = (FORMAT_BOOL_RULES + FORMAT_NUMERIC_DEFAULTS.keys).toList().sorted()

    private fun defaultFormatEnabled(): Set<String> = setOf(
        "indent-spaces",
        "mandatory-single-space-separation",
        "if-brace-same-line"
    )

    private fun defaultFormatValues(): Map<String, Int> = FORMAT_NUMERIC_DEFAULTS

    private fun defaultLintEnabled(): Set<String> = setOf(
        "IdentifierStyleRuleStreaming",
        "PrintlnSimpleArgRuleStreaming"
    )


    // Lee desde la tabla rules_state -> enabledJson y lo parsea a Set<String>
    // Si no hay registro (Optional.empty), volvemos emptySet() y arriba aplicamos defaults.
    private fun readEnabled(type: RulesType): Set<String> =
        rulesStateRepo.findByType(type)
            .map { s -> om.readValue(s.enabledJson, Set::class.java) } //parsea json
            .orElse(emptySet<Any>())
            .map { it.toString() } //lo paso a string
            .toSet()

    // Lee desde la tabla rules_state -> optionsJson y lo parsea a Map<String, Int>
    // Admitimos que el JSON pueda venir con números o strings numéricos (por eso el when).
    private fun readOptions(type: RulesType): Map<String, Int> {

        val json = rulesStateRepo.findByType(type).map { it.optionsJson }.orElse(null) //para quedarse solo con el campo optionsJson
            ?: return emptyMap()

        val raw: Map<String, Any?> = om.readValue( //convierte json en map
            json,
            object : TypeReference<Map<String, Any?>>() {}
        )

        return raw.mapNotNull { (k, v) -> //
            when (v) { //normaliza los valores del JSON para asegurarse de que todos sean Int
                is Number -> k to v.toInt()
                is String -> v.toIntOrNull()?.let { k to it }
                else -> null
            }
        }.toMap()
    }

    fun saveFormatState(rules: List<RuleDto>, configText: String?, configFormat: String?) {
        // habilitadas = ids de reglas con enabled = true
        val enabled: Set<String> = rules.filter { it.enabled }.map { it.id }.toSet()

        // options numéricas: solo las que traen value
        val options: Map<String, Int> = rules.mapNotNull { r -> r.value?.let { v -> r.id to v } }.toMap()

        // upsert en la tabla
        val row = rulesStateRepo.findByType(RulesType.FORMAT).orElse(
            com.printscript.snippets.domain.model.RulesState(
                id = null,
                type = RulesType.FORMAT,
                enabledJson = "[]",
                optionsJson = null,
                configText = null,
                configFormat = null
            )
        )

        row.enabledJson = om.writeValueAsString(enabled)
        row.optionsJson = om.writeValueAsString(options)
        row.configText = configText
        row.configFormat = configFormat

        rulesStateRepo.save(row)
    }

    fun saveLintState(rules: List<RuleDto>, configText: String?, configFormat: String?) {
        val enabled: Set<String> = rules.filter { it.enabled }.map { it.id }.toSet()

        val row = rulesStateRepo.findByType(RulesType.LINT).orElse(
RulesState(
                id = null,
                type = RulesType.LINT,
                enabledJson = "[]",
                optionsJson = null,
                configText = null,
                configFormat = null
            )
        )

        row.enabledJson = om.writeValueAsString(enabled)
        row.optionsJson = null
        row.configText = configText
        row.configFormat = configFormat

        rulesStateRepo.save(row)
    }

    fun getFormatAsRules(): List<RuleDto> {
        val enabled = readEnabled(RulesType.FORMAT).ifEmpty { defaultFormatEnabled() }
        val values = readOptions(RulesType.FORMAT).ifEmpty { defaultFormatValues() }

        return ALL_FORMAT_IDS.map { id ->
            val value = if (FORMAT_NUMERIC_DEFAULTS.containsKey(id)) {
                values[id] ?: FORMAT_NUMERIC_DEFAULTS[id]
            } else null
            RuleDto(id = id, enabled = enabled.contains(id), value = value)
        }
    }

    fun getLintAsRules(): List<RuleDto> {
        val enabled = readEnabled(RulesType.LINT).ifEmpty { defaultLintEnabled() }
        return LINT_RULES.distinct().map { id -> RuleDto(id = id, enabled = enabled.contains(id)) }
    }

}