package com.rw.aircon.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "myair.api")
data class MyAirProperties(
    val baseUrl: String = "http://192.168.0.10:2025",
    val timeoutMs: Long = 5000,
    val retryDelayMs: Long = 4000
)
