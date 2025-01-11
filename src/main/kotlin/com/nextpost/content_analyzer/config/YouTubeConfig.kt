package com.nextpost.content_analyzer.config

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class YouTubeConfig {
    private val logger = LoggerFactory.getLogger(YouTubeConfig::class.java)

    @Value("\${youtube.application-name}")
    private lateinit var applicationName: String

    @Value("\${youtube.api-key}")
    private lateinit var apiKey: String

    @Bean
    fun youTube(): YouTube {
        logger.debug("Initializing YouTube service with application name: {}", applicationName)
        try {
            return YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                null
            )
                .setApplicationName(applicationName)
                .build()
                .also { logger.debug("YouTube service successfully initialized") }
        } catch (e: Exception) {
            logger.error("Error initializing YouTube service: ", e)
            throw e
        }
    }

    @Bean
    fun apiKey() = apiKey
}
