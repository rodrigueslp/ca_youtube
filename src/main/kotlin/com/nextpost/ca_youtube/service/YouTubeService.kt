package com.nextpost.ca_youtube.service

import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.VideoStatistics
import com.nextpost.ca_youtube.model.dto.ChannelDTO
import com.nextpost.ca_youtube.model.dto.ChannelStatsDTO
import com.nextpost.ca_youtube.model.dto.VideoDTO
import com.nextpost.ca_youtube.model.entity.Channel
import com.nextpost.ca_youtube.model.entity.ChannelStats
import com.nextpost.ca_youtube.repository.ChannelRepository
import com.nextpost.ca_youtube.repository.ChannelStatsRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
class YouTubeService(
    private val youtube: YouTube,
    private val channelRepository: ChannelRepository,
    private val channelStatsRepository: ChannelStatsRepository,
    @Autowired private val apiKey: String
) {
    private val logger = LoggerFactory.getLogger(YouTubeService::class.java)

    fun getChannel(channelId: String): Channel? {
        return channelRepository.findByChannelId(channelId)
    }

    fun getAllChannels(): List<ChannelDTO> {
        return channelRepository.findAll().map { channel ->
            ChannelDTO(
                channelId = channel.channelId,
                title = channel.title,
                description = channel.description,
                subscriberCount = channel.subscriberCount,
                videoCount = channel.videoCount,
                viewCount = channel.viewCount
            )
        }
    }

    @Transactional
    fun deleteChannel(channelId: String) {
        val channel = getChannel(channelId)
            ?: throw IllegalArgumentException("Channel not found")

        // Deletar registros de stats relacionados ao canal
        channelStatsRepository.deleteByChannel(channel)

        // Deletar o canal
        channelRepository.delete(channel)
        logger.info("Channel with ID $channelId and its stats deleted successfully")
    }


    @Transactional
    fun deleteAllChannels() {
        channelRepository.deleteAll()
        logger.info("All channels deleted successfully")
    }

    @Transactional
    fun addChannelToTrack(channelIdentifier: String): ChannelDTO {
        val channelId = if (channelIdentifier.startsWith("UC")) {
            channelIdentifier // Já é um ID de canal válido
        } else {
            resolveChannelIdFromHandle(channelIdentifier)
                ?: throw IllegalArgumentException("Invalid YouTube handle or channel not found: $channelIdentifier")
        }

        return addChannelById(channelId)
    }

    private fun resolveChannelIdFromHandle(handle: String): String? {
        try {
            // Remove o "@" se estiver presente
            val cleanHandle = handle.removePrefix("@")

            val searchResponse = youtube.search()
                .list(listOf("snippet"))
                .setKey(apiKey)
                .setQ(cleanHandle) // Handle sem "@"
                .setType(listOf("channel")) // Busca apenas canais
                .setMaxResults(1L)
                .execute()

            val channel = searchResponse.items.firstOrNull()
            return channel?.snippet?.channelId // Retorna o ID do canal
        } catch (e: Exception) {
            logger.error("Failed to resolve channel ID for handle {}: ", handle, e)
            return null
        }
    }


    private fun addChannelById(channelId: String): ChannelDTO {
        logger.debug("Starting to track channel with ID: {}", channelId)

        try {
            val channelResponse = youtube.channels()
                .list(listOf("snippet", "statistics"))
                .setKey(apiKey)
                .setId(listOf(channelId))
                .execute()

            if (channelResponse.items.isNullOrEmpty()) {
                logger.error("No channel found for ID: {}", channelId)
                throw IllegalArgumentException("Channel not found")
            }

            val channelItem = channelResponse.items.first()
            val statistics = channelItem.statistics
            val snippet = channelItem.snippet

            val channel = Channel(
                channelId = channelId,
                title = snippet.title,
                description = snippet.description,
                subscriberCount = statistics.subscriberCount.toLong(),
                videoCount = statistics.videoCount.toLong(),
                viewCount = statistics.viewCount.toLong()
            )

            val savedChannel = channelRepository.save(channel)

            val stats = ChannelStats(
                channel = savedChannel,
                subscriberCount = statistics.subscriberCount.toLong(),
                videoCount = statistics.videoCount.toLong(),
                viewCount = statistics.viewCount.toLong()
            )
            channelStatsRepository.save(stats)

            return ChannelDTO(
                channelId = savedChannel.channelId,
                title = savedChannel.title,
                description = savedChannel.description,
                subscriberCount = savedChannel.subscriberCount,
                videoCount = savedChannel.videoCount,
                viewCount = savedChannel.viewCount
            )
        } catch (e: Exception) {
            logger.error("Error while tracking channel {}: ", channelId, e)
            throw e
        }
    }

    @Transactional
    fun updateChannelStats(channelId: String): ChannelStatsDTO {
        val channel = getChannel(channelId)
            ?: throw IllegalArgumentException("Channel not tracked")

        try {
            val channelResponse = youtube.channels()
                .list(listOf("statistics"))
                .setKey(apiKey)
                .setId(listOf(channelId))
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

            channelStatsRepository.save(newStats)

            // Update channel with latest stats
            channel.subscriberCount = statistics.subscriberCount.toLong()
            channel.videoCount = statistics.videoCount.toLong()
            channel.viewCount = statistics.viewCount.toLong()
            channel.updatedAt = LocalDateTime.now()
            channelRepository.save(channel)

            // Calculate growth rate
            val previousStats = channelStatsRepository
                .findByChannelOrderByCollectedAtDesc(channel)
                .getOrNull(1)

            val growthRate = if (previousStats != null) {
                ((newStats.subscriberCount - previousStats.subscriberCount).toDouble() /
                        previousStats.subscriberCount) * 100
            } else null

            return ChannelStatsDTO(
                channelId = channel.channelId,
                subscriberCount = newStats.subscriberCount,
                videoCount = newStats.videoCount,
                viewCount = newStats.viewCount,
                growthRate = growthRate,
                collectedAt = newStats.collectedAt
            )
        } catch (e: Exception) {
            logger.error("Error updating stats for channel {}: ", channelId, e)
            throw e
        }
    }

    fun getChannelStats(channelId: String): List<ChannelStatsDTO> {
        val channel = getChannel(channelId)
            ?: throw IllegalArgumentException("Channel not tracked")

        return channelStatsRepository
            .findLatestStatsByDay(channel)
            .map { stats ->
                ChannelStatsDTO(
                    channelId = channel.channelId,
                    subscriberCount = stats.subscriberCount,
                    videoCount = stats.videoCount,
                    viewCount = stats.viewCount,
                    collectedAt = stats.collectedAt
                )
            }
    }

    fun getRecentVideos(channelId: String): List<VideoDTO> {
        val channel = getChannel(channelId) ?: throw IllegalArgumentException("Channel not found")

        val searchResponse = youtube.search()
            .list(listOf("id", "snippet"))
            .setKey(apiKey)
            .setChannelId(channelId)
            .setOrder("date")
            .setType(listOf("video"))
            .setMaxResults(3L)
            .execute()

        return searchResponse.items.map { item ->
            val videoResponse = youtube.videos()
                .list(listOf("snippet", "statistics", "contentDetails"))
                .setKey(apiKey)
                .setId(listOf(item.id.videoId))
                .execute()

            val video = videoResponse.items.first()

            // Converter a data corretamente
            val publishedAt = Instant.parse(video.snippet.publishedAt.toStringRfc3339())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()

            VideoDTO(
                videoId = video.id,
                title = video.snippet.title,
                description = video.snippet.description,
                publishedAt = publishedAt,
                duration = Duration.parse(video.contentDetails.duration),
                viewCount = video.statistics.viewCount.toLong(),
                categoryId = video.snippet.categoryId,
                thumbnailUrl = video.snippet.thumbnails.default.url  // ou medium/high para diferentes tamanhos

            )
        }
    }

    private fun calculateTimeAgo(publishedAt: String): String {
        val publishDate = LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME)
        val now = LocalDateTime.now()
        val days = ChronoUnit.DAYS.between(publishDate, now)

        return when {
            days == 0L -> "Today"
            days == 1L -> "Yesterday"
            days < 7 -> "$days days ago"
            days < 30 -> "${days/7} weeks ago"
            else -> "${days/30} months ago"
        }
    }

    private fun calculateEngagement(statistics: VideoStatistics?): String {
        if (statistics == null) return "0.0"

        val views = statistics.viewCount.toLong()
        if (views == 0L) return "0.0"

        val interactions = (statistics.likeCount ?: 0).toLong()
        return String.format("%.1f", (interactions.toDouble() / views) * 100)
    }

    private fun determineTrend(statistics: VideoStatistics?): String {
        if (statistics == null) return "down"

        // Lógica simplificada: se engajamento > 5%, consideramos tendência positiva
        val engagement = calculateEngagement(statistics).toDouble()
        return if (engagement > 5.0) "up" else "down"
    }

    @Transactional
    fun updateAllChannels() {
        val channels = channelRepository.findAll() // Busca todos os canais cadastrados

        logger.info("Found ${channels.size} channels to update")
        channels.forEach { channel ->
            try {
                updateChannelStats(channel.channelId) // Atualiza as estatísticas de cada canal
                logger.info("Successfully updated stats for channel ID: ${channel.channelId}")
            } catch (e: Exception) {
                logger.error("Failed to update stats for channel ID: ${channel.channelId}", e)
            }
        }
    }

}
