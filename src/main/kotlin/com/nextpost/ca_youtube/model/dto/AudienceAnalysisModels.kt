package com.nextpost.ca_youtube.model.dto

data class EngagementPatterns(
    val overallEngagementRate: Double,
    val engagementByDayOfWeek: Map<Int, Double>,
    val engagementByHour: Map<Int, Double>,
    val engagementByContentType: Map<String, Double>,
    val trendAnalysis: EngagementTrend
)

data class EngagementTrend(
    val direction: String, // "increasing", "decreasing", "stable"
    val percentageChange: Double,
    val timeFrame: String,
    val significantChanges: List<SignificantChange>
)

data class SignificantChange(
    val date: String,
    val metric: String,
    val changePercentage: Double,
    val possibleReason: String
)

data class ActivityPeaks(
    val bestDaysToPost: List<Int>,
    val bestHoursToPost: List<Int>,
    val peakEngagementWindows: List<TimeWindow>,
    val seasonalTrends: Map<String, Double>, // Ex: "summer" -> 1.2 (20% mais atividade)
    val specialEvents: List<EventImpact>
)

data class TimeWindow(
    val dayOfWeek: Int,
    val startHour: Int,
    val endHour: Int,
    val engagementMultiplier: Double
)

data class EventImpact(
    val eventName: String,
    val dateRange: String,
    val engagementIncrease: Double,
    val recommendedActions: List<String>
)

data class InteractionTypes(
    val commentToViewRatio: Double,
    val likeToViewRatio: Double,
    val shareToViewRatio: Double,
    val commentSentimentAnalysis: SentimentAnalysis,
    val interactionDistribution: Map<String, Double>, // Ex: "likes" -> 0.7 (70% das interações)
    val commentTopics: List<CommentTopic>
)

data class SentimentAnalysis(
    val overallSentiment: Double, // -1.0 to 1.0
    val sentimentDistribution: Map<String, Double>, // "positive" -> 0.6
    val commonPhrases: List<String>,
    val topEmojis: List<String>
)

data class CommentTopic(
    val topic: String,
    val frequency: Double,
    val sentiment: Double,
    val keywords: List<String>
)

data class RetentionTrends(
    val averageRetentionRate: Double,
    val retentionByVideoLength: Map<String, Double>, // "0-3min" -> 0.85
    val retentionByContentType: Map<String, Double>,
    val dropOffPoints: List<DropOffPoint>,
    val improvements: List<RetentionImprovement>
)

data class DropOffPoint(
    val timestamp: Int, // em segundos
    val percentageDropped: Double,
    val possibleReason: String,
    val recommendedAction: String
)

data class RetentionImprovement(
    val metric: String,
    val currentValue: Double,
    val targetValue: Double,
    val suggestedActions: List<String>
)

data class ContentPreferences(
    val preferredFormats: List<ContentFormat>,
    val topicPreferences: List<TopicPreference>,
    val optimalDuration: Map<String, Int>, // "tutorial" -> 480 (segundos)
    val contentGaps: List<ContentGap>,
    val successPatterns: List<SuccessPattern>
)

data class ContentFormat(
    val format: String, // "tutorial", "vlog", "review", etc.
    val engagementRate: Double,
    val retentionRate: Double,
    val growthPotential: Double
)

data class TopicPreference(
    val topic: String,
    val interestScore: Double,
    val trendsData: TrendData,
    val recommendedApproach: String
)

data class TrendData(
    val currentTrend: String,
    val projectedGrowth: Double,
    val competitionLevel: String,
    val seasonality: Map<String, Double>
)

data class ContentGap(
    val topic: String,
    val demandScore: Double,
    val competitionLevel: String,
    val estimatedViews: Long,
    val recommendedFormat: String
)

data class SuccessPattern(
    val pattern: String,
    val impact: Double,
    val examples: List<String>,
    val implementationTips: List<String>
)