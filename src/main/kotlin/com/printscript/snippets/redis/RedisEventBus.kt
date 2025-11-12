package com.printscript.snippets.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.printscript.snippets.redis.events.DomainEvent
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.env.Environment
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisEventBus(
    @Qualifier("redisTemplateString")
    private val redisString: RedisTemplate<String, String>,
    private val env: Environment,
    private val om: ObjectMapper,
) : EventBus {

    private fun sanitizeKey(raw: String) = raw.trim().trim('"', '\'')

    override fun publish(channel: Channel, event: DomainEvent) {
        val streamKey = sanitizeKey(env.getRequiredProperty("${channel.streamKeyProp}.key"))
        val json = om.writeValueAsString(event)

        val rec = StreamRecords.newRecord()
            .ofObject(json)
            .withStreamKey(streamKey)

        redisString.opsForStream<String, String>().add(rec)
    }
}
