package com.nextpost.content_analyzer.controller

import com.nextpost.content_analyzer.model.dto.ChannelComparisonDTO
import com.nextpost.content_analyzer.service.ComparisonService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/comparison")
class ComparisonController(
    private val comparisonService: ComparisonService
) {
    @PostMapping("/channels")
    fun compareChannels(
        @RequestBody channelIds: List<String>,
        @RequestParam(required = false, defaultValue = "30d") period: String
    ): ResponseEntity<ChannelComparisonDTO> {
        val comparison = comparisonService.compareChannels(channelIds, period)
        return ResponseEntity.ok(comparison)
    }
}
