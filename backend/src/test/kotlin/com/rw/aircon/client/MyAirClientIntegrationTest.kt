package com.rw.aircon.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.rw.aircon.dto.MyAirResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
class MyAirClientIntegrationTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Autowired
    private lateinit var myAirClient: MyAirClient

    private lateinit var mockServer: MockRestServiceServer

    @BeforeEach
    fun setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate)
    }

    @Test
    fun `getSystemData can deserialize actual API response via RestTemplate`() {
        // Given - Actual API response JSON from myapi-response.json
        val actualApiJson = """
        {"aircons":{"ac1":{"info":{"aaAutoFanModeEnabled":true,"activationCodeStatus":"noCode","airconErrorCode":"","cbFWRevMajor":8,"cbFWRevMinor":46,"cbType":1,"climateControlModeEnabled":true,"climateControlModeIsRunning":true,"constant1":0,"constant2":0,"constant3":0,"countDownToOff":0,"countDownToOn":0,"dbFWRevMajor":17,"dbFWRevMinor":22,"fan":"autoAA","filterCleanStatus":0,"freshAirStatus":"none","mode":"cool","myAutoModeCurrentSetMode":"cool","myAutoModeEnabled":false,"myAutoModeIsRunning":false,"myZone":3,"name":"AC","noOfConstants":0,"noOfZones":3,"quietNightModeEnabled":false,"quietNightModeIsRunning":false,"rfFWRevMajor":0,"rfSysID":16,"setTemp":24.0,"state":"on","uid":"12147","unitType":17},"zones":{"z01":{"error":0,"firebaseSensorUpdateStore":{"firebaseLastSentTemperatures":{}},"maxDamper":95,"measuredTemp":25.0,"minDamper":0,"motion":0,"motionConfig":1,"name":"Living","number":1,"rssi":47,"setTemp":19.0,"state":"close","tempSensorClash":false,"type":1,"value":100},"z02":{"error":0,"firebaseSensorUpdateStore":{"firebaseLastSentTemperatures":{}},"maxDamper":70,"measuredTemp":24.6,"minDamper":0,"motion":0,"motionConfig":1,"name":"Guest","number":2,"rssi":63,"setTemp":26.0,"state":"open","tempSensorClash":false,"type":1,"value":5},"z03":{"error":0,"firebaseSensorUpdateStore":{"firebaseLastSentTemperatures":{}},"maxDamper":100,"measuredTemp":21.9,"minDamper":0,"motion":0,"motionConfig":1,"name":"Upstairs","number":3,"rssi":56,"setTemp":22.0,"state":"open","tempSensorClash":false,"type":1,"value":100}}}},"myAddOns":{"hueBridges":{},"hueBridgesOrder":[]},"myGarageRFControllers":{"garageControllers":{},"garageControllersOrder":[]},"myLights":{"groups":{},"groupsOrder":[],"lights":{},"system":{"sunsetTime":"08:42 PM"}},"myMonitors":{"monitors":{},"monitorsOrder":[]},"myScenes":{"scenes":{},"scenesOrder":[]},"mySensors":{"sensors":{},"sensorsOrder":[]},"myThings":{"groups":{},"groupsOrder":[],"system":{},"things":{}},"myView":{"cameras":[],"locks":[]},"snapshots":{},"system":{"aaServiceRev":"14.116","allTspErrorCodes":{},"backupId":"343cfe49-1c93-4ebc-9086-1ecc0e4a2500","country":"Australia","dealerPhoneNumber":"0397382000","deletedDevices":{},"deviceIds":["Ldx1gBLEs8O8kcGQfzMzL0PiRr12"],"deviceIdsV2":{},"deviceNames":{},"deviceNotificationVersion":{},"drawLightsTab":false,"drawThingsTab":false,"garageDoorReminderWaitTime":2,"garageDoorSecurityPinEnabled":true,"hasAircons":true,"hasLights":false,"hasLocks":false,"hasSensors":false,"hasThings":false,"hasThingsBOG":false,"hasThingsLight":false,"isValidSuburbTemp":false,"latitude":-37.825821399999995,"lockDoorReminderWaitTime":2,"logoPIN":"4078","longitude":145.1541397,"membershipStatus":"NotAMember","mid":"12147","myAppRev":"15.1514","name":"MyPlace","needsUpdate":false,"noOfAircons":1,"noOfSnapshots":0,"postCode":"3130","remoteAccessPairingEnabled":true,"rid":"QDV3xs6L5kTvFPSDOz2I1tkszdP2","showMeasuredTemp":true,"splitTypeSystem":false,"suburbTemp":18.4,"sysType":"MyAir5","tspErrorCode":"noError","tspIp":"192.168.0.10","tspModel":"PIC8GS8","versions":{}}}
        """.trimIndent()

        mockServer.expect(requestTo("http://192.168.0.10:2025/getSystemData"))
            .andRespond(withSuccess(actualApiJson, MediaType.APPLICATION_JSON))

        // When
        val result = myAirClient.getSystemData()

        // Then
        mockServer.verify()
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

    @Test
    fun `MyAirResponse can deserialize actual API response with Spring ObjectMapper`() {
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

    @Test
    fun `getSystemData can deserialize full API response from docs file via RestTemplate`() {
        // Given - Read actual full API response from docs/myapi-response.json
        val fullApiJson = File("../docs/myapi-response.json").readText()

        mockServer.expect(requestTo("http://192.168.0.10:2025/getSystemData"))
            .andRespond(withSuccess(fullApiJson, MediaType.APPLICATION_JSON))

        // When
        val result = myAirClient.getSystemData()

        // Then
        mockServer.verify()
        assertNotNull(result)
        assertNotNull(result.aircons)
        assertNotNull(result.aircons?.ac1)
        assertEquals("on", result.aircons?.ac1?.info?.state)
        assertEquals("cool", result.aircons?.ac1?.info?.mode)
        assertEquals(3, result.aircons?.ac1?.zones?.size)

        // Verify system
        assertNotNull(result.system)
        assertEquals("MyPlace", result.system?.name)
    }

    @Test
    fun `getSystemData can deserialize API response with text-plain content type`() {
        // Given - MyAir API returns JSON with Content-Type: text/plain
        val actualApiJson = """
        {"aircons":{"ac1":{"info":{"state":"on","mode":"cool","fan":"autoAA","setTemp":24.0,"myZone":3,"noOfZones":3},"zones":{"z01":{"name":"Living","state":"close","setTemp":19.0,"measuredTemp":25.0}}}},"system":{"name":"MyPlace","sysType":"MyAir5","suburbTemp":18.4}}
        """.trimIndent()

        mockServer.expect(requestTo("http://192.168.0.10:2025/getSystemData"))
            .andRespond(withSuccess(actualApiJson, MediaType.TEXT_PLAIN))

        // When
        val result = myAirClient.getSystemData()

        // Then
        mockServer.verify()
        assertNotNull(result)
        assertNotNull(result.aircons)
        assertNotNull(result.aircons?.ac1)
        assertEquals("on", result.aircons?.ac1?.info?.state)
        assertEquals("cool", result.aircons?.ac1?.info?.mode)
        assertEquals("MyPlace", result.system?.name)
    }
}
