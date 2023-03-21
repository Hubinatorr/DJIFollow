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
    private var pidController = PID()

    lateinit var controller: FlightController
    lateinit var webSocketClient: WebSocketClient

    var tests = TestPipeline()
    var controls = Controls(0,0,0,0)

    var testName = ""
    var j = 0
    var i = 0
    var k = 0

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
            webSocketClient.send(Json.encodeToString(getDroneState()))
        }
    }

    fun calculateFollowData() {
        if (k == tests.gains.size) {
            record = false
            return
        }

        if (j == tests.all.size) {
            i = 0
            j = 0
            k++
            return
        }

        val drone = getDroneState()

        if(j < tests.all.size && i < tests.all[j].size && tests.all[j][i].t < (System.currentTimeMillis() - t0)) {
            pidController.compute(drone, tests.all[j][i])
//            mRoll = pidController.Vx.coerceIn(-15.0, 15.0).toFloat()
//            mPitch = pidController.Vy.coerceIn(-15.0, 15.0).toFloat()
            mRoll = (15 * tanh(pidController.Vx / 8)).toFloat()
            mPitch = (15 * tanh(pidController.Vy/ 8)).toFloat()

            i++
        }

        if (i == tests.all[j].size) {
            record = false
            mRoll = 0f
            mPitch = 0f

            if ((drone.vX + drone.vY) < 0.01) {
                j++
                i = 0
            }
        }

        controller.sendVirtualStickFlightControlData(
            FlightControlData(mPitch, mRoll, mYaw, mThrottle)
        ) { djiError ->
            if (djiError != null) {
                Log.i(MainActivity.DEBUG, djiError.description)
            }
        }

    }

    private fun getDroneState() : DroneData
    {
        if (i == 0) {
            t0 = 0
            x0 = 0.0
            y0 = 0.0
            pidController.Kp = tests.gains[k][0]
            pidController.Kd = tests.gains[k][1]
            pidController.Ki = tests.gains[k][2]
        }

        val drone = DroneData(
            System.currentTimeMillis() - t0,
            testName,
            Deg2UTM(controller.state.aircraftLocation.latitude, controller.state.aircraftLocation.longitude).Northing - x0,
            Deg2UTM(controller.state.aircraftLocation.latitude, controller.state.aircraftLocation.longitude).Easting - y0,
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

        if (i == 0) {
            x0 = drone.x
            y0 = drone.y
            t0 = System.currentTimeMillis()
            testName = tests.all[j][i].id + '_' + pidController.Kp + '_' + pidController.Kd + '_' + pidController.Ki

            drone.x = 0.0
            drone.y = 0.0
            drone.t = 0
            drone.id = testName

            webSocketClient.send(Json.encodeToString(drone))
            record = true
        }

        return drone
    }
}

