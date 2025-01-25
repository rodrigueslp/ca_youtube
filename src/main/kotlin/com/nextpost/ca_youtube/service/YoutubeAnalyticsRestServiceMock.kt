package com.nextpost.ca_youtube.service

import com.nextpost.ca_youtube.model.dto.*
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@Profile("dev")
class YoutubeAnalyticsRestServiceMock : YoutubeAnalyticsRestService {

    override fun getChannelAnalytics(userId: Long, channelId: String, startDate: LocalDateTime?, endDate: LocalDateTime?): ChannelAnalytics? {
        return ChannelAnalytics(
            views = 15000,
            estimatedMinutesWatched = 45000.0,
            averageViewDuration = 180.0, // em segundos
            averageViewPercentage = 65.5,
            subscribersGained = 500,
            subscribersLost = 50,
            likes = 2500,
            comments = 800,
            date = LocalDateTime.now().toString(),
            viewsGrowth = 12.5, // crescimento percentual
            retentionRate = 75.2, // taxa de retenção
            ctr = 4.8, // Click-through rate
            averageWatchTime = 240.0, // tempo médio de visualização
            engagementRate = 8.5, // taxa de engajamento
            shareCount = 350,
            topPerformingVideos = listOf(
                VideoPerformance("video1", 5000, 85.0),
                VideoPerformance("video2", 4200, 78.5)
            )
        )
    }

    override fun getChannelDemographics(
        userId: Long,
        channelId: String,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): List<DemographicData>? {
        return listOf(
            // Idade/Gênero
            DemographicData("13-17", "MALE", 5.0),
            DemographicData("18-24", "MALE", 25.5),
            DemographicData("25-34", "MALE", 30.2),
            DemographicData("35-44", "MALE", 15.8),
            DemographicData("45-54", "MALE", 8.0),
            DemographicData("55+", "MALE", 2.5),

            DemographicData("13-17", "FEMALE", 3.0),
            DemographicData("18-24", "FEMALE", 12.3),
            DemographicData("25-34", "FEMALE", 10.7),
            DemographicData("35-44", "FEMALE", 5.5),
            DemographicData("45-54", "FEMALE", 3.2),
            DemographicData("55+", "FEMALE", 1.8),

            // Regiões
            DemographicData("BR", "REGION", 45.0),
            DemographicData("US", "REGION", 25.0),
            DemographicData("PT", "REGION", 15.0),
            DemographicData("ES", "REGION", 10.0),
            DemographicData("MX", "REGION", 3.0),
            DemographicData("AR", "REGION", 2.0),
            DemographicData("OTHER", "REGION", 5.0),

            // Dispositivos
            DemographicData("MOBILE", "DEVICE", 65.0),
            DemographicData("DESKTOP", "DEVICE", 25.0),
            DemographicData("TABLET", "DEVICE", 7.0),
            DemographicData("TV", "DEVICE", 3.0)
        )
    }

    override fun getChannelTrafficSources(userId: Long, channelId: String, startDate: LocalDateTime?, endDate: LocalDateTime?): List<TrafficSource>? {
        return listOf(
            TrafficSource("SUGGESTED_VIDEO", 5000),
            TrafficSource("YOUTUBE_SEARCH", 3500),
            TrafficSource("EXTERNAL", 2000),
            TrafficSource("PLAYLIST", 1500),
            TrafficSource("CHANNEL_PAGE", 1200),
            TrafficSource("NOTIFICATIONS", 800),
            TrafficSource("BROWSE_FEATURES", 600),
            TrafficSource("END_SCREEN", 400),
            TrafficSource("CARDS", 300),
            TrafficSource("SOCIAL", 250),
            TrafficSource("DIRECT_OR_UNKNOWN", 200)
        )
    }

    override fun getMyChannels(userId: Long): List<ChannelDetails> {
        return listOf(
            ChannelDetails(
                channelId = "UC12345",
                title = "Tech Channel",
                description = "A channel about technology and programming",
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                subscriberCount = 50000,
                videoCount = 200,
                analytics = getChannelAnalytics(userId, "UC12345", LocalDateTime.now(), LocalDateTime.now()),
                demographics = getChannelDemographics(userId, "UC12345", LocalDateTime.now(), LocalDateTime.now()),
                trafficSources = getChannelTrafficSources(userId, "UC12345", LocalDateTime.now(), LocalDateTime.now()),
                isOwnChannel = true
            )
        )
    }
}