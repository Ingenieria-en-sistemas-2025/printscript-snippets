package com.printscript.snippets.service.rules

import com.printscript.snippets.domain.model.RulesState
import com.printscript.snippets.domain.model.RulesType
import com.printscript.snippets.dto.RuleDto

internal interface RuleTypeStrategy {
    val type: RulesType

    fun defaultEnabled(): Set<String>

    fun defaultValues(): Map<String, Any?> // defaults de vals numericos

    fun allRuleIds(): List<String>

    // arma lista de RuleDto desde enabled + values
    fun toRuleDtos(enabled: Set<String>, values: Map<String, Any?>): List<RuleDto>

    // Construye el config efectivo (texto + formato) a partir de la fila en db (si no hay configText, puede generarlo en base a enabled/options)
    fun buildEffectiveConfig(row: RulesState?, rules: List<RuleDto>): Pair<String, String>
}
