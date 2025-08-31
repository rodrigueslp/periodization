package com.extrabox.periodization.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean
import java.util.*

@Component
class RequestLoggingFilter : GenericFilterBean() {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        try {
            val httpRequest = request as HttpServletRequest
            val requestId = UUID.randomUUID().toString()
            val userEmail = httpRequest.userPrincipal?.name ?: "anonymous"

            MDC.put("requestId", requestId)
            MDC.put("userEmail", userEmail)

            chain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }
}
