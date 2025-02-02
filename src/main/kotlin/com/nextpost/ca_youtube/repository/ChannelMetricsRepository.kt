package com.nextpost.ca_youtube.repository

import com.nextpost.ca_youtube.model.entity.Channel
import com.nextpost.ca_youtube.model.entity.ChannelMetrics
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ChannelMetricsRepository : JpaRepository<ChannelMetrics, Long> {

    fun findByChannelOrderByCollectedAtDesc(channel: Channel): List<ChannelMetrics>

    fun findFirstByChannelOrderByCollectedAtDesc(channel: Channel): ChannelMetrics?

    @Query("SELECT cm FROM ChannelMetrics cm WHERE cm.channel = :channel AND cm.collectedAt >= :startDate")
    fun findMetricsInPeriod(channel: Channel, startDate: LocalDateTime): List<ChannelMetrics>

}