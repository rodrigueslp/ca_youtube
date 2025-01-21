package com.nextpost.ca_youtube.service

import com.nextpost.ca_youtube.model.dto.ChannelComparisonDTO
import com.nextpost.ca_youtube.model.dto.ChannelComparisonItemDTO
import com.nextpost.ca_youtube.model.dto.ComparisonMetricsDTO
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class ComparisonService(
    private val youTubeService: YoutubeService,
    private val metricsService: MetricsService
) {
    fun compareChannels(channelIds: List<String>, period: String = "30d"): ChannelComparisonDTO {
        val startDate = when(period) {
            "7d" -> LocalDateTime.now().minus(7, ChronoUnit.DAYS)
            "30d" -> LocalDateTime.now().minus(30, ChronoUnit.DAYS)
            "90d" -> LocalDateTime.now().minus(90, ChronoUnit.DAYS)
            else -> throw IllegalArgumentException("Invalid period. Use: 7d, 30d, or 90d")
        }

        val comparisons = channelIds.map { channelId ->
            val channel = youTubeService.getChannel(channelId)
                ?: throw IllegalArgumentException("Channel not found: $channelId")

            val metrics = metricsService.calculateChannelMetrics(channel)
            val stats = youTubeService.getChannelStats(channelId)

            // Calcula métricas de crescimento
            val initialStats = stats.lastOrNull()
            val currentStats = stats.firstOrNull()

            val subscriberGrowth = if (initialStats != null && currentStats != null) {
                ((currentStats.subscriberCount - initialStats.subscriberCount).toDouble() /
                        initialStats.subscriberCount) * 100
            } else 0.0

            val viewGrowth = if (initialStats != null && currentStats != null) {
                ((currentStats.viewCount - initialStats.viewCount).toDouble() /
                        initialStats.viewCount) * 100
            } else 0.0

            // Calcula score de engajamento
            val engagementScore = calculateEngagementScore(
                currentStats?.viewCount ?: 0,
                currentStats?.subscriberCount ?: 0,
                metrics.videosPerMonth
            )

            ChannelComparisonItemDTO(
                channelId = channel.channelId,
                title = channel.title,
                metrics = ComparisonMetricsDTO(
                    subscriberCount = channel.subscriberCount,
                    subscriberGrowthRate = subscriberGrowth,
                    viewCount = channel.viewCount,
                    viewGrowthRate = viewGrowth,
                    videoCount = channel.videoCount.toInt(),
                    uploadFrequency = metrics.videosPerMonth,
                    avgVideoLength = metrics.averageVideoDuration,
                    engagementScore = engagementScore
                )
            )
        }

        return ChannelComparisonDTO(
            period = period,
            channels = comparisons
        )
    }

    private fun calculateEngagementScore(
        views: Long,
        subscribers: Long,
        monthlyVideos: Double
    ): Double {
        if (subscribers == 0L || monthlyVideos == 0.0) return 0.0

        // Fórmula básica: (views por inscrito) * (frequência de upload)
        val viewsPerSub = views.toDouble() / subscribers
        return (viewsPerSub * monthlyVideos).coerceIn(0.0, 100.0)
    }
}
