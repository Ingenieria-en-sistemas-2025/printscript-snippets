package com.printscript.snippets.dto

data class RuleDto(
    val id: String,
    val enabled: Boolean,
    val value: Int? = null,
)
