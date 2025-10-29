package com.printscript.snippets.redis

import com.printscript.snippets.redis.events.DomainEvent
import org.austral.ingsis.redis.RedisStreamProducer
import org.springframework.core.env.Environment
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisEventBus(
    private val redis: RedisTemplate<String, String>,
    private val env: Environment,
) : EventBus {

    private val producers = mutableMapOf<String, RedisStreamProducer>()

    override fun publish(channel: Channel, event: DomainEvent) {
        val streamKey = env.getRequiredProperty(channel.streamKeyProp)
        val prod = producers.getOrPut(streamKey) {
            object : RedisStreamProducer(streamKey, redis) {}
        }
        prod.emit(event)
    }
}
