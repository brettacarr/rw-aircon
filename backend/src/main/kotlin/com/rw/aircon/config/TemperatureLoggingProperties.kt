package com.rw.aircon.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "temperature-logging")
data class TemperatureLoggingProperties(
    val intervalMinutes: Int = 5,
    val retentionDays: Int = 90
)
