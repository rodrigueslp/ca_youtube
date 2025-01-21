package com.nextpost.ca_youtube.service

import com.nextpost.ca_youtube.controller.YouTubeTokenRepository
import com.nextpost.ca_youtube.model.entity.YouTubeToken
import com.nextpost.ca_youtube.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class YoutubeTokenService(
    private val youTubeTokenRepository: YouTubeTokenRepository,
    private val userChannelRepository: UserRepository
) {
    @Transactional
    fun saveToken(
        userId: Long,
        accessToken: String,
        refreshToken: String?,
        expiresIn: Long
    ): YouTubeToken {
        val expiresAt = LocalDateTime.now().plusSeconds(expiresIn)

        val existingToken = youTubeTokenRepository.findByUserId(userId)

        return if (existingToken.isPresent) {
            existingToken.get().apply {
                this.accessToken = accessToken
                this.refreshToken = refreshToken ?: this.refreshToken
                this.expiresAt = expiresAt
                this.updatedAt = LocalDateTime.now()
            }.also { youTubeTokenRepository.save(it) }
        } else {
            YouTubeToken(
                userId = userId,
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAt = expiresAt
            ).also { youTubeTokenRepository.save(it) }
        }
    }

    fun getValidToken(userId: Long): YouTubeToken {
        val token = youTubeTokenRepository.findByUserId(userId)
            .orElseThrow { IllegalArgumentException("No YouTube token found for user") }

        if (token.expiresAt.isBefore(LocalDateTime.now())) {
            // TODO: Implement token refresh logic
            throw IllegalArgumentException("Token expired")
        }

        return token
    }

}
