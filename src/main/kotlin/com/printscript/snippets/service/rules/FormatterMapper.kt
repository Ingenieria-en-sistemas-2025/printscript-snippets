package com.printscript.snippets.service.rules

import com.printscript.snippets.dto.RuleDto
import com.printscript.snippets.execution.dto.FormatterOptionsDto

object FormatterMapper {
    fun toFormatterOptionsDto(rules: List<RuleDto>): FormatterOptionsDto =
        FormatterOptionsDto(
            spaceBeforeColonInDecl = rules.any { it.id == "enforce-spacing-before-colon-in-declaration" && it.enabled },
            spaceAfterColonInDecl = rules.any { it.id == "enforce-spacing-after-colon-in-declaration" && it.enabled },
            spaceAroundAssignment = when {
                rules.any { it.id == "enforce-spacing-around-equals" && it.enabled } -> true
                rules.any { it.id == "enforce-no-spacing-around-equals" && it.enabled } -> false
                else -> null
            },
            blankLinesAfterPrintln = rules.firstOrNull {
                it.id == "line-breaks-after-println" || it.id == "line_breaks_after_println"
            }?.value,
            indentSpaces = rules.firstOrNull {
                it.id == "indent-spaces" || it.id == "indent_size" || it.id == "tabsize"
            }?.value,
            mandatorySingleSpaceSeparation = rules.any { it.id == "mandatory-single-space-separation" && it.enabled },
            ifBraceBelowLine = rules.any { it.id == "if-brace-below-line" && it.enabled },
            ifBraceSameLine = rules.any { it.id == "if-brace-same-line" && it.enabled }
        )
}