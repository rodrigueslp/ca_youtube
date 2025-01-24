package com.nextpost.ca_youtube.service

import com.nextpost.ca_youtube.model.dto.ChannelAnalytics
import com.nextpost.ca_youtube.model.dto.ChannelDetails
import com.nextpost.ca_youtube.model.dto.DemographicData
import com.nextpost.ca_youtube.model.dto.TrafficSource
import java.time.LocalDateTime

interface YoutubeAnalyticsRestService {
    fun getChannelAnalytics(userId: Long, channelId: String, startDate: LocalDateTime?, endDate: LocalDateTime?): ChannelAnalytics?
    fun getChannelDemographics(userId: Long, channelId: String, startDate: LocalDateTime?, endDate: LocalDateTime?): List<DemographicData>?
    fun getChannelTrafficSources(userId: Long, channelId: String, startDate: LocalDateTime?, endDate: LocalDateTime?): List<TrafficSource>?
    fun getMyChannels(userId: Long): List<ChannelDetails>
}
