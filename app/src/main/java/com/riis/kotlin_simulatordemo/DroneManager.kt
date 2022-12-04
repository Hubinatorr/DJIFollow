package com.riis.kotlin_simulatordemo

import android.location.Location
import android.util.Log
import android.widget.TextView
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

    fun calculateFollowData(controller: FlightController, webSocketClient: WebSocketClient ) {
        val target = targets.peek() ?: return

        val location = controller.state.aircraftLocation
        val compass = controller.compass.heading
        val droneLocation = location(location.latitude, location.longitude, location.altitude.toDouble())

        mPitch = 0f
        mRoll = 0f

        when (followStage) {
            FollowStage.GOTO -> {
                goToLocation(droneLocation, compass)
            }
            FollowStage.ON -> {
                try { webSocketClient.send("T," + location.latitude + "," + location.longitude + ',' + System.currentTimeMillis()) }
                catch (e: Exception) { Log.i(MainActivity.UI, e.toString()) }

                mThrottle = target.Altitude.toFloat()

                mSpeed = sqrt(target.velocityX.pow(2) + target.velocityY.pow(2))

                mPitch = 0f
                mRoll = 0f
                mYaw = compass

                if (mSpeed > 0.0) {
                    var followTargetBearing: Double
                    if (target.velocityY == 0.0) {
                        followTargetBearing = if (target.velocityX > 0) 0.0 else 180.0
                    } else if (target.velocityX == 0.0) {
                        followTargetBearing = if (target.velocityY > 0) 90.0 else -90.0
                    } else {
                        followTargetBearing =
                            Math.toDegrees(Math.atan(target.velocityY / target.velocityX))
                        if (target.velocityX < 0) {
                            followTargetBearing += if (target.velocityY < 0) {
                                -180
                            } else {
                                180
                            }
                        }
                    }

                    val headingAngle =
                        Angle(followTargetBearing).value - Angle(compass.toDouble()).value

                    mPitch = (mSpeed * sin(Math.toRadians(headingAngle))).toFloat()
                    mRoll = (mSpeed * cos(Math.toRadians(headingAngle))).toFloat()
                    mYaw = compass

                }

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

    private fun goToLocation(droneLocation: Location, compass: Float) {
        val target = targets.peek() ?: return

        val lookLocation = location(target.Latitude, target.Longitude, target.Altitude)
        val followLocation = getOffsetLocation(target.Latitude, target.Longitude, target.Altitude)

        val followTargetBearing = droneLocation.bearingTo(followLocation)
        val followTargetDistance = droneLocation.distanceTo(followLocation)
        val followTargetAltitudeDifference = abs(round(droneLocation.altitude) - round(followLocation.altitude))
        val lookAtTargetBearing = droneLocation.bearingTo(lookLocation)

        mThrottle = followLocation.altitude.toFloat()
        if (followTargetDistance <= 0.3) {
            val diff: Double =
                Angle(lookAtTargetBearing.toDouble()).value - Angle(compass.toDouble()).value
            if (diff < 2) {
                followStage = FollowStage.ON
            }
            mYaw = lookAtTargetBearing
            mPitch = 0f
            mRoll = 0f
        } else {
            if (followTargetAltitudeDifference > 0.3 || (Angle(followTargetBearing.toDouble()).value - Angle(compass.toDouble()).value) > 5) {
                mYaw = followTargetBearing
            }

            if (followTargetDistance > 60) {
                mYaw = followTargetBearing
                mRoll = autoFLightSpeed
            }

            if (followTargetDistance > 0.3 && followTargetDistance <= 60) {
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
        d in -180.0..180.0 -> d
        d > 180.0 -> (d - 180.0) % 360.0 - 180.0
        else -> (d + 180.0) % 360.0 + 180.0
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
    val velocityZ: Double
)