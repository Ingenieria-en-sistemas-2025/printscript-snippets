package com.printscript.snippets.redis

import com.printscript.snippets.redis.events.DomainEvent

interface EventBus {
    fun publish(channel: Channel, event: DomainEvent)
}
