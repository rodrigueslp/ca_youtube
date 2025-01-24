package com.nextpost.ca_youtube.service

import com.nextpost.ca_youtube.repository.YoutubeTokenRepository
import com.nextpost.ca_youtube.model.entity.YoutubeAcessToken
import com.nextpost.ca_youtube.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class YoutubeTokenService(
    private val youtubeTokenRepository: YoutubeTokenRepository,
    private val userChannelRepository: UserRepository
) {
    @Transactional
    fun saveToken(
        userId: Long,
        accessToken: String,
        refreshToken: String?,
        expiresIn: Long
    ): YoutubeAcessToken {
        val expiresAt = LocalDateTime.now().plusSeconds(expiresIn)

        val existingToken = youtubeTokenRepository.findByUserId(userId)

        return if (existingToken.isPresent) {
            existingToken.get().apply {
                this.accessToken = accessToken
                this.refreshToken = refreshToken ?: this.refreshToken
                this.expiresAt = expiresAt
                this.updatedAt = LocalDateTime.now()
            }.also { youtubeTokenRepository.save(it) }
        } else {
            YoutubeAcessToken(
                userId = userId,
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAt = expiresAt
            ).also { youtubeTokenRepository.save(it) }
        }
    }

    fun getValidToken(userId: Long): YoutubeAcessToken {
        val token = youtubeTokenRepository.findByUserId(userId)
            .orElseThrow { IllegalArgumentException("No YouTube token found for user") }

        if (token.expiresAt.isBefore(LocalDateTime.now())) {
            // TODO: Implement token refresh logic
            throw IllegalArgumentException("Token expired")
        }

        return token
    }

}
