package com.rw.aircon.repository

import com.rw.aircon.model.Season
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SeasonRepository : JpaRepository<Season, Long> {

    /**
     * Find all active seasons ordered by priority descending.
     * Higher priority seasons are returned first for overlap resolution.
     */
    fun findByActiveTrueOrderByPriorityDesc(): List<Season>

    /**
     * Find all seasons ordered by priority descending.
     * Used for administrative listing.
     */
    fun findAllByOrderByPriorityDesc(): List<Season>

    /**
     * Check if a season name already exists (excluding a specific ID for updates).
     */
    fun existsByNameAndIdNot(name: String, id: Long): Boolean

    /**
     * Check if a season name already exists.
     */
    fun existsByName(name: String): Boolean
}
