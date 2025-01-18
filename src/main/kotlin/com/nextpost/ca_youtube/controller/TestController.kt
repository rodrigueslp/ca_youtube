package com.nextpost.ca_youtube.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController {

    @GetMapping("/api/test")
    fun testEndpoint(@AuthenticationPrincipal jwt: Jwt): Map<String, Any> {
        val email = jwt.claims["email"] as String
        val name = jwt.claims["name"] as String

        return mapOf(
            "message" to "Autenticado com sucesso!",
            "email" to email,
            "name" to name
        )
    }

}