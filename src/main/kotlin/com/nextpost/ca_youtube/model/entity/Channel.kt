package com.nextpost.ca_youtube.model.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "channels")
data class Channel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val channelId: String,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Column(name = "subscriber_count")
    var subscriberCount: Long,

    @Column(name = "video_count")
    var videoCount: Long,

    @Column(name = "view_count")
    var viewCount: Long,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
