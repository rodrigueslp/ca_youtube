package com.nextpost.ca_youtube.controller

import com.nextpost.ca_youtube.service.ContentAnalysisService
import com.nextpost.ca_youtube.service.ContentPatternMetrics
import com.nextpost.ca_youtube.service.RetentionMetrics
import com.nextpost.ca_youtube.service.YoutubeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/content-analysis")
class ContentAnalysisController(
    private val contentAnalysisService: ContentAnalysisService,
    private val youTubeService: YoutubeService
) {
    @GetMapping("/channels/{channelId}/retention")
    fun analyzeRetention(
        @PathVariable channelId: String
    ): ResponseEntity<RetentionMetrics> {
        val channel = youTubeService.getChannel(channelId)
            ?: throw IllegalArgumentException("Channel not found")

        val analysis = contentAnalysisService.analyzeRetention(channel)
        return ResponseEntity.ok(analysis)
    }

    @GetMapping("/channels/{channelId}/patterns")
    fun analyzeContentPatterns(
        @PathVariable channelId: String
    ): ResponseEntity<ContentPatternMetrics> {
        val channel = youTubeService.getChannel(channelId)
            ?: throw IllegalArgumentException("Channel not found")

        val analysis = contentAnalysisService.analyzeContentPatterns(channel)
        return ResponseEntity.ok(analysis)
    }
}