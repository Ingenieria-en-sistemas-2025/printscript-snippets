package com.printscript.snippets.service.rules

import com.printscript.snippets.domain.RulesStateRepo
import com.printscript.snippets.domain.model.RulesState
import com.printscript.snippets.domain.model.RulesType
import com.printscript.snippets.dto.RuleDto
import org.springframework.stereotype.Service

@Service
class RulesStateService(
    private val rulesStateRepo: RulesStateRepo,
) {
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
    private fun readEnabled(type: RulesType, ownerId: String?): Set<String> =
        findRow(type, ownerId)?.enabledJson?.toSet() ?: emptySet()

    // Coercea options a Map<String, Int> desde Map<String, Any?> guardado
    private fun readOptions(type: RulesType, ownerId: String?): Map<String, Int> {
        val raw: Map<String, Any?> = findRow(type, ownerId)?.optionsJson ?: return emptyMap()
        return raw.mapNotNull { (k, v) ->
            when (v) {
                is Number -> k to v.toInt()
                is String -> v.toIntOrNull()?.let { k to it }
                else -> null
            }
        }.toMap()
    }

    fun saveFormatState(ownerId: String, rules: List<RuleDto>, configText: String?, configFormat: String?) {
        val enabled: Set<String> = rules.filter { it.enabled }.map { it.id }.toSet()
        val options: Map<String, Int> = rules.mapNotNull { r -> r.value?.let { v -> r.id to v } }.toMap()

        val row = upsertRow(RulesType.FORMAT, ownerId)
        row.enabledJson = enabled.toList()
        row.optionsJson = options.mapValues { it.value as Any }
        row.configText = configText
        row.configFormat = configFormat
        rulesStateRepo.save(row)
    }

    fun saveLintState(ownerId: String, rules: List<RuleDto>, configText: String?, configFormat: String?) {
        val enabled: Set<String> = rules.filter { it.enabled }.map { it.id }.toSet()

        val row = upsertRow(RulesType.LINT, ownerId)
        row.enabledJson = enabled.toList()
        row.optionsJson = null
        row.configText = configText
        row.configFormat = configFormat
        rulesStateRepo.save(row)
    }

    fun getFormatAsRules(ownerId: String): List<RuleDto> {
        val enabled = readEnabled(RulesType.FORMAT, ownerId).ifEmpty { defaultFormatEnabled() }
        val values = readOptions(RulesType.FORMAT, ownerId).ifEmpty { defaultFormatValues() }

        return allFormatIds.map { id ->
            val value = if (fmtNumericDefaults.containsKey(id)) values[id] ?: fmtNumericDefaults[id] else null
            RuleDto(id = id, enabled = enabled.contains(id), value = value)
        }
    }

    fun getLintAsRules(ownerId: String): List<RuleDto> {
        val enabled = readEnabled(RulesType.LINT, ownerId).ifEmpty { defaultLintEnabled() }
        return lintRules.distinct().map { id -> RuleDto(id = id, enabled = enabled.contains(id)) }
    }

    fun currentFormatConfig(ownerId: String): Pair<String?, String?> {
        val row = findRow(RulesType.FORMAT, ownerId)
        return row?.configText to row?.configFormat
    }

    fun currentLintConfig(ownerId: String): Pair<String?, String?> {
        val row = findRow(RulesType.LINT, ownerId)
        return row?.configText to row?.configFormat
    }
}
