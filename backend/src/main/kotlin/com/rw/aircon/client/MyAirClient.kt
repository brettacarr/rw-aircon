package com.rw.aircon.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.rw.aircon.config.MyAirProperties
import com.rw.aircon.dto.MyAirResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Client for communicating with the MyAir API.
 * Handles GET /getSystemData for reading state and GET /setAircon for commands.
 */
@Service
class MyAirClient(
    private val restTemplate: RestTemplate,
    private val myAirProperties: MyAirProperties,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(MyAirClient::class.java)

    /**
     * Fetches current system data from MyAir API.
     * @return MyAirResponse with all system and zone data, or null if unreachable
     */
    fun getSystemData(): MyAirResponse? {
        return try {
            val url = "${myAirProperties.baseUrl}/getSystemData"
            log.debug("Fetching system data from: {}", url)

            val response = restTemplate.getForObject(url, MyAirResponse::class.java)
            log.debug("Successfully fetched system data")
            response
        } catch (e: RestClientException) {
            log.error("Failed to fetch system data from MyAir API: {}", e.message)
            null
        } catch (e: Exception) {
            log.error("Unexpected error fetching system data: {}", e.message, e)
            null
        }
    }

    /**
     * Sends a command to the MyAir API.
     * Commands are sent as URL-encoded JSON via GET /setAircon?json={...}
     *
     * @param command Map representing the command structure
     * @return true if command was sent successfully (note: API returns {} until confirmed)
     */
    fun setAircon(command: Map<String, Any>): Boolean {
        return try {
            val jsonCommand = objectMapper.writeValueAsString(command)
            val encodedJson = URLEncoder.encode(jsonCommand, StandardCharsets.UTF_8)
            val url = "${myAirProperties.baseUrl}/setAircon?json=$encodedJson"

            log.debug("Sending command to MyAir: {}", jsonCommand)

            // Use URI object to prevent RestTemplate from double-encoding the URL
            val uri = URI.create(url)
            val response = restTemplate.getForObject(uri, String::class.java)
            log.debug("Command response: {}", response)

            true
        } catch (e: RestClientException) {
            log.error("Failed to send command to MyAir API: {}", e.message)
            false
        } catch (e: Exception) {
            log.error("Unexpected error sending command: {}", e.message, e)
            false
        }
    }

    /**
     * Convenience method to set system info properties.
     * @param properties Map of info properties to set (e.g., state, mode, fan, setTemp)
     */
    fun setSystemInfo(properties: Map<String, Any>): Boolean {
        val command = mapOf("ac1" to mapOf("info" to properties))
        return setAircon(command)
    }

    /**
     * Convenience method to set zone properties.
     * @param zoneId The zone ID (e.g., "z01")
     * @param properties Map of zone properties to set (e.g., state, setTemp)
     */
    fun setZone(zoneId: String, properties: Map<String, Any>): Boolean {
        val command = mapOf("ac1" to mapOf("zones" to mapOf(zoneId to properties)))
        return setAircon(command)
    }
}
