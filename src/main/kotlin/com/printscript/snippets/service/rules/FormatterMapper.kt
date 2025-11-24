package com.printscript.snippets.service.rules

import com.printscript.snippets.dto.RuleDto
import io.printscript.contracts.formatter.FormatterOptionsDto
//mm
object FormatterMapper {

    fun toFormatterOptionsDto(rules: List<RuleDto>): FormatterOptionsDto =
        FormatterOptionsDto(
            spaceBeforeColonInDecl = enabled(rules, "enforce-spacing-before-colon-in-declaration"),
            spaceAfterColonInDecl = enabled(rules, "enforce-spacing-after-colon-in-declaration"),
            spaceAroundAssignment = assignmentSpacing(rules),
            blankLinesAfterPrintln = numericValue(rules, "line-breaks-after-println", "line_breaks_after_println"),
            indentSpaces = numericValue(rules, "indent-spaces", "indent_size", "tabsize"),
            mandatorySingleSpaceSeparation = enabled(rules, "mandatory-single-space-separation"),
            ifBraceBelowLine = enabled(rules, "if-brace-below-line"),
            ifBraceSameLine = enabled(rules, "if-brace-same-line"),
        )

    private fun enabled(rules: List<RuleDto>, id: String): Boolean? =
        if (rules.any { it.id == id && it.enabled }) true else null

    private fun assignmentSpacing(rules: List<RuleDto>): Boolean? = when {
        rules.any { it.id == "enforce-spacing-around-equals" && it.enabled } -> true
        rules.any { it.id == "enforce-no-spacing-around-equals" && it.enabled } -> false
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
