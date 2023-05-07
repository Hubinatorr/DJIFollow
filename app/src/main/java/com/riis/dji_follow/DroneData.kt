package com.riis.dji_follow

import kotlinx.serialization.Serializable

@Serializable
data class DroneData(
    var t: Long,
    var id: String,
    var x: Double,
    var y: Double,
    var z: Double,
    var vX: Double,
    var vY: Double,
    var vZ: Double,
)

