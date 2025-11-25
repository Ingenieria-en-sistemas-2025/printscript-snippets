package com.printscript.snippets.service.rules

import com.printscript.snippets.dto.RuleDto

internal object RuleHelpers {

    fun enabledSet(rules: List<RuleDto>): Set<String> =
        rules.asSequence()
            .filter { it.enabled }
            .map { it.id }
            .toSet()

    fun numericOptionValue(raw: Any?): Int? =
        when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull()
            else -> null
        }

    fun configTextOrNull(configText: String?): String? =
        configText
            ?.trim()
            ?.takeUnless { it.isEmpty() || it == "{}" }

    fun defaultConfigFormat(configFormat: String?): String =
        when (configFormat?.lowercase()) {
            "yaml", "yml" -> "yaml"
            "json" -> "json"
            null, "" -> "json"
            else -> "json"
        }
}
