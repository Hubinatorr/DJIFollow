package com.riis.kotlin_simulatordemo

import android.Manifest
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.beust.klaxon.Klaxon
import dji.common.flightcontroller.virtualstick.*
import dji.sdk.products.Aircraft
import dji.thirdparty.org.java_websocket.client.WebSocketClient
import dji.thirdparty.org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.net.URISyntaxException
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var mConnectStatusTextView: TextView
    private lateinit var mBtnEnableVirtualStick: Button
    private lateinit var mBtnDisableVirtualStick: Button
    private lateinit var mBtnTakeOff: Button
    private lateinit var mBtnLand: Button
    private lateinit var mBtnStartMission: Button
    private lateinit var mBtnStartRecord: Button
    private lateinit var mBtnLoad: Button
    private lateinit var mFollowStatusTextView: TextView
    private lateinit var mFrequencyInput: EditText

    lateinit var webSocketClient: WebSocketClient
    private var mSendVirtualStickDataTimer: Timer? = null
    private var mSendVirtualStickDataTask: SendVirtualStickDataTask? = null
    private val viewModel by viewModels<MainViewModel>()

    private var droneManager = DroneManager()
    private var startLocation = Location("")



    private var record = false;

    companion object {
        const val UI = "UIDebug"
        const val TAGDEGUG = "RecordPath"
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
        createWebSocketClient()
    }

    private fun createWebSocketClient() {
        val uri: URI = try {
            URI("ws://147.229.193.119:8000")
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            return
        }
        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(serverHandshake: ServerHandshake) {
                Log.i(UI, "Connected to the DroCo server.")
            }

            override fun onMessage(s: String) {
                val droneData = Klaxon().parse<DroneData>(s)
                if (droneData != null) {
                    droneManager.target = droneData
                    droneManager.followStage = DroneManager.FollowStage.READY
                } else {
                    Log.i(UI, "Parse incorrect")
                }
            }

            override fun onClose(i: Int, s: String, b: Boolean) {
                Log.i(UI, "Connection to the DroCo server closed.")
            }

            override fun onError(e: Exception) {
                Log.i(UI, "Error connection")
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



        viewModel.getRemoteController()?.let { remoteController ->
            remoteController.setHardwareStateCallback { state ->
                var lstick = state.leftStick
                var rstick = state.rightStick
                LeftH = lstick!!.horizontalPosition
                LeftV = lstick.verticalPosition
                RightH = rstick!!.horizontalPosition
                RightV =rstick.verticalPosition
                if (lstick != null) {
                    Log.i(UI, "mam st")
                }
            }
        }
    }

    var LeftH: Int = 0
    var LeftV: Int = 0
    var RightH: Int = 0
    var RightV: Int = 0

    private fun initUi() {
        mBtnEnableVirtualStick = findViewById(R.id.btn_enable_virtual_stick)
        mBtnEnableVirtualStick.setOnClickListener(this)

        mBtnDisableVirtualStick = findViewById(R.id.btn_disable_virtual_stick)
        mBtnDisableVirtualStick.setOnClickListener(this)

        mBtnTakeOff = findViewById(R.id.btn_take_off)
        mBtnTakeOff.setOnClickListener(this)

        mBtnLand = findViewById(R.id.btn_land)
        mBtnLand.setOnClickListener(this)

        mBtnStartMission = findViewById(R.id.btn_start_mission)
        mBtnStartMission.setOnClickListener(this)

        mBtnStartRecord = findViewById(R.id.btn_start_record)
        mBtnStartRecord.setOnClickListener(this)

        mBtnLoad = findViewById(R.id.btn_load)
        mBtnLoad.setOnClickListener(this)

        mFrequencyInput = findViewById(R.id.frequency)


        mConnectStatusTextView = findViewById(R.id.ConnectStatusTextView)
        mFollowStatusTextView = findViewById(R.id.followStatusTextVIew)
        mFollowStatusTextView.text = "Follow Not Ready"
    }

    private fun initFlightController() {
        viewModel.getFlightController()?.let {
            it.rollPitchControlMode = RollPitchControlMode.VELOCITY
            it.yawControlMode = YawControlMode.ANGLE
            it.verticalControlMode = VerticalControlMode.POSITION
            it.rollPitchCoordinateSystem = FlightCoordinateSystem.GROUND
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_enable_virtual_stick -> {
                viewModel.getFlightController()?.let { controller ->
                    controller.setVirtualStickModeEnabled(true) { djiError ->
                        if (djiError != null) {
                            Log.i(UI, djiError.description)
                            showToast("Virtual Stick: Could not enable virtual stick")
                        } else {
                            Log.i(UI, "Enable Virtual Stick Success")
                            showToast("Virtual Sticks Enabled")
                        }
                    }
                }

            }
            R.id.btn_disable_virtual_stick -> {
                viewModel.getFlightController()?.let { controller ->
                    controller.setVirtualStickModeEnabled(false) { djiError ->
                        if (djiError != null) {
                            Log.i(UI, djiError.description)
                            showToast("Virtual Stick: Could not disable virtual stick")
                        } else {
                            Log.i(UI, "Disable Virtual Stick Success")
                            showToast("Virtual Sticks Disabled")
                        }
                    }
                }
            }
            R.id.btn_take_off -> {
                viewModel.getFlightController()?.let { controller ->
                    controller.startTakeoff { djiError ->
                        if (djiError != null) {
                            Log.i(UI, djiError.description)
                            showToast("Takeoff Error: ${djiError.description}")
                        } else {
                            Log.i(UI, "Takeoff Success")
                            showToast("Takeoff Success")
                        }
                    }
                }
            }
            R.id.btn_land -> {
                viewModel.getFlightController()?.let { controller ->
                    controller.startLanding { djiError ->
                        if (djiError != null) {
                            Log.i(UI, djiError.description)
                            showToast("Landing Error: ${djiError.description}")
                        } else {
                            Log.i(UI, "Start Landing Success")
                            showToast("Start Landing Success")
                        }
                    }
                }
            }
            R.id.btn_start_mission -> {
                droneManager.target = Klaxon().parse("{\"Altitude\" : 1.100000023841858, \"Compass\" : 0.0, \"DroneId\" : \"DJI-Mavic\", \"Latitude\" : 49.226579703075075, \"LeftH\" : 0, \"LeftV\" : 0, \"Longitude\" : 16.59658932489439, \"Pitch\" : 1.0, \"RightH\" : 0, \"RightV\" : 0, \"Roll\" : 0.0, \"Timestamp\" : 1671373700213, \"Yaw\" : 0.0, \"velocityX\" : 0.0, \"velocityY\" : 0.0, \"velocityZ\" : 0.0}")!!

                if (mSendVirtualStickDataTimer == null) {
                    mSendVirtualStickDataTask = SendVirtualStickDataTask()
                    mSendVirtualStickDataTimer = Timer()
                    mSendVirtualStickDataTimer?.schedule(mSendVirtualStickDataTask, 0, 200)
                }
            }
            R.id.btn_start_record -> {
                record = true
                if (mSendVirtualStickDataTimer == null) {
                    mSendVirtualStickDataTask = SendVirtualStickDataTask()
                    mSendVirtualStickDataTimer = Timer()
                    mSendVirtualStickDataTimer?.schedule(mSendVirtualStickDataTask, 0, 40)
                }
            }
            R.id.btn_load -> {
                val flightData = resources.openRawResource(R.raw.data)
                val droneData = Klaxon().parseArray<DroneData>(flightData)
                if (droneData != null) {
                    for (target in droneData) {
                        droneManager.targets.add(target)
                    }
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

    inner class SendVirtualStickDataTask : TimerTask() {
        override fun run() {
            viewModel.getFlightController()?.let { controller ->
                if (record) {
                    droneManager.recordPath(controller, webSocketClient, LeftV, LeftH, RightV, RightH)
                } else {
                    droneManager.calculateFollowData(controller, webSocketClient)

                }
            }
        }
    }
}