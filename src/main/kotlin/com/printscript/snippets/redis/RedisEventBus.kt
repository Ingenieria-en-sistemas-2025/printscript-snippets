package com.printscript.snippets.redis

import com.printscript.snippets.redis.events.DomainEvent
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.env.Environment
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisEventBus(
    @Qualifier("redisTemplateJson")
    private val redisJson: RedisTemplate<String, Any>,
    private val env: Environment,
) : EventBus {

    private fun sanitizeKey(raw: String) = raw.trim().trim('"', '\'')

    override fun publish(channel: Channel, event: DomainEvent) {
        val raw = env.getRequiredProperty("${channel.streamKeyProp}.key")
        val streamKey = sanitizeKey(raw)
        val rec = StreamRecords.newRecord().ofObject(event).withStreamKey(streamKey)
        redisJson.opsForStream<String, Any>().add(rec)
    }
}
