package com.printscript.snippets.logs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class RequestLogFilter : WebFilter {

    val logger = LoggerFactory.getLogger(RequestLogFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val uri = exchange.request.uri
        val method = exchange.request.method.toString()
        val prefix = "$method $uri"
        try {
            return chain.filter(exchange)
        } finally {
            val statusCode = exchange.response.statusCode
            logger.info("$prefix - $statusCode")
        }
    }
}
