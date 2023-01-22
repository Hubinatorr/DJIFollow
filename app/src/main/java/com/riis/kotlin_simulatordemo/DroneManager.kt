package com.riis.kotlin_simulatordemo

import android.util.Log
import com.beust.klaxon.Klaxon
import dji.common.flightcontroller.virtualstick.FlightControlData
import dji.sdk.flightcontroller.FlightController
import dji.sdksharedlib.nhf.gfd.fdd.Ma
import dji.thirdparty.org.java_websocket.client.WebSocketClient
import java.util.*

class DroneManager {
    lateinit var target: DroneData
    var targets: Queue<DroneData> = LinkedList()

    var followStage = FollowStage.GOTO

    private var beginPrimaryTimestamp: Long = 0
    private var beginSecondaryTimestamp: Long = 0

    private var pidController = PID()
    private var i = 0

    fun calculateFollowData(controller: FlightController, webSocketClient: WebSocketClient ) {
        var mRoll = 0f
        var mPitch = 0f
        val mYaw = 0f
        val mThrottle = target.Altitude.toFloat()

        val drone = getDroneDataFromController(controller, 0 , 0 , 0, 0 )

        if (followStage === FollowStage.READY) {
//            target = targets.peek() ?: return
//            targets.remove()

            if (i == 0) {
                beginSecondaryTimestamp = System.currentTimeMillis()
                beginPrimaryTimestamp = target.Timestamp
            }

            i++

            try {
                drone.Timestamp = (beginPrimaryTimestamp + (System.currentTimeMillis() - beginSecondaryTimestamp))
//                webSocketClient.send(Klaxon().toJsonString(drone))
                webSocketClient.send("T," + drone.Latitude + "," + drone.Longitude + "," + (beginPrimaryTimestamp + (System.currentTimeMillis() - beginSecondaryTimestamp)))

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

        controller.sendVirtualStickFlightControlData(
            FlightControlData(mPitch, mRoll, mYaw, mThrottle)
        ) { djiError ->
            if (djiError != null) {
                Log.i(MainActivity.DEBUG, djiError.description)
            }
        }
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
}

