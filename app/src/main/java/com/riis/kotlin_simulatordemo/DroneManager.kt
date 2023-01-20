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

    lateinit var target: DroneData
    private lateinit var prevDrone: DroneData
    private lateinit var prevTarget: DroneData

    private var beginPrimaryTimestamp: Long = 0
    private var beginSecondaryTimestamp: Long = 0

    private var first = true
    private var i = 0

    private var integralOffset = Vector(0.0, 0.0)
    private var prevError = Vector(0.0, 0.0)

    private var pid = PID()

    fun calculateFollowData(controller: FlightController, webSocketClient: WebSocketClient ) {
        mRoll = 0f
        mPitch = 0f
        mRoll = 0f
        mThrottle = target.Altitude.toFloat()
//        mYaw = location(location.latitude, location.longitude, location.altitude.toDouble()).bearingTo(location(target.Latitude, target.Longitude, target.Altitude))

        val drone = DroneData(
            "Follower",
            controller.state.aircraftLocation.altitude.toDouble(),
            controller.state.aircraftLocation.latitude,
            controller.state.aircraftLocation.longitude,
            controller.state.attitude.pitch,
            controller.state.attitude.roll,
            controller.state.attitude.yaw,
            controller.state.velocityX.toDouble(),
            controller.state.velocityY.toDouble(),
            controller.state.velocityZ.toDouble(),
            System.currentTimeMillis(),
            0,
            0,
            0,
            0
        );

        if (followStage === FollowStage.READY) {
            target = targets.peek() ?: return

            if (first) {
                beginSecondaryTimestamp = System.currentTimeMillis()
                beginPrimaryTimestamp = target.Timestamp
                first = false
            }

            try {
                drone.Timestamp = (beginPrimaryTimestamp + (System.currentTimeMillis() - beginSecondaryTimestamp))
                webSocketClient.send(Klaxon().toJsonString(drone))
            } catch (e: Exception) {
                Log.i(MainActivity.UI, e.toString())
            }

            targets.remove()
        }

        pid.compute(drone, target)

        mRoll = pid.Vx.coerceIn(-10.0, 10.0).toFloat()
        mPitch = pid.Vy.coerceIn(-10.0, 10.0).toFloat()

        if (followStage === FollowStage.GOTO) {
            if (mPitch < 0.1 && mRoll < 0.1) {
                followStage = FollowStage.READY
                targets.remove()
            }
        }


        controller.sendVirtualStickFlightControlData(
            FlightControlData(mPitch, mRoll, mYaw, mThrottle)
        ) { djiError ->
            if (djiError != null) {
                Log.i(MainActivity.UI, "$mPitch $mRoll $mYaw $mThrottle")
                Log.i(MainActivity.UI, djiError.description)
            }
        }

        i++
    }

    fun recordPath (
        controller: FlightController,
        webSocketClient: WebSocketClient,
        LeftV: Int,
        LeftH: Int,
        RightV: Int,
        RightH: Int
    ) {
        val droneData = DroneData(
            "DJI-Mavic",
            controller.state.aircraftLocation.altitude.toDouble(),
            controller.state.aircraftLocation.latitude,
            controller.state.aircraftLocation.longitude,
            controller.state.attitude.pitch,
            controller.state.attitude.roll,
            controller.state.attitude.yaw,
            controller.state.velocityX.toDouble(),
            controller.state.velocityY.toDouble(),
            controller.state.velocityZ.toDouble(),
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

