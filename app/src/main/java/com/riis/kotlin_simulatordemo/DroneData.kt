package com.riis.kotlin_simulatordemo

class DroneData(
    val DroneId: String,
    val Altitude: Double,
    val Latitude: Double,
    val Longitude: Double,
    val Pitch: Double,
    val Roll: Double,
    val Yaw: Double,
    val velocityX: Double,
    val velocityY: Double,
    val velocityZ: Double,
    var Timestamp: Long,
    var LeftH: Int,
    var LeftV: Int,
    var RightH: Int,
    var RightV: Int
) {
    var x = Deg2UTM(Latitude, Longitude).Northing
    var y = Deg2UTM(Latitude, Longitude).Easting
    var z = Altitude
}