package com.printscript.snippets.service.rules

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.printscript.snippets.domain.model.RulesState
import com.printscript.snippets.dto.RuleDto
import com.printscript.snippets.enums.RulesType
import org.springframework.stereotype.Component

@Component
class LintRuleStrategy : RuleTypeStrategy {

    override val type: RulesType = RulesType.LINT

    override fun defaultEnabled(): Set<String> =
        setOf(
            LintRuleId.IDENTIFIER_STYLE.id,
            LintRuleId.PRINTLN_SIMPLE_ARG.id,
        )

    override fun defaultValues(): Map<String, Any?> = emptyMap()

    override fun allRuleIds(): List<String> =
        LintRuleId.entries.map { it.id }

    override fun toRuleDtos(
        enabled: Set<String>,
        values: Map<String, Any?>,
    ): List<RuleDto> =
        LintRuleId.entries.map { rule ->
            val raw = values[rule.id]
            val value = when (raw) {
                is String -> raw
                is Number -> raw.toInt()
                else -> null
            }
            RuleDto(
                id = rule.id,
                enabled = enabled.contains(rule.id),
                value = value,
            )
        }

    override fun buildEffectiveConfig(
        row: RulesState?,
        rules: List<RuleDto>,
    ): Pair<String, String> {
        val enabledFromRow = row?.enabledJson?.toSet() ?: defaultEnabled()

        val cfgText =
            RuleHelpers.configTextOrNull(row?.configText)
                ?: buildLintConfigFromEnabled(enabledFromRow, rules)

        val cfgFmt = RuleHelpers.defaultConfigFormat(row?.configFormat)

        return cfgText to cfgFmt
    }

    override fun buildStateFromDtos(
        rules: List<RuleDto>,
        configText: String?,
        configFormat: String?,
    ): RuleStatePieces {
        val enabled = RuleHelpers.enabledSet(rules)

        val options = rules
            .mapNotNull { r -> r.value?.let { v -> r.id to v } }
            .toMap()

        val normalizedConfigText = RuleHelpers.configTextOrNull(configText)

        return RuleStatePieces(
            enabled = enabled,
            options = options,
            configText = normalizedConfigText,
            configFormat = RuleHelpers.defaultConfigFormat(configFormat),
        )
    }

    private fun buildLintConfigFromEnabled(
        enabled: Set<String>,
        rules: List<RuleDto>,
    ): String {
        val identifierRule = rules.firstOrNull { it.id == LintRuleId.IDENTIFIER_STYLE.id }
        val identifierValue = identifierRule?.value?.toString()?.uppercase()

        val style = when (identifierValue) {
            "SNAKE_CASE" -> "SNAKE_CASE"
            "CAMEL_CASE" -> "CAMEL_CASE"
            else -> "CAMEL_CASE"
        }

        val cfg = mapOf(
            "identifiers" to mapOf(
                "enabled" to enabled.contains(LintRuleId.IDENTIFIER_STYLE.id),
                "style" to style,
            ),
            "printlnRule" to mapOf(
                "enabled" to enabled.contains(LintRuleId.PRINTLN_SIMPLE_ARG.id),
            ),
            "readInputRule" to mapOf(
                "enabled" to enabled.contains(LintRuleId.READ_INPUT_SIMPLE_ARG.id),
            ),
        )

        return jacksonObjectMapper().writeValueAsString(cfg)
    }
}
