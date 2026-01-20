package com.rw.aircon.repository

import com.rw.aircon.model.Zone
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ZoneRepository : JpaRepository<Zone, Long> {
    fun findByMyAirZoneId(myAirZoneId: String): Zone?
}
