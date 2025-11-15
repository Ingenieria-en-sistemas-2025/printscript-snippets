package com.printscript.snippets.execution.dto

import io.printscript.contracts.formatter.FormatterOptionsDto

data class FormatReq(
    val language: String,
    val version: String,
    val content: String,
    val configText: String? = null,
    val configFormat: String? = null,
    val options: FormatterOptionsDto? = null,
)
