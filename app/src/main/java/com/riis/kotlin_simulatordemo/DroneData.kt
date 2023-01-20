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
    val Timestamp: Long,
    val LeftH: Int,
    val LeftV: Int,
    val RightH: Int,
    val RightV: Int
) {
    val utm = Deg2UTM(Latitude, Longitude)
    val x = utm.Northing
    val y = utm.Easting
    val z = Altitude
}