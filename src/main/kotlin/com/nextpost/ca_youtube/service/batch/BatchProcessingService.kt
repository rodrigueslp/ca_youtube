package com.nextpost.ca_youtube.service.batch

import com.nextpost.ca_youtube.model.entity.Channel
import com.nextpost.ca_youtube.repository.ChannelRepository
import com.nextpost.ca_youtube.service.MetricsService
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BatchProcessingService(
    private val channelRepository: ChannelRepository,
    private val metricsService: MetricsService
) {
    private val logger = LoggerFactory.getLogger(BatchProcessingService::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Método público para iniciar o processamento em lote
    suspend fun updateAllChannels(): BatchProcessingResult {
        val channels = channelRepository.findAll()
        logger.info("Starting batch update for ${channels.size} channels")

        val results = processChannels(channels)

        logger.info("""
            Batch processing completed:
            Total processed: ${results.totalProcessed}
            Successful: ${results.successCount}
            Failed: ${results.failureCount}
            ${if (results.failedChannels.isNotEmpty()) "Failed channels: ${results.failedChannels.joinToString()}" else ""}
        """.trimIndent())

        return results
    }

    private suspend fun processChannels(channels: List<Channel>): BatchProcessingResult = coroutineScope {
        val totalChannels = channels.size
        val results = channels.mapAsync { channel ->
            processChannel(channel, totalChannels)
        }

        results.fold(BatchProcessingResult()) { acc, result ->
            acc.copy(
                totalProcessed = acc.totalProcessed + 1,
                successCount = acc.successCount + if (result.success) 1 else 0,
                failureCount = acc.failureCount + if (!result.success) 1 else 0,
                failedChannels = if (!result.success) acc.failedChannels + result.channelId else acc.failedChannels
            )
        }
    }

    private suspend fun processChannel(channel: Channel, total: Int): ChannelProcessResult = withContext(Dispatchers.IO) {
        try {
            metricsService.updateChannelStats(channel)
            logger.info("Successfully processed channel ID: ${channel.channelId}")
            ChannelProcessResult(channel.channelId, true)
        } catch (e: Exception) {
            logger.error("Failed to process channel ID: ${channel.channelId}", e)
            ChannelProcessResult(channel.channelId, false)
        }
    }

    private suspend fun <A, B> Iterable<A>.mapAsync(f: suspend (A) -> B): List<B> = coroutineScope {
        map { async { f(it) } }.awaitAll()
    }

    @PreDestroy
    fun cleanup() {
        coroutineScope.cancel()
    }
}

// Classes de resultado
data class BatchProcessingResult(
    val totalProcessed: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val failedChannels: List<String> = emptyList()
)

data class ChannelProcessResult(
    val channelId: String,
    val success: Boolean
)
