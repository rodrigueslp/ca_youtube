package com.nextpost.content_analyzer.model.entity

import com.nextpost.content_analyzer.model.dto.VideoDTO
import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.Duration

@Entity
@Table(name = "videos")
data class Video(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    val channel: Channel,

    @Column(nullable = false, unique = true)
    val videoId: String,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(name = "published_at", nullable = false)
    val publishedAt: LocalDateTime,

    @Column(name = "duration")
    val duration: Duration,

    @Column(name = "view_count")
    var viewCount: Long,

    @Column(name = "category_id")
    val categoryId: String,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
