package com.nextpost.content_analyzer.model.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "channel_metrics")
data class ChannelMetrics(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    val channel: Channel,

    // Métricas de crescimento
    @Column(name = "daily_subscriber_growth")
    val dailySubscriberGrowth: Double,

    @Column(name = "weekly_subscriber_growth")
    val weeklySubscriberGrowth: Double,

    @Column(name = "monthly_subscriber_growth")
    val monthlySubscriberGrowth: Double,

    @Column(name = "daily_view_growth")
    val dailyViewGrowth: Double,

    // Métricas de conteúdo
    @Column(name = "videos_per_week")
    val videosPerWeek: Double,

    @Column(name = "videos_per_month")
    val videosPerMonth: Double,

    @Column(name = "avg_video_duration")
    val averageVideoDuration: Long, // em segundos

    // Horários mais comuns de postagem
    @Column(name = "most_common_upload_hour")
    val mostCommonUploadHour: Int,

    @Column(name = "most_common_upload_day")
    val mostCommonUploadDay: Int,

    // Categorias
    @Column(name = "top_category_id")
    val topCategoryId: String,

    @Column(name = "top_category_percentage")
    val topCategoryPercentage: Double,

    @Column(name = "collected_at")
    val collectedAt: LocalDateTime = LocalDateTime.now()
)
