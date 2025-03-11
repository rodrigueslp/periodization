package com.extrabox.periodization.controller

import com.extrabox.periodization.model.auth.JwtResponse
import com.extrabox.periodization.model.auth.TokenRefreshRequest
import com.extrabox.periodization.model.auth.TokenRefreshResponse
import com.extrabox.periodization.model.auth.UserInfoResponse
import com.extrabox.periodization.service.AuthenticationService
import com.extrabox.periodization.service.RefreshTokenService
import com.extrabox.periodization.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Endpoints para autenticação e gestão de usuários")
class AuthController(
    private val userService: UserService,
    private val refreshTokenService: RefreshTokenService,
    private val authenticationService: AuthenticationService
) {

    @GetMapping("/validate")
    @Operation(summary = "Validar token JWT", description = "Valida o token JWT e retorna as informações do usuário")
    fun validateToken(@RequestParam("token") token: String): ResponseEntity<JwtResponse> {
        val jwtResponse = authenticationService.authenticateWithGoogle(token)
        return ResponseEntity.ok(jwtResponse)
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar token", description = "Gera um novo token de acesso usando um refresh token")
    fun refreshToken(@RequestBody request: TokenRefreshRequest): ResponseEntity<TokenRefreshResponse> {
        val response = authenticationService.refreshToken(request.refreshToken)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalida o refresh token do usuário")
    fun logoutUser(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<Map<String, String>> {
        refreshTokenService.deleteByUserEmail(userDetails.username)
        return ResponseEntity.ok(mapOf("message" to "Log out bem-sucedido!"))
    }

    @GetMapping("/me")
    @Operation(summary = "Obter informações do usuário", description = "Retorna as informações do usuário autenticado")
    fun getUserInfo(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<UserInfoResponse> {
        val userInfoResponse = userService.getUserInfo(userDetails.username)
        return ResponseEntity.ok(userInfoResponse)
    }

    @GetMapping("/check-subscription")
    @Operation(summary = "Verificar assinatura", description = "Verifica se o usuário possui uma assinatura ativa")
    fun checkSubscription(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<Map<String, Any>> {
        val subscriptionInfo = userService.checkSubscription(userDetails.username)
        return ResponseEntity.ok(subscriptionInfo)
    }

    @PostMapping("/subscription/update")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar assinatura", description = "Atualiza a assinatura de um usuário (apenas admin)")
    fun updateSubscription(
        @RequestParam("email") email: String,
        @RequestParam("plan") plan: String,
        @RequestParam("months") months: Int
    ): ResponseEntity<Map<String, String>> {
        userService.updateSubscription(email, plan, months)
        return ResponseEntity.ok(mapOf("message" to "Assinatura atualizada com sucesso"))
    }
}