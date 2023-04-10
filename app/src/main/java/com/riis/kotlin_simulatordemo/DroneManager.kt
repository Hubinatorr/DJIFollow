package com.riis.kotlin_simulatordemo

import android.util.Log
import dji.common.flightcontroller.virtualstick.FlightControlData
import dji.sdk.flightcontroller.FlightController

import kotlin.math.tanh

class DroneManager {
    var pidController = PID()
    lateinit var controller: FlightController

    var mRoll = 0f
    var mPitch = 0f
    var mYaw = 0f
    var mThrottle = 0f


    fun calculateFollowData(target: DroneData, drone: DroneData ) {
        pidController.compute(drone, target)
//        mRoll = (tanh(pidController.Vx)).toFloat()
//        mPitch = (tanh(pidController.Vy)).toFloat()
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
}

