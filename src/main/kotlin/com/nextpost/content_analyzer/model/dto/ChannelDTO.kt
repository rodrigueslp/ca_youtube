package com.nextpost.content_analyzer.model.dto

import java.time.LocalDateTime

data class ChannelDTO(
    val channelId: String,
    val title: String,
    val description: String,
    val subscriberCount: Long,
    val videoCount: Long,
    val viewCount: Long
)

data class ChannelStatsDTO(
    val channelId: String,
    val subscriberCount: Long,
    val videoCount: Long,
    val viewCount: Long,
    val growthRate: Double? = null,
    val collectedAt: LocalDateTime
)
