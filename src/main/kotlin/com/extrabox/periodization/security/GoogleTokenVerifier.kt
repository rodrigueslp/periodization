package com.extrabox.periodization.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class GoogleTokenVerifier(
    private val httpClient: OkHttpClient,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(GoogleTokenVerifier::class.java)

    fun verify(idTokenString: String): GoogleUserInfo? {
        try {
            // Decodifica o token para extrair o payload
            val parts = idTokenString.split(".")
            if (parts.size != 3) {
                logger.error("Token inválido: formato incorreto")
                return null
            }

            val payload = parts[1]
            val decodedBytes = Base64.getUrlDecoder().decode(payload)
            val payloadJson = String(decodedBytes)

            val tokenInfo = objectMapper.readValue<Map<String, Any>>(payloadJson)

            // Verificações básicas do token
            val expirationTime = (tokenInfo["exp"] as Number).toLong()
            if (System.currentTimeMillis() / 1000 > expirationTime) {
                logger.error("Token expirado")
                return null
            }

            // Retorna as informações do usuário
            return GoogleUserInfo(
                sub = tokenInfo["sub"] as String,
                email = tokenInfo["email"] as String,
                name = tokenInfo["name"] as String,
                picture = tokenInfo["picture"] as? String
            )
        } catch (e: Exception) {
            logger.error("Erro ao verificar token do Google", e)
            return null
        }
    }

    // Método alternativo usando o endpoint de tokeninfo do Google
    fun verifyWithGoogle(idTokenString: String): GoogleUserInfo? {
        try {
            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/tokeninfo?id_token=$idTokenString")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.error("Erro ao verificar token com o Google: ${response.code}")
                    return null
                }

                val responseBody = response.body?.string() ?: return null
                val tokenInfo = objectMapper.readValue<Map<String, Any>>(responseBody)

                return GoogleUserInfo(
                    sub = tokenInfo["sub"] as String,
                    email = tokenInfo["email"] as String,
                    name = tokenInfo["name"] as String,
                    picture = tokenInfo["picture"] as? String
                )
            }
        } catch (e: Exception) {
            logger.error("Erro ao verificar token com o Google", e)
            return null
        }
    }
}

data class GoogleUserInfo(
    val sub: String,
    val email: String,
    val name: String,
    val picture: String?
)
