package com.nextpost.content_analyzer.repository

import com.nextpost.content_analyzer.model.entity.Video
import com.nextpost.content_analyzer.model.entity.Channel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface VideoRepository : JpaRepository<Video, Long> {
    fun findByChannel(channel: Channel): List<Video>
    fun findByChannelAndPublishedAtAfter(channel: Channel, date: LocalDateTime): List<Video>

    @Query("SELECT v FROM Video v WHERE v.channel = :channel ORDER BY v.publishedAt DESC")
    fun findRecentVideos(channel: Channel): List<Video>
}