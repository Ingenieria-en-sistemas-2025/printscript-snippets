package com.printscript.snippets.service.rules

import com.printscript.snippets.domain.RulesStateRepo
import com.printscript.snippets.domain.model.RulesState
import com.printscript.snippets.dto.RuleDto
import com.printscript.snippets.enums.RulesType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RulesStateService( // preferencias del usuario sobre reglas
    private val rulesStateRepo: RulesStateRepo,
) {

    private val logger = LoggerFactory.getLogger(RulesStateService::class.java)

    private val strategies: Map<RulesType, RuleTypeStrategy> =
        listOf(
            FormatRuleStrategy(),
            LintRuleStrategy(),
        ).associateBy { it.type }

    private fun strategyFor(type: RulesType): RuleTypeStrategy =
        strategies.getValue(type)

    private fun findRowForOwnerOrDefault(type: RulesType, ownerId: String?): RulesState? =
        rulesStateRepo.findByTypeAndOwnerId(type, ownerId).orElseGet {
            rulesStateRepo.findByTypeAndOwnerId(type, null).orElse(null)
        }

    private fun findOrCreateRowForOwner(type: RulesType, ownerId: String?): RulesState =
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
        val row = findRowForOwnerOrDefault(type, ownerId)
        val strategy = strategyFor(type)
        return row?.enabledJson?.toSet() ?: strategy.defaultEnabled()
    }

    private fun readOptions(type: RulesType, ownerId: String?): Map<String, Any?> {
        val row = findRowForOwnerOrDefault(type, ownerId)
        val raw = row?.optionsJson ?: return emptyMap()
        return raw.filterValues { it != null }
    }

    private fun normalizeFormat(configFormat: String?): String =
        RuleHelpers.defaultConfigFormat(configFormat)

    fun saveState(
        type: RulesType,
        ownerId: String,
        rules: List<RuleDto>,
        configText: String?,
        configFormat: String?,
    ) {
        val strategy = strategyFor(type)
        val normalizedFormat = normalizeFormat(configFormat)

        val state = strategy.buildStateFromDtos(
            rules = rules,
            configText = configText,
            configFormat = normalizedFormat,
        )

        val row = findOrCreateRowForOwner(type, ownerId)
        row.enabledJson = state.enabled.toList()
        row.optionsJson = state.options.ifEmpty { null }
        row.configText = state.configText
        row.configFormat = state.configFormat
        rulesStateRepo.save(row)
    }

    fun getRules(type: RulesType, ownerId: String): List<RuleDto> {
        val strategy = strategyFor(type)
        val enabled = readEnabled(type, ownerId)
        val values = readOptions(type, ownerId)
        return strategy.toRuleDtos(enabled, values)
    }

    fun currentConfig(type: RulesType, ownerId: String): Pair<String?, String?> {
        val strategy = strategyFor(type)
        val row = findRowForOwnerOrDefault(type, ownerId)
        val rules = getRules(type, ownerId)
        return strategy.buildEffectiveConfig(row, rules)
    }

    fun currentConfigEffective(type: RulesType, ownerId: String): Pair<String, String> {
        val (cfgText, cfgFmt) = currentConfig(type, ownerId)
        return (cfgText ?: "{}") to (cfgFmt ?: "json")
    }

    fun saveFormatState(ownerId: String, rules: List<RuleDto>, configText: String?, configFormat: String?) =
        saveState(RulesType.FORMAT, ownerId, rules, configText, configFormat)

    fun saveLintState(ownerId: String, rules: List<RuleDto>, configText: String?, configFormat: String?) {
        logger.info(
            "Saving lint rules for owner={} enabled={} cfgFmt={} cfgText={}",
            ownerId,
            rules.joinToString { "${it.id}=${it.enabled}, value=${it.value}" },
            configFormat,
            configText,
        )
        saveState(RulesType.LINT, ownerId, rules, configText, configFormat)
    }

    fun getFormatAsRules(ownerId: String): List<RuleDto> =
        getRules(RulesType.FORMAT, ownerId)

    fun getLintAsRules(ownerId: String): List<RuleDto> =
        getRules(RulesType.LINT, ownerId)

    fun currentFormatConfig(ownerId: String): Pair<String?, String?> =
        currentConfig(RulesType.FORMAT, ownerId)

    fun currentLintConfig(ownerId: String): Pair<String?, String?> =
        currentConfig(RulesType.LINT, ownerId)

    fun currentFormatConfigEffective(ownerId: String): Pair<String, String> =
        currentConfigEffective(RulesType.FORMAT, ownerId)

    fun currentLintConfigEffective(ownerId: String): Pair<String, String> =
        currentConfigEffective(RulesType.LINT, ownerId)
}
