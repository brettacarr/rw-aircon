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

    @Test
    fun `MyAirResponse can deserialize actual API response`() {
        // Given - Actual API response JSON from myapi-response.json
        val actualApiJson = """
        {"aircons":{"ac1":{"info":{"aaAutoFanModeEnabled":true,"activationCodeStatus":"noCode","airconErrorCode":"","cbFWRevMajor":8,"cbFWRevMinor":46,"cbType":1,"climateControlModeEnabled":true,"climateControlModeIsRunning":true,"constant1":0,"constant2":0,"constant3":0,"countDownToOff":0,"countDownToOn":0,"dbFWRevMajor":17,"dbFWRevMinor":22,"fan":"autoAA","filterCleanStatus":0,"freshAirStatus":"none","mode":"cool","myAutoModeCurrentSetMode":"cool","myAutoModeEnabled":false,"myAutoModeIsRunning":false,"myZone":3,"name":"AC","noOfConstants":0,"noOfZones":3,"quietNightModeEnabled":false,"quietNightModeIsRunning":false,"rfFWRevMajor":0,"rfSysID":16,"setTemp":24.0,"state":"on","uid":"12147","unitType":17},"zones":{"z01":{"error":0,"firebaseSensorUpdateStore":{"firebaseLastSentTemperatures":{}},"maxDamper":95,"measuredTemp":25.0,"minDamper":0,"motion":0,"motionConfig":1,"name":"Living","number":1,"rssi":47,"setTemp":19.0,"state":"close","tempSensorClash":false,"type":1,"value":100},"z02":{"error":0,"firebaseSensorUpdateStore":{"firebaseLastSentTemperatures":{}},"maxDamper":70,"measuredTemp":24.6,"minDamper":0,"motion":0,"motionConfig":1,"name":"Guest","number":2,"rssi":63,"setTemp":26.0,"state":"open","tempSensorClash":false,"type":1,"value":5},"z03":{"error":0,"firebaseSensorUpdateStore":{"firebaseLastSentTemperatures":{}},"maxDamper":100,"measuredTemp":21.9,"minDamper":0,"motion":0,"motionConfig":1,"name":"Upstairs","number":3,"rssi":56,"setTemp":22.0,"state":"open","tempSensorClash":false,"type":1,"value":100}}}},"myAddOns":{"hueBridges":{},"hueBridgesOrder":[]},"myGarageRFControllers":{"garageControllers":{},"garageControllersOrder":[]},"myLights":{"groups":{},"groupsOrder":[],"lights":{},"system":{"sunsetTime":"08:42 PM"}},"myMonitors":{"monitors":{},"monitorsOrder":[]},"myScenes":{"scenes":{},"scenesOrder":[]},"mySensors":{"sensors":{},"sensorsOrder":[]},"myThings":{"groups":{},"groupsOrder":[],"system":{},"things":{}},"myView":{"cameras":[],"locks":[]},"snapshots":{},"system":{"aaServiceRev":"14.116","allTspErrorCodes":{},"backupId":"343cfe49-1c93-4ebc-9086-1ecc0e4a2500","country":"Australia","dealerPhoneNumber":"0397382000","deletedDevices":{},"deviceIds":["Ldx1gBLEs8O8kcGQfzMzL0PiRr12"],"deviceIdsV2":{},"deviceNames":{},"deviceNotificationVersion":{},"drawLightsTab":false,"drawThingsTab":false,"garageDoorReminderWaitTime":2,"garageDoorSecurityPinEnabled":true,"hasAircons":true,"hasLights":false,"hasLocks":false,"hasSensors":false,"hasThings":false,"hasThingsBOG":false,"hasThingsLight":false,"isValidSuburbTemp":false,"latitude":-37.825821399999995,"lockDoorReminderWaitTime":2,"logoPIN":"4078","longitude":145.1541397,"membershipStatus":"NotAMember","mid":"12147","myAppRev":"15.1514","name":"MyPlace","needsUpdate":false,"noOfAircons":1,"noOfSnapshots":0,"postCode":"3130","remoteAccessPairingEnabled":true,"rid":"QDV3xs6L5kTvFPSDOz2I1tkszdP2","showMeasuredTemp":true,"splitTypeSystem":false,"suburbTemp":18.4,"sysType":"MyAir5","tspErrorCode":"noError","tspIp":"192.168.0.10","tspModel":"PIC8GS8","versions":{}}}
        """.trimIndent()

        // When
        val result = objectMapper.readValue(actualApiJson, MyAirResponse::class.java)

        // Then
        assertNotNull(result)
        assertNotNull(result.aircons)
        assertNotNull(result.aircons?.ac1)
        assertEquals("on", result.aircons?.ac1?.info?.state)
        assertEquals("cool", result.aircons?.ac1?.info?.mode)
        assertEquals("autoAA", result.aircons?.ac1?.info?.fan)
        assertEquals(24.0, result.aircons?.ac1?.info?.setTemp)
        assertEquals(3, result.aircons?.ac1?.info?.myZone)
        assertEquals(3, result.aircons?.ac1?.info?.noOfZones)

        // Verify zones
        val zones = result.aircons?.ac1?.zones
        assertNotNull(zones)
        assertEquals(3, zones?.size)

        val livingZone = zones?.get("z01")
        assertNotNull(livingZone)
        assertEquals("Living", livingZone?.name)
        assertEquals("close", livingZone?.state)
        assertEquals(25.0, livingZone?.measuredTemp)
        assertEquals(19.0, livingZone?.setTemp)

        // Verify system
        assertNotNull(result.system)
        assertEquals("MyPlace", result.system?.name)
        assertEquals("MyAir5", result.system?.sysType)
        assertEquals(18.4, result.system?.suburbTemp)
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
