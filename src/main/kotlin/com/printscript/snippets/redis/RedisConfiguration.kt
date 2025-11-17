package com.printscript.snippets.redis

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

@Profile("!test")
@Configuration
class RedisConfiguration(
    @Value("\${spring.data.redis.host}") private val host: String,
    @Value("\${spring.data.redis.port}") private val port: Int,
) {
    @Bean
    fun connectionFactory() = LettuceConnectionFactory(RedisStandaloneConfiguration(host, port)) // devuelve un lettuce connection factory -> Driver Lettuce para conectarse a Redis
    // otras partes como redis template usan esta factory para abrir conextiones

    @Bean
    @Primary
    fun stringTemplate(cf: RedisConnectionFactory): RedisTemplate<String, String> =
        RedisTemplate<String, String>().apply {
            setConnectionFactory(cf) // setea conexion con redis
            keySerializer = StringRedisSerializer()
            valueSerializer = StringRedisSerializer()
            hashKeySerializer = StringRedisSerializer()
            hashValueSerializer = StringRedisSerializer()
            afterPropertiesSet()
        }
}
