package com.nextpost.content_analyzer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ContentAnalyzerApplication

fun main(args: Array<String>) {
	runApplication<ContentAnalyzerApplication>(*args)
}
