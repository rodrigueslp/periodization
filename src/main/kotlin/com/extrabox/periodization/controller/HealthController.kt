package com.extrabox.periodization.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import com.newrelic.api.agent.Trace

@RestController
class HealthController {

    @Trace(dispatcher = true)
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        val response = mapOf(
            "status" to "UP",
            "message" to "Aplicação está funcionando corretamente",
            "timestamp" to LocalDateTime.now().toString()
        )
        return ResponseEntity.ok(response)
    }
}
