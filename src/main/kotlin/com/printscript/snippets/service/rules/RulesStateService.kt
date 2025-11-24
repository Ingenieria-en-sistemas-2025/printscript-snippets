package com.printscript.snippets.service.rules

import com.printscript.snippets.domain.RulesStateRepo
import com.printscript.snippets.domain.model.RulesState
import com.printscript.snippets.dto.RuleDto
import com.printscript.snippets.enums.RulesType
import org.springframework.stereotype.Service

@Service
class RulesStateService( // preferencias del usuario sobre reglas
    private val rulesStateRepo: RulesStateRepo,
) {

    private val strategies: Map<RulesType, RuleTypeStrategy> =
        listOf(
            FormatRuleStrategy(),
            LintRuleStrategy(),
        ).associateBy { it.type }

    private fun findRow(type: RulesType, ownerId: String?): RulesState? =
        rulesStateRepo.findByTypeAndOwnerId(type, ownerId).orElseGet {
            rulesStateRepo.findByTypeAndOwnerId(type, null).orElse(null)
        }

    private fun upsertRow(type: RulesType, ownerId: String?): RulesState =
        rulesStateRepo.findByTypeAndOwnerId(type, ownerId).orElse(
            RulesState(
                id = null,
                type = type,
                ownerId = ownerId,
                enabledJson = emptyList(),
                optionsJson = null,
                configText = null,
                configFormat = null,
            ),
        )

    private fun readEnabled(type: RulesType, ownerId: String?): Set<String> {
        val row = findRow(type, ownerId)
        val strategy = strategies.getValue(type)
        return row?.enabledJson?.toSet() ?: strategy.defaultEnabled()
    }

    private fun readOptions(type: RulesType, ownerId: String?): Map<String, Any?> {
        val row = findRow(type, ownerId)
        val raw = row?.optionsJson ?: return emptyMap()
        return raw.filterValues { it != null }
    }

    private fun normalizeFormat(configFormat: String?): String =
        when (configFormat?.lowercase()) {
            "yaml", "yml" -> "yaml"
            "json" -> "json"
            null, "" -> "json"
            else -> "json"
        }

    fun saveFormatState(ownerId: String, rules: List<RuleDto>, configText: String?, configFormat: String?) {
        val enabled: Set<String> = rules.filter { it.enabled }.map { it.id }.toSet()

        val options: Map<String, Int> = rules.mapNotNull { r ->
            val intValue = when (val v = r.value) {
                is Number -> v.toInt()
                is String -> v.toIntOrNull()
                else -> null
            }
            intValue?.let { r.id to it }
        }.toMap()

        val normalizedConfigText = configText
            ?.trim()
            ?.takeUnless { it.isEmpty() || it == "{}" }

        val row = upsertRow(RulesType.FORMAT, ownerId)
        row.enabledJson = enabled.toList()
        row.optionsJson = options.ifEmpty { null }
        row.configText = normalizedConfigText
        row.configFormat = normalizeFormat(configFormat)
        rulesStateRepo.save(row)
    }

    fun getFormatAsRules(ownerId: String): List<RuleDto> {
        val type = RulesType.FORMAT
        val strategy = strategies.getValue(type)

        val enabled = readEnabled(type, ownerId)
        val values = readOptions(type, ownerId)

        return strategy.toRuleDtos(enabled, values)
    }

    fun currentFormatConfig(ownerId: String): Pair<String?, String?> {
        val type = RulesType.FORMAT
        val row = findRow(type, ownerId)
        val rules = getFormatAsRules(ownerId)

        // A diferencia de lint, acá permitís null si no tenés nada configurado
        val (cfgText, cfgFmt) = strategies.getValue(type).buildEffectiveConfig(row, rules)
        return cfgText to cfgFmt
    }

    fun saveLintState(ownerId: String, rules: List<RuleDto>, configText: String?, configFormat: String?) {
        val enabled: Set<String> = rules.filter { it.enabled }.map { it.id }.toSet()
        val options: Map<String, Any> = rules.mapNotNull { r ->
            r.value?.let { v -> r.id to v }
        }.toMap()

        val row = upsertRow(RulesType.LINT, ownerId)
        row.enabledJson = enabled.toList()
        row.optionsJson = options.ifEmpty { null }
        row.configText = configText
        row.configFormat = configFormat
        rulesStateRepo.save(row)
    }

    fun getLintAsRules(ownerId: String): List<RuleDto> {
        val type = RulesType.LINT
        val strategy = strategies.getValue(type)

        val enabled = readEnabled(type, ownerId)
        val values = readOptions(type, ownerId)

        return strategy.toRuleDtos(enabled, values)
    }

    fun currentLintConfig(ownerId: String): Pair<String?, String?> {
        val type = RulesType.LINT
        val row = findRow(type, ownerId)
        val rules = getLintAsRules(ownerId)
        return strategies.getValue(type).buildEffectiveConfig(row, rules)
    }

    fun currentLintConfigEffective(ownerId: String): Pair<String, String> {
        val (cfgText, cfgFmt) = currentLintConfig(ownerId)
        return (cfgText ?: "{}") to (cfgFmt ?: "json")
    }

    fun currentFormatConfigEffective(ownerId: String): Pair<String, String> {
        val (cfgText, cfgFmt) = currentFormatConfig(ownerId)
        return (cfgText ?: "{}") to (cfgFmt ?: "json")
    }
}
