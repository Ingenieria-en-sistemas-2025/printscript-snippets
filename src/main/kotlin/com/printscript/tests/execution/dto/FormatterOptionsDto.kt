package com.printscript.tests.execution.dto

data class FormatterOptionsDto(
    val spaceBeforeColonInDecl: Boolean? = null,
    val spaceAfterColonInDecl: Boolean? = null,
    val spaceAroundAssignment: Boolean? = null,
    val blankLinesAfterPrintln: Int? = null,
    val indentSpaces: Int? = null,
    val mandatorySingleSpaceSeparation: Boolean? = null,
    val ifBraceBelowLine: Boolean? = null,
    val ifBraceSameLine: Boolean? = null
)

