package com.rw.aircon

import com.rw.aircon.config.MyAirProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(MyAirProperties::class)
class RwAirconApplication

fun main(args: Array<String>) {
    runApplication<RwAirconApplication>(*args)
}
