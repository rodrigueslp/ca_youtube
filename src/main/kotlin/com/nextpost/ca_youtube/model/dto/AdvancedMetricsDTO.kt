package com.nextpost.ca_youtube.model.dto

data class AdvancedComparisonDTO(
    val channels: List<AdvancedChannelMetricsDTO>,
    val marketAnalysis: MarketAnalysisDTO
)

data class AdvancedChannelMetricsDTO(
    val channelId: String,
    val title: String,
    val baseMetrics: ComparisonMetricsDTO,
    val advancedMetrics: AdvancedMetricsDTO
)

data class AdvancedMetricsDTO(
    val efficiencyScore: Double, // Crescimento por vídeo publicado
    val consistencyIndex: Double, // Regularidade de uploads e crescimento
    val marketShareScore: Double, // Participação no mercado do nicho
    val growthVelocity: Double, // Velocidade de crescimento comparada à média
    val trendsAnalysis: TrendsAnalysisDTO
)

data class TrendsAnalysisDTO(
    val uploadTrend: String, // "increasing", "stable", "decreasing"
    val growthTrend: String,
    val viewsTrend: String,
    val bestPerformingDay: String,
    val predictedGrowth: Double
)

data class MarketAnalysisDTO(
    val totalMarketSize: Long, // Total de inscritos no nicho
    val averageGrowthRate: Double,
    val competitiveIndex: Map<String, Double>, // channelId -> índice competitivo
    val marketPositions: List<MarketPosition>
)

data class MarketPosition(
    val channelId: String,
    val title: String,
    val marketShare: Double,
    val competitiveStrength: String // "leader", "challenger", "follower"
)