package com.nextpost.ca_youtube.service

import com.google.api.services.youtube.YouTube
import com.nextpost.ca_youtube.util.toDTO
import com.nextpost.ca_youtube.model.dto.VideoDTO
import com.nextpost.ca_youtube.model.entity.Channel
import com.nextpost.ca_youtube.model.entity.ChannelMetrics
import com.nextpost.ca_youtube.model.entity.ChannelStats
import com.nextpost.ca_youtube.model.entity.Video
import com.nextpost.ca_youtube.repository.ChannelMetricsRepository
import com.nextpost.ca_youtube.repository.ChannelStatsRepository
import com.nextpost.ca_youtube.repository.VideoRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.*
import java.util.*

@Service
class MetricsService(
    private val youtube: YouTube,
    private val channelMetricsRepository: ChannelMetricsRepository,
    private val videoRepository: VideoRepository,
    private val channelStatsRepository: ChannelStatsRepository,
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
                    commentCount = videoItem.statistics.commentCount?.toLong() ?: 0,
                    thumbnail = videoItem.snippet.thumbnails.default.url
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
                    thumbnail = videoItem.snippet.thumbnails.default.url,
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
    suspend fun updateChannelStats(channel: Channel): ChannelStats {
        try {
            val channelResponse = youtube.channels()
                .list(listOf("statistics"))
                .setKey(apiKey)
                .setId(listOf(channel.channelId))
                .execute()

            if (channelResponse.items.isNullOrEmpty()) {
                throw IllegalArgumentException("Channel not found on YouTube")
            }

            val statistics = channelResponse.items.first().statistics

            val newStats = ChannelStats(
                channel = channel,
                subscriberCount = statistics.subscriberCount.toLong(),
                videoCount = statistics.videoCount.toLong(),
                viewCount = statistics.viewCount.toLong()
            )

            // Salvar as novas estatísticas
            channelStatsRepository.save(newStats)

            // Atualizar os vídeos
            updateChannelVideos(channel)

            // Calcular e salvar métricas
            calculateChannelMetrics(channel)

            return newStats
        } catch (e: Exception) {
            logger.error("Error updating stats for channel ${channel.channelId}: ", e)
            throw e
        }
    }

    @Transactional
    fun calculateChannelMetrics(channel: Channel): ChannelMetrics {
        val now = LocalDateTime.now()
        val oneMonthAgo = now.minusMonths(1)
        val oneWeekAgo = now.minusWeeks(1)
        val oneDayAgo = now.minusDays(1)

        // Calcular taxas de crescimento com os períodos corretos
        val dailyGrowth = calculateGrowthRate(channel, oneDayAgo)
        val weeklyGrowth = calculateGrowthRate(channel, oneWeekAgo)
        val monthlyGrowth = calculateGrowthRate(channel, oneMonthAgo)
        val dailyViews = calculateViewGrowthRate(channel, oneDayAgo)

        // Cálculos de vídeos por período
        val videos = videoRepository.findByChannel(channel)
        val recentVideos = videos.filter { it.publishedAt.isAfter(oneMonthAgo) }

        val videosLastWeek = recentVideos.count { it.publishedAt.isAfter(oneWeekAgo) }.toDouble()
        val videosLastMonth = recentVideos.size.toDouble()

        // Cálculo de duração média
        val avgDuration = if (recentVideos.isNotEmpty()) {
            recentVideos.map { it.duration.seconds }.average().toLong()
        } else 0L

        // Calcular padrão de uploads
        val uploadPatterns = calculateUploadPatterns(recentVideos)

        // Análise de horários de upload
        val uploadHours = recentVideos.groupBy { it.publishedAt.hour }
        val mostCommonHour = uploadHours.maxByOrNull { it.value.size }?.key ?: 0

        val uploadDays = recentVideos.groupBy { it.publishedAt.dayOfWeek.value }
        val mostCommonDay = uploadDays.maxByOrNull { it.value.size }?.key ?: 1

        // Análise de categorias
        val categories = recentVideos.groupBy { it.categoryId }
        val topCategory = categories.maxByOrNull { it.value.size }
        val topCategoryPercentage = if (recentVideos.isNotEmpty()) {
            (topCategory?.value?.size?.toDouble() ?: 0.0) / recentVideos.size * 100
        } else 0.0

        val channelMetrics =  ChannelMetrics(
            channel = channel,
            dailySubscriberGrowth = dailyGrowth,
            weeklySubscriberGrowth = weeklyGrowth,
            monthlySubscriberGrowth = monthlyGrowth,
            dailyViewGrowth = dailyViews,
            videosPerWeek = videosLastWeek,
            videosPerMonth = videosLastMonth,
            averageVideoDuration = avgDuration,
            uploadPatternByHour = uploadPatterns,
            mostCommonUploadHour = mostCommonHour,
            mostCommonUploadDay = mostCommonDay,
            topCategoryId = topCategory?.key ?: "",
            topCategoryPercentage = topCategoryPercentage
        )

        channelMetricsRepository.save(channelMetrics)

        return channelMetrics
    }

    private fun calculateGrowthRate(channel: Channel, fromDate: LocalDateTime): Double {
        val stats = channelStatsRepository.findStatsInPeriod(channel, fromDate)
        if (stats.size < 2) return 0.0

        val oldestStat = stats.minByOrNull { it.collectedAt }
        val newestStat = stats.maxByOrNull { it.collectedAt }

        if (oldestStat == null || newestStat == null || oldestStat.subscriberCount == 0L) {
            return 0.0
        }

        return ((newestStat.subscriberCount - oldestStat.subscriberCount).toDouble() /
                oldestStat.subscriberCount * 100)
            .let {
                if (it.isNaN()) 0.0
                else "%.2f".format(Locale.US, it).toDouble() // Usando Locale.US para garantir o ponto como separador decimal
            }
    }

    private fun calculateViewGrowthRate(channel: Channel, fromDate: LocalDateTime): Double {
        val stats = channelStatsRepository.findStatsInPeriod(channel, fromDate)
        if (stats.size < 2) return 0.0

        val oldestStat = stats.minByOrNull { it.collectedAt }
        val newestStat = stats.maxByOrNull { it.collectedAt }

        if (oldestStat == null || newestStat == null || oldestStat.viewCount == 0L) {
            return 0.0
        }

        return ((newestStat.viewCount - oldestStat.viewCount).toDouble() /
                oldestStat.viewCount * 100)
            .let {
                if (it.isNaN()) 0.0
                else "%.2f".format(Locale.US, it).toDouble() // Usando Locale.US para garantir o ponto como separador decimal
            }
    }

    private fun calculateUploadPatterns(videos: List<Video>): Map<Int, Int> {
        // Agrupa os vídeos por hora de upload e conta a frequência
        return videos
            .groupBy { it.publishedAt.hour }
            .mapValues { it.value.size }
            .let { hourMap ->
                // Garante que todas as 24 horas estejam representadas
                (0..23).associateWith { hour ->
                    hourMap[hour] ?: 0
                }
            }
    }

    fun getChannelStats(channel: Channel): List<ChannelStats> {  // Mudando para retornar List
        return channelStatsRepository.findByChannelOrderByCollectedAtDesc(channel)
            .takeIf { it.isNotEmpty() }
            ?: throw NoSuchElementException("No stats found for channel")
    }

    fun getChannelMetrics(channel: Channel): ChannelMetrics {
        return channelMetricsRepository.findFirstByChannelOrderByCollectedAtDesc(channel)
            ?: throw NoSuchElementException("No metrics found for channel ${channel.channelId}")
    }

    fun getRecentVideos(channel: Channel, limit: Int = 10): List<VideoDTO> {
        val videos = videoRepository.findByChannelOrderByPublishedAtDesc(channel)
            .take(limit)
            .map { video ->
                VideoDTO(
                    videoId = video.videoId,
                    title = video.title,
                    description = video.description,
                    publishedAt = video.publishedAt,
                    duration = video.duration,
                    viewCount = video.viewCount,
                    categoryId = video.categoryId,
                    thumbnailUrl = video.thumbnail.takeIf { it.isNotBlank() }
                        ?: "https://i.ytimg.com/vi/${video.videoId}/default.jpg"  // URL padrão do YouTube
                )
            }
        return videos
    }

}
