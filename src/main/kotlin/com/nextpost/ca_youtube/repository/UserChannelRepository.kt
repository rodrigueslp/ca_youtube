package com.nextpost.ca_youtube.repository

import com.nextpost.ca_youtube.model.entity.UserChannel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface UserChannelRepository: JpaRepository<UserChannel, Long> {
    fun findByUserIdAndHasOAuthAccessTrue(userId: Long): List<UserChannel>
    fun findByUserIdAndOauthChannelId(userId: Long, oauthChannelId: String): Optional<UserChannel>
    @Query("SELECT uc FROM UserChannel uc WHERE uc.user.id = :userId AND uc.channel.channelId = :youtubeChannelId")
    fun findByUserIdAndChannelYoutubeId(userId: Long, youtubeChannelId: String): Optional<UserChannel>
}