package com.extrabox.periodization.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class HealthController {

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
