package com.printscript.snippets.redis

import com.printscript.snippets.redis.events.DomainEvent
import org.springframework.core.env.Environment
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisEventBus(
    private val redisJson: RedisTemplate<String, String>,
    private val env: Environment,
) : EventBus {

    private val streamKeys = mutableMapOf<Channel, String>()

    override fun publish(channel: Channel, event: DomainEvent) {
        val streamKey = streamKeys.getOrPut(channel) { env.getRequiredProperty(channel.streamKeyProp) }
        val rec = StreamRecords.newRecord().ofObject(event).withStreamKey(streamKey)
        redisJson.opsForStream<String, Any>().add(rec)
    }
}
