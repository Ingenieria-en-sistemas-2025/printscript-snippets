package com.printscript.snippets.redis

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StreamsConfig(
    @Value("\${streams.linting.key}") private val lintKeyRaw: String,
    @Value("\${streams.formatting.key}") private val fmtKeyRaw: String,
) {
    private fun clean(k: String) = k.trim().trim('"', '\'')

    @Bean
    fun streamKeys(): Map<EventChannel, String> = mapOf(
        EventChannel.LINTING to clean(lintKeyRaw),
        EventChannel.FORMATTING to clean(fmtKeyRaw),
    )
}
