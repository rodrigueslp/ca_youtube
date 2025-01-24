package com.nextpost.ca_youtube.controller

import com.nextpost.ca_youtube.model.dto.ChannelAnalytics
import com.nextpost.ca_youtube.model.dto.ChannelDetails
import com.nextpost.ca_youtube.model.dto.DemographicData
import com.nextpost.ca_youtube.service.UserService
import com.nextpost.ca_youtube.service.YoutubeAnalyticsRestService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/youtube/private")
class YoutubePrivateController(
    private val youtubeAnalyticsRestService: YoutubeAnalyticsRestService,
    private val userService: UserService
) {
    @GetMapping("/channels/mine")
    fun getMyChannels(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<ChannelDetails>> {
        val user = userService.getOrCreateUser(
            email = jwt.claims["email"] as String,
            name = jwt.claims["name"] as String,
            picture = jwt.claims["picture"] as String?
        )

        val channels = youtubeAnalyticsRestService.getMyChannels(user.id!!)
        return ResponseEntity.ok(channels)
    }

    @GetMapping("/channel/{channelId}/analytics")
    fun getChannelAnalytics(
        @PathVariable channelId: String,
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime?
    ): ResponseEntity<ChannelAnalytics> {
        val userId = jwt.subject.toLong()
        val analytics = youtubeAnalyticsRestService.getChannelAnalytics(
            userId,
            channelId,
            startDate ?: LocalDateTime.now().minusDays(30),
            endDate ?: LocalDateTime.now()
        )
        return ResponseEntity.ok(analytics)
    }

    @GetMapping("/channel/{channelId}/demographics")
    fun getChannelDemographics(
        @PathVariable channelId: String,
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime?
    ): ResponseEntity<List<DemographicData>> {
        val userId = jwt.subject.toLong()
        val demographics = youtubeAnalyticsRestService.getChannelDemographics(
            userId,
            channelId,
            startDate ?: LocalDateTime.now().minusDays(30),
            endDate ?: LocalDateTime.now()
        )
        return ResponseEntity.ok(demographics)
    }
}
