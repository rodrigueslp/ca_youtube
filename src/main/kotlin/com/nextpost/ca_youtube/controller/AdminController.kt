package com.nextpost.ca_youtube.controller

import com.nextpost.ca_youtube.repository.ChannelRepository
import com.nextpost.ca_youtube.service.MetricsService
import com.nextpost.ca_youtube.service.batch.BatchProcessingService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val channelRepository: ChannelRepository,
    private val metricsService: MetricsService,
    private val batchProcessingService: BatchProcessingService
) {

    private val logger = LoggerFactory.getLogger(AdminController::class.java)

    @PostMapping("/recalculate-metrics")
    fun recalculateAllMetrics(): ResponseEntity<String> {
        channelRepository.findAll().forEach { channel ->
            try {
                metricsService.calculateChannelMetrics(channel)
            } catch (e: Exception) {
                logger.error("Error recalculating metrics for channel ${channel.channelId}", e)
            }
        }
        return ResponseEntity.ok("Metrics recalculation completed")
    }

    @PostMapping("/channels/update-all")
    suspend fun updateAllChannels(): ResponseEntity<String> {
        return try {
            coroutineScope {
                launch {
                    batchProcessingService.updateAllChannels()
                }
            }
            ResponseEntity.accepted()
                .body("Update process started. Check logs for progress.")
        } catch (e: Exception) {
            ResponseEntity.status(500)
                .body("Error starting update process: ${e.message}")
        }
    }
}
