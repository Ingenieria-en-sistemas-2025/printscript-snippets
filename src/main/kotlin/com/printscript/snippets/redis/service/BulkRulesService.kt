package com.printscript.snippets.redis.service

import com.printscript.snippets.domain.SnippetRepo
import com.printscript.snippets.logs.CorrelationIdFilter.Companion.CORRELATION_ID_KEY
import com.printscript.snippets.redis.RedisEventBus
import com.printscript.snippets.service.LintReevaluationService
import com.printscript.snippets.service.rules.FormatterMapper
import com.printscript.snippets.service.rules.RulesStateService
import io.printscript.contracts.events.FormattingRulesUpdated
import io.printscript.contracts.events.LintingRulesUpdated
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BulkRulesService(
    private val snippetRepo: SnippetRepo,
    private val rulesStateService: RulesStateService,
    private val lintReevaluationService: LintReevaluationService,
    private val bus: RedisEventBus,
) {

    private val logger = LoggerFactory.getLogger(BulkRulesService::class.java)

    fun onFormattingRulesChanged(ownerId: String) {
        val rules = rulesStateService.getFormatAsRules(ownerId)
        val options = FormatterMapper.toFormatterOptionsDto(rules)
        val (cfgText, cfgFmt) = rulesStateService.currentFormatConfigEffective(ownerId)

        val corr = MDC.get(CORRELATION_ID_KEY) ?: UUID.randomUUID().toString()
        val ids = snippetRepo.findAllIdsByOwner(ownerId)

        logger.info(
            "onFormattingRulesChanged: owner={} snippets={} corrId={}",
            ownerId,
            ids.size,
            corr,
        )

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
        val corr = MDC.get(CORRELATION_ID_KEY) ?: UUID.randomUUID().toString()

        val ids = lintReevaluationService.markOwnerSnippetsPending(ownerId)

        logger.info(
            "onLintingRulesChanged: owner={} snippets={} corrId={}",
            ownerId,
            ids.size,
            corr,
        )

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
