package com.printscript.snippets.dto

data class SaveRulesReq(
    val rules: List<RuleDto>,
    val configText: String? = null,
    val configFormat: String? = null,
)
