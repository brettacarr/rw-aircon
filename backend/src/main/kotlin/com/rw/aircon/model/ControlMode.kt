package com.rw.aircon.model

/**
 * Control mode determines how the HVAC system temperature is managed.
 * Only one mode can be active at a time.
 */
enum class ControlMode {
    /**
     * Manual mode: User directly controls all settings.
     * No automatic adjustments are made.
     */
    MANUAL,

    /**
     * Auto mode: System automatically adjusts temperature based on min/max ranges.
     * AutoModeExecutionService makes heating/cooling decisions every minute.
     */
    AUTO,

    /**
     * Schedule mode: Settings follow the configured season/schedule.
     * ScheduleExecutionService applies settings based on time of day.
     */
    SCHEDULE;

    companion object {
        /**
         * Parse a control mode from string, defaulting to MANUAL if invalid.
         */
        fun fromString(value: String?): ControlMode {
            return when (value?.uppercase()) {
                "AUTO" -> AUTO
                "SCHEDULE" -> SCHEDULE
                else -> MANUAL
            }
        }
    }
}
