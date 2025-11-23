package com.printscript.snippets.service.rules

data class RuleStatePieces(
    val enabled: Set<String>,
    val options: Map<String, Any?>,
    val configText: String?,
    val configFormat: String?,
)
