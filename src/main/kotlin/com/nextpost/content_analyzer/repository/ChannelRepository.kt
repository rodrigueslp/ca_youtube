package com.nextpost.content_analyzer.repository

import com.nextpost.content_analyzer.model.entity.Channel
import com.nextpost.content_analyzer.model.entity.ChannelStats
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

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
