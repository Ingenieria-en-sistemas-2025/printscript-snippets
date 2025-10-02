package com.printscript.tests.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@Profile("dev")
class DevSecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain =
        http.csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .build()
}
