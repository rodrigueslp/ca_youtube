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
import com.nextpost.ca_youtube.repository.UserRepository
import com.nextpost.ca_youtube.service.batch.BatchProcessingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
class YoutubeService(
    private val youtube: YouTube,
    private val channelRepository: ChannelRepository,
    private val channelStatsRepository: ChannelStatsRepository,
    private val userRepository: UserRepository,
    private val metricsService: MetricsService,
    @Autowired private val apiKey: String
)  {
    private val logger = LoggerFactory.getLogger(YoutubeService::class.java)

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
    fun addChannelToTrackForUser(channelIdentifier: String, userId: Long): ChannelDTO {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Tenta encontrar o canal existente primeiro
        val channelId = if (channelIdentifier.startsWith("UC")) {
            channelIdentifier
        } else {
            resolveChannelIdFromHandle(channelIdentifier)
                ?: throw IllegalArgumentException("Invalid YouTube handle or channel not found: $channelIdentifier")
        }

        // Verifica se o canal já existe no sistema
        val existingChannel = channelRepository.findByChannelId(channelId)

        val channel = if (existingChannel != null) {
            // Se o canal já existe, apenas adiciona ao usuário
            existingChannel
        } else {
            // Se não existe, cria um novo canal
            val newChannel = addChannelById(channelId)
            channelRepository.findByChannelId(newChannel.channelId)
                ?: throw IllegalStateException("Channel not found after creation")
        }

        // Adiciona o canal à lista do usuário
        user.channels.add(channel)
        userRepository.save(user)

        // Atualizar os vídeos
        metricsService.updateChannelVideos(channel)

        // Calcular e salvar métricas iniciais
        metricsService.calculateChannelMetrics(channel)

        return ChannelDTO(
            channelId = channel.channelId,
            title = channel.title,
            description = channel.description,
            subscriberCount = channel.subscriberCount,
            videoCount = channel.videoCount,
            viewCount = channel.viewCount
        )
    }

    fun getChannelsForUser(userId: Long): List<ChannelDTO> {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        return user.channels.map { channel ->
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

        // Retorna os vídeos do banco de dados
        return metricsService.getRecentVideos(channel, 3)

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

}
