import com.nextpost.ca_youtube.service.YouTubeService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ChannelUpdaterScheduler(
    private val youTubeService: YouTubeService
) {
    private val logger = LoggerFactory.getLogger(ChannelUpdaterScheduler::class.java)

    @Scheduled(cron = "0 0 3 * * ?") // Executa todos os dias às 3h da manhã
    fun updateChannels() {
        logger.info("Starting daily channel stats update job")
        try {
            youTubeService.updateAllChannels()
        } catch (e: Exception) {
            logger.error("Error updating channels: ", e)
        }
    }
}
