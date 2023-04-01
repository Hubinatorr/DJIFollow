package com.riis.kotlin_simulatordemo

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import dji.common.flightcontroller.virtualstick.FlightControlData
import dji.sdk.flightcontroller.FlightController
import dji.thirdparty.org.java_websocket.client.WebSocketClient
import java.util.*
import kotlin.math.tanh

class DroneManager {
    var target: DroneData? = null
    var pidController = PID()

    lateinit var controller: FlightController
    lateinit var webSocketClient: WebSocketClient

    var tests = TestPipeline()
    var controls = Controls(0, 0, 0, 0)

    var testName = ""
    var j = 0
    var i = 0
    var k = 0

    var record = false;

    var mRoll = 0f
    var mPitch = 0f
    var mYaw = 0f
    var mThrottle = 0f

    var x0 = 0.0
    var y0 = 0.0
    var t0 = 0L
    fun onStateChange() {
        if (record) {
            webSocketClient.send(Json.encodeToString(getDroneState("target")))
        }
    }

    fun calculateFollowData(target: DroneData) {
        val drone = getDroneState("follower")
        pidController.compute(drone, target)
        mRoll = pidController.Vx.coerceIn(-5.0, 5.0).toFloat()
        mPitch = pidController.Vy.coerceIn(-5.0, 5.0).toFloat()

        controller.sendVirtualStickFlightControlData(
            FlightControlData(mPitch, mRoll, mYaw, mThrottle)
        ) { djiError ->
            if (djiError != null) {
                Log.i(MainActivity.DEBUG, djiError.description)
            }
        }

    }

    fun getDroneState(id: String): DroneData {
        return DroneData(
            System.currentTimeMillis(),
            id,
            Deg2UTM(
                controller.state.aircraftLocation.latitude,
                controller.state.aircraftLocation.longitude
            ).Northing,
            Deg2UTM(
                controller.state.aircraftLocation.latitude,
                controller.state.aircraftLocation.longitude
            ).Easting,
            controller.state.aircraftLocation.altitude.toDouble(),
            controller.state.velocityX.toDouble(),
            controller.state.velocityY.toDouble(),
            controller.state.velocityZ.toDouble(),
            controller.state.attitude.roll,
            controller.state.attitude.pitch,
            controller.state.attitude.yaw,
            controls.lh,
            controls.lv,
            controls.rh,
            controls.rv
        )
    }
}

