package com.nextpost.ca_youtube.model.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "youtube_token")
data class YouTubeToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id")
    val userId: Long,

    @Column(name = "access_token", length = 2000)
    var accessToken: String,

    @Column(name = "refresh_token", length = 2000)
    var refreshToken: String?,

    @Column(name = "expires_at")
    var expiresAt: LocalDateTime,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    val user: User? = null
)
