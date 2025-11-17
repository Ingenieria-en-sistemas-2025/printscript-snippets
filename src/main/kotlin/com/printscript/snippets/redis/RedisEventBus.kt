package com.printscript.snippets.redis

import com.fasterxml.jackson.databind.ObjectMapper
import io.printscript.contracts.events.DomainEvent
import io.printscript.contracts.events.FormattingRulesUpdated
import io.printscript.contracts.events.LintingRulesUpdated
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisEventBus(
    private val om: ObjectMapper,
    @Qualifier("stringTemplate") private val redis: RedisTemplate<String, String>,
    private val streamKeys: Map<EventChannel, String>,
) {

    fun publish(event: DomainEvent) {
        val channel = getChannel(event)

        val streamKey = streamKeys[channel]
            ?: error("No stream key for channel $channel")

        val json = om.writeValueAsString(event)
        val record: ObjectRecord<String, String> = ObjectRecord.create(streamKey, json)
        redis.opsForStream<String, String>().add(record)
    }

    private fun getChannel(event: DomainEvent): EventChannel {
        return when (event) {
            is LintingRulesUpdated -> EventChannel.LINTING
            is FormattingRulesUpdated -> EventChannel.FORMATTING
            else -> error("No channel configured for ${event::class.simpleName}")
        }
    }
}
