package com.rw.aircon.service

import com.rw.aircon.client.MyAirClient
import com.rw.aircon.dto.MyAirResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

/**
 * Caches the last successful MyAir API response.
 * Returns cached data when the API is unreachable.
 */
@Service
class MyAirCacheService(
    private val myAirClient: MyAirClient
) {
    private val log = LoggerFactory.getLogger(MyAirCacheService::class.java)

    private val cachedResponse = AtomicReference<MyAirResponse?>(null)
    private val lastSuccessfulPoll = AtomicReference<Instant?>(null)

    /**
     * Gets system data, using cache if API is unreachable.
     * @return Pair of (response, isFromCache)
     */
    fun getSystemData(): Pair<MyAirResponse?, Boolean> {
        val freshData = myAirClient.getSystemData()

        return if (freshData != null) {
            cachedResponse.set(freshData)
            lastSuccessfulPoll.set(Instant.now())
            log.debug("Using fresh data from MyAir API")
            Pair(freshData, false)
        } else {
            val cached = cachedResponse.get()
            if (cached != null) {
                log.warn("MyAir API unreachable, returning cached data from {}", lastSuccessfulPoll.get())
            } else {
                log.error("MyAir API unreachable and no cached data available")
            }
            Pair(cached, true)
        }
    }

    /**
     * Returns the timestamp of the last successful poll.
     */
    fun getLastSuccessfulPoll(): Instant? = lastSuccessfulPoll.get()

    /**
     * Returns whether we have any cached data.
     */
    fun hasCachedData(): Boolean = cachedResponse.get() != null

    /**
     * Checks if the MyAir API is currently reachable.
     */
    fun isConnected(): Boolean {
        return myAirClient.getSystemData() != null
    }
}
