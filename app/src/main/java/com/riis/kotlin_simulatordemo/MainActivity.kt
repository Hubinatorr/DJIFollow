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
import androidx.lifecycle.lifecycleScope
import com.beust.klaxon.Klaxon
import dji.common.flightcontroller.simulator.InitializationData
import dji.common.flightcontroller.virtualstick.*
import dji.common.model.LocationCoordinate2D
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
    private var followDroneLocation = Location("")
    private var lookAtLocation = Location("")
    private lateinit var mBtnConnect: Button
    private lateinit var webSocketClient: WebSocketClient

    private var mSendVirtualStickDataTimer: Timer? = null
    private var mSendVirtualStickDataTask: SendVirtualStickDataTask? = null

    private val HPRadius: Float = 10f
    private val AutoFLightSpeed = 15f
    private var mPitch: Float = 0f
    private var mRoll: Float = 0f
    private var mYaw: Float = 0f
    private var mThrottle: Float = 0f
    private var mission: Int = 3

    private val viewModel by viewModels<MainViewModel>()

    companion object {
        const val TAG = "UserAppMainAct"
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
        initObservers()
        initUi()
    }

    class DroneData(
        val DroneId: String,
        val Altitude: Double,
        val Latitude: Double,
        val Longitude: Double,
        val Pitch : Double,
        val Roll: Double,
        val Yaw: Double,
        val Compass: Double
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
                        followDroneLocation.altitude = droneData.Altitude
                        followDroneLocation.latitude = droneData.Latitude
                        followDroneLocation.longitude = droneData.Longitude

                        lookAtLocation.latitude = droneData.Latitude
                        lookAtLocation.longitude = droneData.Longitude
                        lookAtLocation.altitude = droneData.Altitude
                        Log.i(TAG, "Updated follow drone location")
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

        mBtnConnect = findViewById(R.id.btn_connect)
        mBtnConnect.setOnClickListener(this)

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

//        mScreenJoystickRight.setJoystickListener(object : OnScreenJoystickListener {
//            override fun onTouch(joystick: OnScreenJoystick?, pXP: Float, pYP: Float) {
//                var pX = pXP
//                var pY = pYP
//                if (abs(pX) < 0.02) {
//                    pX = 0f
//                }
//                if (abs(pY) < 0.02) {
//                    pY = 0f
//                }
//                val pitchJoyControlMaxSpeed = 10f
//                val rollJoyControlMaxSpeed = 10f
//                mPitch = (pitchJoyControlMaxSpeed * pX)
//                mRoll = (rollJoyControlMaxSpeed * pY)
//                if (null == mSendVirtualStickDataTimer) {
//                    mSendVirtualStickDataTask = SendVirtualStickDataTask()
//                    mSendVirtualStickDataTimer = Timer()
//                    mSendVirtualStickDataTimer?.schedule(mSendVirtualStickDataTask, 100, 200)
//                }
//            }
//        })
//
//        mScreenJoystickLeft.setJoystickListener(object : OnScreenJoystickListener {
//            override fun onTouch(joystick: OnScreenJoystick?, pX: Float, pY: Float) {
//                var pX = pX
//                var pY = pY
//                if (abs(pX) < 0.02) {
//                    pX = 0f
//                }
//                if (abs(pY) < 0.02) {
//                    pY = 0f
//                }
//                val verticalJoyControlMaxSpeed = 2f
//                val yawJoyControlMaxSpeed = 30f
//                mYaw = (yawJoyControlMaxSpeed * pX)
//                mThrottle = (verticalJoyControlMaxSpeed * pY)
//                if (null == mSendVirtualStickDataTimer) {
//                    mSendVirtualStickDataTask = SendVirtualStickDataTask()
//                    mSendVirtualStickDataTimer = Timer()
//                    mSendVirtualStickDataTimer?.schedule(mSendVirtualStickDataTask, 0, 200)
//                }
//            }
//        })
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
                followDroneLocation.latitude = 49.229633
                followDroneLocation.longitude =  16.591430
                followDroneLocation.altitude = 10f.toDouble()

                lookAtLocation.latitude = 49.229633
                lookAtLocation.longitude = 16.591430
                lookAtLocation.altitude = 10f.toDouble()

                showToast("Waypoint set")
                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = SendVirtualStickDataTask()
                    mSendVirtualStickDataTimer = Timer()
                    mSendVirtualStickDataTimer?.schedule(mSendVirtualStickDataTask, 0, 200)
                }
            }
            R.id.btn_follow -> {
                val droneLocation = Location("")
                droneLocation.latitude = 22.005
                droneLocation.longitude = 113.0
                droneLocation.altitude = 10f.toDouble()

                val waypointBearing = droneLocation.bearingTo(followDroneLocation)
                Log.i(TAG, waypointBearing.toString())
                val lookAtBearing = droneLocation.bearingTo(lookAtLocation)
                Log.i(TAG, lookAtBearing.toString())
                val angle = abs(lookAtBearing - waypointBearing)
                Log.i(TAG, angle.toString())

            }
            R.id.btn_hotpoint -> {
                followDroneLocation.latitude = 49.228542
                followDroneLocation.longitude =  16.597291
                followDroneLocation.altitude = 10f.toDouble()
                mission = 2
                showToast("Hotpoint waypoint set")
                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = SendVirtualStickDataTask()
                    mSendVirtualStickDataTimer = Timer()
                    mSendVirtualStickDataTimer?.schedule(mSendVirtualStickDataTask, 0, 200)
                }
            }
            R.id.btn_connect -> {
                createWebSocketClient()
                webSocketClient.connect()
            }
            else -> {

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

    inner class SendVirtualStickDataTask: TimerTask() {
        override fun run() {
            viewModel.getFlightController()?.let { controller ->
                val droneLocation = Location("")
                droneLocation.latitude = controller.state.aircraftLocation.latitude
                droneLocation.longitude = controller.state.aircraftLocation.longitude
                droneLocation.altitude = controller.state.aircraftLocation.altitude.toDouble()

                val VirtualStickBearing = droneLocation.bearingTo(followDroneLocation)
                val VirtualStickDistance = droneLocation.distanceTo(followDroneLocation)
                val VirtualStickAltitudeDifference = abs(round(droneLocation.altitude) -followDroneLocation.altitude)

                val state = controller.state;
                val location = state.aircraftLocation
                val attitude = state.attitude
                val compass = controller.compass.heading
                webSocketClient.send(
                    "{\"DroneId\":\"DJI-Mavic"+ "\",\"Altitude\":" + location.altitude + ",\"Latitude\":"
                            + location.latitude + ",\"Longitude\":" + location.longitude
                            + ",\"Pitch\":" + attitude.pitch + ",\"Roll\":" + attitude.roll + ",\"Yaw\":" + attitude.yaw
                            + ",\"Compass\":" + compass + "}"
                )

                if (mission == 1) {
                    if (VirtualStickAltitudeDifference>2) {
                        mYaw=VirtualStickBearing
                    }

                    if ( abs(controller.state.attitude.yaw-VirtualStickBearing) > 5) {
                        mYaw=VirtualStickBearing
                    }

                    if (VirtualStickDistance>60) {
                        mYaw=VirtualStickBearing
                        mRoll=AutoFLightSpeed
                    }

                    if (VirtualStickDistance>2 && VirtualStickDistance<=60) {
                        mYaw=VirtualStickBearing
                        mRoll= min(VirtualStickDistance/4, AutoFLightSpeed)
                    }

                    if (!((VirtualStickAltitudeDifference>2)||(abs(controller.state.attitude.yaw-VirtualStickBearing) > 5)||(VirtualStickDistance>2 && VirtualStickDistance<=60)||(VirtualStickDistance>60))) {
                        mYaw=VirtualStickBearing
                        mRoll=0f
                    }

                    controller.sendVirtualStickFlightControlData(
                        FlightControlData(0f, mRoll, mYaw, followDroneLocation.altitude.toFloat())
                    ) { djiError ->
                        if (djiError != null) {
                            Log.i(TAG, djiError.description)
                            showToast("Error while sending data: ${djiError.description}")
                        }
                    }
                }

                if (mission == 2) {
                    // Hotpoint
                    if (VirtualStickAltitudeDifference > 2) {
                        mThrottle= followDroneLocation.altitude.toFloat()
                        mYaw = VirtualStickBearing
                    } else {
                        mThrottle= followDroneLocation.altitude.toFloat()
                        mYaw=VirtualStickBearing

                        if (abs(VirtualStickDistance - HPRadius) > 10) {
                            mPitch = 0f
                        } else {
                            mPitch = min(15f,(HPRadius/5f))
                        }

                        if (abs(VirtualStickDistance - HPRadius) > 0.2) {
                            mRoll = min(15f, (VirtualStickDistance - HPRadius)/4)
                        } else {
                            mRoll = 0f
                        }
                    }

                    controller.sendVirtualStickFlightControlData(
                        FlightControlData(-mPitch, mRoll, mYaw, mThrottle)
                    ) { djiError ->
                        if (djiError != null) {
                            Log.i(TAG, djiError.description)
                            showToast("Error while sending data: ${djiError.description}")
                        }
                    }
                }

                // waypointy
                if (mission == 3) {
                    val waypointBearing = droneLocation.bearingTo(followDroneLocation)
                    val lookAtBearing = droneLocation.bearingTo(lookAtLocation)

                    val angle = abs(lookAtBearing - waypointBearing)

                    if (VirtualStickAltitudeDifference > 2) {
                        mThrottle= followDroneLocation.altitude.toFloat()
                        mYaw = VirtualStickBearing
                    } else {
                        mThrottle= followDroneLocation.altitude.toFloat()
                        mYaw = VirtualStickBearing
                        mPitch = 10f * sin(Math.toRadians(angle.toDouble()).toFloat())
                        mRoll = 10f * cos(Math.toRadians(angle.toDouble()).toFloat())
                    }

                    Log.i(TAG, "Pitch $mPitch")
                    Log.i(TAG, "Roll $mRoll")
                    Log.i(TAG, "Yaw $mYaw")
                    Log.i(TAG, "Throttle $mThrottle")

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
}