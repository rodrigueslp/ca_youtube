package com.nextpost.ca_youtube.controller

import com.nextpost.ca_youtube.model.entity.YouTubeToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface YouTubeTokenRepository : JpaRepository<YouTubeToken, Long> {
    fun findByUserId(userId: Long): Optional<YouTubeToken>
    fun findByUserIdAndExpiresAtGreaterThan(userId: Long, currentTime: LocalDateTime): Optional<YouTubeToken>
}