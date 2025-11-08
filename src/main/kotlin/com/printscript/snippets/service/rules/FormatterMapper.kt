package com.printscript.snippets.service.rules

import com.printscript.snippets.dto.RuleDto
import com.printscript.snippets.execution.dto.FormatterOptionsDto

object FormatterMapper {
    fun toFormatterOptionsDto(rules: List<RuleDto>): FormatterOptionsDto =
        FormatterOptionsDto(
            // Si está activa -> true; si no -> null (no tocar)
            spaceBeforeColonInDecl = if (rules.any { it.id == "enforce-spacing-before-colon-in-declaration" && it.enabled }) true else null,
            spaceAfterColonInDecl = if (rules.any { it.id == "enforce-spacing-after-colon-in-declaration" && it.enabled }) true else null,

            // Mutuamente excluyentes; si ninguna está activa -> null
            spaceAroundAssignment = when {
                rules.any { it.id == "enforce-spacing-around-equals" && it.enabled } -> true
                rules.any { it.id == "enforce-no-spacing-around-equals" && it.enabled } -> false
                else -> null
            },

            // Numéricas: toma el primer alias que venga con value
            blankLinesAfterPrintln = rules.firstOrNull {
                it.id == "line-breaks-after-println" || it.id == "line_breaks_after_println"
            }?.value,

            indentSpaces = rules.firstOrNull {
                it.id == "indent-spaces" || it.id == "indent_size" || it.id == "tabsize"
            }?.value,

            // Otros flags → true si están activos; null si no (no tocar)
            mandatorySingleSpaceSeparation = if (rules.any { it.id == "mandatory-single-space-separation" && it.enabled }) true else null,
            ifBraceBelowLine = if (rules.any { it.id == "if-brace-below-line" && it.enabled }) true else null,
            ifBraceSameLine = if (rules.any { it.id == "if-brace-same-line" && it.enabled }) true else null,
        )
}
