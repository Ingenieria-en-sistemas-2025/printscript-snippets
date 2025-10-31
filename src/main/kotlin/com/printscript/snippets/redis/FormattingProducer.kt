package com.printscript.snippets.redis

import org.austral.ingsis.redis.RedisStreamProducer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class FormattingProducer(
    @Value("\${streams.formatting}") streamKey: String,
    @Qualifier("redisTemplateString")
    redis: RedisTemplate<String, String>,
) : RedisStreamProducer(streamKey, redis)
