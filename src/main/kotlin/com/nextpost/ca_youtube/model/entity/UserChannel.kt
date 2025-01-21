package com.nextpost.ca_youtube.model.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "user_channels")
data class UserChannel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id")
    val channel: Channel,

    @Column(name = "has_oauth_access")
    var hasOAuthAccess: Boolean = false,

    @Column(name = "oauth_channel_id")
    var oauthChannelId: String? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
