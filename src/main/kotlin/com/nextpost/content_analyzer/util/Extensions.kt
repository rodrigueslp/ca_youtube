package com.nextpost.content_analyzer.util

import com.nextpost.content_analyzer.model.dto.ChannelMetricsDTO
import com.nextpost.content_analyzer.model.dto.VideoDTO
import com.nextpost.content_analyzer.model.entity.ChannelMetrics
import com.nextpost.content_analyzer.model.entity.Video
import java.time.Duration

fun ChannelMetrics.toDTO(): ChannelMetricsDTO {
    return ChannelMetricsDTO(
        channelId = this.channel.channelId,
        dailySubscriberGrowth = this.dailySubscriberGrowth,
        weeklySubscriberGrowth = this.weeklySubscriberGrowth,
        monthlySubscriberGrowth = this.monthlySubscriberGrowth,
        dailyViewGrowth = this.dailyViewGrowth,
        videosPerWeek = this.videosPerWeek,
        videosPerMonth = this.videosPerMonth,
        averageVideoDuration = Duration.ofSeconds(this.averageVideoDuration),
        mostCommonUploadHour = this.mostCommonUploadHour,
        mostCommonUploadDay = getDayName(this.mostCommonUploadDay),
        topCategory = getCategoryName(this.topCategoryId),
        topCategoryPercentage = this.topCategoryPercentage,
        collectedAt = this.collectedAt
    )
}

fun Video.toDTO(): VideoDTO {
    return VideoDTO(
        videoId = this.videoId,
        title = this.title,
        description = this.description,
        publishedAt = this.publishedAt,
        duration = this.duration,
        viewCount = this.viewCount,
        categoryId = this.categoryId,
        thumbnailUrl = "teste"
    )
}

private fun getDayName(dayNumber: Int): String {
    return when (dayNumber) {
        1 -> "Segunda-feira"
        2 -> "Terça-feira"
        3 -> "Quarta-feira"
        4 -> "Quinta-feira"
        5 -> "Sexta-feira"
        6 -> "Sábado"
        7 -> "Domingo"
        else -> "Desconhecido"
    }
}

private fun getCategoryName(categoryId: String): String {
    // Mapeamento de IDs de categoria do YouTube para nomes
    return when (categoryId) {
        "1" -> "Film & Animation"
        "2" -> "Autos & Vehicles"
        "10" -> "Music"
        "15" -> "Pets & Animals"
        "17" -> "Sports"
        "19" -> "Travel & Events"
        "20" -> "Gaming"
        "22" -> "People & Blogs"
        "23" -> "Comedy"
        "24" -> "Entertainment"
        "25" -> "News & Politics"
        "26" -> "Howto & Style"
        "27" -> "Education"
        "28" -> "Science & Technology"
        "29" -> "Nonprofits & Activism"
        else -> "Other"
    }
}
