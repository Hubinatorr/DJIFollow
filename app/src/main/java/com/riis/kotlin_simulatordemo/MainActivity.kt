package com.riis.kotlin_simulatordemo

import android.Manifest
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.Person
import androidx.lifecycle.lifecycleScope
import com.beust.klaxon.Klaxon
import dji.common.flightcontroller.simulator.InitializationData
import dji.common.flightcontroller.virtualstick.*
import dji.common.model.LocationCoordinate2D
import dji.sdk.flightcontroller.FlightController
import dji.sdk.products.Aircraft
import dji.thirdparty.org.java_websocket.client.WebSocketClient
import dji.thirdparty.org.java_websocket.handshake.ServerHandshake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import kotlin.math.*

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var mConnectStatusTextView: TextView
    private lateinit var mBtnEnableVirtualStick: Button
    private lateinit var mBtnDisableVirtualStick: Button
    private lateinit var mBtnSimulator: ToggleButton
    private lateinit var mBtnTakeOff: Button
    private lateinit var mBtnLand: Button
    private lateinit var mTextView: TextView
    private lateinit var mScreenJoystickLeft: OnScreenJoystick
    private lateinit var mScreenJoystickRight: OnScreenJoystick
    private lateinit var mBtnWaypoint: Button
    private lateinit var mBtnFollow: Button
    private lateinit var mBtnHotpoint: Button
    private var followTargetLocation: DroneData? = null
    private var lookAtLocation = Location("")
    private lateinit var mBtnStartMission: Button
    private lateinit var webSocketClient: WebSocketClient
    private var mSendVirtualStickDataTimer: Timer? = null
    private var mSendVirtualStickDataTask: SendVirtualStickDataTask? = null
    private var targets: Queue<DroneData> = LinkedList<DroneData>()

    private val rEarth =6378
    private val followRadius = 2f
    private val HPRadius: Float = 5f
    private val AutoFLightSpeed = 5f
    private var mPitch: Float = 0f
    private var mRoll: Float = 0f
    private var mYaw: Float = 0f
    private var mThrottle: Float = 0f
    private var mission: Int = 0
    private var isClose = false
    private val viewModel by viewModels<MainViewModel>()

    companion object {
        const val TAG = "UserAppMainAct"
        const val TAGDEGUG = "Kokot"
    }

    private enum class MISSION (val type: Int){
        SIMPLE_FOLLOW(1),
        LOOK_AT_FOLLOW(2),
        HOTPOINT(3),
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.VIBRATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.SYSTEM_ALERT_WINDOW,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.RECORD_AUDIO
            ), 1
        )
        viewModel.startSdkRegistration(this)
        val flightData = resources.openRawResource(R.raw.data)
        val droneData = Klaxon().parseArray<DroneData>(flightData)
        if (droneData != null) {
            for (target in droneData) {
                targets.add(target)
            }
        }

        initObservers()
        initUi()
        createWebSocketClient()
    }

    class DroneData(
        val DroneId: String,
        val Altitude: Double,
        val Latitude: Double,
        val Longitude: Double,
        val Pitch : Double,
        val Roll: Double,
        val Yaw: Double,
        val Compass: Double,
        val velocityX: Double,
        val velocityY: Double,
        val velocityZ: Double
        )

    private fun createWebSocketClient() {
        val uri: URI = try {
            URI("ws://147.229.193.119:8000")
            //uri = new URI("ws://10.42.0.1:5555");
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            return
        }
        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(serverHandshake: ServerHandshake) {
                Log.i(TAG, "Connected to the DroCo server.")
            }

            override fun onMessage(s: String) {
                val droneData = Klaxon().parse<DroneData>(s)
                if (droneData != null) {
                    Log.i(TAG, droneData.DroneId)
                    if (droneData.DroneId == "FollowDrone") {
                        followTargetLocation = droneData
                        targets.add(droneData)
                    }
                } else {
                    Log.i(TAG, "Parse incorrect")
                }
            }
            override fun onClose(i: Int, s: String, b: Boolean) {
                Log.i(TAG, "Connection to the DroCo server closed.")
            }

            override fun onError(e: Exception) {
                Log.i(TAG, "Error connection")
            }
        }

        webSocketClient.connect()
    }

    private fun initObservers() {
        viewModel.connectionStatus.observe(this, androidx.lifecycle.Observer<Boolean> {
            initFlightController()
            var ret = false
            viewModel.product?.let {
                if (it.isConnected) {
                    mConnectStatusTextView.text = it.model.toString() + " Connected"
                    ret = true
                } else {
                    if ((it as Aircraft?)?.remoteController != null && it.remoteController.isConnected
                    ) {
                        mConnectStatusTextView.text = "only RC Connected"
                        ret = true
                    }

                }
            }
            if (!ret) {
                mConnectStatusTextView.text = "Disconnected"
            }
        })
    }

    private fun initUi() {
        mBtnEnableVirtualStick = findViewById(R.id.btn_enable_virtual_stick)
        mBtnDisableVirtualStick = findViewById(R.id.btn_disable_virtual_stick)
        mBtnTakeOff = findViewById(R.id.btn_take_off)
        mBtnLand = findViewById(R.id.btn_land)
        mBtnSimulator = findViewById(R.id.btn_start_simulator)
        mTextView = findViewById(R.id.textview_simulator)
        mConnectStatusTextView = findViewById(R.id.ConnectStatusTextView)
        mScreenJoystickRight = findViewById(R.id.directionJoystickRight)
        mScreenJoystickLeft = findViewById(R.id.directionJoystickLeft)

        mBtnStartMission = findViewById(R.id.btn_start_mission)
        mBtnStartMission.setOnClickListener(this)

        mBtnWaypoint = findViewById(R.id.btn_waypoint)
        mBtnWaypoint.setOnClickListener(this)

        mBtnFollow = findViewById(R.id.btn_follow)
        mBtnFollow.setOnClickListener(this)

        mBtnHotpoint = findViewById(R.id.btn_hotpoint)
        mBtnHotpoint.setOnClickListener(this)

        mBtnEnableVirtualStick.setOnClickListener(this)
        mBtnDisableVirtualStick.setOnClickListener(this)
        mBtnTakeOff.setOnClickListener(this)
        mBtnLand.setOnClickListener(this)

        mBtnSimulator.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                mTextView.visibility = View.VISIBLE
                viewModel.getFlightController()?.simulator?.start(
                    InitializationData.createInstance(
                        LocationCoordinate2D(23.0, 113.0), 10, 10
                    )
                ) { djiError ->
                    if (djiError != null) {
                        Log.i(TAG, djiError.description)
                        showToast("Simulator Error: ${djiError.description}")
                    } else {
                        Log.i(TAG,"Start Simulator Success")
                        showToast("Start Simulator Success")
                    }
                }
            } else {
                mTextView.visibility = View.INVISIBLE
                if(viewModel.getFlightController() == null) {
                    Log.i(TAG, "isFlightController Null ")
                }
                viewModel.getFlightController()?.simulator?.stop { djiError ->
                    if (djiError != null) {
                        Log.i(TAG, djiError.description)
                        showToast("Simulator Error: ${djiError.description}")
                    } else {
                        Log.i(TAG,"Stop Simulator Success")
                        showToast("Stop Simulator Success")
                    }
                }
            }
        }
    }

    private fun initFlightController() {
        viewModel.getFlightController()?.let {
            it.rollPitchControlMode = RollPitchControlMode.VELOCITY
            it.yawControlMode = YawControlMode.ANGLE
            it.verticalControlMode = VerticalControlMode.POSITION
            it.rollPitchCoordinateSystem = FlightCoordinateSystem.BODY
            it.simulator.setStateCallback { stateData ->
                val yaw = String.format("%.2f", stateData.yaw)
                val pitch = String.format("%.2f", stateData.pitch)
                val roll = String.format("%.2f", stateData.roll)
                val positionX = String.format("%.2f", stateData.positionX)
                val positionY = String.format("%.2f", stateData.positionY)
                val positionZ = String.format("%.2f", stateData.positionZ)

                lifecycleScope.launch(Dispatchers.Main) {
                    mTextView.text = "Yaw: $yaw, Pitch $pitch, Roll: $roll, \n, PosX: $positionX, PosY: $positionY, PosZ: $positionZ"
                }

            }
        }
    }
    private var debug = false
    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btn_enable_virtual_stick -> {
                viewModel.getFlightController()?.let { controller ->
                    controller.setVirtualStickModeEnabled(true) { djiError ->
                        if (djiError != null) {
                            Log.i(TAG, djiError.description)
                            showToast("Virtual Stick: Could not enable virtual stick")
                        } else {
                            Log.i(TAG,"Enable Virtual Stick Success")
                            showToast("Virtual Sticks Enabled")
                        }
                    }
                }

            }
            R.id.btn_disable_virtual_stick -> {
                viewModel.getFlightController()?.let { controller ->
                    controller.setVirtualStickModeEnabled(false) { djiError ->
                        if (djiError != null) {
                            Log.i(TAG, djiError.description)
                            showToast("Virtual Stick: Could not disable virtual stick")
                        } else {
                            Log.i(TAG,"Disable Virtual Stick Success")
                            showToast("Virtual Sticks Disabled")
                        }
                    }
                }
            }
            R.id.btn_take_off -> {
                viewModel.getFlightController()?.let { controller ->
                    controller.startTakeoff { djiError ->
                        if (djiError != null) {
                            Log.i(TAG, djiError.description)
                            showToast("Takeoff Error: ${djiError.description}")
                        } else {
                            Log.i(TAG,"Takeoff Success")
                            showToast("Takeoff Success")
                        }
                    }
                }
            }
            R.id.btn_land -> {
                viewModel.getFlightController()?.let { controller ->
                    controller.startLanding { djiError ->
                        if (djiError != null) {
                            Log.i(TAG, djiError.description)
                            showToast("Landing Error: ${djiError.description}")
                        } else {
                            Log.i(TAG,"Start Landing Success")
                            showToast("Start Landing Success")
                        }
                    }
                }
            }
            R.id.btn_waypoint -> {
                showToast("Simple Follow set")
                mission = MISSION.SIMPLE_FOLLOW.type
            }
            R.id.btn_follow -> {
                debug = true
                mission = MISSION.LOOK_AT_FOLLOW.type
            }
            R.id.btn_hotpoint -> {
                isClose = !isClose
            }
            R.id.btn_start_mission -> {
                if (mSendVirtualStickDataTimer == null) {
                    mSendVirtualStickDataTask = SendVirtualStickDataTask()
                    mSendVirtualStickDataTimer = Timer()
                    mSendVirtualStickDataTimer?.schedule(mSendVirtualStickDataTask, 0, 50)
                }
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun onReturn(view: View) {
        this.finish()
    }

    override fun onDestroy() {
        mSendVirtualStickDataTask?.let {
            it.cancel()
            mSendVirtualStickDataTask = null
        }

        mSendVirtualStickDataTimer?.let {
            it.cancel()
            it.purge()
            mSendVirtualStickDataTimer = null
        }
        super.onDestroy()
    }

    class Angle(d: Double) {
        val value = when {
            d in -180.0 .. 180.0 -> d
            d > 180.0            -> (d - 180.0) % 360.0 - 180.0
            else                 -> (d + 180.0) % 360.0 + 180.0
        }

        operator fun minus(other: Angle) = Angle(this.value - other.value)
    }

    private fun getLocation(latitude: Double, longitude: Double, altitude: Double): Location
    {
        val location = Location("")
        location.latitude = latitude
        location.longitude = longitude
        location.altitude = altitude

        return location
    }
    private var countIteration = 0
    private var speed = 0.0;
    private var start = false
    private lateinit var cureentTarget : DroneData
    inner class SendVirtualStickDataTask: TimerTask() {
        override fun run() {
            viewModel.getFlightController()?.let { controller ->
                val droneLocation = Location("")
                debug = true
                val state = controller.state;
                val location = state.aircraftLocation
                val attitude = state.attitude
                val compass = controller.compass.heading

                droneLocation.latitude = location.latitude
                droneLocation.longitude = location.longitude
                droneLocation.altitude = location.altitude.toDouble()

                mPitch = 0f
                mRoll = 0f
                countIteration ++
                if (isClose) {
                    try {
                        webSocketClient.send(
                            "S," + location.latitude + "," + location.longitude
                        )
                    } catch (e: Exception) {
                        Log.i(TAG, e.toString())
                    }

                    if (targets.count() > 0) {
                        var target = targets.peek()

                        mThrottle = target.Altitude.toFloat()

                        speed = sqrt(target.velocityX.pow(2) + target.velocityY.pow(2))

                        if (speed > 0.0) {
                            var followTargetBearing: Double
                            if (target.velocityY == 0.0) {
                                followTargetBearing = if (target.velocityX > 0) 0.0 else 180.0
                            } else if (target.velocityX == 0.0) {
                                followTargetBearing = if (target.velocityY > 0) 90.0 else -90.0
                            } else {
                                followTargetBearing = Math.toDegrees(Math.atan(target.velocityY / target.velocityX))
                                if (target.velocityX < 0) {
                                    if (target.velocityY < 0) {
                                        followTargetBearing += -180
                                    } else {
                                        followTargetBearing += 180
                                    }
                                }
                            }

                            val headingAngle = Angle(followTargetBearing).value - Angle(compass.toDouble()).value

                            mPitch = (speed * sin(Math.toRadians(headingAngle))).toFloat()
                            mRoll = (speed * cos(Math.toRadians(headingAngle))).toFloat()
                            mYaw = compass

                            if (countIteration == 10) {
                                Log.i(TAGDEGUG, "-----------------------------")
                                Log.i(TAGDEGUG, target.velocityX.toString())
                                Log.i(TAGDEGUG, target.velocityY.toString())
                                Log.i(TAGDEGUG, "followTargetBearing $followTargetBearing")
                                Log.i(TAGDEGUG, "compass $compass")
                                Log.i(TAGDEGUG, "headingAngle $headingAngle")
                                Log.i(TAGDEGUG, "")
                                Log.i(TAGDEGUG, "mPitch $mPitch")
                                Log.i(TAGDEGUG, "mRoll $mRoll")
                                Log.i(TAGDEGUG, "mYaw $mYaw")
                                Log.i(TAGDEGUG, "mThrottle $mThrottle")
                                Log.i(TAGDEGUG, "-----------------------------")
                                countIteration = 0
                            }
                        } else {
                            mPitch = 0f
                            mRoll = 0f
                            mYaw = compass
                        }
                        targets.remove()
                    }
                } else {
                    val goTo = targets.peek()
                    val lookLocation = Location("")
                    lookLocation.altitude = goTo.Altitude
                    lookLocation.latitude = goTo.Latitude
                    lookLocation.longitude = goTo.Longitude

                    val followLocation = Location("")
                    followLocation.altitude = goTo.Altitude
                    followLocation.latitude = goTo.Latitude
                    followLocation.longitude = goTo.Longitude + 0.00003

                    var followTargetBearing = droneLocation.bearingTo(followLocation)
                    var followTargetDistance = droneLocation.distanceTo(followLocation)
                    var followTargetAltitudeDifference = abs(round(droneLocation.altitude) - followLocation.altitude)
                    var lookAtTargetBearing = droneLocation.bearingTo(lookLocation)

                    mThrottle = followLocation.altitude.toFloat()
                    if (followTargetDistance <= 0.3) {
                        val diff : Double = Angle(lookAtTargetBearing.toDouble()).value - Angle(compass.toDouble()).value
                        if (diff < 2) {
                            isClose = true
                            Log.i(TAGDEGUG, "jeClose")
                        }
                        mYaw= lookAtTargetBearing
                        mPitch = 0f
                        mRoll = 0f
                    } else {
                        if (followTargetAltitudeDifference>0.3 || ( abs(controller.state.attitude.yaw-followTargetBearing) > 5) ) {
                            mYaw=followTargetBearing
                        }

                        if (followTargetDistance>60) {
                            mYaw=followTargetBearing
                            mRoll=AutoFLightSpeed
                        }

                        if (followTargetDistance>0.3 && followTargetDistance<=60) {
                            mYaw=followTargetBearing
                            mRoll= min(followTargetDistance/4, AutoFLightSpeed)
                        }
                    }
                }

                controller.sendVirtualStickFlightControlData(
                    FlightControlData(mPitch, mRoll, mYaw, mThrottle)
                ) { djiError ->
                    if (djiError != null) {
                        Log.i(TAG, djiError.description)
                        showToast("Error while sending data: ${djiError.description}")
                    }
                }
            }
        }
    }
}