import com.nextpost.ca_youtube.repository.ChannelRepository
import com.nextpost.ca_youtube.service.YouTubeService
import com.nextpost.ca_youtube.service.batch.BatchProcessingService
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ChannelUpdaterScheduler(
    private val batchProcessingService: BatchProcessingService,
    private val channelRepository: ChannelRepository
) {
    private val logger = LoggerFactory.getLogger(ChannelUpdaterScheduler::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Scheduled(cron = "0 0 0 * * *") // A cada 24 horas
    fun scheduleChannelUpdates() {
        scope.launch {
            try {
                batchProcessingService.updateAllChannels()
            } catch (e: Exception) {
                logger.error("Error during scheduled channel update", e)
            }
        }
    }

    @PreDestroy
    fun cleanup() {
        scope.cancel()
    }
}

