package com.nextpost.ca_youtube.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsConfig {

    @Bean
    fun corsFilter(): CorsFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()

        // Permite requisições de qualquer origem em desenvolvimento
        config.allowedOrigins = listOf(
            "http://localhost:3000",
            "http://localhost:3001",
            "https://ca_youtube.railway.internal",
            "https://ca-youtube.railway.internal")
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true
        config.allowedHeaders = listOf(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        )

        source.registerCorsConfiguration("/**", config)
        return CorsFilter(source)
    }
}
