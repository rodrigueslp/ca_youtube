package com.nextpost.ca_youtube.controller

import com.nextpost.ca_youtube.model.dto.ChannelDTO
import com.nextpost.ca_youtube.model.dto.ChannelStatsDTO
import com.nextpost.ca_youtube.model.dto.VideoDTO
import com.nextpost.ca_youtube.service.UserService
import com.nextpost.ca_youtube.service.YouTubeService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/youtube")
class YoutubeAnalyticsController(
    private val youTubeService: YouTubeService,
    private val userService: UserService
) {

    @PostMapping("/channels")
    fun addChannelToTrack(
        @RequestParam channelIdentifier: String,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ChannelDTO> {
        // Obtém ou cria o usuário baseado no token
        val user = userService.getOrCreateUser(
            email = jwt.claims["email"] as String,
            name = jwt.claims["name"] as String,
            picture = jwt.claims["picture"] as String?
        )

        val channel = youTubeService.addChannelToTrackForUser(channelIdentifier, user.id!!)
        return ResponseEntity.ok(channel)
    }

    @GetMapping("/channels")
    fun getUserChannels(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<List<ChannelDTO>> {
        val user = userService.getOrCreateUser(
            email = jwt.claims["email"] as String,
            name = jwt.claims["name"] as String,
            picture = jwt.claims["picture"] as String?
        )

        val channels = youTubeService.getChannelsForUser(user.id!!)
        return ResponseEntity.ok(channels)
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
