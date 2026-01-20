package com.rw.aircon.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rw.aircon.config.MyAirProperties
import com.rw.aircon.dto.MyAirResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@ExtendWith(MockitoExtension::class)
class MyAirClientTest {

    @Mock
    private lateinit var restTemplate: RestTemplate

    private lateinit var myAirProperties: MyAirProperties
    private lateinit var objectMapper: ObjectMapper
    private lateinit var myAirClient: MyAirClient

    @BeforeEach
    fun setUp() {
        myAirProperties = MyAirProperties(
            baseUrl = "http://test-host:2025",
            timeoutMs = 5000,
            retryDelayMs = 4000
        )
        objectMapper = jacksonObjectMapper()
        myAirClient = MyAirClient(restTemplate, myAirProperties, objectMapper)
    }

    @Test
    fun `getSystemData returns response when API call succeeds`() {
        // Given
        val mockResponse = createMockResponse()
        whenever(restTemplate.getForObject(
            eq("http://test-host:2025/getSystemData"),
            eq(MyAirResponse::class.java)
        )).thenReturn(mockResponse)

        // When
        val result = myAirClient.getSystemData()

        // Then
        assertNotNull(result)
        assertEquals("on", result!!.aircons?.ac1?.info?.state)
        assertEquals("cool", result.aircons?.ac1?.info?.mode)
        assertEquals(3, result.aircons?.ac1?.info?.myZone)
    }

    @Test
    fun `getSystemData returns null when RestClientException occurs`() {
        // Given
        whenever(restTemplate.getForObject(
            any<String>(),
            eq(MyAirResponse::class.java)
        )).thenThrow(RestClientException("Connection refused"))

        // When
        val result = myAirClient.getSystemData()

        // Then
        assertNull(result)
    }

    @Test
    fun `getSystemData returns null when unexpected exception occurs`() {
        // Given
        whenever(restTemplate.getForObject(
            any<String>(),
            eq(MyAirResponse::class.java)
        )).thenThrow(RuntimeException("Unexpected error"))

        // When
        val result = myAirClient.getSystemData()

        // Then
        assertNull(result)
    }

    @Test
    fun `setAircon returns true when command succeeds`() {
        // Given
        whenever(restTemplate.getForObject(
            argThat<String> { contains("/setAircon?json=") },
            eq(String::class.java)
        )).thenReturn("{}")

        // When
        val command = mapOf("ac1" to mapOf("info" to mapOf("state" to "on")))
        val result = myAirClient.setAircon(command)

        // Then
        assertTrue(result)
        verify(restTemplate).getForObject(
            argThat<String> { contains("setAircon") },
            eq(String::class.java)
        )
    }

    @Test
    fun `setAircon returns false when RestClientException occurs`() {
        // Given
        whenever(restTemplate.getForObject(
            any<String>(),
            eq(String::class.java)
        )).thenThrow(RestClientException("Connection refused"))

        // When
        val command = mapOf("ac1" to mapOf("info" to mapOf("state" to "on")))
        val result = myAirClient.setAircon(command)

        // Then
        assertFalse(result)
    }

    @Test
    fun `setSystemInfo sends correct command structure`() {
        // Given
        whenever(restTemplate.getForObject(
            any<String>(),
            eq(String::class.java)
        )).thenReturn("{}")

        // When
        val result = myAirClient.setSystemInfo(mapOf("state" to "on", "mode" to "cool"))

        // Then
        assertTrue(result)
        verify(restTemplate).getForObject(
            argThat<String> {
                contains("ac1") && contains("info") && contains("state") && contains("mode")
            },
            eq(String::class.java)
        )
    }

    @Test
    fun `setZone sends correct command structure`() {
        // Given
        whenever(restTemplate.getForObject(
            any<String>(),
            eq(String::class.java)
        )).thenReturn("{}")

        // When
        val result = myAirClient.setZone("z01", mapOf("setTemp" to 22))

        // Then
        assertTrue(result)
        verify(restTemplate).getForObject(
            argThat<String> {
                contains("ac1") && contains("zones") && contains("z01") && contains("setTemp")
            },
            eq(String::class.java)
        )
    }

    @Test
    fun `setAircon URL encodes the JSON command`() {
        // Given
        whenever(restTemplate.getForObject(
            any<String>(),
            eq(String::class.java)
        )).thenReturn("{}")

        // When
        myAirClient.setAircon(mapOf("ac1" to mapOf("info" to mapOf("state" to "on"))))

        // Then - Verify URL contains encoded JSON (no raw braces in query param)
        verify(restTemplate).getForObject(
            argThat<String> {
                contains("/setAircon?json=") && contains("%7B") // %7B is encoded {
            },
            eq(String::class.java)
        )
    }

    private fun createMockResponse(): MyAirResponse {
        val json = """
        {
            "aircons": {
                "ac1": {
                    "info": {
                        "state": "on",
                        "mode": "cool",
                        "fan": "autoAA",
                        "setTemp": 24.0,
                        "myZone": 3,
                        "noOfZones": 3,
                        "filterCleanStatus": 0,
                        "airconErrorCode": ""
                    },
                    "zones": {
                        "z01": {
                            "name": "Living",
                            "state": "close",
                            "setTemp": 19.0,
                            "measuredTemp": 25.0,
                            "type": 1,
                            "number": 1,
                            "rssi": 47,
                            "error": 0,
                            "value": 100
                        }
                    }
                }
            },
            "system": {
                "suburbTemp": 18.4,
                "isValidSuburbTemp": false
            }
        }
        """.trimIndent()
        return objectMapper.readValue(json, MyAirResponse::class.java)
    }
}
