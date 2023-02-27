package com.riis.kotlin_simulatordemo

data class Pose(
    val position : Position,
    val orientation : Orientation
)

data class Position(
    val x: Double,
    val y: Double,
    val z: Double
)

data class Orientation(
    val x: Double,
    val y: Double,
    val z: Double,
    val v: Double
)