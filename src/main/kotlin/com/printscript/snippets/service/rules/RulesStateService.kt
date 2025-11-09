package com.printscript.snippets.service.rules

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.printscript.snippets.domain.RulesStateRepo
import com.printscript.snippets.domain.model.RulesState
import com.printscript.snippets.domain.model.RulesType
import com.printscript.snippets.dto.RuleDto
import org.springframework.stereotype.Service

@Service
class RulesStateService(
    private val rulesStateRepo: RulesStateRepo,
) {

    private val om = jacksonObjectMapper()

    private companion object {
        private const val DEFAULT_INDENT = 3
        private const val DEFAULT_TABSIZE = 3
        private const val DEFAULT_PRINTLN_BREAKS = 0
    }

    private val fmtBoolRules = setOf(
        "enforce-spacing-around-equals",
        "enforce-no-spacing-around-equals",
        "enforce-spacing-after-colon-in-declaration",
        "enforce-spacing-before-colon-in-declaration",
        "indent-inside-if",
        "mandatory-single-space-separation",
        "if-brace-below-line",
        "if-brace-same-line",
    )

    // Numéricas (van en optionsJson)
    private val fmtNumericDefaults = mapOf(
        "indent_size" to DEFAULT_INDENT,
        "indent-spaces" to DEFAULT_INDENT,
        "tabsize" to DEFAULT_TABSIZE,
        "line-breaks-after-println" to DEFAULT_PRINTLN_BREAKS,
        "line_breaks_after_println" to DEFAULT_PRINTLN_BREAKS,
    )

    private val lintRules = listOf(
        "IdentifierStyleRuleStreaming",
        "PrintlnSimpleArgRuleStreaming",
        "ReadInputSimpleArgRuleStreaming",
    )

    private val allFormatIds = (fmtBoolRules + fmtNumericDefaults.keys).toList().sorted()

    private fun defaultFormatEnabled(): Set<String> = setOf(
        "indent-spaces",
        "mandatory-single-space-separation",
        "if-brace-same-line",
    )

    private fun defaultFormatValues(): Map<String, Int> = fmtNumericDefaults

    private fun defaultLintEnabled(): Set<String> = setOf(
        "IdentifierStyleRuleStreaming",
        "PrintlnSimpleArgRuleStreaming",
    )

    private fun findRow(type: RulesType, ownerId: String?): RulesState? =
        rulesStateRepo.findByTypeAndOwnerId(type, ownerId).orElseGet {
            rulesStateRepo.findByTypeAndOwnerId(type, null).orElse(null)
        }

    private fun upsertRow(type: RulesType, ownerId: String?): RulesState =
        rulesStateRepo.findByTypeAndOwnerId(type, ownerId).orElse(
            RulesState(
                id = null,
                type = type,
                ownerId = ownerId,
                enabledJson = emptyList(),
                optionsJson = null,
                configText = null,
                configFormat = null,
            ),
        )

    // Lee enabled directamente (Hibernate ya deserializa JSON → List<String>)
    private fun readEnabled(type: RulesType, ownerId: String?): Set<String> {
        val row = findRow(type, ownerId)
        if (row == null) {
            return when (type) {
                RulesType.FORMAT -> defaultFormatEnabled()
                RulesType.LINT -> defaultLintEnabled()
            }
        }
        return row.enabledJson.toSet()
    }

    private fun readOptions(type: RulesType, ownerId: String?): Map<String, Any?> {
        val row = findRow(type, ownerId)
        val raw = row?.optionsJson ?: return emptyMap()
        return raw.filterValues { it != null }
    }

    fun saveFormatState(ownerId: String, rules: List<RuleDto>, configText: String?, configFormat: String?) {
        val enabled: Set<String> = rules.filter { it.enabled }.map { it.id }.toSet()
        val options: Map<String, Int> = rules.mapNotNull { r ->
            val intValue = when (val v = r.value) {
                is Number -> v.toInt()
                is String -> v.toIntOrNull()
                else -> null
            }
            intValue?.let { r.id to it }
        }.toMap()

        val normalizedConfigText = configText
            ?.trim()
            ?.takeUnless { it.isEmpty() || it == "{}" }

        val normalizedConfigFormat = when (configFormat?.lowercase()) {
            "yaml", "yml" -> "yaml"
            "json" -> "json"
            null, "" -> "json"
            else -> "json"
        }

        val row = upsertRow(RulesType.FORMAT, ownerId)
        row.enabledJson = enabled.toList()
        row.optionsJson = options.ifEmpty { null }
        row.configText = normalizedConfigText
        row.configFormat = normalizedConfigFormat
        rulesStateRepo.save(row)
    }

    fun saveLintState(ownerId: String, rules: List<RuleDto>, configText: String?, configFormat: String?) {
        val enabled: Set<String> = rules.filter { it.enabled }.map { it.id }.toSet()
        val options: Map<String, Any> = rules.mapNotNull { r ->
            r.value?.let { v -> r.id to v }
        }.toMap()

        val row = upsertRow(RulesType.LINT, ownerId)
        row.enabledJson = enabled.toList()
        row.optionsJson = options.ifEmpty { null }
        row.configText = configText
        row.configFormat = configFormat
        rulesStateRepo.save(row)
    }

    fun getFormatAsRules(ownerId: String): List<RuleDto> {
        val enabled = readEnabled(RulesType.FORMAT, ownerId)
        val values = readOptions(RulesType.FORMAT, ownerId)
        val defaults = defaultFormatValues()

        return allFormatIds.map { id ->
            val raw = values[id] ?: defaults[id]
            val intVal = when (raw) {
                is Number -> raw.toInt()
                is String -> raw.toIntOrNull()
                else -> null
            }
            RuleDto(id = id, enabled = enabled.contains(id), value = intVal)
        }
    }

    fun getLintAsRules(ownerId: String): List<RuleDto> {
        val enabled = readEnabled(RulesType.LINT, ownerId)
        val values = readOptions(RulesType.LINT, ownerId)

        return lintRules.distinct().map { id ->
            val raw = values[id]
            val value: Any? = when (raw) {
                is String -> raw
                is Number -> raw.toInt()
                else -> null
            }

            RuleDto(
                id = id,
                enabled = enabled.contains(id),
                value = value,
            )
        }
    }

    fun currentFormatConfig(ownerId: String): Pair<String?, String?> {
        val row = findRow(RulesType.FORMAT, ownerId)

        val cfgText: String? = row
            ?.configText
            ?.takeUnless { it.isBlank() || it == "{}" }

        val cfgFmt: String? = row?.configFormat

        return cfgText to cfgFmt
    }

    fun currentLintConfig(ownerId: String): Pair<String?, String?> {
        val row = findRow(RulesType.LINT, ownerId)
        return row?.configText to row?.configFormat
    }

    fun buildFormatterConfigFromRules(rules: List<RuleDto>): String {
        val opts = FormatterMapper.toFormatterOptionsDto(rules)
        val config = mapOf(
            "spaceBeforeColonInDecl" to opts.spaceBeforeColonInDecl,
            "spaceAfterColonInDecl" to opts.spaceAfterColonInDecl,
            "spaceAroundAssignment" to opts.spaceAroundAssignment,
            "blankLinesAfterPrintln" to opts.blankLinesAfterPrintln,
            "indentSpaces" to opts.indentSpaces,
            "mandatorySingleSpaceSeparation" to opts.mandatorySingleSpaceSeparation,
            "ifBraceBelowLine" to opts.ifBraceBelowLine,
            "ifBraceSameLine" to opts.ifBraceSameLine,
        )
        return om.writeValueAsString(config)
    }

    private fun buildLintConfigFromEnabled(enabled: Set<String>, rules: List<RuleDto>? = null): String {
        val identifierValue: String? = rules
            ?.firstOrNull { it.id == "IdentifierStyleRuleStreaming" }
            ?.value
            ?.toString()
            ?.uppercase()

        val style = when (identifierValue) {
            "SNAKE_CASE" -> "SNAKE_CASE"
            "CAMEL_CASE" -> "CAMEL_CASE"
            else -> "CAMEL_CASE"
        }

        val cfg = mapOf(
            "identifiers" to mapOf(
                "enabled" to enabled.contains("IdentifierStyleRuleStreaming"),
                "style" to style,
            ),
            "printlnRule" to mapOf(
                "enabled" to enabled.contains("PrintlnSimpleArgRuleStreaming"),
            ),
            "readInputRule" to mapOf(
                "enabled" to enabled.contains("ReadInputSimpleArgRuleStreaming"),
            ),
        )
        return om.writeValueAsString(cfg)
    }

    fun currentLintConfigEffective(ownerId: String): Pair<String, String> {
        val row = findRow(RulesType.LINT, ownerId)
        val enabled: Set<String> = row?.enabledJson?.toSet() ?: defaultLintEnabled()
        val rules = getLintAsRules(ownerId)

        val cfgText: String = row
            ?.configText
            ?.takeUnless { it.isBlank() || it == "{}" }
            ?: buildLintConfigFromEnabled(enabled, rules)

        val cfgFmt: String = row?.configFormat ?: "json"

        return cfgText to cfgFmt
    }
}
