package com.printscript.snippets.service.rules

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.printscript.snippets.domain.model.RulesState
import com.printscript.snippets.dto.RuleDto
import com.printscript.snippets.enums.RulesType

internal class LintRuleStrategy : RuleTypeStrategy {
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

    override fun buildStateFromDtos(
        rules: List<RuleDto>,
        rawConfigText: String?,
        rawConfigFormat: String?,
    ): RuleStatePieces {
        val enabled: Set<String> = rules.filter { it.enabled }.map { it.id }.toSet()
        val options: Map<String, Any?> = rules.mapNotNull { r ->
            r.value?.let { v -> r.id to v }
        }.toMap()

        // acá podés decidir si querés normalizar o no
        val format = rawConfigFormat ?: "json"

        return RuleStatePieces(
            enabled = enabled,
            options = options,
            configText = rawConfigText, // en tu caso hoy siempre null, pero queda preparado
            configFormat = format,
        )
    }

    override fun buildEffectiveConfig(
        row: RulesState?,
        rules: List<RuleDto>,
    ): Pair<String, String> {
        val enabledFromRules: Set<String> =
            rules.filter { it.enabled }.map { it.id }.toSet()

        val cfgText: String = buildLintConfigFromEnabled(enabledFromRules, rules)

        val cfgFmt: String = "json"

        return cfgText to cfgFmt
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
