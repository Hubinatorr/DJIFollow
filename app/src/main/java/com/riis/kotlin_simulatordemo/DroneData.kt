package com.riis.kotlin_simulatordemo

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
    val roll: Double,
    val pitch: Double,
    val yaw: Double,
    var lh: Int,
    var lv: Int,
    var rh: Int,
    var rv: Int
)


