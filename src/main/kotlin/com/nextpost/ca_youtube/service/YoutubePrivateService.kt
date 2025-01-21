package com.nextpost.ca_youtube.service

import com.google.api.services.youtube.YouTube
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.services.youtubeAnalytics.v2.YouTubeAnalytics
import com.nextpost.ca_youtube.config.YouTubeConfig
import com.nextpost.ca_youtube.model.dto.ChannelAnalytics
import com.nextpost.ca_youtube.model.dto.ChannelDetails
import com.nextpost.ca_youtube.model.dto.DemographicData
import com.nextpost.ca_youtube.model.entity.YouTubeToken
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class YoutubePrivateService(
    private val youTubeConfig: YouTubeConfig,
    private val youTubeTokenService: YoutubeTokenService
) {
    private fun createAuthorizedYouTube(token: YouTubeToken): YouTube {
        val credential = GoogleCredential().apply {
            accessToken = token.accessToken
            refreshToken = token.refreshToken
        }

        return YouTube.Builder(
            youTubeConfig.httpTransport(),
            youTubeConfig.jsonFactory(),
            credential
        )
            .setApplicationName(youTubeConfig.getApplicationName())
            .build()
    }

    private fun createAuthorizedYouTubeAnalytics(token: YouTubeToken): YouTubeAnalytics {
        val credential = GoogleCredential().apply {
            accessToken = token.accessToken
            refreshToken = token.refreshToken
        }

        return YouTubeAnalytics.Builder(
            youTubeConfig.httpTransport(),
            youTubeConfig.jsonFactory(),
            credential
        )
            .setApplicationName(youTubeConfig.getApplicationName())
            .build()
    }

    fun getMyChannels(userId: Long): List<ChannelDetails> {
        val token = youTubeTokenService.getValidToken(userId)
        val youtube = createAuthorizedYouTube(token)

        val request = youtube.channels().list(listOf("snippet", "statistics", "contentDetails"))
        request.mine = true

        val response = request.execute()
        return response.items.map { channel ->
            ChannelDetails(
                channelId = channel.id,
                title = channel.snippet.title,
                description = channel.snippet.description,
                thumbnailUrl = channel.snippet.thumbnails.default.url,
                subscriberCount = channel.statistics.subscriberCount.toLong(),
                videoCount = channel.statistics.videoCount.toLong(),
                analytics = null,
                demographics = null,
                trafficSources = null,
                isOwnChannel = true
            )
        }
    }

    fun getChannelAnalytics(
        userId: Long,
        channelId: String,
        startDate: LocalDateTime = LocalDateTime.now().minusDays(30),
        endDate: LocalDateTime = LocalDateTime.now()
    ): ChannelAnalytics {
        val token = youTubeTokenService.getValidToken(userId)
        val analytics = createAuthorizedYouTubeAnalytics(token)
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val request = analytics.reports().query()
            .setIds("channel==$channelId")
            .setStartDate(startDate.format(dateFormat))
            .setEndDate(endDate.format(dateFormat))
            .setMetrics("views,estimatedMinutesWatched,averageViewDuration,averageViewPercentage,subscribersGained,subscribersLost,likes,comments")
            .setDimensions("day")
            .setSort("-day")

        val response = request.execute()
        val row = response.rows.first()

        return ChannelAnalytics(
            views = row[1] as Long,
            estimatedMinutesWatched = row[2] as Double,
            averageViewDuration = row[3] as Double,
            averageViewPercentage = row[4] as Double,
            subscribersGained = (row[5] as Long).toInt(),
            subscribersLost = (row[6] as Long).toInt(),
            likes = row[7] as Long,
            comments = row[8] as Long,
            date = row[0] as String
        )
    }

    fun getChannelDemographics(
        userId: Long,
        channelId: String,
        startDate: LocalDateTime = LocalDateTime.now().minusDays(30),
        endDate: LocalDateTime = LocalDateTime.now()
    ): List<DemographicData> {
        val token = youTubeTokenService.getValidToken(userId)
        val analytics = createAuthorizedYouTubeAnalytics(token)
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val request = analytics.reports().query()
            .setIds("channel==$channelId")
            .setStartDate(startDate.format(dateFormat))
            .setEndDate(endDate.format(dateFormat))
            .setMetrics("viewerPercentage")
            .setDimensions("ageGroup,gender")

        val response = request.execute()
        return response.rows.map { row ->
            DemographicData(
                ageGroup = row[0] as String,
                gender = row[1] as String,
                percentage = row[2] as Double
            )
        }
    }
}