package com.nextpost.content_analyzer.model.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "channel_stats")
data class ChannelStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    val channel: Channel,

    @Column(name = "subscriber_count")
    val subscriberCount: Long,

    @Column(name = "video_count")
    val videoCount: Long,

    @Column(name = "view_count")
    val viewCount: Long,

    @Column(name = "collected_at")
    val collectedAt: LocalDateTime = LocalDateTime.now()
)
