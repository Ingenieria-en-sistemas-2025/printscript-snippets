package com.printscript.snippets.service.rules

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.printscript.snippets.domain.model.RulesState
import com.printscript.snippets.dto.RuleDto
import com.printscript.snippets.enums.RulesType
import io.printscript.contracts.formatter.FormatterOptionsDto

internal class FormatRuleStrategy : RuleTypeStrategy {
    companion object {
        private const val DEFAULT_INDENT = 3
        private const val DEFAULT_TABSIZE = 3
        private const val DEFAULT_PRINTLN_BREAKS = 0
    }

    private val formatBoolRules = setOf(
        "enforce-spacing-around-equals",
        "enforce-no-spacing-around-equals",
        "enforce-spacing-after-colon-in-declaration",
        "enforce-spacing-before-colon-in-declaration",
        "indent-inside-if",
        "mandatory-single-space-separation",
        "if-brace-below-line",
        "if-brace-same-line",
    )

    private val formatNumericDefaults: Map<String, Int> = mapOf(
        "indent_size" to DEFAULT_INDENT,
        "indent-spaces" to DEFAULT_INDENT,
        "tabsize" to DEFAULT_TABSIZE,
        "line-breaks-after-println" to DEFAULT_PRINTLN_BREAKS,
    )

    private val allIds: List<String> = (formatBoolRules + formatNumericDefaults.keys).toList().sorted()

    override val type: RulesType = RulesType.FORMAT

    override fun defaultEnabled(): Set<String> = setOf(
        "indent-spaces",
        "mandatory-single-space-separation",
        "if-brace-same-line",
    )

    override fun defaultValues(): Map<String, Any?> = formatNumericDefaults

    override fun allRuleIds(): List<String> = allIds

    // construye la lista de RuleDto que va a usar la ui y el dominio
    override fun toRuleDtos(
        enabled: Set<String>,
        values: Map<String, Any?>,
    ): List<RuleDto> {
        val numDefaults = defaultValues()

        return allRuleIds().map { id ->
            val raw = values[id] ?: numDefaults[id]
            val intVal = RuleHelpers.numericOptionValue(raw)

            RuleDto(
                id = id,
                enabled = enabled.contains(id),
                value = intVal,
            )
        }
    }

    // construye el config efectivo para el formatter
    override fun buildEffectiveConfig(
        row: RulesState?,
        rules: List<RuleDto>,
    ): Pair<String, String> {
        val cfgText: String =
            RuleHelpers.configTextOrNull(row?.configText)
                ?: buildFormatterConfigFromRules(rules)

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
                    RuleHelpers.numericOptionValue(r.value)?.let { r.id to it }
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

    private fun buildFormatterConfigFromRules(rules: List<RuleDto>): String {
        val opts: FormatterOptionsDto = FormatterMapper.toFormatterOptionsDto(rules)
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
        return jacksonObjectMapper().writeValueAsString(config)
    }
}
