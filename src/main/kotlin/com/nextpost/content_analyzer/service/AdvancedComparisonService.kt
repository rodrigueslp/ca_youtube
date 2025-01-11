package com.nextpost.content_analyzer.service

import com.nextpost.content_analyzer.model.dto.*
import com.nextpost.content_analyzer.model.entity.Channel
import com.nextpost.content_analyzer.model.entity.ChannelMetrics
import org.springframework.stereotype.Service
import kotlin.math.pow

@Service
class AdvancedComparisonService(
    private val youTubeService: YouTubeService,
    private val metricsService: MetricsService,
    private val comparisonService: ComparisonService
) {
    fun analyzeChannels(channelIds: List<String>): AdvancedComparisonDTO {
        val channels = channelIds.map { youTubeService.getChannel(it) ?: throw IllegalArgumentException("Channel not found: $it") }
        val baseComparison = comparisonService.compareChannels(channelIds)

        val totalSubscribers = channels.sumOf { it.subscriberCount }
        val averageGrowthRate = channels.map { calculateGrowthRate(it) }.average()

        val advancedMetrics = channels.map { channel ->
            val metrics = metricsService.calculateChannelMetrics(channel)
            val baseMetrics = baseComparison.channels.find { it.channelId == channel.channelId }?.metrics
                ?: throw IllegalStateException("Metrics not found for channel: ${channel.channelId}")

            val efficiency = calculateEfficiencyScore(channel, metrics)
            val consistency = calculateConsistencyIndex(channel, metrics)
            val marketShare = (channel.subscriberCount.toDouble() / totalSubscribers) * 100
            val growthVelocity = calculateGrowthVelocity(channel, averageGrowthRate)

            AdvancedChannelMetricsDTO(
                channelId = channel.channelId,
                title = channel.title,
                baseMetrics = baseMetrics,
                advancedMetrics = AdvancedMetricsDTO(
                    efficiencyScore = efficiency,
                    consistencyIndex = consistency,
                    marketShareScore = marketShare,
                    growthVelocity = growthVelocity,
                    trendsAnalysis = analyzeTrends(channel, metrics)
                )
            )
        }

        return AdvancedComparisonDTO(
            channels = advancedMetrics,
            marketAnalysis = analyzeMarket(channels, advancedMetrics)
        )
    }

    private fun calculateEfficiencyScore(channel: Channel, metrics: ChannelMetrics): Double {
        val monthlyGrowth = calculateGrowthRate(channel)
        val videosPerMonth = metrics.videosPerMonth

        // Eficiência = crescimento percentual / número de vídeos
        return if (videosPerMonth > 0) {
            (monthlyGrowth / videosPerMonth).coerceIn(0.0, 100.0)
        } else 0.0
    }

    private fun calculateConsistencyIndex(channel: Channel, metrics: ChannelMetrics): Double {
        val uploadConsistency = calculateUploadConsistency(metrics)
        val growthConsistency = calculateGrowthConsistency(channel)

        return ((uploadConsistency + growthConsistency) / 2).coerceIn(0.0, 100.0)
    }

    private fun calculateUploadConsistency(metrics: ChannelMetrics): Double {
        // Analisa a regularidade dos uploads
        val expectedUploadsPerWeek = metrics.videosPerWeek
        val actualUploadsVariance = 0.0 // TODO: Implementar cálculo de variância real

        return (100 * (1 - (actualUploadsVariance / expectedUploadsPerWeek))).coerceIn(0.0, 100.0)
    }

    private fun calculateGrowthConsistency(channel: Channel): Double {
        val stats = youTubeService.getChannelStats(channel.channelId)
        if (stats.size < 2) return 0.0

        val growthRates = stats.zipWithNext().map { (newer, older) ->
            (newer.subscriberCount - older.subscriberCount).toDouble() / older.subscriberCount
        }

        val average = growthRates.average()
        val variance = growthRates.map { (it - average).pow(2) }.average()

        return (100 * (1 - variance)).coerceIn(0.0, 100.0)
    }

    private fun calculateGrowthVelocity(channel: Channel, averageGrowthRate: Double): Double {
        val channelGrowthRate = calculateGrowthRate(channel)
        return ((channelGrowthRate / averageGrowthRate) * 100).coerceIn(0.0, 200.0)
    }

    private fun calculateGrowthRate(channel: Channel): Double {
        val stats = youTubeService.getChannelStats(channel.channelId)
        if (stats.size < 2) return 0.0

        val newest = stats.first()
        val oldest = stats.last()
        return ((newest.subscriberCount - oldest.subscriberCount).toDouble() / oldest.subscriberCount) * 100
    }

    private fun analyzeTrends(channel: Channel, metrics: ChannelMetrics): TrendsAnalysisDTO {
        val uploadTrend = analyzeUploadTrend(metrics)
        val growthTrend = analyzeGrowthTrend(channel)
        val viewsTrend = analyzeViewsTrend(channel)

        return TrendsAnalysisDTO(
            uploadTrend = uploadTrend,
            growthTrend = growthTrend,
            viewsTrend = viewsTrend,
            bestPerformingDay = metrics.mostCommonUploadDay.toString(),
            predictedGrowth = predictGrowth(channel)
        )
    }

    private fun analyzeUploadTrend(metrics: ChannelMetrics): String {
        return when {
            metrics.videosPerMonth > metrics.videosPerWeek * 4 * 1.1 -> "increasing"
            metrics.videosPerMonth < metrics.videosPerWeek * 4 * 0.9 -> "decreasing"
            else -> "stable"
        }
    }

    private fun analyzeGrowthTrend(channel: Channel): String {
        val stats = youTubeService.getChannelStats(channel.channelId)
        if (stats.size < 3) return "stable"

        val growthRates = stats.zipWithNext().map { (newer, older) ->
            (newer.subscriberCount - older.subscriberCount).toDouble() / older.subscriberCount
        }

        val recentGrowth = growthRates.first()
        val averageGrowth = growthRates.average()

        return when {
            recentGrowth > averageGrowth * 1.1 -> "increasing"
            recentGrowth < averageGrowth * 0.9 -> "decreasing"
            else -> "stable"
        }
    }

    private fun analyzeViewsTrend(channel: Channel): String {
        val stats = youTubeService.getChannelStats(channel.channelId)
        if (stats.size < 3) return "stable"

        val viewsGrowth = stats.zipWithNext().map { (newer, older) ->
            (newer.viewCount - older.viewCount).toDouble() / older.viewCount
        }

        val recentGrowth = viewsGrowth.first()
        val averageGrowth = viewsGrowth.average()

        return when {
            recentGrowth > averageGrowth * 1.1 -> "increasing"
            recentGrowth < averageGrowth * 0.9 -> "decreasing"
            else -> "stable"
        }
    }

    private fun predictGrowth(channel: Channel): Double {
        val stats = youTubeService.getChannelStats(channel.channelId)
        if (stats.size < 3) return 0.0

        val growthRates = stats.zipWithNext().map { (newer, older) ->
            (newer.subscriberCount - older.subscriberCount).toDouble() / older.subscriberCount
        }

        // Média móvel ponderada com mais peso para dados recentes
        val weights = List(growthRates.size) { idx -> (idx + 1).toDouble() }
        val weightSum = weights.sum()

        return (growthRates.zip(weights) { rate, weight -> rate * weight }.sum() / weightSum)
            .coerceIn(-100.0, 100.0)
    }

    private fun analyzeMarket(
        channels: List<Channel>,
        metrics: List<AdvancedChannelMetricsDTO>
    ): MarketAnalysisDTO {
        val totalSubscribers = channels.sumOf { it.subscriberCount }
        val averageGrowthRate = channels.map { calculateGrowthRate(it) }.average()

        val competitiveIndex = channels.associate { channel ->
            val metric = metrics.find { it.channelId == channel.channelId }
                ?: throw IllegalStateException("Metrics not found for channel: ${channel.channelId}")

            val marketShare = (channel.subscriberCount.toDouble() / totalSubscribers) * 100
            val efficiency = metric.advancedMetrics.efficiencyScore
            val consistency = metric.advancedMetrics.consistencyIndex

            // Índice competitivo baseado em market share, eficiência e consistência
            val index = (marketShare * 0.4 + efficiency * 0.3 + consistency * 0.3)

            channel.channelId to index
        }

        val positions = channels.map { channel ->
            val marketShare = (channel.subscriberCount.toDouble() / totalSubscribers) * 100
            val strength = when {
                marketShare > 40 -> "leader"
                marketShare > 20 -> "challenger"
                else -> "follower"
            }

            MarketPosition(
                channelId = channel.channelId,
                title = channel.title,
                marketShare = marketShare,
                competitiveStrength = strength
            )
        }

        return MarketAnalysisDTO(
            totalMarketSize = totalSubscribers,
            averageGrowthRate = averageGrowthRate,
            competitiveIndex = competitiveIndex,
            marketPositions = positions
        )
    }
}
