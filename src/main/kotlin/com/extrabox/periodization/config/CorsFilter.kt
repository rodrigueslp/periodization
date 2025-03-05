package com.extrabox.periodization.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorsFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val origin = request.getHeader("Origin") ?: "https://app.planilize.com.br"

        response.setHeader("Access-Control-Allow-Origin", origin)
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, Origin")
        response.setHeader("Access-Control-Allow-Credentials", "true")
        response.setHeader("Access-Control-Max-Age", "3600")

        // Caso específico do problema com Cross-Origin-Opener-Policy
        // Adicionando cabeçalhos para permitir window.postMessage
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin-allow-popups")

        if ("OPTIONS".equals(request.method, ignoreCase = true)) {
            response.status = HttpServletResponse.SC_OK
        } else {
            filterChain.doFilter(request, response)
        }
    }
}