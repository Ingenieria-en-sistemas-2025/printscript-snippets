package com.printscript.snippets.logs

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import kotlin.system.measureNanoTime

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class RequestLogFilter : OncePerRequestFilter() {

    private val log = logger

    companion object {
        private const val NANO_TO_MILLISECOND = 1_000_000
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val method = request.method
        val uri = request.requestURI + (request.queryString?.let { "?$it" } ?: "")

        var status: Int
        val elapsedMs = measureNanoTime {
            filterChain.doFilter(request, response)
        }.let { it / NANO_TO_MILLISECOND }

        status = response.status
        log.info("$method $uri - $status (${elapsedMs}ms)")
    }
}
