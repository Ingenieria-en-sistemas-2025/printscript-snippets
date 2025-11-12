package com.printscript.snippets.redis

import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.RedisTemplate

abstract class SimpleRedisStreamProducer(
    private val streamKey: String,
    private val redis: RedisTemplate<String, String>,
) {
    fun emit(json: String): RecordId? {
        val record = StreamRecords.newRecord()
            .ofObject(json)
            .withStreamKey(streamKey)

        return redis.opsForStream<String, String>().add(record)
    }
}
