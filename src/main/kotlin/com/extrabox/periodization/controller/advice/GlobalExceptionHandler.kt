package com.extrabox.periodization.controller.advice

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.HashMap

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> {
        val errors = HashMap<String, String>()
        ex.bindingResult.allErrors.forEach { error ->
            val fieldName = (error as FieldError).field
            val errorMessage = error.getDefaultMessage()
            errors[fieldName] = errorMessage ?: "Erro de validação"
        }
        return ResponseEntity.badRequest().body(errors)
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<Map<String, String>> {
        logger.error("Runtime exception", ex)
        val errors = HashMap<String, String>()
        errors["error"] = ex.message ?: "Ocorreu um erro interno"
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errors)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<Map<String, String>> {
        logger.error("Unhandled exception", ex)
        val errors = HashMap<String, String>()
        errors["error"] = "Ocorreu um erro interno no servidor"
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errors)
    }
}
