package com.printscript.snippets.redis.service

import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.domain.model.Compliance
import com.printscript.snippets.domain.model.LintStatus
import com.printscript.snippets.redis.RedisEventBus
import com.printscript.snippets.redis.events.SnippetsFormattingRulesUpdated
import com.printscript.snippets.redis.events.SnippetsLintingRulesUpdated
import com.printscript.snippets.service.rules.FormatterMapper
import com.printscript.snippets.service.rules.RulesStateService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BulkRulesService(
    private val snippetRepo: SnippetRepo,
    private val versionRepo: SnippetVersionRepo,
    private val rulesStateService: RulesStateService,
    private val bus: RedisEventBus,
) {
    fun onFormattingRulesChanged(ownerId: String) {
        val rules = rulesStateService.getFormatAsRules(ownerId)
        val options = FormatterMapper.toFormatterOptionsDto(rules)
        val cfgText = rulesStateService.buildFormatterConfigFromRules(rules)
        val cfgFmt = "json"
        val corr = UUID.randomUUID().toString()
        val ids = snippetRepo.findAllIdsByOwner(ownerId)
        ids.forEach { snippetId ->
            val lv = snippetRepo.getLangAndVersion(snippetId)
            bus.publishFormatting(SnippetsFormattingRulesUpdated(corr, snippetId, lv.language, lv.languageVersion, cfgText, cfgFmt, options))
        }
    }

    fun onLintingRulesChanged(ownerId: String) {
        val (cfg, fmt) = rulesStateService.currentLintConfigEffective(ownerId)
        val corr = UUID.randomUUID().toString()

        val ids = snippetRepo.findAllIdsByOwner(ownerId)

        ids.forEach { id ->
            versionRepo.updateLatestLintStatus(id, LintStatus.PENDING)
            val s = snippetRepo.findById(id).orElseThrow()
            s.compliance = Compliance.PENDING
            snippetRepo.save(s)
        }

        ids.forEach { id ->
            val lv = snippetRepo.getLangAndVersion(id)
            bus.publishLint(SnippetsLintingRulesUpdated(corr, id, lv.language, lv.languageVersion, cfg, fmt))
        }
    }
}
