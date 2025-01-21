package com.nextpost.ca_youtube.service

import com.google.api.services.youtube.YouTube
import com.nextpost.ca_youtube.model.entity.Channel
import com.nextpost.ca_youtube.model.dto.*
import com.nextpost.ca_youtube.model.entity.Video
import com.nextpost.ca_youtube.repository.VideoRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs

@Service
class AudienceAnalysisService(
    private val youtube: YouTube,
    private val videoRepository: VideoRepository,
    private val youTubeService: YoutubeService,
    private val contentAnalysisService: ContentAnalysisService
) {
    /**
     * Analisa sobreposição de audiência entre canais
     */
    fun analyzeAudienceOverlap(channelIds: List<String>): AudienceOverlapMetrics {
        val channels = channelIds.map {
            youTubeService.getChannel(it) ?: throw IllegalArgumentException("Channel not found: $it")
        }

        // Analisar comentaristas em comum
        val commonCommenters = findCommonCommenters(channels)

        // Analisar padrões de visualização
        val viewingPatterns = analyzeViewingPatterns(channels)

        // Calcular score de similaridade
        val similarityScores = calculateSimilarityScores(channels)

        // Identificar nichos relacionados
        val relatedNiches = identifyRelatedNiches(channels)

        return AudienceOverlapMetrics(
            channels = channels.map { it.channelId },
            commonAudiencePercentage = calculateOverlapPercentage(commonCommenters, channels),
            viewingPatterns = viewingPatterns,
            similarityScores = similarityScores,
            relatedNiches = relatedNiches,
            recommendedCollaborations = suggestCollaborations(channels, similarityScores)
        )
    }

    /**
     * Analisa comportamento da audiência de um canal específico
     */
    fun analyzeAudienceBehavior(channel: Channel): AudienceBehaviorMetrics {
        val videos = videoRepository.findByChannel(channel)
        val recentVideos = videos.filter {
            it.publishedAt.isAfter(LocalDateTime.now().minusMonths(3))
        }

        // Analisar padrões de engajamento
        val engagementPatterns = analyzeEngagementPatterns(recentVideos)

        // Analisar horários de maior atividade
        val activityPeaks = analyzeActivityPeaks(recentVideos)

        // Analisar tipos de interação
        val interactionTypes = analyzeInteractionTypes(recentVideos)

        return AudienceBehaviorMetrics(
            channelId = channel.channelId,
            engagementPatterns = engagementPatterns,
            activityPeaks = activityPeaks,
            interactionTypes = interactionTypes,
            audienceRetentionTrends = analyzeRetentionTrends(recentVideos),
            contentPreferences = analyzeContentPreferences(recentVideos)
        )
    }

    /**
     * Identifica segmentos específicos da audiência
     */
    fun identifyTargetSegments(channel: Channel): List<AudienceSegment> {
        val videos = videoRepository.findByChannel(channel)

        // Agrupar por padrões de comportamento
        val behaviorClusters = clusterByBehavior(videos)

        // Analisar preferências por segmento
        val segmentPreferences = analyzeSegmentPreferences(behaviorClusters)

        return segmentPreferences.map { (behavior, preferences) ->
            AudienceSegment(
                segmentType = behavior,
                size = calculateSegmentSize(preferences, channel.subscriberCount),
                preferences = preferences,
                engagementLevel = calculateEngagementLevel(preferences),
                recommendedContent = generateContentRecommendations(preferences)
            )
        }
    }

    private fun clusterByBehavior(videos: List<Video>): Map<String, List<Video>> {
        // Agrupa vídeos por padrões de comportamento do espectador
        val behaviorClusters = mutableMapOf<String, MutableList<Video>>()

        videos.forEach { video ->
            val engagementRate = (video.likeCount + video.commentCount).toDouble() / video.viewCount
            val cluster = when {
                engagementRate > 0.1 -> "high_engagement"
                engagementRate > 0.05 -> "medium_engagement"
                else -> "low_engagement"
            }
            behaviorClusters.getOrPut(cluster) { mutableListOf() }.add(video)
        }

        return behaviorClusters
    }

    private fun analyzeSegmentPreferences(behaviorClusters: Map<String, List<Video>>): Map<String, Map<String, Double>> {
        return behaviorClusters.mapValues { (_, videos) ->
            // Para cada cluster, analisa preferências
            mapOf(
                "format" to analyzeFormatPreference(videos),
                "duration" to analyzeDurationPreference(videos),
                "topics" to analyzeTopicPreference(videos),
                "timing" to analyzeTimingPreference(videos)
            )
        }
    }

    private fun analyzeFormatPreference(videos: List<Video>): Double {
        // Calcula formato preferido baseado em engajamento
        val formatEngagement = videos.groupBy { video ->
            when {
                video.duration.toSeconds() < 60 -> "short"
                video.duration.toSeconds() < 600 -> "medium"
                else -> "long"
            }
        }.mapValues { (_, formatVideos) ->
            formatVideos.map {
                (it.likeCount + it.commentCount).toDouble() / it.viewCount
            }.average()
        }

        return formatEngagement.values.maxOrNull() ?: 0.0
    }

    private fun analyzeDurationPreference(videos: List<Video>): Double {
        // Retorna a duração média dos vídeos mais engajados
        return videos.sortedByDescending {
            (it.likeCount + it.commentCount).toDouble() / it.viewCount
        }.take(5).map {
            it.duration.seconds
        }.average()
    }

    private fun analyzeTopicPreference(videos: List<Video>): Double {
        // Análise simplificada de tópicos baseada em títulos
        val topicEngagement = videos.groupBy { video ->
            when {
                video.title.contains(Regex("tutorial|how to|como", RegexOption.IGNORE_CASE)) -> "educational"
                video.title.contains(Regex("review|análise", RegexOption.IGNORE_CASE)) -> "review"
                else -> "entertainment"
            }
        }.mapValues { (_, topicVideos) ->
            topicVideos.map {
                (it.likeCount + it.commentCount).toDouble() / it.viewCount
            }.average()
        }

        return topicEngagement.values.maxOrNull() ?: 0.0
    }

    private fun analyzeTimingPreference(videos: List<Video>): Double {
        // Análise de horário preferido baseado em engajamento
        return videos.groupBy {
            it.publishedAt.hour
        }.mapValues { (_, timeVideos) ->
            timeVideos.map {
                (it.likeCount + it.commentCount).toDouble() / it.viewCount
            }.average()
        }.values.maxOrNull() ?: 0.0
    }

    private fun calculateSegmentSize(
        preferences: Map<String, Double>,
        totalSubscribers: Long
    ): Double {
        // Calcula o tamanho estimado do segmento baseado nas preferências
        val averagePreference = preferences.values.average()
        return (averagePreference * totalSubscribers) / 100
    }

    private fun calculateEngagementLevel(preferences: Map<String, Double>): Double {
        // Calcula nível de engajamento baseado nas preferências
        return preferences.values.average()
    }

    private fun generateContentRecommendations(preferences: Map<String, Double>): List<String> {
        val recommendations = mutableListOf<String>()

        // Gera recomendações baseadas nas preferências do segmento
        preferences.forEach { (metric, value) ->
            when {
                metric == "format" && value > 0.7 ->
                    recommendations.add("Mantenha o formato atual, está performando bem")
                metric == "duration" && value > 900 ->
                    recommendations.add("Considere criar conteúdo mais longo")
                metric == "topics" && value > 0.8 ->
                    recommendations.add("Este tipo de conteúdo tem alta demanda")
                metric == "timing" && value > 0.6 ->
                    recommendations.add("Mantenha este horário de publicação")
            }
        }

        return recommendations
    }

    private fun calculateOverlapPercentage(
        commonCommenters: Map<String, Int>,
        channels: List<Channel>
    ): Double {
        if (channels.isEmpty() || commonCommenters.isEmpty()) return 0.0

        val averageSubscribers = channels.map {
            it.subscriberCount
        }.average()

        val commonSubscribers = commonCommenters.values.sum()

        return (commonSubscribers.toDouble() / averageSubscribers) * 100
    }

    private fun findCommonCommenters(channels: List<Channel>): Map<String, Int> {
        // Implementar lógica para encontrar usuários que comentam em múltiplos canais
        return mapOf() // Placeholder
    }

    private fun analyzeViewingPatterns(channels: List<Channel>): ViewingPatterns {
        val peakHours = mutableMapOf<Int, Int>()
        val weekdayDistribution = mutableMapOf<Int, Int>()

        channels.forEach { channel ->
            val videos = videoRepository.findByChannel(channel)
            videos.forEach { video ->
                val hour = video.publishedAt.hour
                val dayOfWeek = video.publishedAt.dayOfWeek.value

                peakHours[hour] = (peakHours[hour] ?: 0) + video.viewCount.toInt()
                weekdayDistribution[dayOfWeek] = (weekdayDistribution[dayOfWeek] ?: 0) +
                        video.viewCount.toInt()
            }
        }

        return ViewingPatterns(
            peakHours = peakHours.mapValues { it.value.toDouble() / channels.size },
            weekdayDistribution = weekdayDistribution.mapValues { it.value.toDouble() / channels.size }
        )
    }

    private fun calculateSimilarityScores(channels: List<Channel>): Map<Pair<String, String>, Double> {
        val scores = mutableMapOf<Pair<String, String>, Double>()

        for (i in channels.indices) {
            for (j in i + 1 until channels.size) {
                val channelA = channels[i]
                val channelB = channels[j]

                val score = calculateChannelSimilarity(channelA, channelB)
                scores[Pair(channelA.channelId, channelB.channelId)] = score
            }
        }

        return scores
    }

    private fun calculateChannelSimilarity(channelA: Channel, channelB: Channel): Double {
        val videosA = videoRepository.findByChannel(channelA)
        val videosB = videoRepository.findByChannel(channelB)

        // Comparar categorias de conteúdo
        val categoryOverlap = calculateCategoryOverlap(videosA, videosB)

        // Comparar padrões de publicação
        val publishingPatternSimilarity = calculatePublishingPatternSimilarity(videosA, videosB)

        // Comparar métricas de engajamento
        val engagementSimilarity = calculateEngagementSimilarity(videosA, videosB)

        // Peso composto das diferentes métricas
        return (categoryOverlap * 0.4 +
                publishingPatternSimilarity * 0.3 +
                engagementSimilarity * 0.3)
    }

    private fun calculateCategoryOverlap(videosA: List<Video>, videosB: List<Video>): Double {
        val categoriesA = videosA.map { it.categoryId }.toSet()
        val categoriesB = videosB.map { it.categoryId }.toSet()

        val intersection = categoriesA.intersect(categoriesB).size
        val union = categoriesA.union(categoriesB).size

        return if (union > 0) intersection.toDouble() / union else 0.0
    }

    private fun calculatePublishingPatternSimilarity(
        videosA: List<Video>,
        videosB: List<Video>
    ): Double {
        // Comparar horários de publicação
        val hoursA = videosA.map { it.publishedAt.hour }
        val hoursB = videosB.map { it.publishedAt.hour }

        var hourSimilarity = 0.0
        for (hour in 0..23) {
            val freqA = hoursA.count { it == hour }.toDouble() / hoursA.size
            val freqB = hoursB.count { it == hour }.toDouble() / hoursB.size
            hourSimilarity += 1 - abs(freqA - freqB)
        }

        return hourSimilarity / 24
    }

    private fun calculateEngagementSimilarity(
        videosA: List<Video>,
        videosB: List<Video>
    ): Double {
        val engagementA = calculateAverageEngagement(videosA)
        val engagementB = calculateAverageEngagement(videosB)

        val maxEngagement = maxOf(engagementA, engagementB)
        val minEngagement = minOf(engagementA, engagementB)

        return if (maxEngagement > 0) minEngagement / maxEngagement else 1.0
    }

    private fun calculateAverageEngagement(videos: List<Video>): Double {
        if (videos.isEmpty()) return 0.0

        return videos.map { video ->
            (video.likeCount + video.commentCount).toDouble() / video.viewCount
        }.average()
    }

    private fun identifyRelatedNiches(channels: List<Channel>): List<RelatedNiche> {
        val niches = mutableListOf<RelatedNiche>()

        // Analisar categorias de conteúdo
        val commonCategories = channels.flatMap { channel ->
            videoRepository.findByChannel(channel)
                .map { it.categoryId }
        }.groupBy { it }
            .mapValues { it.value.size }
            .filter { it.value >= channels.size / 2 }

        commonCategories.forEach { (category, count) ->
            niches.add(
                RelatedNiche(
                    name = category,
                    relevanceScore = count.toDouble() / channels.size,
                    commonTopics = findCommonTopics(channels, category)
                )
            )
        }

        return niches
    }

    private fun findCommonTopics(channels: List<Channel>, category: String): List<String> {
        // Implementar lógica para encontrar tópicos comuns dentro de uma categoria
        return listOf() // Placeholder
    }

    private fun suggestCollaborations(
        channels: List<Channel>,
        similarityScores: Map<Pair<String, String>, Double>
    ): List<CollaborationSuggestion> {
        return similarityScores
            .filter { it.value > 0.7 } // Threshold para sugestões
            .map { (channelPair, score) ->
                CollaborationSuggestion(
                    channelA = channelPair.first,
                    channelB = channelPair.second,
                    compatibilityScore = score,
                    potentialTopics = suggestCollaborationTopics(
                        channels.find { it.channelId == channelPair.first }!!,
                        channels.find { it.channelId == channelPair.second }!!
                    )
                )
            }
    }

    private fun suggestCollaborationTopics(channelA: Channel, channelB: Channel): List<String> {
        // Implementar lógica para sugerir tópicos para colaboração
        return listOf() // Placeholder
    }

    private fun analyzeEngagementPatterns(videos: List<Video>): EngagementPatterns {
        val overallEngagement = calculateOverallEngagement(videos)
        val engagementByDay = calculateEngagementByDay(videos)
        val engagementByHour = calculateEngagementByHour(videos)
        val engagementByType = calculateEngagementByType(videos)
        val trend = analyzeEngagementTrend(videos)

        return EngagementPatterns(
            overallEngagementRate = overallEngagement,
            engagementByDayOfWeek = engagementByDay,
            engagementByHour = engagementByHour,
            engagementByContentType = engagementByType,
            trendAnalysis = trend
        )
    }

    private fun calculateOverallEngagement(videos: List<Video>): Double {
        if (videos.isEmpty()) return 0.0

        return videos.map { video ->
            val totalEngagements = video.likeCount + video.commentCount
            (totalEngagements.toDouble() / video.viewCount) * 100
        }.average()
    }

    private fun calculateEngagementByDay(videos: List<Video>): Map<Int, Double> {
        val engagementByDay = mutableMapOf<Int, Double>()

        videos.groupBy {
            it.publishedAt.dayOfWeek.value
        }.forEach { (day, dayVideos) ->
            val avgEngagement = dayVideos.map { video ->
                val totalEngagements = video.likeCount + video.commentCount
                (totalEngagements.toDouble() / video.viewCount) * 100
            }.average()

            engagementByDay[day] = avgEngagement
        }

        return engagementByDay
    }

    private fun calculateEngagementByHour(videos: List<Video>): Map<Int, Double> {
        val engagementByHour = mutableMapOf<Int, Double>()

        videos.groupBy {
            it.publishedAt.hour
        }.forEach { (hour, hourVideos) ->
            val avgEngagement = hourVideos.map { video ->
                val totalEngagements = video.likeCount + video.commentCount
                (totalEngagements.toDouble() / video.viewCount) * 100
            }.average()

            engagementByHour[hour] = avgEngagement
        }

        return engagementByHour
    }

    private fun calculateEngagementByType(videos: List<Video>): Map<String, Double> {
        return videos.groupBy { video ->
            categorizeContent(video)
        }.mapValues { (_, typeVideos) ->
            typeVideos.map { video ->
                val totalEngagements = video.likeCount + video.commentCount
                (totalEngagements.toDouble() / video.viewCount) * 100
            }.average()
        }
    }

    private fun categorizeContent(video: Video): String {
        return when {
            video.duration.toSeconds() <= 60 -> "shorts"
            video.title.contains(Regex("(review|análise)", RegexOption.IGNORE_CASE)) -> "review"
            video.title.contains(Regex("(tutorial|how to|como)", RegexOption.IGNORE_CASE)) -> "tutorial"
            video.title.contains(Regex("(vlog|daily|dia)", RegexOption.IGNORE_CASE)) -> "vlog"
            else -> "other"
        }
    }

    private fun analyzeEngagementTrend(videos: List<Video>): EngagementTrend {
        val sortedVideos = videos.sortedBy { it.publishedAt }
        if (sortedVideos.size < 2) {
            return EngagementTrend(
                direction = "stable",
                percentageChange = 0.0,
                timeFrame = "last 3 months",
                significantChanges = emptyList()
            )
        }

        // Dividir vídeos em dois períodos
        val midPoint = sortedVideos.size / 2
        val firstHalf = sortedVideos.subList(0, midPoint)
        val secondHalf = sortedVideos.subList(midPoint, sortedVideos.size)

        // Calcular engajamento médio para cada período
        val firstHalfEngagement = calculateOverallEngagement(firstHalf)
        val secondHalfEngagement = calculateOverallEngagement(secondHalf)

        // Calcular mudança percentual
        val percentageChange = ((secondHalfEngagement - firstHalfEngagement) / firstHalfEngagement) * 100

        // Determinar direção da tendência
        val direction = when {
            percentageChange > 10.0 -> "increasing"
            percentageChange < -10.0 -> "decreasing"
            else -> "stable"
        }

        // Identificar mudanças significativas
        val significantChanges = findSignificantChanges(sortedVideos)

        return EngagementTrend(
            direction = direction,
            percentageChange = percentageChange,
            timeFrame = "last 3 months",
            significantChanges = significantChanges
        )
    }

    private fun findSignificantChanges(videos: List<Video>): List<SignificantChange> {
        val changes = mutableListOf<SignificantChange>()
        val baselineEngagement = videos.take(3).map {
            (it.likeCount + it.commentCount).toDouble() / it.viewCount
        }.average()

        videos.windowed(3).forEach { window ->
            val windowEngagement = window.map {
                (it.likeCount + it.commentCount).toDouble() / it.viewCount
            }.average()

            val change = ((windowEngagement - baselineEngagement) / baselineEngagement) * 100

            if (Math.abs(change) > 20) { // Mudança significativa: mais de 20%
                changes.add(SignificantChange(
                    date = window[0].publishedAt.toString(),
                    metric = "engagement",
                    changePercentage = change,
                    possibleReason = analyzePossibleReason(window[0])
                ))
            }
        }

        return changes
    }

    private fun analyzePossibleReason(video: Video): String {
        // Análise simplificada de possíveis razões para mudanças
        return when {
            video.title.contains(Regex("(novo|new|launch|lançamento)", RegexOption.IGNORE_CASE)) ->
                "Novo formato ou série"
            video.duration.toSeconds() <= 60 ->
                "Experimentação com formato curto"
            video.title.contains(Regex("(collab|feat|com|ft)", RegexOption.IGNORE_CASE)) ->
                "Colaboração com outro criador"
            else -> "Mudança no conteúdo ou formato"
        }
    }

    private fun analyzeActivityPeaks(videos: List<Video>): ActivityPeaks {
        val engagementByHour = calculateEngagementByHour(videos)
        val engagementByDay = calculateEngagementByDay(videos)

        // Encontrar melhores horários
        val bestHours = engagementByHour.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        // Encontrar melhores dias
        val bestDays = engagementByDay.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        // Identificar janelas de pico
        val peakWindows = findPeakWindows(videos)

        // Analisar tendências sazonais
        val seasonalTrends = analyzeSeasonalTrends(videos)

        // Analisar impacto de eventos especiais
        val eventImpacts = analyzeEventImpacts(videos)

        return ActivityPeaks(
            bestDaysToPost = bestDays,
            bestHoursToPost = bestHours,
            peakEngagementWindows = peakWindows,
            seasonalTrends = seasonalTrends,
            specialEvents = eventImpacts
        )
    }

    private fun findPeakWindows(videos: List<Video>): List<TimeWindow> {
        val windows = mutableListOf<TimeWindow>()

        for (day in 1..7) {
            for (hour in 0..23) {
                val relevantVideos = videos.filter {
                    it.publishedAt.dayOfWeek.value == day &&
                            it.publishedAt.hour in hour..(hour + 2)
                }

                if (relevantVideos.isNotEmpty()) {
                    val avgEngagement = calculateOverallEngagement(relevantVideos)
                    val overallAvg = calculateOverallEngagement(videos)

                    if (avgEngagement > overallAvg * 1.2) { // 20% acima da média
                        windows.add(TimeWindow(
                            dayOfWeek = day,
                            startHour = hour,
                            endHour = (hour + 2).coerceAtMost(23),
                            engagementMultiplier = avgEngagement / overallAvg
                        ))
                    }
                }
            }
        }

        return windows.sortedByDescending { it.engagementMultiplier }
    }

    private fun analyzeSeasonalTrends(videos: List<Video>): Map<String, Double> {
        val seasonalEngagement = mutableMapOf<String, Double>()

        videos.groupBy {
            getSeason(it.publishedAt.monthValue)
        }.forEach { (season, seasonVideos) ->
            val avgEngagement = calculateOverallEngagement(seasonVideos)
            val overallAvg = calculateOverallEngagement(videos)

            seasonalEngagement[season] = avgEngagement / overallAvg
        }

        return seasonalEngagement
    }

    private fun getSeason(month: Int): String {
        return when (month) {
            in 12..2 -> "summer"
            in 3..5 -> "autumn"
            in 6..8 -> "winter"
            else -> "spring"
        }
    }

    private fun analyzeEventImpacts(videos: List<Video>): List<EventImpact> {
        val events = mutableListOf<EventImpact>()
        val overallAvgEngagement = calculateOverallEngagement(videos)

        // Agrupar vídeos por mês e detectar picos
        videos.groupBy {
            "${it.publishedAt.year}-${it.publishedAt.monthValue}"
        }.forEach { (monthYear, monthVideos) ->
            val monthlyEngagement = calculateOverallEngagement(monthVideos)

            if (monthlyEngagement > overallAvgEngagement * 1.3) { // 30% acima da média
                events.add(EventImpact(
                    eventName = detectEventType(monthVideos),
                    dateRange = monthYear,
                    engagementIncrease = ((monthlyEngagement / overallAvgEngagement) - 1) * 100,
                    recommendedActions = generateEventRecommendations(monthVideos)
                ))
            }
        }

        return events
    }

    private fun detectEventType(videos: List<Video>): String {
        val titleKeywords = videos.flatMap {
            it.title.lowercase().split(" ")
        }.groupBy { it }
            .mapValues { it.value.size }
            .entries.sortedByDescending { it.value }
            .take(5)
            .map { it.key }

        return when {
            titleKeywords.any { it in listOf("natal", "christmas", "festas") } -> "Festas de Fim de Ano"
            titleKeywords.any { it in listOf("férias", "vacation", "summer") } -> "Período de Férias"
            titleKeywords.any { it in listOf("especial", "special") } -> "Evento Especial"
            else -> "Pico Sazonal"
        }
    }

    private fun generateEventRecommendations(videos: List<Video>): List<String> {
        val successfulFormats = videos.sortedByDescending {
            (it.likeCount + it.commentCount).toDouble() / it.viewCount
        }.take(3)

        return successfulFormats.map { video ->
            "Considere criar conteúdo similar a '${video.title}' que teve boa performance"
        } + "Planeje conteúdo antecipadamente para o próximo evento similar"
    }

    private fun analyzeInteractionTypes(videos: List<Video>): InteractionTypes {
        if (videos.isEmpty()) return InteractionTypes(
            commentToViewRatio = 0.0,
            likeToViewRatio = 0.0,
            shareToViewRatio = 0.0,
            commentSentimentAnalysis = analyzeSentiment(emptyList()),
            interactionDistribution = emptyMap(),
            commentTopics = emptyList()
        )

        val totalViews = videos.sumOf { it.viewCount }
        val totalComments = videos.sumOf { it.commentCount }
        val totalLikes = videos.sumOf { it.likeCount }
        val totalShares = videos.sumOf { it.shareCount ?: 0 }

        val totalInteractions = totalComments + totalLikes + totalShares

        return InteractionTypes(
            commentToViewRatio = (totalComments.toDouble() / totalViews) * 100,
            likeToViewRatio = (totalLikes.toDouble() / totalViews) * 100,
            shareToViewRatio = (totalShares.toDouble() / totalViews) * 100,
            commentSentimentAnalysis = analyzeSentiment(videos),
            interactionDistribution = mapOf(
                "comments" to (totalComments.toDouble() / totalInteractions) * 100,
                "likes" to (totalLikes.toDouble() / totalInteractions) * 100,
                "shares" to (totalShares.toDouble() / totalInteractions) * 100
            ),
            commentTopics = analyzeCommentTopics(videos)
        )
    }

    private fun analyzeSentiment(videos: List<Video>): SentimentAnalysis {
        // Em uma implementação real, você usaria uma API de análise de sentimento
        // Por enquanto, vamos simular com uma análise básica
        return SentimentAnalysis(
            overallSentiment = 0.5, // Neutro a positivo
            sentimentDistribution = mapOf(
                "positive" to 0.6,
                "neutral" to 0.3,
                "negative" to 0.1
            ),
            commonPhrases = listOf("ótimo vídeo", "muito bom", "parabéns"),
            topEmojis = listOf("👍", "❤️", "😍")
        )
    }

    private fun analyzeCommentTopics(videos: List<Video>): List<CommentTopic> {
        // Em uma implementação real, você usaria processamento de linguagem natural
        // Por enquanto, vamos retornar alguns tópicos simulados
        return listOf(
            CommentTopic(
                topic = "Qualidade do Conteúdo",
                frequency = 0.4,
                sentiment = 0.8,
                keywords = listOf("qualidade", "conteúdo", "excelente")
            ),
            CommentTopic(
                topic = "Sugestões",
                frequency = 0.3,
                sentiment = 0.6,
                keywords = listOf("poderia", "próximo", "sugiro")
            )
        )
    }

    private fun analyzeRetentionTrends(videos: List<Video>): RetentionTrends {
        val avgRetention = calculateAverageRetention(videos)
        val retentionByLength = analyzeRetentionByLength(videos)
        val retentionByType = analyzeRetentionByType(videos)
        val dropOffPoints = findCommonDropOffPoints(videos)
        val improvements = suggestRetentionImprovements(videos)

        return RetentionTrends(
            averageRetentionRate = avgRetention,
            retentionByVideoLength = retentionByLength,
            retentionByContentType = retentionByType,
            dropOffPoints = dropOffPoints,
            improvements = improvements
        )
    }

    private fun analyzeRetentionByLength(videos: List<Video>): Map<String, Double> {
        val retentionByLength = mutableMapOf<String, Double>()

        videos.groupBy { video ->
            when {
                video.duration.toSeconds() <= 180 -> "0-3min"
                video.duration.toSeconds() <= 600 -> "3-10min"
                video.duration.toSeconds() <= 1200 -> "10-20min"
                else -> "20min+"
            }
        }.forEach { (length, lengthVideos) ->
            retentionByLength[length] = calculateAverageRetention(lengthVideos)
        }

        return retentionByLength
    }

    private fun analyzeRetentionByType(videos: List<Video>): Map<String, Double> {
        return videos.groupBy {
            categorizeContent(it)
        }.mapValues { (_, typeVideos) ->
            calculateAverageRetention(typeVideos)
        }
    }

    fun calculateAverageRetention(videos: List<Video>): Double {
        if (videos.isEmpty()) return 0.0

        return videos.map { video ->
            val watchTime = video.viewCount * video.duration.seconds // tempo total assistido estimado
            val potentialWatchTime = video.viewCount * video.duration.seconds // tempo total possível

            if (potentialWatchTime > 0) {
                (watchTime.toDouble() / potentialWatchTime) * 100
            } else 0.0
        }.average()
    }

    private fun findCommonDropOffPoints(videos: List<Video>): List<DropOffPoint> {
        // Em uma implementação real, você usaria dados da API do YouTube Analytics
        // Por enquanto, vamos retornar alguns pontos comuns de abandono
        return listOf(
            DropOffPoint(
                timestamp = 30,
                percentageDropped = 20.0,
                possibleReason = "Introdução muito longa",
                recommendedAction = "Torne a introdução mais dinâmica e concisa"
            ),
            DropOffPoint(
                timestamp = 300,
                percentageDropped = 15.0,
                possibleReason = "Perda de ritmo no conteúdo",
                recommendedAction = "Adicione mais pontos de engajamento ou transições"
            )
        )
    }

    private fun suggestRetentionImprovements(videos: List<Video>): List<RetentionImprovement> {
        val avgRetention = calculateAverageRetention(videos)
        val improvements = mutableListOf<RetentionImprovement>()

        if (avgRetention < 0.5) {
            improvements.add(RetentionImprovement(
                metric = "Retenção Média",
                currentValue = avgRetention * 100,
                targetValue = 50.0,
                suggestedActions = listOf(
                    "Adicione timestamps nos vídeos longos",
                    "Melhore a qualidade da introdução",
                    "Mantenha um ritmo mais consistente"
                )
            ))
        }

        return improvements
    }

    private fun analyzeContentPreferences(videos: List<Video>): ContentPreferences {
        val formats = analyzePreferredFormats(videos)
        val topics = analyzeTopicPreferences(videos)
        val duration = calculateOptimalDurations(videos)
        val gaps = identifyContentGaps(videos)
        val patterns = identifySuccessPatterns(videos)

        return ContentPreferences(
            preferredFormats = formats,
            topicPreferences = topics,
            optimalDuration = duration,
            contentGaps = gaps,
            successPatterns = patterns
        )
    }

    private fun analyzePreferredFormats(videos: List<Video>): List<ContentFormat> {
        return videos.groupBy {
            categorizeContent(it)
        }.map { (format, formatVideos) ->
            ContentFormat(
                format = format,
                engagementRate = calculateOverallEngagement(formatVideos),
                retentionRate = calculateAverageRetention(formatVideos),
                growthPotential = calculateGrowthPotential(format, formatVideos)
            )
        }
    }

    private fun calculateGrowthPotential(format: String, videos: List<Video>): Double {
        val recentVideos = videos.sortedByDescending { it.publishedAt }.take(5)
        val olderVideos = videos.sortedByDescending { it.publishedAt }.drop(5)

        if (recentVideos.isEmpty() || olderVideos.isEmpty()) return 0.0

        val recentEngagement = calculateOverallEngagement(recentVideos)
        val olderEngagement = calculateOverallEngagement(olderVideos)

        return ((recentEngagement - olderEngagement) / olderEngagement) * 100
    }

    private fun analyzeTopicPreferences(videos: List<Video>): List<TopicPreference> {
        val topics = videos.groupBy { extractMainTopic(it) }

        return topics.map { (topic, topicVideos) ->
            val engagementScore = calculateOverallEngagement(topicVideos)
            val trendData = analyzeTrendData(topic, topicVideos)

            TopicPreference(
                topic = topic,
                interestScore = engagementScore,
                trendsData = trendData,
                recommendedApproach = generateTopicRecommendation(topic, trendData)
            )
        }
    }

    private fun extractMainTopic(video: Video): String {
        // Em uma implementação real, você usaria NLP ou categorização mais sofisticada
        return when {
            video.title.contains(Regex("(review|análise)", RegexOption.IGNORE_CASE)) -> "Reviews"
            video.title.contains(Regex("(tutorial|how to|como)", RegexOption.IGNORE_CASE)) -> "Tutoriais"
            video.title.contains(Regex("(gameplay|playing|jogando)", RegexOption.IGNORE_CASE)) -> "Gameplay"
            else -> "Outros"
        }
    }

    private fun analyzeTrendData(topic: String, videos: List<Video>): TrendData {
        val recentVideos = videos.sortedByDescending { it.publishedAt }.take(5)
        val olderVideos = videos.sortedByDescending { it.publishedAt }.drop(5)

        val recentEngagement = if (recentVideos.isNotEmpty()) {
            calculateOverallEngagement(recentVideos)
        } else 0.0

        val olderEngagement = if (olderVideos.isNotEmpty()) {
            calculateOverallEngagement(olderVideos)
        } else 0.0

        val trend = when {
            recentEngagement > olderEngagement * 1.2 -> "increasing"
            recentEngagement < olderEngagement * 0.8 -> "decreasing"
            else -> "stable"
        }

        return TrendData(
            currentTrend = trend,
            projectedGrowth = ((recentEngagement - olderEngagement) / olderEngagement) * 100,
            competitionLevel = "medium", // Em uma implementação real, você analisaria concorrentes
            seasonality = analyzeTopicSeasonality(videos)
        )
    }

    private fun analyzeTopicSeasonality(videos: List<Video>): Map<String, Double> {
        return videos.groupBy {
            getSeason(it.publishedAt.monthValue)
        }.mapValues { (_, seasonVideos) ->
            calculateOverallEngagement(seasonVideos)
        }
    }

    private fun generateTopicRecommendation(topic: String, trendData: TrendData): String {
        return when (trendData.currentTrend) {
            "increasing" -> "Aumente a frequência de conteúdo sobre $topic"
            "decreasing" -> "Inove na abordagem de $topic ou reduza frequência"
            else -> "Mantenha a estratégia atual para $topic"
        }
    }

    private fun calculateOptimalDurations(videos: List<Video>): Map<String, Int> {
        return videos.groupBy {
            categorizeContent(it)
        }.mapValues { (_, formatVideos) ->
            val highEngagementVideos = formatVideos.sortedByDescending {
                (it.likeCount + it.commentCount).toDouble() / it.viewCount
            }.take(3)

            highEngagementVideos.map {
                it.duration.seconds.toInt()
            }.average().toInt()
        }
    }

    private fun identifyContentGaps(videos: List<Video>): List<ContentGap> {
        // Em uma implementação real, você analisaria tendências do mercado e concorrentes
        return listOf(
            ContentGap(
                topic = "Shorts",
                demandScore = 0.8,
                competitionLevel = "alta",
                estimatedViews = 10000,
                recommendedFormat = "Vídeos de 30-60 segundos"
            )
        )
    }

    private fun identifySuccessPatterns(videos: List<Video>): List<SuccessPattern> {
        val patterns = mutableListOf<SuccessPattern>()

        // Análise de títulos bem sucedidos
        val titlePattern = analyzeSuccessfulTitles(videos)
        patterns.add(titlePattern)

        // Análise de duração ideal
        val durationPattern = analyzeSuccessfulDurations(videos)
        patterns.add(durationPattern)

        // Análise de horários de publicação
        val timingPattern = analyzeSuccessfulTiming(videos)
        patterns.add(timingPattern)

        // Análise de thumbnails/miniaturas
        val thumbnailPattern = analyzeSuccessfulThumbnails(videos)
        patterns.add(thumbnailPattern)

        return patterns
    }

    private fun analyzeSuccessfulTitles(videos: List<Video>): SuccessPattern {
        val successfulVideos = videos.sortedByDescending {
            (it.likeCount + it.commentCount).toDouble() / it.viewCount
        }.take(5)

        return SuccessPattern(
            pattern = "Padrão de Título",
            impact = calculatePatternImpact(successfulVideos, videos),
            examples = successfulVideos.map { it.title },
            implementationTips = listOf(
                "Use números nos títulos quando relevante",
                "Mantenha títulos entre 40-60 caracteres",
                "Inclua palavras-chave no início do título",
                "Use emojis estrategicamente"
            )
        )
    }

    private fun analyzeSuccessfulDurations(videos: List<Video>): SuccessPattern {
        val successfulVideos = videos.sortedByDescending {
            (it.likeCount + it.commentCount).toDouble() / it.viewCount
        }.take(5)

        val avgDuration = successfulVideos.map {
            it.duration.seconds
        }.average().toInt()

        return SuccessPattern(
            pattern = "Duração Ideal",
            impact = calculatePatternImpact(successfulVideos, videos),
            examples = successfulVideos.map {
                "Vídeo de ${it.duration.toMinutes()} minutos: ${it.title}"
            },
            implementationTips = listOf(
                "Mantenha vídeos próximos a $avgDuration segundos",
                "Organize o conteúdo em blocos de 2-3 minutos",
                "Use timestamps para vídeos mais longos",
                "Mantenha a introdução com menos de 30 segundos"
            )
        )
    }

    private fun analyzeSuccessfulTiming(videos: List<Video>): SuccessPattern {
        val successfulVideos = videos.sortedByDescending {
            (it.likeCount + it.commentCount).toDouble() / it.viewCount
        }.take(5)

        val commonHours = successfulVideos.groupBy {
            it.publishedAt.hour
        }.maxByOrNull { it.value.size }?.key ?: 0

        val commonDays = successfulVideos.groupBy {
            it.publishedAt.dayOfWeek
        }.maxByOrNull { it.value.size }?.key

        return SuccessPattern(
            pattern = "Horário de Publicação",
            impact = calculatePatternImpact(successfulVideos, videos),
            examples = successfulVideos.map {
                "Publicado em ${it.publishedAt.dayOfWeek} às ${it.publishedAt.hour}h"
            },
            implementationTips = listOf(
                "Publique preferencialmente às $commonHours horas",
                "Priorize publicações em $commonDays",
                "Mantenha consistência nos horários",
                "Considere o fuso horário do seu público principal"
            )
        )
    }

    private fun analyzeSuccessfulThumbnails(videos: List<Video>): SuccessPattern {
        // Em uma implementação real, você analisaria as thumbnails usando processamento de imagem
        return SuccessPattern(
            pattern = "Estilo de Thumbnail",
            impact = 25.0, // Impacto estimado
            examples = videos.sortedByDescending {
                it.viewCount
            }.take(3).map { it.title },
            implementationTips = listOf(
                "Use texto grande e legível",
                "Limite o texto a 3-4 palavras",
                "Use contraste alto entre texto e fundo",
                "Inclua expressões faciais marcantes quando relevante",
                "Mantenha consistência na identidade visual"
            )
        )
    }

    private fun calculatePatternImpact(
        successfulVideos: List<Video>,
        allVideos: List<Video>
    ): Double {
        val successfulEngagement = calculateOverallEngagement(successfulVideos)
        val averageEngagement = calculateOverallEngagement(allVideos)

        return ((successfulEngagement - averageEngagement) / averageEngagement) * 100
    }

    // Função auxiliar para formatar durações
    private fun Duration.toMinutes(): String {
        val minutes = this.toMinutes()
        val seconds = this.minusMinutes(minutes).seconds
        return if (seconds > 0) {
            "$minutes:${String.format("%02d", seconds)}"
        } else {
            "$minutes:00"
        }
    }
}

// Data classes para retorno das análises
data class AudienceOverlapMetrics(
    val channels: List<String>,
    val commonAudiencePercentage: Double,
    val viewingPatterns: ViewingPatterns,
    val similarityScores: Map<Pair<String, String>, Double>,
    val relatedNiches: List<RelatedNiche>,
    val recommendedCollaborations: List<CollaborationSuggestion>
)

data class ViewingPatterns(
    val peakHours: Map<Int, Double>,
    val weekdayDistribution: Map<Int, Double>
)

data class RelatedNiche(
    val name: String,
    val relevanceScore: Double,
    val commonTopics: List<String>
)

data class CollaborationSuggestion(
    val channelA: String,
    val channelB: String,
    val compatibilityScore: Double,
    val potentialTopics: List<String>
)

data class AudienceBehaviorMetrics(
    val channelId: String,
    val engagementPatterns: EngagementPatterns,
    val activityPeaks: ActivityPeaks,
    val interactionTypes: InteractionTypes,
    val audienceRetentionTrends: RetentionTrends,
    val contentPreferences: ContentPreferences
)

data class AudienceSegment(
    val segmentType: String,
    val size: Double,
    val preferences: Map<String, Double>,
    val engagementLevel: Double,
    val recommendedContent: List<String>
)

