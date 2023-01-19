package com.riis.kotlin_simulatordemo

import android.location.Location
import android.util.Log
import com.beust.klaxon.Klaxon
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import dji.common.flightcontroller.virtualstick.FlightControlData
import dji.sdk.flightcontroller.FlightController
import dji.thirdparty.org.java_websocket.client.WebSocketClient
import java.util.*
import kotlin.math.*

class DroneManager {
    enum class FollowStage {
        GOTO, READY, ON
    }

    var targets: Queue<DroneData> = LinkedList<DroneData>()

    private val autoFLightSpeed = 5f

    private var mPitch = 0f
    private var mRoll = 0f
    private var mThrottle = 0f
    private var mYaw = 0f

    var followStage = FollowStage.GOTO

    private var mLatitudeOffset = 0.0
    private var mLongitudeOffset = 3.0
    private var mAltitudeOffset = 0.0

    lateinit var target: DroneData
    private lateinit var prevDrone: DroneData
    private lateinit var prevTarget: DroneData

    private var beginPrimaryTimestamp: Long = 0
    private var beginSecondaryTimestamp: Long = 0

    var first = true
    private var i = 0

    private var integralOffset = Vector(0.0, 0.0)
    private var prevError = Vector(0.0, 0.0)


    fun calculateFollowData(controller: FlightController, webSocketClient: WebSocketClient ) {
        mRoll = 0f
        mPitch = 0f
        mRoll = 0f
        mThrottle = 0f

        val state = controller.state
        val location = state.aircraftLocation
        val attitude = state.attitude

//        var prevTargetUMT = Deg2UTM(prevTarget.Latitude, prevTarget.Longitude)
//        var prevTargetPosVector = Vector(prevTargetUMT.Northing, prevTargetUMT.Easting)

        var targetLocation = getOffsetLocation(target.Latitude, target.Longitude, target.Altitude)
        var targetUMT = Deg2UTM(targetLocation.latitude, targetLocation.longitude)
        var targetPosVector = Vector(targetUMT.Northing, targetUMT.Easting)
        var targetSpeedVector = Vector(target.velocityX, target.velocityY)

        var droneUMT = Deg2UTM(location.latitude, location.longitude)
        var dronePosVector = Vector(droneUMT.Northing, droneUMT.Easting)
        var droneSpeedVector = Vector(state.velocityX.toDouble(), state.velocityY.toDouble())

        mThrottle = target.Altitude.toFloat()
//        mYaw = location(location.latitude, location.longitude, location.altitude.toDouble()).bearingTo(location(target.Latitude, target.Longitude, target.Altitude))


        var positionOffset = targetPosVector.subtract(dronePosVector).multiply(2.0)

        integralOffset = integralOffset.add(positionOffset.multiply(0.2)).multiply(0.001)

        var velocityOffset = targetSpeedVector.subtract(droneSpeedVector).multiply(1.5)

        var result = velocityOffset.add(positionOffset).add(integralOffset)

        mRoll = min(10.0,  result.getEntries()[0]).toFloat()
        mPitch = min(10.0, result.getEntries()[1]).toFloat()

        if (followStage === FollowStage.READY) {
            if (first) {
                beginSecondaryTimestamp = System.currentTimeMillis()
                beginPrimaryTimestamp = target.Timestamp
                first = false
            }
            try {
                webSocketClient.send("T," + location.latitude + "," + location.longitude + "," + (beginPrimaryTimestamp + (System.currentTimeMillis() - beginSecondaryTimestamp)))
            } catch (e: Exception) {
                Log.i(MainActivity.UI, e.toString())
            }
        }

        controller.sendVirtualStickFlightControlData(
            FlightControlData(mPitch, mRoll, mYaw, mThrottle)
        ) { djiError ->
            if (djiError != null) {
                Log.i(MainActivity.UI, djiError.description)
            }
        }

        i++
    }

    private fun location(latitude: Double, longitude: Double, altitude: Double): Location {
        val location = Location("")
        location.latitude = latitude
        location.longitude = longitude
        location.altitude = altitude

        return location
    }

    fun getOffsetLocation(latitude: Double, longitude: Double, altitude: Double): Location {
        val offsetLocation = SphericalUtil.computeOffset(
            SphericalUtil.computeOffset(LatLng(latitude, longitude), mLatitudeOffset, 0.0),
            mLongitudeOffset, 90.0
        )

        return location(
            offsetLocation.latitude,
            offsetLocation.longitude,
            altitude + mAltitudeOffset
        )
    }

    fun recordPath (
        controller: FlightController,
        webSocketClient: WebSocketClient,
        LeftV: Int,
        LeftH: Int,
        RightV: Int,
        RightH: Int
    ) {
        val state = controller.state;
        val location = state.aircraftLocation
        val attitude = state.attitude
        val compass = controller.compass.heading


        val droneData = RecordData(
            "DJI-Mavic",
            location.altitude.toDouble(),
            location.latitude,
            location.longitude,
            attitude.pitch,
            attitude.roll,
            attitude.yaw,
            state.velocityX.toDouble(),
            state.velocityY.toDouble(),
            state.velocityZ.toDouble(),
            System.currentTimeMillis(),
            LeftH,
            LeftV,
            RightH,
            RightV
        );

        Log.i(MainActivity.TAGDEGUG, Klaxon().toJsonString(droneData))
//        try {
//            webSocketClient.send(
//                Klaxon().toJsonString(droneData)
//            )
//        } catch (e: Exception) {
//            Log.i(MainActivity.TAGDEGUG, e.toString())
//        }
    }
}

class Angle(d: Double) {
    val value = when (d) {
        in 0.0..180.0 -> d
        else -> 360.0 + d
    }

    operator fun minus(other: Angle) = Angle(this.value - other.value)
}

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
)

class RecordData(
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
)