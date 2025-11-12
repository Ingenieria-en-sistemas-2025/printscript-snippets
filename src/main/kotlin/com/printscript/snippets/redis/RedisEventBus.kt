package com.printscript.snippets.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.printscript.snippets.redis.events.SnippetsFormattingRulesUpdated
import com.printscript.snippets.redis.events.SnippetsLintingRulesUpdated
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.connection.stream.MapRecord
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

    private val lintKey = clean(rawLintKey)
    private val fmtKey = clean(rawFmtKey)

    fun publishLint(ev: SnippetsLintingRulesUpdated) {
        val json = om.writeValueAsString(ev)
        redis.opsForStream<String, String>()
            .add(MapRecord.create(lintKey, mapOf("value" to json)))
        println("[RedisEventBus] publishLint -> stream=$lintKey field=value size=${json.length}")
    }

    fun publishFormatting(ev: SnippetsFormattingRulesUpdated) {
        val json = om.writeValueAsString(ev)
        redis.opsForStream<String, String>()
            .add(MapRecord.create(fmtKey, mapOf("value" to json)))
        println("[RedisEventBus] publishFormatting -> stream=$fmtKey field=value size=${json.length}")
    }
}
