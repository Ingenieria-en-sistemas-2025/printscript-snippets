package com.printscript.snippets.config

import com.printscript.snippets.auth.Auth0TokenService
import com.printscript.snippets.logs.CorrelationIdFilter.Companion.CORRELATION_ID_HEADER
import com.printscript.snippets.logs.CorrelationIdFilter.Companion.CORRELATION_ID_KEY
import org.slf4j.MDC
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class HttpClientConfig {

    companion object {
        private const val CONNECT_TIMEOUT_SECONDS = 5L
        private const val READ_TIMEOUT_SECONDS = 30L
    }

    @Bean
    fun m2mRestTemplate(tokenService: Auth0TokenService): RestTemplate {
        val authInterceptor = ClientHttpRequestInterceptor { req, body, exec ->
            val token = tokenService.getAccessToken()
            req.headers.setBearerAuth(token)
            exec.execute(req, body)
        }

        val correlationInterceptor = ClientHttpRequestInterceptor { req, body, exec ->
            MDC.get(CORRELATION_ID_KEY)?.let { corrId ->
                req.headers.add(CORRELATION_ID_HEADER, corrId)
            }
            exec.execute(req, body)
        }

        return RestTemplateBuilder()
            .requestFactorySettings { settings ->
                settings
                    .withConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                    .withReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
            }
            .additionalInterceptors(authInterceptor, correlationInterceptor)
            .build()
    }
}