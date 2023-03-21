package com.riis.kotlin_simulatordemo

import android.content.res.Resources
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.util.LinkedList
import java.util.Queue

class TestPipeline {
    lateinit var change_speed:  List<DroneData>
    lateinit var circles:       List<DroneData>
    lateinit var cirles_turn:   List<DroneData>
    lateinit var full:          List<DroneData>
    lateinit var full_turn:     List<DroneData>
    lateinit var full_zig_zag:  List<DroneData>
    lateinit var normal:        List<DroneData>
    lateinit var normal_turn:   List<DroneData>
    lateinit var normal_up:     List<DroneData>
    lateinit var outside:       List<DroneData>

    var all: MutableList<List<DroneData>> = LinkedList()

    var gains: MutableList<List<Double>> = LinkedList()

    @OptIn(ExperimentalSerializationApi::class)
    fun init(resources: Resources) {
        change_speed = Json.decodeFromStream(resources.openRawResource(R.raw.change_speed))
        circles = Json.decodeFromStream(resources.openRawResource(R.raw.circles))
        cirles_turn = Json.decodeFromStream(resources.openRawResource(R.raw.cirles_turn))
        full = Json.decodeFromStream(resources.openRawResource(R.raw.full))
        full_turn = Json.decodeFromStream(resources.openRawResource(R.raw.full_turn))
        full_zig_zag = Json.decodeFromStream(resources.openRawResource(R.raw.full_zig_zag))
        normal = Json.decodeFromStream(resources.openRawResource(R.raw.normal))
        normal_turn = Json.decodeFromStream(resources.openRawResource(R.raw.normal_turn))
        normal_up = Json.decodeFromStream(resources.openRawResource(R.raw.normal_up))
        outside = Json.decodeFromStream(resources.openRawResource(R.raw.outside))
//        all.add(change_speed)
//        all.add(cirles_turn)
//        all.add(full_turn)
//        all.add(full_zig_zag)
//        all.add(normal_turn)
//        all.add(normal_up)
//        all.add(outside)
        gains.add(listOf(2.0, 1.8, 0.0))

        all.add(normal)
//        all.add(full)
//        all.add(circles)

    }
}