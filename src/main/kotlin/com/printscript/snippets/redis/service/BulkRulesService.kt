package com.printscript.snippets.redis.service

import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.execution.dto.FormatterOptionsDto
import com.printscript.snippets.redis.Channel
import com.printscript.snippets.redis.EventBus
import com.printscript.snippets.redis.events.SnippetsFormattingRulesUpdated
import com.printscript.snippets.redis.events.SnippetsLintingRulesUpdated
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BulkRulesService(
    private val snippetRepo: SnippetRepo,
    private val bus: EventBus,
) {
    fun onFormattingRulesChanged(cfg: String?, fmt: String?, opt: FormatterOptionsDto?) {
        val corr = UUID.randomUUID().toString()
        snippetRepo.findAllIds().forEach { id ->
            val lv = snippetRepo.getLangAndVersion(id)
            bus.publish(
                Channel.FORMATTING,
                SnippetsFormattingRulesUpdated(corr, id, lv.language(), lv.languageVersion(), cfg, fmt, opt),
            )
        }
    }

    fun onLintingRulesChanged(cfg: String?, fmt: String?) {
        val corr = UUID.randomUUID().toString()
        snippetRepo.findAllIds().forEach { id ->
            val lv = snippetRepo.getLangAndVersion(id)
            bus.publish(
                Channel.LINTING,
                SnippetsLintingRulesUpdated(corr, id, lv.language(), lv.languageVersion(), cfg, fmt),
            )
        }
    }
}
