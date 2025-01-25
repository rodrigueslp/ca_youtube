package com.nextpost.ca_youtube.service

import com.nextpost.ca_youtube.model.dto.ChannelAnalytics
import com.nextpost.ca_youtube.model.dto.ChannelDetails
import com.nextpost.ca_youtube.model.dto.DemographicData
import com.nextpost.ca_youtube.model.dto.TrafficSource
import com.nextpost.ca_youtube.model.entity.YoutubeAcessToken
import com.nextpost.ca_youtube.repository.YoutubeTokenRepository
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Profile("prod")
class YoutubeAnalyticsRestServiceImpl(
    private val restTemplate: RestTemplate,
    private val youtubeTokenRepository: YoutubeTokenRepository,
    private val tokenRefreshService: TokenRefreshService
) : YoutubeAnalyticsRestService {
    companion object {
        private const val BASE_URL = "https://youtubeanalytics.googleapis.com/v2/reports"
    }

    private fun getValidToken(userId: Long): YoutubeAcessToken {
        val token = youtubeTokenRepository.findByUserId(userId)
            .orElseThrow { RuntimeException("No YouTube token found for user") }
        return tokenRefreshService.refreshTokenIfNeeded(token)
    }

    override fun getMyChannels(userId: Long): List<ChannelDetails> {
        val token = getValidToken(userId)

        val headers = HttpHeaders().apply {
            setBearerAuth(token.accessToken)
        }

        // First, get channels list from YouTube Data API
        val channelsUrl = "https://www.googleapis.com/youtube/v3/channels?part=snippet,statistics,contentDetails&mine=true"

        val channelsResponse = restTemplate.exchange(
            channelsUrl,
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            ChannelsResponse::class.java
        )

        return channelsResponse.body?.items?.map { channel ->
            val analytics = try {
                getChannelAnalytics(userId, channel.id, LocalDateTime.now(), LocalDateTime.now().minusDays(30))
            } catch (e: Exception) {
                null
            }

            val demographics = try {
                getChannelDemographics(userId, channel.id, LocalDateTime.now(), LocalDateTime.now().minusDays(30))
            } catch (e: Exception) {
                null
            }

            val trafficSources = try {
                getChannelTrafficSources(userId, channel.id, LocalDateTime.now(), LocalDateTime.now().minusDays(30))
            } catch (e: Exception) {
                null
            }

            ChannelDetails(
                channelId = channel.id,
                title = channel.snippet.title,
                description = channel.snippet.description,
                thumbnailUrl = channel.snippet.thumbnails.default.url,
                subscriberCount = channel.statistics.subscriberCount.toLong(),
                videoCount = channel.statistics.videoCount.toLong(),
                analytics = analytics,
                demographics = demographics,
                trafficSources = trafficSources,
                isOwnChannel = true
            )
        } ?: emptyList()
    }

    override fun getChannelAnalytics(
        userId: Long,
        channelId: String,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): ChannelAnalytics? {
        val token = getValidToken(userId)

        val url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
            .queryParam("ids", "channel==$channelId")
            .queryParam("startDate", getDafaultDateIfIsNull(startDate, true).format(DateTimeFormatter.ISO_DATE))
            .queryParam("endDate", getDafaultDateIfIsNull(endDate, false).format(DateTimeFormatter.ISO_DATE))
            .queryParam("metrics", "views,estimatedMinutesWatched,averageViewDuration,averageViewPercentage,subscribersGained,subscribersLost,likes,comments")
            .queryParam("dimensions", "day")
            .build()
            .toUriString()

        val headers = HttpHeaders().apply {
            setBearerAuth(token.accessToken)
        }

        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            AnalyticsResponse::class.java
        )

        return response.body?.rows?.firstOrNull()?.let { row ->
            ChannelAnalytics(
                views = row[1] as Long,
                estimatedMinutesWatched = (row[2] as Number).toDouble(),
                averageViewDuration = (row[3] as Number).toDouble(),
                averageViewPercentage = (row[4] as Number).toDouble(),
                subscribersGained = (row[5] as Number).toInt(),
                subscribersLost = (row[6] as Number).toInt(),
                likes = row[7] as Long,
                comments = row[8] as Long,
                date = row[0] as String,
                viewsGrowth = 0.0, // Valores padrão para campos não disponíveis na API
                retentionRate = 0.0,
                ctr = 0.0,
                averageWatchTime = 0.0,
                engagementRate = 0.0,
                shareCount = 0,
                topPerformingVideos = emptyList()
            )
        }
    }

    override fun getChannelDemographics(
        userId: Long,
        channelId: String,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): List<DemographicData>? {
        val token = getValidToken(userId)

        val url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
            .queryParam("ids", "channel==$channelId")
            .queryParam("startDate", getDafaultDateIfIsNull(startDate, true).format(DateTimeFormatter.ISO_DATE))
            .queryParam("endDate", getDafaultDateIfIsNull(endDate, false).format(DateTimeFormatter.ISO_DATE))
            .queryParam("metrics", "viewerPercentage")
            .queryParam("dimensions", "ageGroup,gender")
            .build()
            .toUriString()

        val headers = HttpHeaders().apply {
            setBearerAuth(token.accessToken)
        }

        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            AnalyticsResponse::class.java
        )

        return response.body?.rows?.let { rows ->
            if (rows.isEmpty()) null
            else rows.map { row ->
                DemographicData(
                    category = row[0] as String,
                    type = row[1] as String,
                    percentage = (row[2] as Number).toDouble()
                )
            }
        }
    }

    override fun getChannelTrafficSources(
        userId: Long,
        channelId: String,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): List<TrafficSource>? {
        val token = getValidToken(userId)

        val url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
            .queryParam("ids", "channel==$channelId")
            .queryParam("startDate", getDafaultDateIfIsNull(startDate, true).format(DateTimeFormatter.ISO_DATE))
            .queryParam("endDate", getDafaultDateIfIsNull(endDate, false).format(DateTimeFormatter.ISO_DATE))
            .queryParam("metrics", "views")
            .queryParam("dimensions", "insightTrafficSourceType")
            .build()
            .toUriString()

        val headers = HttpHeaders().apply {
            setBearerAuth(token.accessToken)
        }

        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            AnalyticsResponse::class.java
        )

        return response.body?.rows?.let { rows ->
            if (rows.isEmpty()) null
            else rows.map { row ->
                TrafficSource(
                    source = row[0] as String,
                    views = (row[1] as Number).toLong()
                )
            }
        }

    }

    private fun getDafaultDateIfIsNull(date: LocalDateTime?, isStartDate: Boolean) =
        date ?: if (isStartDate) LocalDateTime.now().minusDays(30) else LocalDateTime.now()

    data class AnalyticsResponse(
        val kind: String? = null,
        val rows: List<List<Any>>? = null,
        val columnHeaders: List<ColumnHeader>? = null
    )

    data class ColumnHeader(
        val name: String,
        val columnType: String,
        val dataType: String
    )

    data class ChannelsResponse(
        val items: List<Channel>? = null
    ) {
        data class Channel(
            val id: String,
            val snippet: Snippet,
            val statistics: Statistics
        ) {
            data class Snippet(
                val title: String,
                val description: String,
                val thumbnails: Thumbnails
            ) {
                data class Thumbnails(
                    val default: Thumbnail
                ) {
                    data class Thumbnail(
                        val url: String
                    )
                }
            }

            data class Statistics(
                val subscriberCount: String,
                val videoCount: String
            )
        }
    }
}
