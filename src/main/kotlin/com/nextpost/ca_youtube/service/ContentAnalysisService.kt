package com.nextpost.ca_youtube.service

import com.nextpost.ca_youtube.model.entity.*
import com.nextpost.ca_youtube.repository.VideoRepository
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDateTime

@Service
class ContentAnalysisService(
    private val videoRepository: VideoRepository,
    private val youTubeService: YouTubeService
) {
    /**
     * Analisa padrões de retenção dos vídeos de um canal
     */
    fun analyzeRetention(channel: Channel): RetentionMetrics {
        val videos = videoRepository.findByChannel(channel)
        val recentVideos = videos.filter {
            it.publishedAt.isAfter(LocalDateTime.now().minusMonths(3))
        }

        // Análise por duração do vídeo
        val retentionByDuration = recentVideos.groupBy { video ->
            when {
                video.duration.toMinutes() < 5 -> "muito_curto"
                video.duration.toMinutes() < 10 -> "curto"
                video.duration.toMinutes() < 20 -> "medio"
                else -> "longo"
            }
        }.mapValues { (_, videos) ->
            calculateRetentionMetrics(videos)
        }

        // Análise por horário de publicação
        val retentionByHour = recentVideos.groupBy {
            it.publishedAt.hour
        }.mapValues { (_, videos) ->
            calculateRetentionMetrics(videos)
        }

        // Análise por dia da semana
        val retentionByDayOfWeek = recentVideos.groupBy {
            it.publishedAt.dayOfWeek
        }.mapValues { (_, videos) ->
            calculateRetentionMetrics(videos)
        }

        // Identificar os melhores padrões
        val bestPerformingDuration = retentionByDuration.maxByOrNull {
            it.value.averageRetentionRate
        }?.key

        val bestPerformingHour = retentionByHour.maxByOrNull {
            it.value.averageRetentionRate
        }?.key

        val bestPerformingDay = retentionByDayOfWeek.maxByOrNull {
            it.value.averageRetentionRate
        }?.key

        return RetentionMetrics(
            channelId = channel.channelId,
            retentionByDuration = retentionByDuration,
            retentionByHour = retentionByHour,
            retentionByDayOfWeek = retentionByDayOfWeek,
            recommendedDuration = bestPerformingDuration,
            recommendedHour = bestPerformingHour,
            recommendedDay = bestPerformingDay,
            overallRetention = calculateOverallRetention(recentVideos)
        )
    }

    /**
     * Analisa padrões nos títulos, descrições e tags dos vídeos
     */
    fun analyzeContentPatterns(channel: Channel): ContentPatternMetrics {
        val videos = videoRepository.findByChannel(channel)
        val recentVideos = videos.filter {
            it.publishedAt.isAfter(LocalDateTime.now().minusMonths(3))
        }

        // Análise de padrões nos títulos
        val titlePatterns = analyzeTitlePatterns(recentVideos)

        // Análise de tópicos/categorias
        val topicAnalysis = analyzeTopics(recentVideos)

        // Análise de performance por tipo de conteúdo
        val contentTypeAnalysis = analyzeContentTypes(recentVideos)

        return ContentPatternMetrics(
            channelId = channel.channelId,
            titlePatterns = titlePatterns,
            topicAnalysis = topicAnalysis,
            contentTypeAnalysis = contentTypeAnalysis,
            recommendations = generateRecommendations(
                titlePatterns,
                topicAnalysis,
                contentTypeAnalysis
            )
        )
    }

    private fun calculateRetentionMetrics(videos: List<Video>): VideoRetentionStats {
        if (videos.isEmpty()) {
            return VideoRetentionStats(
                averageRetentionRate = 0.0,
                averageEngagementRate = 0.0,
                commonDropoffPoints = emptyList(),
                viewDuration = 0.0
            )
        }

        return VideoRetentionStats(
            averageRetentionRate = calculateAverageRetention(videos),
            averageEngagementRate = calculateEngagementRate(videos),
            commonDropoffPoints = identifyDropoffPoints(videos),
            viewDuration = calculateAverageViewDuration(videos)
        )
    }

    private fun calculateAverageRetention(videos: List<Video>): Double {
        if (videos.isEmpty()) return 0.0

        return videos.map { video ->
            val totalViews = video.viewCount.toDouble()
            val averageViewDuration = video.duration.seconds.toDouble()
            val totalPossibleWatchTime = totalViews * video.duration.seconds

            if (totalPossibleWatchTime > 0) {
                (averageViewDuration / totalPossibleWatchTime) * 100
            } else 0.0
        }.average()
    }

    private fun calculateEngagementRate(videos: List<Video>): Double {
        if (videos.isEmpty()) return 0.0

        return videos.map { video ->
            val totalViews = video.viewCount.toDouble()
            val totalEngagements = video.likeCount + video.commentCount // Assumindo que esses campos existem

            if (totalViews > 0) {
                (totalEngagements / totalViews) * 100
            } else 0.0
        }.average()
    }

    private fun identifyDropoffPoints(videos: List<Video>): List<Double> {
        if (videos.isEmpty()) return emptyList()

        // Aqui precisaríamos de dados mais granulares da API do YouTube
        // sobre os pontos onde os espectadores param de assistir
        // Por enquanto, vamos simular alguns pontos comuns
        val dropoffPoints = mutableListOf<Double>()

        videos.forEach { video ->
            val duration = video.duration.seconds.toDouble()

            // Pontos comuns de abandono (30 segundos, 1 minuto, etc)
            val points = listOf(30.0, 60.0, 120.0, 300.0)
            points.forEach { point ->
                if (point < duration) {
                    dropoffPoints.add((point / duration) * 100)
                }
            }
        }

        return dropoffPoints.distinct().sorted()
    }

    private fun calculateAverageViewDuration(videos: List<Video>): Double {
        if (videos.isEmpty()) return 0.0

        return videos.map { video ->
            val retentionRate = calculateAverageRetention(listOf(video)) / 100
            video.duration.seconds * retentionRate
        }.average()
    }

    private fun calculateOverallRetention(videos: List<Video>): OverallRetentionStats {
        // Implementar cálculo de retenção geral do canal
        return OverallRetentionStats(
            averageRetentionRate = 0.0,
            trendOverTime = "stable",
            retentionByVideoLength = mapOf(),
            topPerformingVideos = listOf()
        )
    }

    private fun analyzeTitlePatterns(videos: List<Video>): TitlePatternAnalysis {
        if (videos.isEmpty()) {
            return TitlePatternAnalysis(
                highPerformingKeywords = emptyList(),
                optimalLength = 0,
                emojiImpact = emptyMap()
            )
        }

        return TitlePatternAnalysis(
            highPerformingKeywords = findHighPerformingKeywords(videos),
            optimalLength = calculateOptimalTitleLength(videos),
            emojiImpact = analyzeEmojiImpact(videos)
        )
    }

    private fun findHighPerformingKeywords(videos: List<Video>): List<String> {
        if (videos.isEmpty()) return emptyList()

        // Mapeia palavras-chave para seu desempenho médio
        val keywordPerformance = mutableMapOf<String, Double>()
        val keywordCount = mutableMapOf<String, Int>()

        videos.forEach { video ->
            // Extrair palavras do título
            val words = video.title.split(" ")
                .map { it.lowercase().trim() }
                .filter { it.length > 3 } // Ignora palavras muito curtas

            val viewsPerWord = video.viewCount.toDouble() / words.size

            words.forEach { word ->
                keywordPerformance[word] = keywordPerformance.getOrDefault(word, 0.0) + viewsPerWord
                keywordCount[word] = keywordCount.getOrDefault(word, 0) + 1
            }
        }

        // Calcula média de performance por palavra
        val averagePerformance = keywordPerformance.map { (word, totalViews) ->
            word to (totalViews / keywordCount[word]!!)
        }

        // Retorna as top 10 palavras com melhor performance
        return averagePerformance
            .sortedByDescending { it.second }
            .take(10)
            .map { it.first }
    }

    private fun calculateOptimalTitleLength(videos: List<Video>): Int {
        if (videos.isEmpty()) return 0

        // Agrupa vídeos por faixas de comprimento de título
        val performanceByLength = videos
            .groupBy { video ->
                video.title.length / 10 * 10 // Agrupa em faixas de 10 caracteres
            }
            .mapValues { (_, videos) ->
                videos.map { it.viewCount }.average()
            }

        // Encontra a faixa de comprimento com melhor performance
        return performanceByLength
            .maxByOrNull { it.value }
            ?.key ?: 50 // 50 é um valor padrão razoável se não houver dados
    }

    private fun analyzeEmojiImpact(videos: List<Video>): Map<String, Double> {
        if (videos.isEmpty()) return emptyMap()

        val emojiPattern = Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+")
        val emojiPerformance = mutableMapOf<String, MutableList<Double>>()

        videos.forEach { video ->
            val emojis = emojiPattern.findAll(video.title)
            emojis.forEach { matchResult ->
                val emoji = matchResult.value
                emojiPerformance
                    .getOrPut(emoji) { mutableListOf() }
                    .add(video.viewCount.toDouble())
            }
        }

        return emojiPerformance.mapValues { (_, viewCounts) ->
            viewCounts.average()
        }
    }

    private fun analyzeTopics(videos: List<Video>): TopicAnalysis {
        if (videos.isEmpty()) {
            return TopicAnalysis(
                bestPerformingTopics = emptyList(),
                trendingTopics = emptyList(),
                saturatedTopics = emptyList()
            )
        }

        return TopicAnalysis(
            bestPerformingTopics = findBestPerformingTopics(videos),
            trendingTopics = identifyTrendingTopics(videos),
            saturatedTopics = identifySaturatedTopics(videos)
        )
    }

    private fun findBestPerformingTopics(videos: List<Video>): List<String> {
        if (videos.isEmpty()) return emptyList()

        // Agrupa vídeos por categoria
        val performanceByCategory = videos
            .groupBy { it.categoryId }
            .mapValues { (_, categoryVideos) ->
                val avgViews = categoryVideos.map { it.viewCount }.average()
                val avgEngagement = categoryVideos.map {
                    (it.likeCount + it.commentCount).toDouble() / it.viewCount
                }.average()

                // Score composto de views e engajamento
                avgViews * avgEngagement
            }

        // Retorna as top 5 categorias
        return performanceByCategory
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }

    private fun identifyTrendingTopics(videos: List<Video>): List<String> {
        if (videos.isEmpty()) return emptyList()

        val now = LocalDateTime.now()
        val recentVideos = videos.filter {
            it.publishedAt.isAfter(now.minusMonths(1))
        }

        // Agrupa por categoria e calcula crescimento
        val topicGrowth = recentVideos
            .groupBy { it.categoryId }
            .mapValues { (_, categoryVideos) ->
                val chronologicalVideos = categoryVideos.sortedBy { it.publishedAt }
                if (chronologicalVideos.size < 2) return@mapValues 0.0

                val firstHalf = chronologicalVideos.take(chronologicalVideos.size / 2)
                val secondHalf = chronologicalVideos.drop(chronologicalVideos.size / 2)

                val firstHalfAvgViews = firstHalf.map { it.viewCount }.average()
                val secondHalfAvgViews = secondHalf.map { it.viewCount }.average()

                if (firstHalfAvgViews > 0) {
                    ((secondHalfAvgViews - firstHalfAvgViews) / firstHalfAvgViews) * 100
                } else 0.0
            }

        // Retorna tópicos com crescimento positivo
        return topicGrowth
            .filter { it.value > 20.0 } // 20% de crescimento como threshold
            .map { it.key }
    }

    private fun identifySaturatedTopics(videos: List<Video>): List<String> {
        if (videos.isEmpty()) return emptyList()

        val now = LocalDateTime.now()
        val recentVideos = videos.filter {
            it.publishedAt.isAfter(now.minusMonths(1))
        }

        // Análise similar ao trending, mas procurando por declínio
        val topicDecline = recentVideos
            .groupBy { it.categoryId }
            .mapValues { (_, categoryVideos) ->
                val chronologicalVideos = categoryVideos.sortedBy { it.publishedAt }
                if (chronologicalVideos.size < 2) return@mapValues 0.0

                val firstHalf = chronologicalVideos.take(chronologicalVideos.size / 2)
                val secondHalf = chronologicalVideos.drop(chronologicalVideos.size / 2)

                val firstHalfAvgViews = firstHalf.map { it.viewCount }.average()
                val secondHalfAvgViews = secondHalf.map { it.viewCount }.average()

                if (firstHalfAvgViews > 0) {
                    ((secondHalfAvgViews - firstHalfAvgViews) / firstHalfAvgViews) * 100
                } else 0.0
            }

        // Retorna tópicos com declínio significativo
        return topicDecline
            .filter { it.value < -20.0 } // -20% como threshold de declínio
            .map { it.key }
    }

    private fun analyzeContentTypes(videos: List<Video>): ContentTypeAnalysis {
        if (videos.isEmpty()) {
            return ContentTypeAnalysis(
                formatPerformance = emptyMap(),
                seriesPerformance = emptyMap(),
                collaborationImpact = 0.0
            )
        }

        return ContentTypeAnalysis(
            formatPerformance = analyzeFormatPerformance(videos),
            seriesPerformance = analyzeSeriesPerformance(videos),
            collaborationImpact = analyzeCollaborationImpact(videos)
        )
    }

    private fun analyzeFormatPerformance(videos: List<Video>): Map<String, Double> {
        if (videos.isEmpty()) return emptyMap()

        // Classificar vídeos por formato
        val videosByFormat = videos.groupBy { video ->
            when {
                video.duration.toSeconds() <= 60 -> "shorts"
                video.duration.toSeconds() <= 300 -> "curto" // até 5 minutos
                video.duration.toSeconds() <= 900 -> "medio" // até 15 minutos
                video.duration.toSeconds() <= 1800 -> "longo" // até 30 minutos
                else -> "extra_longo"
            }
        }

        // Calcular performance média para cada formato
        return videosByFormat.mapValues { (_, formatVideos) ->
            val avgViews = formatVideos.map { it.viewCount }.average()
            val avgEngagement = formatVideos.map { video ->
                (video.likeCount + video.commentCount).toDouble() / video.viewCount
            }.average()

            // Score composto (views * engagement rate)
            avgViews * avgEngagement
        }
    }

    private fun analyzeSeriesPerformance(videos: List<Video>): Map<String, Double> {
        if (videos.isEmpty()) return emptyMap()

        // Identificar séries por padrões no título
        val seriesPatterns = findSeriesPatterns(videos)
        val seriesPerformance = mutableMapOf<String, Double>()

        seriesPatterns.forEach { (seriesName, seriesVideos) ->
            // Calculando métricas para a série
            val avgViews = seriesVideos.map { it.viewCount }.average()
            val retention = seriesVideos.map { video ->
                calculateRetentionRate(video)
            }.average()
            val viewTrend = calculateViewTrend(seriesVideos)

            // Score composto para a série
            seriesPerformance[seriesName] = avgViews * retention * viewTrend
        }

        return seriesPerformance
    }

    private fun findSeriesPatterns(videos: List<Video>): Map<String, List<Video>> {
        // Padrões comuns de séries em títulos
        val patterns = listOf(
            Regex("(.*?)\\s*[#|Ep|Episode|Parte|Part]\\s*\\d+", RegexOption.IGNORE_CASE),
            Regex("(.*?)\\s*-\\s*S\\d+E\\d+", RegexOption.IGNORE_CASE),
            Regex("(.*?)\\s*\\d+\\s*[-|:]", RegexOption.IGNORE_CASE)
        )

        val seriesVideos = mutableMapOf<String, MutableList<Video>>()

        videos.forEach { video ->
            var matched = false
            for (pattern in patterns) {
                pattern.find(video.title)?.let { matchResult ->
                    val seriesName = matchResult.groupValues[1].trim()
                    if (seriesName.length >= 3) { // Evita matches muito curtos
                        seriesVideos.getOrPut(seriesName) { mutableListOf() }.add(video)
                        matched = true
                    }
                }
                if (matched) break
            }
        }

        // Filtra séries com pelo menos 2 vídeos
        return seriesVideos.filter { it.value.size >= 2 }
    }

    private fun calculateRetentionRate(video: Video): Double {
        // Implementação simplificada da taxa de retenção
        // Na prática, você precisaria de dados mais granulares da API do YouTube
        val avgViewDuration = video.duration.seconds * 0.6 // Assumindo 60% de retenção média
        return avgViewDuration.toDouble() / video.duration.seconds
    }

    private fun calculateViewTrend(videos: List<Video>): Double {
        if (videos.size < 2) return 1.0

        val sortedVideos = videos.sortedBy { it.publishedAt }
        val firstHalf = sortedVideos.take(sortedVideos.size / 2)
        val secondHalf = sortedVideos.drop(sortedVideos.size / 2)

        val firstHalfAvgViews = firstHalf.map { it.viewCount }.average()
        val secondHalfAvgViews = secondHalf.map { it.viewCount }.average()

        return if (firstHalfAvgViews > 0) {
            (secondHalfAvgViews / firstHalfAvgViews).coerceIn(0.5, 2.0)
        } else 1.0
    }

    private fun analyzeCollaborationImpact(videos: List<Video>): Double {
        if (videos.isEmpty()) return 0.0

        // Identificar vídeos de colaboração por padrões no título e descrição
        val collabVideos = videos.filter { video ->
            isCollaboration(video)
        }

        if (collabVideos.isEmpty()) return 0.0

        // Calcular performance média de vídeos normais
        val regularVideos = videos.filter { !isCollaboration(it) }
        val regularAvgViews = regularVideos.map { it.viewCount }.average()

        // Calcular performance média de colaborações
        val collabAvgViews = collabVideos.map { it.viewCount }.average()

        // Calcular o impacto percentual das colaborações
        return if (regularAvgViews > 0) {
            ((collabAvgViews - regularAvgViews) / regularAvgViews) * 100
        } else 0.0
    }

    private fun isCollaboration(video: Video): Boolean {
        val collabKeywords = listOf(
            "ft", "feat", "featuring", "com", "with", "collab", "colaboração",
            "participação", "part.", "part", "especial"
        )

        // Verificar no título
        val titleLower = video.title.lowercase()
        if (collabKeywords.any { keyword ->
                titleLower.contains(" $keyword ") ||
                        titleLower.contains("($keyword)") ||
                        titleLower.contains("[$keyword]")
            }) {
            return true
        }

        // Verificar na descrição
        val descriptionLower = video.description.lowercase()
        return collabKeywords.any { keyword ->
            descriptionLower.contains(" $keyword ") ||
                    descriptionLower.contains("($keyword)") ||
                    descriptionLower.contains("[$keyword]")
        }
    }

    private fun generateRecommendations(
        titlePatterns: TitlePatternAnalysis,
        topicAnalysis: TopicAnalysis,
        contentTypeAnalysis: ContentTypeAnalysis
    ): List<ContentRecommendation> {
        // Gerar recomendações baseadas nas análises
        return listOf(
            // Exemplo de recomendações
            ContentRecommendation(
                type = "title",
                recommendation = "Use keywords X, Y, Z in titles",
                confidence = 0.85
            ),
            ContentRecommendation(
                type = "topic",
                recommendation = "Focus on topic A",
                confidence = 0.92
            )
        )
    }
}

// Data classes para retorno das análises
data class RetentionMetrics(
    val channelId: String,
    val retentionByDuration: Map<String, VideoRetentionStats>,
    val retentionByHour: Map<Int, VideoRetentionStats>,
    val retentionByDayOfWeek: Map<DayOfWeek, VideoRetentionStats>,
    val recommendedDuration: String?,
    val recommendedHour: Int?,
    val recommendedDay: DayOfWeek?,
    val overallRetention: OverallRetentionStats
)

data class ContentPatternMetrics(
    val channelId: String,
    val titlePatterns: TitlePatternAnalysis,
    val topicAnalysis: TopicAnalysis,
    val contentTypeAnalysis: ContentTypeAnalysis,
    val recommendations: List<ContentRecommendation>
)

// Classes auxiliares
data class VideoRetentionStats(
    val averageRetentionRate: Double,
    val averageEngagementRate: Double,
    val commonDropoffPoints: List<Double>,
    val viewDuration: Double
)

data class OverallRetentionStats(
    val averageRetentionRate: Double,
    val trendOverTime: String,
    val retentionByVideoLength: Map<String, Double>,
    val topPerformingVideos: List<String>
)

data class TitlePatternAnalysis(
    val highPerformingKeywords: List<String>,
    val optimalLength: Int,
    val emojiImpact: Map<String, Double>
)

data class TopicAnalysis(
    val bestPerformingTopics: List<String>,
    val trendingTopics: List<String>,
    val saturatedTopics: List<String>
)

data class ContentTypeAnalysis(
    val formatPerformance: Map<String, Double>,
    val seriesPerformance: Map<String, Double>,
    val collaborationImpact: Double
)

data class ContentRecommendation(
    val type: String,
    val recommendation: String,
    val confidence: Double
)