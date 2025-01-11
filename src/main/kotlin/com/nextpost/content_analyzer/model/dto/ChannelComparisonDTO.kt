package com.nextpost.content_analyzer.model.dto

data class ChannelComparisonDTO(
    val period: String,
    val channels: List<ChannelComparisonItemDTO>
)

data class ChannelComparisonItemDTO(
    val channelId: String,
    val title: String,
    val metrics: ComparisonMetricsDTO
)

data class ComparisonMetricsDTO(
    val subscriberCount: Long,
    val subscriberGrowthRate: Double,
    val viewCount: Long,
    val viewGrowthRate: Double,
    val videoCount: Int,
    val uploadFrequency: Double,
    val avgVideoLength: Long,
    val engagementScore: Double
)