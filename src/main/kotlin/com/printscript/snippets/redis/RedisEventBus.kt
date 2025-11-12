package com.printscript.snippets.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.printscript.snippets.redis.events.SnippetsFormattingRulesUpdated
import com.printscript.snippets.redis.events.SnippetsLintingRulesUpdated
import org.austral.ingsis.redis.RedisStreamProducer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisEventBus(
    private val om: ObjectMapper,
    @Qualifier("stringTemplate") private val redis: RedisTemplate<String, String>,
    @Value("\${streams.linting.key}") rawLintKey: String,
    @Value("\${streams.formatting.key}") rawFmtKey: String,
) {
    private fun clean(k: String) = k.trim().trim('"', '\'')

    private val lintingProducer = object : RedisStreamProducer(clean(rawLintKey), redis) {}
    private val formattingProducer = object : RedisStreamProducer(clean(rawFmtKey), redis) {}

    fun publishLint(ev: SnippetsLintingRulesUpdated) {
        lintingProducer.emit(om.writeValueAsString(ev))
    }

    fun publishFormatting(ev: SnippetsFormattingRulesUpdated) {
        formattingProducer.emit(om.writeValueAsString(ev))
    }
}
