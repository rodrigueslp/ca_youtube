package com.nextpost.ca_youtube.model.dto

import java.time.LocalDateTime

data class ChannelAnalytics(
    val views: Long,
    val estimatedMinutesWatched: Double,
    val averageViewDuration: Double,
    val averageViewPercentage: Double,
    val subscribersGained: Int,
    val subscribersLost: Int,
    val likes: Long,
    val comments: Long,
    val date: String
)

data class DemographicData(
    val ageGroup: String,
    val gender: String,
    val percentage: Double
)

data class TrafficSource(
    val source: String,
    val views: Long
)

data class VideoAnalytics(
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val analytics: Map<String, Double>,
    val publishedAt: LocalDateTime,
    val description: String?,
    val tags: List<String>?,
    val viewCount: Long,
    val likeCount: Long?,
    val commentCount: Long?,
    val retentionData: Map<String, Double>?,
    val trafficSources: List<Map<String, Double>>?,
    val deviceStats: Map<String, Double>?,
    val viewerDemographics: Map<String, Double>?
)

data class ChannelDetails(
    val channelId: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String,
    val subscriberCount: Long,
    val videoCount: Long,
    val analytics: ChannelAnalytics?,
    val demographics: List<DemographicData>?,
    val trafficSources: List<TrafficSource>?,
    val isOwnChannel: Boolean = false
)

data class YouTubeTokenInfo(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: LocalDateTime
)