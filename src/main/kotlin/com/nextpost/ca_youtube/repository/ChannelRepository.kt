package com.nextpost.ca_youtube.repository

import com.nextpost.ca_youtube.model.entity.Channel
import com.nextpost.ca_youtube.model.entity.ChannelStats
import com.nextpost.ca_youtube.model.entity.YoutubeAcessToken
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface ChannelRepository : JpaRepository<Channel, Long> {
    fun findByChannelId(channelId: String): Channel?
}

@Repository
interface ChannelStatsRepository : JpaRepository<ChannelStats, Long> {
    fun findByChannelOrderByCollectedAtDesc(channel: Channel): List<ChannelStats>

    @Query("SELECT cs FROM ChannelStats cs WHERE cs.channel = :channel AND cs.collectedAt >= :startDate")
    fun findStatsInPeriod(channel: Channel, startDate: java.time.LocalDateTime): List<ChannelStats>

    @Query("""
        SELECT cs 
        FROM ChannelStats cs 
        WHERE cs.collectedAt IN (
            SELECT MAX(cs2.collectedAt) 
            FROM ChannelStats cs2 
            WHERE cs2.channel = :channel
            GROUP BY FUNCTION('DATE', cs2.collectedAt)
        )
        ORDER BY cs.collectedAt DESC
    """)
    fun findLatestStatsByDay(channel: Channel): List<ChannelStats>

    @Transactional
    fun deleteByChannel(channel: Channel)
}

@Repository
interface YoutubeTokenRepository : JpaRepository<YoutubeAcessToken, Long> {
    fun findByUserId(userId: Long): Optional<YoutubeAcessToken>
    fun findByUserIdAndExpiresAtGreaterThan(userId: Long, currentTime: LocalDateTime): Optional<YoutubeAcessToken>
}