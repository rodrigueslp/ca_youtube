package com.nextpost.ca_youtube.controller

import com.nextpost.ca_youtube.model.dto.ChannelComparisonDTO
import com.nextpost.ca_youtube.service.ComparisonService
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
