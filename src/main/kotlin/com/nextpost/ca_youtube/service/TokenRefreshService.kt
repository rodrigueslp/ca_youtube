package com.nextpost.ca_youtube.service

import com.nextpost.ca_youtube.model.entity.YoutubeAcessToken
import com.nextpost.ca_youtube.repository.YoutubeTokenRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime

@Service
class TokenRefreshService(
    private val youtubeTokenRepository: YoutubeTokenRepository,
    private val restTemplate: RestTemplate
) {
    @Value("\${youtube.analytics.client-id}")
    private lateinit var clientId: String

    @Value("\${youtube.analytics.client-secret}")
    private lateinit var clientSecret: String

    fun refreshTokenIfNeeded(token: YoutubeAcessToken): YoutubeAcessToken {
        if (token.expiresAt.isAfter(LocalDateTime.now())) {
            return token
        }

        if (token.refreshToken == null) {
            throw RuntimeException("No refresh token available")
        }

        val params = mapOf(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "refresh_token" to token.refreshToken,
            "grant_type" to "refresh_token"
        )

        val response = restTemplate.postForObject(
            "https://oauth2.googleapis.com/token",
            params,
            RefreshTokenResponse::class.java
        )

        response?.let {
            token.accessToken = it.access_token
            token.expiresAt = LocalDateTime.now().plusSeconds(it.expires_in)
            token.updatedAt = LocalDateTime.now()
            return youtubeTokenRepository.save(token)
        } ?: throw RuntimeException("Failed to refresh token")
    }

    data class RefreshTokenResponse(
        val access_token: String,
        val expires_in: Long,
        val scope: String,
        val token_type: String
    )
}
