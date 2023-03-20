package com.riis.kotlin_simulatordemo

import kotlinx.serialization.Serializable

data class Controls(
    var lh: Int,
    var lv: Int,
    var rh: Int,
    var rv: Int
)
@Serializable
data class DroneData(
    var t: Long,
    var id: String,
    var x: Double,
    var y: Double,
    var z: Double,
    val vX: Double,
    val vY: Double,
    val vZ: Double,
    val roll: Double,
    val pitch: Double,
    val yaw: Double,
    var lh: Int,
    var lv: Int,
    var rh: Int,
    var rv: Int
)


