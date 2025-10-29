package com.printscript.snippets.execution.dto

data class LintReq(
    val language: String,
    val version: String,
    val content: String?,
    val configText: String? = null,
    val configFormat: String? = null,
)
