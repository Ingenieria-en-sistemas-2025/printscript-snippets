package com.printscript.snippets.service.rules

import com.printscript.snippets.dto.RuleDto
import io.printscript.contracts.formatter.FormatterOptionsDto

object FormatterMapper {

    fun toFormatterOptionsDto(rules: List<RuleDto>): FormatterOptionsDto =
        FormatterOptionsDto(
            spaceBeforeColonInDecl = enabled(rules, FormatRuleId.SPACE_BEFORE_COLON_IN_DECL.id),
            spaceAfterColonInDecl = enabled(rules, FormatRuleId.SPACE_AFTER_COLON_IN_DECL.id),
            spaceAroundAssignment = assignmentSpacing(rules),
            blankLinesAfterPrintln = numericValue(
                rules,
                FormatRuleId.LINE_BREAKS_AFTER_PRINTLN.id,
                "line_breaks_after_println", // alias legacy
            ),
            indentSpaces = numericValue(
                rules,
                FormatRuleId.INDENT_SPACES.id,
                FormatRuleId.INDENT_SIZE.id,
                FormatRuleId.TABSIZE.id,
            ),
            mandatorySingleSpaceSeparation = enabled(
                rules,
                FormatRuleId.MANDATORY_SINGLE_SPACE_SEPARATION.id,
            ),
            ifBraceBelowLine = enabled(rules, FormatRuleId.IF_BRACE_BELOW_LINE.id),
            ifBraceSameLine = enabled(rules, FormatRuleId.IF_BRACE_SAME_LINE.id),
        )

    private fun enabled(rules: List<RuleDto>, id: String): Boolean? =
        if (rules.any { it.id == id && it.enabled }) true else null

    private fun assignmentSpacing(rules: List<RuleDto>): Boolean? = when {
        rules.any {
            it.id == FormatRuleId.SPACING_AROUND_EQUALS.id && it.enabled
        } -> true
        rules.any {
            it.id == FormatRuleId.NO_SPACING_AROUND_EQUALS.id && it.enabled
        } -> false
        else -> null
    }

    private fun numericValue(rules: List<RuleDto>, vararg ids: String): Int? =
        rules.firstOrNull { ids.contains(it.id) }?.let { rule ->
            when (val v = rule.value) {
                is Number -> v.toInt()
                is String -> v.toIntOrNull()
                else -> null
            }
        }
}
