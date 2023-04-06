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
    lateinit var controller: FlightController
    lateinit var webSocketClient: WebSocketClient

    var controls = Controls(0, 0, 0, 0)
    var record = false;

    fun onStateChange() {
        if (record) {
            webSocketClient.send(Json.encodeToString(getDroneState("target")))
        }
    }
    private fun getDroneState(id: String): DroneData {
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

