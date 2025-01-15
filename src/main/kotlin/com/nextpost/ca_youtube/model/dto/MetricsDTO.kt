package com.nextpost.ca_youtube.model.dto

import java.time.Duration
import java.time.LocalDateTime

data class VideoDTO(
    val videoId: String,
    val title: String,
    val description: String,
    val publishedAt: LocalDateTime,
    val duration: Duration,
    val viewCount: Long,
    val categoryId: String,
    val thumbnailUrl: String
)

data class ChannelMetricsDTO(
    val channelId: String,
    // Crescimento
    val dailySubscriberGrowth: Double,
    val weeklySubscriberGrowth: Double,
    val monthlySubscriberGrowth: Double,
    val dailyViewGrowth: Double,
    // Métricas de conteúdo
    val videosPerWeek: Double,
    val videosPerMonth: Double,
    val averageVideoDuration: Duration,
    // Padrões de upload
    val mostCommonUploadHour: Int,
    val mostCommonUploadDay: String, // Retornamos o nome do dia para melhor legibilidade
    // Categorias
    val topCategory: String, // Nome da categoria ao invés do ID
    val topCategoryPercentage: Double,
    // Data da coleta
    val collectedAt: LocalDateTime
)

data class DetailedChannelStatsDTO(
    val channelId: String,
    val title: String,
    val subscriberCount: Long,
    val viewCount: Long,
    val videoCount: Long,
    val metrics: ChannelMetricsDTO,
    val recentVideos: List<VideoDTO>
)
