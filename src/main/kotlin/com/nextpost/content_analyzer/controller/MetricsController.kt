package com.nextpost.content_analyzer.controller

import com.nextpost.content_analyzer.model.dto.ChannelMetricsDTO
import com.nextpost.content_analyzer.model.dto.DetailedChannelStatsDTO
import com.nextpost.content_analyzer.model.dto.VideoDTO
import com.nextpost.content_analyzer.service.MetricsService
import com.nextpost.content_analyzer.service.YouTubeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.nextpost.content_analyzer.util.toDTO

@RestController
@RequestMapping("/api/metrics")
class MetricsController(
    private val metricsService: MetricsService,
    private val youTubeService: YouTubeService
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

    @GetMapping("/channels/{channelId}/metrics")
    fun getChannelMetrics(@PathVariable channelId: String): ResponseEntity<ChannelMetricsDTO> {
        val channel = youTubeService.getChannel(channelId)
            ?: throw IllegalArgumentException("Channel not found")

        val metrics = metricsService.calculateChannelMetrics(channel)
        return ResponseEntity.ok(metrics.toDTO())
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
