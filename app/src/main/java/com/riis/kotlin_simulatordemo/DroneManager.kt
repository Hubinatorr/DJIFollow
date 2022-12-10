package com.riis.kotlin_simulatordemo

import android.location.Location
import android.util.Log
import android.widget.TextView
import com.beust.klaxon.Klaxon
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import dji.common.flightcontroller.LocationCoordinate3D
import dji.common.flightcontroller.virtualstick.FlightControlData
import dji.sdk.flightcontroller.FlightController
import dji.thirdparty.org.java_websocket.client.WebSocketClient
import java.util.*
import kotlin.math.*

class DroneManager {
    var targets: Queue<DroneData> = LinkedList<DroneData>()
    private val autoFLightSpeed = 5f

    private var mPitch = 0f
    private var mRoll = 0f
    private var mThrottle = 0f
    private var mYaw = 0f

    enum class FollowStage {
        GOTO, READY, ON
    }

    var followStage = FollowStage.GOTO

    private var mLatitudeOffset = 0.0
    private var mLongitudeOffset = 3.0
    private var mAltitudeOffset = 0.0
    private var mSpeed = 0.0

    private lateinit var target: DroneData
    private lateinit var prevLocation: Location
    private lateinit var prevTarget: DroneData

    private var beginPrimaryTimestamp = 0L
    private var beginSecondaryTimestamp = 0L
    private var first = false
    fun calculateFollowData(controller: FlightController, webSocketClient: WebSocketClient ) {
        val location = controller.state.aircraftLocation
        val compass = controller.state.attitude.yaw
        val droneLocation = location(location.latitude, location.longitude, location.altitude.toDouble())

        mPitch = 0f
        mRoll = 0f

        when (followStage) {
            FollowStage.GOTO -> {
                goToLocation(droneLocation, compass)
            }
            FollowStage.ON -> {
                target = targets.peek() ?: return

                mThrottle = target.Altitude.toFloat()

                var prevTargetLocation = getOffsetLocation(prevTarget.Latitude, prevTarget.Longitude, prevTarget.Altitude)
                var targetLocation = getOffsetLocation(target.Latitude, target.Longitude, target.Altitude)

                if (first) {
                    beginSecondaryTimestamp = System.currentTimeMillis()
                    beginPrimaryTimestamp = prevTarget.Timestamp
                    var s = targetLocation.distanceTo(prevTargetLocation)
                    var t = (target.Timestamp - prevTarget.Timestamp) / 1000.0
                    mSpeed = s/t
                    first = false

                } else {
                    var bearingTarget = prevTargetLocation.bearingTo(targetLocation)
                    var bearingDrone = droneLocation.bearingTo(targetLocation)
//                    var s = droneLocation.distanceTo(targetLocation)
//                    var t = ((target.Timestamp - beginPrimaryTimestamp) - (System.currentTimeMillis() - beginSecondaryTimestamp)) / 1000.0
//
//                    mSpeed = (s/t)
                    mSpeed = (sqrt(target.velocityX.pow(2) + target.velocityY.pow(2)) + sqrt(prevTarget.velocityX.pow(2) + prevTarget.velocityY.pow(2))) / 2

                    var bearingDiff = Angle(bearingDrone.toDouble()).value - Angle(bearingTarget.toDouble()).value
                    if (bearingDiff < 0) {
                        bearingDiff+=360
                    }

                    if (bearingDiff in 90.0..270.0 ) {
                        mSpeed = 0.0
                    }

                    Log.i(MainActivity.TAGDEGUG, "------------")
                    Log.i(MainActivity.TAGDEGUG, "tt ${target.Timestamp}")
                    Log.i(MainActivity.TAGDEGUG, "droneTT ${System.currentTimeMillis()}")
                    Log.i(MainActivity.TAGDEGUG, "bearingTarget $bearingTarget")
                    Log.i(MainActivity.TAGDEGUG, "bearingDrone $bearingDrone")
//                    Log.i(MainActivity.TAGDEGUG, "bearingDiff $bearingDiff")
//                    Log.i(MainActivity.TAGDEGUG, "s $s")
//                    Log.i(MainActivity.TAGDEGUG, "t $t")
                    Log.i(MainActivity.TAGDEGUG, "speed $mSpeed")
                }

                try { webSocketClient.send("T," + location.latitude + "," + location.longitude + "," + (beginPrimaryTimestamp + (System.currentTimeMillis() - beginSecondaryTimestamp))) }
                catch (e: Exception) { Log.i(MainActivity.UI, e.toString()) }

                mPitch = 0f
                mRoll = 0f
                mYaw = compass.toFloat()

                if (mSpeed > 0.1) {
                    var bearingAngle = droneLocation.bearingTo(targetLocation)

                    var headingAngle = Angle(bearingAngle.toDouble()).value - Angle(compass).value
                    if (headingAngle < 0.0) {
                        headingAngle += 360
                    }
                    mPitch = (mSpeed * sin(Math.toRadians(headingAngle))).toFloat()
                    mRoll = (mSpeed * cos(Math.toRadians(headingAngle))).toFloat()

                    Log.i(MainActivity.TAGDEGUG, "compass $compass")
                    Log.i(MainActivity.TAGDEGUG, "bearingAngle $bearingAngle")
                    Log.i(MainActivity.TAGDEGUG, "headingAngle $headingAngle")
                    Log.i(MainActivity.TAGDEGUG, "mPitch $mPitch")
                    Log.i(MainActivity.TAGDEGUG, "mRoll $mRoll")
                }
                prevLocation = droneLocation
                prevTarget = target
                targets.remove()
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
        if (followTargetDistance <= 0.1) {
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
}

class Angle(d: Double) {
    val value = when {
        d in 0.0..180.0 -> d
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
    val Timestamp: Long
)