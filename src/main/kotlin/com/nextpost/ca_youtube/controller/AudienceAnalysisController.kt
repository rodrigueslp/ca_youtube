package com.nextpost.ca_youtube.controller

import com.nextpost.ca_youtube.model.dto.*
import com.nextpost.ca_youtube.service.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/audience")
class AudienceAnalysisController(
    private val audienceAnalysisService: AudienceAnalysisService,
    private val youTubeService: YouTubeService
) {
    @PostMapping("/overlap")
    fun analyzeAudienceOverlap(
        @RequestBody channelIds: List<String>
    ): ResponseEntity<AudienceOverlapMetrics> {
        val analysis = audienceAnalysisService.analyzeAudienceOverlap(channelIds)
        return ResponseEntity.ok(analysis)
    }

    @GetMapping("/channels/{channelId}/behavior")
    fun analyzeAudienceBehavior(
        @PathVariable channelId: String
    ): ResponseEntity<AudienceBehaviorMetrics> {
        val channel = youTubeService.getChannel(channelId)
            ?: throw IllegalArgumentException("Channel not found")

        val analysis = audienceAnalysisService.analyzeAudienceBehavior(channel)
        return ResponseEntity.ok(analysis)
    }

    @GetMapping("/channels/{channelId}/segments")
    fun identifyTargetSegments(
        @PathVariable channelId: String
    ): ResponseEntity<List<AudienceSegment>> {
        val channel = youTubeService.getChannel(channelId)
            ?: throw IllegalArgumentException("Channel not found")

        val segments = audienceAnalysisService.identifyTargetSegments(channel)
        return ResponseEntity.ok(segments)
    }

    @PostMapping("/channels/{channelId}/recommendations")
    fun getAudienceBasedRecommendations(
        @PathVariable channelId: String,
        @RequestParam(required = false) segmentType: String?
    ): ResponseEntity<AudienceRecommendations> {
        val channel = youTubeService.getChannel(channelId)
            ?: throw IllegalArgumentException("Channel not found")

        // Se um segmentType for especificado, retorna recomendações específicas para aquele segmento
        // Caso contrário, retorna recomendações gerais baseadas em todos os segmentos
        val segments = if (segmentType != null) {
            audienceAnalysisService.identifyTargetSegments(channel)
                .filter { it.segmentType == segmentType }
        } else {
            audienceAnalysisService.identifyTargetSegments(channel)
        }

        return ResponseEntity.ok(
            AudienceRecommendations(
                channelId = channelId,
                recommendations = segments.flatMap { it.recommendedContent },
                targetSegments = segments.map { it.segmentType },
                potentialReach = calculatePotentialReach(segments)
            )
        )
    }

    private fun calculatePotentialReach(segments: List<AudienceSegment>): Double {
        return segments.sumOf { it.size }
    }
}

// Data class adicional para recomendações baseadas em audiência
data class AudienceRecommendations(
    val channelId: String,
    val recommendations: List<String>,
    val targetSegments: List<String>,
    val potentialReach: Double
)