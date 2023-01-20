package com.riis.kotlin_simulatordemo

class Angle(d: Double) {
    val value = when (d) {
        in 0.0..180.0 -> d
        else -> 360.0 + d
    }

    operator fun minus(other: Angle) = Angle(this.value - other.value)
}