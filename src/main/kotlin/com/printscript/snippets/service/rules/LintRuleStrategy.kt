package com.printscript.snippets.service.rules

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.printscript.snippets.domain.model.RulesState
import com.printscript.snippets.dto.RuleDto
import com.printscript.snippets.enums.RulesType
import org.springframework.stereotype.Component

@Component
class LintRuleStrategy : RuleTypeStrategy {
    private val lintRules = listOf(
        "IdentifierStyleRuleStreaming",
        "PrintlnSimpleArgRuleStreaming",
        "ReadInputSimpleArgRuleStreaming",
    )

    override val type: RulesType = RulesType.LINT

    override fun defaultEnabled(): Set<String> = setOf(
        "IdentifierStyleRuleStreaming",
        "PrintlnSimpleArgRuleStreaming",
    )

    override fun defaultValues(): Map<String, Any?> = emptyMap()

    override fun allRuleIds(): List<String> = lintRules.distinct()

    override fun toRuleDtos(
        enabled: Set<String>,
        values: Map<String, Any?>,
    ): List<RuleDto> =
        allRuleIds().map { id ->
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

    override fun buildEffectiveConfig(
        row: RulesState?,
        rules: List<RuleDto>,
    ): Pair<String, String> {
        val enabledFromRow: Set<String> =
            row?.enabledJson?.toSet() ?: defaultEnabled()

        val cfgText: String =
            RuleHelpers.configTextOrNull(row?.configText)
                ?: buildLintConfigFromEnabled(enabledFromRow, rules)

        val cfgFmt: String = RuleHelpers.defaultConfigFormat(row?.configFormat)

        return cfgText to cfgFmt
    }

    override fun buildStateFromDtos(
        rules: List<RuleDto>,
        configText: String?,
        configFormat: String?,
    ): RuleStatePieces {
        val enabled = RuleHelpers.enabledSet(rules)

        val options: Map<String, Any?> =
            rules
                .mapNotNull { r ->
                    r.value?.let { v -> r.id to v }
                }
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
        val identifierValue: String? = rules
            .firstOrNull { it.id == "IdentifierStyleRuleStreaming" }
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
        return jacksonObjectMapper().writeValueAsString(cfg)
    }
}
