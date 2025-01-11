package com.nextpost.content_analyzer.controller

import com.nextpost.content_analyzer.model.dto.ChannelDTO
import com.nextpost.content_analyzer.model.dto.ChannelStatsDTO
import com.nextpost.content_analyzer.model.dto.VideoDTO
import com.nextpost.content_analyzer.service.YouTubeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/youtube")
class YoutubeAnalyticsController(
    private val youTubeService: YouTubeService
) {


    @PostMapping("/channels")
    fun addChannelToTrack(@RequestParam channelIdentifier: String): ResponseEntity<ChannelDTO> {
        val channel = youTubeService.addChannelToTrack(channelIdentifier)
        return ResponseEntity.ok(channel)
    }

    @GetMapping("/channels/{channelId}/videos")
    fun getRecentVideos(@PathVariable channelId: String): ResponseEntity<List<VideoDTO>> {
        val videos = youTubeService.getRecentVideos(channelId)
        return ResponseEntity.ok(videos)
    }

    @PostMapping("/channels/{channelId}/stats")
    fun updateChannelStats(@PathVariable channelId: String): ResponseEntity<ChannelStatsDTO> {
        val stats = youTubeService.updateChannelStats(channelId)
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/channels/{channelId}/stats")
    fun getChannelStats(@PathVariable channelId: String): ResponseEntity<List<ChannelStatsDTO>> {
        val stats = youTubeService.getChannelStats(channelId)
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/channels")
    fun getAllChannels(): ResponseEntity<List<ChannelDTO>> {
        val channels = youTubeService.getAllChannels()
        return ResponseEntity.ok(channels)
    }

    @PostMapping("/channels/update-all")
    fun updateAllChannels(): ResponseEntity<String> {
        return try {
            youTubeService.updateAllChannels() // Chama o método de atualização
            ResponseEntity.ok("All channels updated successfully")
        } catch (e: Exception) {
            ResponseEntity.status(500).body("Error updating channels: ${e.message}")
        }
    }

    @DeleteMapping("/channels/{channelId}")
    fun deleteChannel(@PathVariable channelId: String): ResponseEntity<String> {
        return try {
            youTubeService.deleteChannel(channelId)
            ResponseEntity.ok("Channel with ID $channelId deleted successfully")
        } catch (e: Exception) {
            ResponseEntity.status(500).body("Error deleting channel: ${e.message}")
        }
    }

    @DeleteMapping("/channels")
    fun deleteAllChannels(@RequestHeader("Authorization-Key") authorizationKey: String): ResponseEntity<String> {
        val validKey = "MY_SECURE_KEY" // Substitua por uma chave segura
        if (authorizationKey != validKey) {
            return ResponseEntity.status(403).body("Unauthorized")
        }

        return try {
            youTubeService.deleteAllChannels()
            ResponseEntity.ok("All channels deleted successfully")
        } catch (e: Exception) {
            ResponseEntity.status(500).body("Error deleting all channels: ${e.message}")
        }
    }



}
