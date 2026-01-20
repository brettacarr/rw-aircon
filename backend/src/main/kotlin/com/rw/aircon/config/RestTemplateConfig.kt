package com.rw.aircon.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
@EnableConfigurationProperties(MyAirProperties::class)
class RestTemplateConfig(
    private val myAirProperties: MyAirProperties,
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        val restTemplate = builder
            .setConnectTimeout(Duration.ofMillis(myAirProperties.timeoutMs))
            .setReadTimeout(Duration.ofMillis(myAirProperties.timeoutMs))
            .build()

        // MyAir API returns JSON with Content-Type: text/plain
        // Configure Jackson converter to accept text/plain as JSON
        val jacksonConverter = MappingJackson2HttpMessageConverter(objectMapper)
        jacksonConverter.supportedMediaTypes = listOf(
            MediaType.APPLICATION_JSON,
            MediaType.TEXT_PLAIN
        )

        // Replace the default Jackson converter with our custom one
        restTemplate.messageConverters.removeIf { it is MappingJackson2HttpMessageConverter }
        restTemplate.messageConverters.add(jacksonConverter)

        return restTemplate
    }
}
