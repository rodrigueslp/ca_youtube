package com.nextpost.ca_youtube.service

import com.nextpost.ca_youtube.model.entity.Channel

interface ChannelUpdateOperations {
    suspend fun updateChannelVideos(channel: Channel)
    suspend fun updateChannelStats(channelId: String)
}