package com.riis.kotlin_simulatordemo

import android.util.Log
import dji.common.flightcontroller.virtualstick.FlightControlData
import dji.sdk.flightcontroller.FlightController
import dji.thirdparty.org.java_websocket.client.WebSocketClient
import java.util.*

class DroneManager {
    lateinit var target: DroneData
    lateinit var drone: DroneData

    var targets: Queue<DroneData> = LinkedList()

    var followStage = FollowStage.GOTO

    var beginPrimaryTimestamp: Long = 0
    var beginSecondaryTimestamp: Long = 0

    private var pidController = PID()

    private var i = 1

    var mRoll = 0f
    var mPitch = 0f
    var mYaw = 0f
    var mThrottle =0f

    fun calculateFollowData(controller: FlightController, webSocketClient: WebSocketClient ) {
        val drone = getDroneDataFromController(controller, 0 , 0 , 0, 0 )

        if (followStage === FollowStage.READY) {
//            target = targets.peek() ?: return
//            targets.remove()

            try {
//                webSocketClient.send(Klaxon().toJsonString(drone))
                drone.Timestamp = beginPrimaryTimestamp + (System.currentTimeMillis() - beginSecondaryTimestamp)
                webSocketClient.send("T," + drone.Latitude + "," + drone.Longitude + ",")

            } catch (e: Exception) { Log.i(MainActivity.DEBUG, e.toString()) }
        }

        pidController.compute(drone, target)

        mRoll = pidController.Vx.coerceIn(-10.0, 10.0).toFloat()
        mPitch = pidController.Vy.coerceIn(-10.0, 10.0).toFloat()

//        if (followStage === FollowStage.GOTO) {
//            if (mPitch < 0.1 && mRoll < 0.1) {
//                followStage = FollowStage.READY
//                targets.remove()
//            }
//        }

        mThrottle = target.Altitude.toFloat()

        controller.sendVirtualStickFlightControlData(
            FlightControlData(mPitch, mRoll, mYaw, mThrottle)
        ) { djiError ->
            if (djiError != null) {
                Log.i(MainActivity.DEBUG, djiError.description)
            }
        }
        i++
    }

    fun getDroneDataFromController(controller: FlightController, LeftV: Int, LeftH: Int, RightV: Int, RightH: Int): DroneData {
        return DroneData(
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
            LeftH,
            LeftV,
            RightH,
            RightV
        );
    }

    private var prev = 0.0
    private var prevPrev = 0.0
    private var prevMax = 0.0
    private var prevMaxTimestamp = 0L
    private var prevTimestamp = 0L

    fun calculateZieglerNicholsAmplitude() {
        if (i % 5 == 0) {
            val diff = drone.x - target.x

            val t = System.currentTimeMillis()
            if (prev != 0.0 && prevPrev != 0.0) {
                if (diff < prev && prevPrev < prev) {
                    Log.i(MainActivity.DEBUG, "max: $prev diff: ${prev - prevMax} period:${prevTimestamp - prevMaxTimestamp} t:$t" )
                    prevMax = prev
                    prevMaxTimestamp = prevTimestamp
                }
            }

            prevTimestamp = t
            prevPrev = prev
            prev = diff
        }
    }
}

