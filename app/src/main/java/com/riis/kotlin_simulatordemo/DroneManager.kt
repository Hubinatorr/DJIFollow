package com.riis.kotlin_simulatordemo

import android.location.Location
import android.util.Log
import com.beust.klaxon.Klaxon
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import dji.common.flightcontroller.virtualstick.FlightControlData
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem
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
    private var mSpeed = 0.0

    private lateinit var target: DroneData
    private lateinit var prevDrone: DroneData
    private lateinit var prevTarget: DroneData

    private var beginPrimaryTimestamp = 0L
    private var beginSecondaryTimestamp = 0L
    private var first = false
    private var iterationCounter = 0


    fun calculateFollowData(controller: FlightController, webSocketClient: WebSocketClient ) {
        val location = controller.state.aircraftLocation
        val compass = controller.state.attitude.yaw
        val droneLocation = location(location.latitude, location.longitude, location.altitude.toDouble())
        val state = controller.state;
        val attitude = state.attitude
        mPitch = 0f
        mRoll = 0f

        when (followStage) {
            FollowStage.GOTO -> {
                goToLocation(droneLocation, compass)
            }
            FollowStage.ON -> {
                controller.rollPitchCoordinateSystem = FlightCoordinateSystem.GROUND
                target = targets.peek() ?: return

                var targetLocation = getOffsetLocation(target.Latitude, target.Longitude, target.Altitude)
                var targetBearing = droneLocation.bearingTo(targetLocation);

                var targetUMT = Deg2UTM(targetLocation.latitude, targetLocation.longitude)
                var droneUMT = Deg2UTM(droneLocation.latitude, droneLocation.longitude)
                var prevTargetUMT = Deg2UTM(prevTarget.Latitude, prevTarget.Longitude)
                var prevTargetVector = Vector(prevTargetUMT.Northing, prevTargetUMT.Easting)


                var dronePosVector = Vector(droneUMT.Northing, droneUMT.Easting)

                var errorVector = prevTargetVector.subtract(dronePosVector)


                var targetPosVector = Vector(targetUMT.Northing, targetUMT.Easting)
                var positionOffset = targetPosVector.subtract(dronePosVector)
                var targetSpeedVector = Vector(target.velocityX, target.velocityY)
                var droneSpeedVector = Vector(controller.state.velocityX.toDouble(), controller.state.velocityY.toDouble())

                var velocityOffset = targetSpeedVector.subtract(droneSpeedVector)

                var res = velocityOffset

                if (first) {
                    beginSecondaryTimestamp = System.currentTimeMillis()
                    beginPrimaryTimestamp = prevTarget.Timestamp
                    first = false
                } else {
                    var acc = (droneSpeedVector.subtract(Vector(prevDrone.Latitude, prevDrone.Longitude))).cross(Vector(0.5,0.5))
                    var accelerationT = (targetSpeedVector.subtract(Vector(prevTarget.velocityX, prevTarget.velocityY))).cross(Vector(0.5,0.5))
                    velocityOffset = velocityOffset.add(accelerationT.subtract(acc))
                }

                var result = velocityOffset


                mRoll = min(10.0,  result.getEntries()[0]).toFloat()
                mPitch = min(10.0, result.getEntries()[1]).toFloat()


                if (first) {
                    beginSecondaryTimestamp = System.currentTimeMillis()
                    beginPrimaryTimestamp = prevTarget.Timestamp
                    first = false

                }
                prevDrone = DroneData(
                    "DJI-Mavic",
                    location.altitude.toDouble(),
                    location.latitude,
                    location.longitude,
                    controller.state.attitude.pitch,
                    attitude.roll,
                    attitude.yaw,
                    compass.toDouble(),
                    state.velocityX.toDouble(),
                    state.velocityY.toDouble(),
                    state.velocityZ.toDouble(),
                    System.currentTimeMillis(),
                    0,
                    0,
                    0,
                    0
                );
                prevTarget = target
                targets.remove()
                iterationCounter++;
            } else -> {

            }
        }

        controller.sendVirtualStickFlightControlData(
            FlightControlData(mPitch, mRoll, mYaw, mThrottle)
        ) { djiError ->
            if (djiError != null) {
                Log.i(MainActivity.UI, djiError.description)
            }
        }
    }

    private fun goToLocation(droneLocation: Location, compass: Double) {
        val target = targets.peek()
            ?: return
        val lookLocation = location(target.Latitude, target.Longitude, target.Altitude)
        val followLocation = getOffsetLocation(target.Latitude, target.Longitude, target.Altitude)

        val followTargetBearing = droneLocation.bearingTo(followLocation)
        val followTargetDistance = droneLocation.distanceTo(followLocation)
        val followTargetAltitudeDifference = abs(round(droneLocation.altitude) - round(followLocation.altitude))
        val lookAtTargetBearing = droneLocation.bearingTo(lookLocation)

        mThrottle = followLocation.altitude.toFloat()
        if (followTargetDistance <= 0.2) {
            val diff: Double =
                abs(Angle(lookAtTargetBearing.toDouble()).value - Angle(compass).value)
            if (diff < 1) {
                followStage = FollowStage.ON
                prevTarget = target
                first = true
                targets.remove()
            }
            mYaw = lookAtTargetBearing
            mPitch = 0f
            mRoll = 0f
        } else {
            if (followTargetAltitudeDifference > 0.3 || abs(Angle(followTargetBearing.toDouble()).value - Angle(compass).value) > 5) {
                mYaw = followTargetBearing
            } else if (followTargetDistance > 60) {
                mYaw = followTargetBearing
                mRoll = autoFLightSpeed
            }else if (followTargetDistance > 0.2 && followTargetDistance <= 60) {
                mYaw = followTargetBearing
                mRoll = min(followTargetDistance / 4, autoFLightSpeed)
            }
        }
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
            compass.toDouble(),
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
    val Compass: Double,
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
    val Compass: Double,
    val velocityX: Double,
    val velocityY: Double,
    val velocityZ: Double,
    val Timestamp: Long,
    val LeftH: Int,
    val LeftV: Int,
    val RightH: Int,
    val RightV: Int
)