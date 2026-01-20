package com.rw.aircon.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Zone entity representing the mapping between our database IDs and MyAir zone IDs.
 * This allows us to abstract the MyAir zone naming (z01, z02, z03) from our API.
 */
@Entity
@Table(name = "zone")
data class Zone(
    @Id
    val id: Long = 0,

    @Column(nullable = false)
    val name: String = "",

    @Column(name = "my_air_zone_id", nullable = false, unique = true)
    val myAirZoneId: String = ""
)
