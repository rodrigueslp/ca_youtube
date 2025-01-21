package com.nextpost.ca_youtube.controller

import com.nextpost.ca_youtube.model.dto.ChannelMetricsDTO
import com.nextpost.ca_youtube.model.dto.DetailedChannelStatsDTO
import com.nextpost.ca_youtube.model.dto.VideoDTO
import com.nextpost.ca_youtube.model.entity.ChannelStats
import com.nextpost.ca_youtube.service.MetricsService
import com.nextpost.ca_youtube.service.YoutubeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.nextpost.ca_youtube.util.toDTO

@RestController
@RequestMapping("/api/metrics")
class MetricsController(
    private val metricsService: MetricsService,
    private val youTubeService: YoutubeService
) {

    @PostMapping("/channels/{channelId}/analyze")
    fun analyzeChannel(@PathVariable channelId: String): ResponseEntity<DetailedChannelStatsDTO> {
        val channel = youTubeService.getChannel(channelId)
            ?: throw IllegalArgumentException("Channel not found")

        metricsService.updateChannelVideos(channel)
        val metrics = metricsService.calculateChannelMetrics(channel)

        // Converter para DTO e retornar
        return ResponseEntity.ok(
            DetailedChannelStatsDTO(
                channelId = channel.channelId,
                title = channel.title,
                subscriberCount = channel.subscriberCount,
                viewCount = channel.viewCount,
                videoCount = channel.videoCount,
                metrics = metrics.toDTO(),
                recentVideos = metricsService.getRecentVideos(channel)
            )
        )
    }

    @GetMapping("/channels/{channelId}/stats")
    fun getChannelStats(@PathVariable channelId: String): ResponseEntity<List<ChannelStats>> {  // Mudando para List
        val channel = youTubeService.getChannel(channelId)
            ?: throw IllegalArgumentException("Channel not found")

        val stats = metricsService.getChannelStats(channel)
        return ResponseEntity.ok(stats)
    }

    @PostMapping("/channels/{channelId}/stats")
    suspend fun updateChannelStats(@PathVariable channelId: String): ResponseEntity<ChannelStats> {
        val channel = youTubeService.getChannel(channelId)
            ?: throw IllegalArgumentException("Channel not found")

        val stats = metricsService.updateChannelStats(channel)
        return ResponseEntity.ok(stats)
    }

    @PostMapping("/channels/{channelId}/videos/update")
    fun updateChannelVideos(@PathVariable channelId: String): ResponseEntity<String> {
        val channel = youTubeService.getChannel(channelId)
            ?: throw IllegalArgumentException("Channel not found")

        metricsService.updateChannelVideos(channel)
        return ResponseEntity.ok("Videos updated successfully")
    }

    @GetMapping("/channels/{channelId}/metrics")
    fun getChannelMetrics(@PathVariable channelId: String): ResponseEntity<ChannelMetricsDTO> {
        val channel = youTubeService.getChannel(channelId)
            ?: throw IllegalArgumentException("Channel not found")

        val metric = metricsService.getChannelMetrics(channel)
        return ResponseEntity.ok(metric.toDTO())
    }

    @GetMapping("/channels/{channelId}/videos")
    fun getChannelVideos(
        @PathVariable channelId: String,
        @RequestParam(required = false) limit: Int?
    ): ResponseEntity<List<VideoDTO>> {
        val channel = youTubeService.getChannel(channelId)
            ?: throw IllegalArgumentException("Channel not found")

        val videos = metricsService.getRecentVideos(channel, limit ?: 10)
        return ResponseEntity.ok(videos)
    }
}
