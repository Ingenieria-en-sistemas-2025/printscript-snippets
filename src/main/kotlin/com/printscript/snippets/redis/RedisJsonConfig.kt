package com.printscript.snippets.redis

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer

@Configuration
class RedisJsonConfig {

    @Bean
    fun redisObjectMapper() = jacksonObjectMapper()
        .registerKotlinModule()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    @Bean
    fun genericJsonSerializer(
        redisObjectMapper: com.fasterxml.jackson.databind.ObjectMapper,
    ): GenericJackson2JsonRedisSerializer =
        GenericJackson2JsonRedisSerializer(redisObjectMapper)
}
