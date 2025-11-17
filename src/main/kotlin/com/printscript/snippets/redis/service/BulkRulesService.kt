package com.printscript.snippets.redis.service

import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.redis.RedisEventBus
import com.printscript.snippets.service.LintReevaluationService
import com.printscript.snippets.service.rules.FormatterMapper
import com.printscript.snippets.service.rules.RulesStateService
import io.printscript.contracts.events.FormattingRulesUpdated
import io.printscript.contracts.events.LintingRulesUpdated
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BulkRulesService(
    private val snippetRepo: SnippetRepo,
    private val rulesStateService: RulesStateService,
    private val lintReevaluationService: LintReevaluationService,
    private val bus: RedisEventBus,
) {
    fun onFormattingRulesChanged(ownerId: String) {
        val rules = rulesStateService.getFormatAsRules(ownerId)
        val options = FormatterMapper.toFormatterOptionsDto(rules)
        var cfgText = rulesStateService.buildFormatterConfigFromRules(rules)

        if (cfgText.isNullOrBlank()) {
            cfgText = rulesStateService.buildFormatterConfigFromRules(rules)
        }

        val cfgFmt = "json"
        val corr = UUID.randomUUID().toString()
        val ids = snippetRepo.findAllIdsByOwner(ownerId)
        ids.forEach { snippetId ->
            val lv = snippetRepo.getLangAndVersion(snippetId)
            val event = FormattingRulesUpdated(
                correlationalId = corr,
                snippetId = snippetId,
                language = lv.language,
                version = lv.languageVersion,
                configText = cfgText,
                configFormat = cfgFmt,
                options = options,
            )
            bus.publish(event)
        }
    }

    fun onLintingRulesChanged(ownerId: String) {
        val (cfg, fmt) = rulesStateService.currentLintConfigEffective(ownerId)
        val corr = UUID.randomUUID().toString()

        val ids = lintReevaluationService.markOwnerSnippetsPending(ownerId)

        ids.forEach { id ->
            val lv = snippetRepo.getLangAndVersion(id)

            val event = LintingRulesUpdated(
                correlationalId = corr,
                snippetId = id,
                language = lv.language,
                version = lv.languageVersion,
                configText = cfg,
                configFormat = fmt,
            )

            bus.publish(event)
        }
    }
}
