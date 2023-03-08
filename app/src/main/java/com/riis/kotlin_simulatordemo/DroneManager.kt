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

    lateinit var controller: FlightController
    lateinit var webSocketClient: WebSocketClient
    var controls = Controls(0,0,0,0)

    private var i = 0

    var record = false;

    var mRoll = 0f
    var mPitch = 0f
    var mYaw = 0f
    var mThrottle =0f


    var x0 = 0.0
    var y0 = 0.0
    var t0 = 0L
    fun onStateChange() {
        if (record) {
            webSocketClient.send(Klaxon().toJsonString(getDroneState("t")))
        }
    }

    fun calculateFollowData() {
        val drone = getDroneState("f")
        if (i == 0) {
            x0 = drone.x
            y0 = drone.y
            t0 = System.currentTimeMillis()
            drone.x = 0.0
            drone.y = 0.0
            drone.t = 0
        }

        if (i < targets.size) {
            pidController.compute(drone, targets[i])

            webSocketClient.send(Klaxon().toJsonString(drone))
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
    companion object {
        const val DEBUG = "drone"
    }

    private fun getDroneState(name: String) : DroneData
    {
        return DroneData(
            System.currentTimeMillis() - t0,
            name,
            Deg2UTM(controller.state.aircraftLocation.latitude, controller.state.aircraftLocation.longitude).Northing - x0,
            Deg2UTM(controller.state.aircraftLocation.latitude, controller.state.aircraftLocation.longitude).Easting - y0,
            controller.state.aircraftLocation.altitude.toDouble(),
            controller.state.velocityX.toDouble(),
            controller.state.velocityY.toDouble(),
            controller.state.velocityZ.toDouble(),
            controller.state.attitude.roll,
            controller.state.attitude.pitch,
            controller.state.attitude.yaw,
            controls
        )
    }
}

