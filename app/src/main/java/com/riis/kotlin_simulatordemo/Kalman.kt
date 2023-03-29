package com.riis.kotlin_simulatordemo

import android.util.Log
import org.jetbrains.kotlinx.multik.api.*
import org.jetbrains.kotlinx.multik.api.linalg.inv
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.minus
import org.jetbrains.kotlinx.multik.ndarray.operations.plus
import org.jetbrains.kotlinx.multik.ndarray.operations.times
import kotlin.math.pow

class Kalman {
    private val q_std = mk.ndarray(mk[0.05, 0.05, 0.05, 0.2, 0.2, 0.1, 0.2, 0.2, 0.1])
    private val est_std = mk.ndarray(mk[.1, .1, .3, .1, .1, .3, .1, .1, .3])
    private val gps_std = mk.ndarray(mk[1.0, 1.0, 300.0, .1, .1, .3, .1, .1, .3])

    var state =
        mk.ndarray(mk[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0])

    private lateinit var K: D2Array<Double>
    private lateinit var predictedState: D1Array<Double>
    private lateinit var predictedP: D2Array<Double>
    var P = mk.ndarray(
        mk[
                mk[est_std[0].pow(2), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
                mk[0.0, est_std[1].pow(2), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
                mk[0.0, 0.0, est_std[2].pow(2), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
                mk[0.0, 0.0, 0.0, est_std[3].pow(2), 0.0, 0.0, 0.0, 0.0, 0.0],
                mk[0.0, 0.0, 0.0, 0.0, est_std[4].pow(2), 0.0, 0.0, 0.0, 0.0],
                mk[0.0, 0.0, 0.0, 0.0, 0.0, est_std[5].pow(2), 0.0, 0.0, 0.0],
                mk[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, est_std[6].pow(2), 0.0, 0.0],
                mk[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, est_std[7].pow(2), 0.0],
                mk[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, est_std[8].pow(2)],
        ]
    )

    val R = mk.ndarray(
        mk[
                mk[gps_std[0].pow(2), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
                mk[0.0, gps_std[1].pow(2), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
                mk[0.0, 0.0, gps_std[2].pow(2), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
                mk[0.0, 0.0, 0.0, gps_std[3].pow(2), 0.0, 0.0, 0.0, 0.0, 0.0],
                mk[0.0, 0.0, 0.0, 0.0, gps_std[4].pow(2), 0.0, 0.0, 0.0, 0.0],
                mk[0.0, 0.0, 0.0, 0.0, 0.0, gps_std[5].pow(2), 0.0, 0.0, 0.0],
                mk[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, gps_std[6].pow(2), 0.0, 0.0],
                mk[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, gps_std[7].pow(2), 0.0],
                mk[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, gps_std[8].pow(2)],
        ]
    )

    val H = mk.identity<Double>(9)

    private fun getQ(dt: Double): D2Array<Double> {
        return mk.ndarray(
            mk[
                    mk[q_std[0].pow(2), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
                    mk[0.0, q_std[1].pow(2), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
                    mk[0.0, 0.0, q_std[2].pow(2), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
                    mk[0.0, 0.0, 0.0, q_std[3].pow(2), 0.0, 0.0, 0.0, 0.0, 0.0],
                    mk[0.0, 0.0, 0.0, 0.0, q_std[4].pow(2), 0.0, 0.0, 0.0, 0.0],
                    mk[0.0, 0.0, 0.0, 0.0, 0.0, q_std[5].pow(2), 0.0, 0.0, 0.0],
                    mk[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, q_std[6].pow(2), 0.0, 0.0],
                    mk[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, q_std[7].pow(2), 0.0],
                    mk[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, q_std[8].pow(2)],
            ]
        ) * dt
    }

    private fun getF(dt: Double): D2Array<Double> {
        return mk.ndarray(
            mk[
                    mk[1.0, 0.0, 0.0, dt, 0.0, 0.0, 0.5 * dt.pow(2), 0.0, 0.0],
                    mk[0.0, 1.0, 0.0, 0.0, dt, 0.0, 0.0, 0.5 * dt.pow(2), 0.0],
                    mk[0.0, 0.0, 1.0, 0.0, 0.0, dt, 0.0, 0.0, 0.5 * dt.pow(2)],
                    mk[0.0, 0.0, 0.0, 1.0, 0.0, 0.0, dt, 0.0, 0.0],
                    mk[0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, dt, 0.0],
                    mk[0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, dt],
                    mk[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0],
                    mk[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0],
                    mk[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0],
            ]
        )
    }

    private fun mulMV(m: D2Array<Double>, v: D1Array<Double>): D1Array<Double> {
        val res = mk.ndarray(mk[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0])

        for (i in m[0].indices) {
            for (j in m[0].indices) {
                res[i] += m[i, j] * v[j]
            }
        }
        return res
    }

    fun predict(dt: Double) {
        val F = getF(dt)
        val Q = getQ(dt)

        predictedState = mulMV(F, state)
        predictedP = F * P * F.transpose() + Q
    }

    fun update(dt: Double, data: DroneData) {
        val z = mk.ndarray(mk[
                    data.x, data.y, data.z,
                    data.vX, data.vX, data.vZ,
                    (data.vX - state[3]) / dt, (data.vY - state[4]) / dt, (data.vZ - state[5]) / dt]
            )

        val I = mk.identity<Double>(9)
        val inv = H * predictedP * H.transpose() + R
        val zDist = z - mulMV(H,predictedState)
        K = predictedP * H.transpose() * mk.linalg.inv(inv)

        state = predictedState + mulMV(K,zDist)
        P = (I - K * H) * predictedP * (I - K * H).transpose() + K * R * K.transpose()
    }
}