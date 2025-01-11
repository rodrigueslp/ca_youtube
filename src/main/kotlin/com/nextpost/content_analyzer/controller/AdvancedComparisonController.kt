package com.nextpost.content_analyzer.controller

import com.nextpost.content_analyzer.model.dto.AdvancedComparisonDTO
import com.nextpost.content_analyzer.service.AdvancedComparisonService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/comparison/advanced")
class AdvancedComparisonController(
    private val advancedComparisonService: AdvancedComparisonService
) {
    @PostMapping("/channels")
    fun analyzeChannels(
        @RequestBody channelIds: List<String>
    ): ResponseEntity<AdvancedComparisonDTO> {
        val analysis = advancedComparisonService.analyzeChannels(channelIds)
        return ResponseEntity.ok(analysis)
    }
}
