package com.rw.aircon.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
@EnableConfigurationProperties(MyAirProperties::class)
class RestTemplateConfig(
    private val myAirProperties: MyAirProperties
) {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder
            .setConnectTimeout(Duration.ofMillis(myAirProperties.timeoutMs))
            .setReadTimeout(Duration.ofMillis(myAirProperties.timeoutMs))
            .build()
    }
}
