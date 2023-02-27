package com.riis.kotlin_simulatordemo

import android.util.Log
import com.beust.klaxon.Klaxon
import dji.common.flightcontroller.virtualstick.FlightControlData
import dji.sdk.flightcontroller.FlightController
import dji.thirdparty.org.java_websocket.client.WebSocketClient
import java.util.*

class DroneManager {
    var targets: List<DroneData> = LinkedList()

    private var pidController = PID()

    private var i = 0

    var mRoll = 0f
    var mPitch = 0f
    var mYaw = 0f
    var mThrottle =0f

    fun calculateFollowData(controller: FlightController, webSocketClient: WebSocketClient, mainActivity: MainActivity) {
        val drone = getDroneDataFromController(controller, 0 , 0 , 0, 0 )
        webSocketClient.send(Klaxon().toJsonString(drone))
        if (i < targets.size) {
            pidController.compute(drone, targets[i])

            mRoll = pidController.Vx.coerceIn(-10.0, 10.0).toFloat()
            mPitch = pidController.Vy.coerceIn(-10.0, 10.0).toFloat()
            mThrottle = 0f
            i++
        }

        controller.sendVirtualStickFlightControlData(
            FlightControlData(mPitch, mRoll, mYaw, mThrottle)
        ) { djiError ->
            if (djiError != null) {
                Log.i(MainActivity.DEBUG, djiError.description)
            }
        }

    }

    private var oX = 0.0
    private var oY = 0.0
    private var oZ = 0.0
    private var oT = 0L
    fun getDroneDataFromController(controller: FlightController, LeftV: Int, LeftH: Int, RightV: Int, RightH: Int): DroneData {
        val timestamp = System.currentTimeMillis()
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
            timestamp,
            LeftH,
            LeftV,
            RightH,
            RightV
        )

        if (i == 0) {
            oX = drone.x
            oY = drone.y
            oZ = drone.z
            oT = timestamp
        }

        drone.x -= oX
        drone.y -= oY
        drone.z -= oZ + 1
        drone.Timestamp -= oT

        return drone
    }

//    private var prev = 0.0
//    private var prevPrev = 0.0
//    private var prevMax = 0.0
//    private var prevMaxTimestamp = 0L
//    private var prevTimestamp = 0L

//    fun calculateZieglerNicholsAmplitude() {
//        if (i % 5 == 0) {
//            val diff = drone.x - target.x
//
//            val t = System.currentTimeMillis()
//            if (prev != 0.0 && prevPrev != 0.0) {
//                if (diff < prev && prevPrev < prev) {
//                    Log.i(MainActivity.DEBUG, "max: $prev diff: ${prev - prevMax} period:${prevTimestamp - prevMaxTimestamp} t:$t" )
//                    prevMax = prev
//                    prevMaxTimestamp = prevTimestamp
//                }
//            }
//
//            prevTimestamp = t
//            prevPrev = prev
//            prev = diff
//        }
//    }
}

