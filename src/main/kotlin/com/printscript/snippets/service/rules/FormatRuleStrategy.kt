package com.printscript.snippets.service.rules

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.printscript.snippets.domain.model.RulesState
import com.printscript.snippets.dto.RuleDto
import com.printscript.snippets.enums.RulesType
import io.printscript.contracts.formatter.FormatterOptionsDto
import org.springframework.stereotype.Component

@Component
class FormatRuleStrategy : RuleTypeStrategy {
    companion object {
        private const val DEFAULT_INDENT = 3
        private const val DEFAULT_TABSIZE = 3
        private const val DEFAULT_PRINTLN_BREAKS = 0
    }
    private val formatBoolRules: Set<String> = setOf(
        FormatRuleId.SPACING_AROUND_EQUALS.id,
        FormatRuleId.NO_SPACING_AROUND_EQUALS.id,
        FormatRuleId.SPACE_AFTER_COLON_IN_DECL.id,
        FormatRuleId.SPACE_BEFORE_COLON_IN_DECL.id,
        FormatRuleId.INDENT_INSIDE_IF.id,
        FormatRuleId.MANDATORY_SINGLE_SPACE_SEPARATION.id,
        FormatRuleId.IF_BRACE_BELOW_LINE.id,
        FormatRuleId.IF_BRACE_SAME_LINE.id,
    )

    private val formatNumericDefaults: Map<String, Int> = mapOf(
        FormatRuleId.INDENT_SIZE.id to DEFAULT_INDENT,
        FormatRuleId.INDENT_SPACES.id to DEFAULT_INDENT,
        FormatRuleId.TABSIZE.id to DEFAULT_TABSIZE,
        FormatRuleId.LINE_BREAKS_AFTER_PRINTLN.id to DEFAULT_PRINTLN_BREAKS,
    )

    private val allIds: List<String> =
        (formatBoolRules + formatNumericDefaults.keys).toList().sorted()

    override val type: RulesType = RulesType.FORMAT

    override fun defaultEnabled(): Set<String> = setOf(
        FormatRuleId.INDENT_SPACES.id,
        FormatRuleId.MANDATORY_SINGLE_SPACE_SEPARATION.id,
        FormatRuleId.IF_BRACE_SAME_LINE.id,
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
