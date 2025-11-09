package com.printscript.snippets.redis.service

import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.domain.SnippetVersionRepo
import com.printscript.snippets.domain.model.Compliance
import com.printscript.snippets.domain.model.LintStatus
import com.printscript.snippets.redis.Channel
import com.printscript.snippets.redis.EventBus
import com.printscript.snippets.redis.events.SnippetsFormattingRulesUpdated
import com.printscript.snippets.redis.events.SnippetsLintingRulesUpdated
import com.printscript.snippets.service.rules.RulesStateService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BulkRulesService(
    private val snippetRepo: SnippetRepo,
    private val versionRepo: SnippetVersionRepo,
    private val rulesStateService: RulesStateService,
    private val bus: EventBus,
) {
    fun onFormattingRulesChanged(ownerId: String) {
        val (cfg, fmt) = rulesStateService.currentFormatConfig(ownerId)
        val corr = UUID.randomUUID().toString()
        val ids = snippetRepo.findAllIdsByOwner(ownerId)
        ids.forEach { snippetId ->
            val lv = snippetRepo.getLangAndVersion(snippetId)
            bus.publish(
                Channel.FORMATTING,
                SnippetsFormattingRulesUpdated(corr, snippetId, lv.language, lv.languageVersion, cfg, fmt),
            )
        }
    }

    fun onLintingRulesChanged(ownerId: String) {
        val (cfg, fmt) = rulesStateService.currentLintConfig(ownerId)
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
            bus.publish(
                Channel.LINTING,
                SnippetsLintingRulesUpdated(corr, id, lv.language, lv.languageVersion, cfg, fmt),
            )
        }
    }
}
