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

    private fun strategyFor(type: RulesType): RuleTypeStrategy =
        strategies.getValue(type)


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

    //guardar estado para cualquier tipo de reglas
    fun saveState(
        type: RulesType,
        ownerId: String,
        rules: List<RuleDto>,
        configText: String?,
        configFormat: String?,
    ) {
        val strategy = strategyFor(type) //strategy correcta
        val state = strategy.buildStateFromDtos(rules, configText, configFormat) //convuerte ruledto en un dto para guardar en la db

        val row = upsertRow(type, ownerId) //busca la fila de ese owner si no existe la crea
        row.enabledJson = state.enabled.toList() //setea lista de ids activados
        row.optionsJson = state.options.ifEmpty { null } //setea los nros solo si toene vals
        row.configText = state.configText
        row.configFormat = state.configFormat
        rulesStateRepo.save(row)
    }

    //lo q le doy a la ui para mostrar las rules
    fun getRules(type: RulesType, ownerId: String): List<RuleDto> {
        val strategy = strategyFor(type)
        val enabled = readEnabled(type, ownerId) //busca las rules activadas
        val values = readOptions(type, ownerId) //busca options guardadas
        return strategy.toRuleDtos(enabled, values) //la misma strategy arma la lista de RuleDto
    }


    //obtener config (configText + formato) para cualquier tipo
    fun currentConfig(type: RulesType, ownerId: String): Pair<String?, String?> {
        val strategy = strategyFor(type)
        val row = findRow(type, ownerId)
        val rules = getRules(type, ownerId) //regla ya lista para la ui
        return strategy.buildEffectiveConfig(row, rules) //la strategy decide como generar json final
    }

    fun currentConfigEffective(type: RulesType, ownerId: String): Pair<String, String> {
        val (cfgText, cfgFmt) = currentConfig(type, ownerId)
        return (cfgText ?: "{}") to (cfgFmt ?: "json")
    }

    fun saveFormatState(ownerId: String, rules: List<RuleDto>, configText: String?, configFormat: String?) =
        saveState(RulesType.FORMAT, ownerId, rules, configText, configFormat)

    fun saveLintState(ownerId: String, rules: List<RuleDto>, configText: String?, configFormat: String?) =
        saveState(RulesType.LINT, ownerId, rules, configText, configFormat)

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
