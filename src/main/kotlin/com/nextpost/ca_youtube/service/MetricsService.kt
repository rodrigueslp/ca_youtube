package com.nextpost.ca_youtube.service

import com.google.api.services.youtube.YouTube
import com.nextpost.ca_youtube.util.toDTO
import com.nextpost.ca_youtube.model.dto.VideoDTO
import com.nextpost.ca_youtube.model.entity.Channel
import com.nextpost.ca_youtube.model.entity.ChannelMetrics
import com.nextpost.ca_youtube.model.entity.Video
import com.nextpost.ca_youtube.repository.ChannelMetricsRepository
import com.nextpost.ca_youtube.repository.VideoRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.*
import kotlin.math.roundToInt

@Service
class MetricsService(
    private val youtube: YouTube,
    private val videoRepository: VideoRepository,
    private val channelMetricsRepository: ChannelMetricsRepository,
    @Autowired private val apiKey: String
) {
    private val logger = LoggerFactory.getLogger(MetricsService::class.java)

    @Transactional
    fun updateChannelVideos(channel: Channel) {
        try {
            val videosResponse = youtube.search()
                .list(listOf("id", "snippet"))
                .setKey(apiKey)
                .setChannelId(channel.channelId)
                .setOrder("date")
                .setType(listOf("video"))
                .setMaxResults(50L)
                .execute()

            val videoIds = videosResponse.items.map { it.id.videoId }

            val videoDetails = youtube.videos()
                .list(listOf("snippet", "contentDetails", "statistics"))
                .setKey(apiKey)
                .setId(videoIds)
                .execute()

            videoDetails.items.forEach { videoItem ->
                val existingVideo = videoRepository.findByVideoId(videoItem.id)

                val publishedAt = Instant.parse(videoItem.snippet.publishedAt.toStringRfc3339())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()

                val video = existingVideo?.copy(
                    title = videoItem.snippet.title,
                    description = videoItem.snippet.description,
                    publishedAt = publishedAt,
                    duration = Duration.parse(videoItem.contentDetails.duration),
                    viewCount = videoItem.statistics.viewCount.toLong(),
                    categoryId = videoItem.snippet.categoryId,
                    likeCount = videoItem.statistics.likeCount?.toLong() ?: 0,
                    commentCount = videoItem.statistics.commentCount?.toLong() ?: 0
                ) ?: Video(
                    channel = channel,
                    videoId = videoItem.id,
                    title = videoItem.snippet.title,
                    description = videoItem.snippet.description,
                    publishedAt = publishedAt,
                    duration = Duration.parse(videoItem.contentDetails.duration),
                    viewCount = videoItem.statistics.viewCount.toLong(),
                    categoryId = videoItem.snippet.categoryId,
                    likeCount = videoItem.statistics.likeCount?.toLong() ?: 0,
                    commentCount = videoItem.statistics.commentCount?.toLong() ?: 0,
                    shareCount = 0
                )

                videoRepository.save(video)
            }
        } catch (e: Exception) {
            logger.error("Error updating videos for channel ${channel.channelId}: ", e)
            throw e
        }
    }

    @Transactional
    fun calculateChannelMetrics(channel: Channel): ChannelMetrics {
        val now = LocalDateTime.now()
        val oneMonthAgo = now.minusMonths(1)
        val oneWeekAgo = now.minusWeeks(1)
        val oneDayAgo = now.minusDays(1)

        val videos = videoRepository.findByChannel(channel)
        val recentVideos = videos.filter { it.publishedAt.isAfter(oneMonthAgo) }

        // Calculando crescimento
        val dailyGrowth = calculateGrowthRate(channel, oneDayAgo)
        val weeklyGrowth = calculateGrowthRate(channel, oneWeekAgo)
        val monthlyGrowth = calculateGrowthRate(channel, oneMonthAgo)

        // Calculando frequência de postagem
        val videosLastWeek = recentVideos.count { it.publishedAt.isAfter(oneWeekAgo) }
        val videosLastMonth = recentVideos.size

        // Calculando duração média
        val avgDuration = if (recentVideos.isNotEmpty()) {
            recentVideos
                .map { it.duration.seconds }
                .average()
                .roundToInt()
        } else 0

        // Analisando horários de postagem
        val uploadHours = recentVideos.groupBy { it.publishedAt.hour }
        val mostCommonHour = uploadHours.maxByOrNull { it.value.size }?.key ?: 0

        val uploadDays = recentVideos.groupBy { it.publishedAt.dayOfWeek.value }
        val mostCommonDay = uploadDays.maxByOrNull { it.value.size }?.key ?: 1

        // Analisando categorias
        val categories = recentVideos.groupBy { it.categoryId }
        val topCategory = categories.maxByOrNull { it.value.size }
        val topCategoryPercentage = topCategory?.let {
            (it.value.size.toDouble() / recentVideos.size) * 100
        } ?: 0.0

        return ChannelMetrics(
            channel = channel,
            dailySubscriberGrowth = dailyGrowth,
            weeklySubscriberGrowth = weeklyGrowth,
            monthlySubscriberGrowth = monthlyGrowth,
            dailyViewGrowth = calculateViewGrowth(channel, oneDayAgo),
            videosPerWeek = videosLastWeek.toDouble(),
            videosPerMonth = videosLastMonth.toDouble(),
            averageVideoDuration = avgDuration.toLong(),
            mostCommonUploadHour = mostCommonHour,
            mostCommonUploadDay = mostCommonDay,
            topCategoryId = topCategory?.key ?: "",
            topCategoryPercentage = topCategoryPercentage
        )
    }

    private fun calculateGrowthRate(channel: Channel, fromDate: LocalDateTime): Double {
        val stats = channelMetricsRepository.findMetricsInPeriod(channel, fromDate)
        if (stats.size < 2) return 0.0

        val oldestStat = stats.last()
        val newestStat = stats.first()

        return ((newestStat.dailySubscriberGrowth - oldestStat.dailySubscriberGrowth) /
                oldestStat.dailySubscriberGrowth) * 100
    }

    private fun calculateViewGrowth(channel: Channel, fromDate: LocalDateTime): Double {
        val videos = videoRepository.findByChannelAndPublishedAtAfter(channel, fromDate)
        return videos.sumOf { it.viewCount }.toDouble()
    }

    fun getRecentVideos(channel: Channel, limit: Int = 10): List<VideoDTO> {
        return videoRepository.findRecentVideos(channel)
            .take(limit)
            .map { it.toDTO() }
    }
}
