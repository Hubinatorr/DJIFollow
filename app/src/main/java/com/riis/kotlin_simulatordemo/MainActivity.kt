package com.riis.kotlin_simulatordemo

import android.Manifest
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import dji.common.flightcontroller.FlightControllerState
import dji.common.flightcontroller.simulator.InitializationData
import dji.common.flightcontroller.virtualstick.*
import dji.common.model.LocationCoordinate2D
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
    private lateinit var mBtnLoad: Button
    private lateinit var mBtnRecord: Button
    private lateinit var mBtnStartMission: Button
    private lateinit var mBtnStartSimulator: Button
    private lateinit var mFrequencyText: EditText

    private var mSendVirtualStickDataTimer: Timer? = null
    private var mSendVirtualStickDataTask: SendVirtualStickDataTask? = null
    private val viewModel by viewModels<MainViewModel>()

    private var droneManager = DroneManager()

    private var interval: Long = 100

    companion object {
        const val DEBUG = "PrintDebug"
        const val RECORD = "PrintRecord"
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
        createWebSocketClient()
        initObservers()
        initUi()

    }

    private fun createWebSocketClient() {
        val uri: URI = try {
            URI("ws://147.229.193.119:8000")
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            return
        }
        droneManager.webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(serverHandshake: ServerHandshake) {
                Log.i(DEBUG, "Connected to the DroCo server.")
            }

            override fun onMessage(s: String) {
//                val droneData = Klaxon().parse<DroneData>(s)
//                if (droneData != null) {
//                    droneManager.target = droneData
//                    droneManager.followStage = FollowStage.READY
//                } else {
//                    Log.i(DEBUG, "Parse incorrect")
//                }
            }

            override fun onClose(i: Int, s: String, b: Boolean) {
                Log.i(DEBUG, "Connection to the DroCo server closed.")
            }

            override fun onError(e: Exception) {
                Log.i(DEBUG, "Error connection")
            }
        }

        droneManager.webSocketClient.connect()
    }

    private fun initObservers() {
        viewModel.connectionStatus.observe(this, androidx.lifecycle.Observer<Boolean> {
            initFlightController()
            var ret = false
            viewModel.product?.let {
                if (it.isConnected) {
                    mConnectStatusTextView.text = it.model.toString() + " Connected"
                    ret = true
                    viewModel.getRemoteController()?.let { remoteController ->
                        remoteController.setHardwareStateCallback { state ->
                            val lstick = state.leftStick!!
                            val rstick = state.rightStick!!
                            droneManager.controls = Controls(
                                lstick.horizontalPosition,
                                lstick.verticalPosition,
                                rstick.horizontalPosition,
                                rstick.verticalPosition
                            )
                        }
                    }
                } else {
                    if ((it as Aircraft?)?.remoteController != null && it.remoteController.isConnected) {
                        mConnectStatusTextView.text = "only RC Connected"
                        ret = true
                    } else {}
                }
            }

            if (!ret) {
                mConnectStatusTextView.text = "Disconnected"
            }
        })


    }

    private fun initUi() {
        mBtnEnableVirtualStick = findViewById(R.id.btn_enable_virtual_stick)
        mBtnEnableVirtualStick.setOnClickListener(this)

        mBtnDisableVirtualStick = findViewById(R.id.btn_disable_virtual_stick)
        mBtnDisableVirtualStick.setOnClickListener(this)

        mBtnTakeOff = findViewById(R.id.btn_take_off)
        mBtnTakeOff.setOnClickListener(this)

        mBtnLand = findViewById(R.id.btn_land)
        mBtnLand.setOnClickListener(this)

        mBtnRecord = findViewById(R.id.btn_record)
        mBtnRecord.setOnClickListener(this)

        mBtnStartMission = findViewById(R.id.btn_start_mission)
        mBtnStartMission.setOnClickListener(this)

        mBtnStartSimulator = findViewById(R.id.btn_start_simulation)
        mBtnStartSimulator.setOnClickListener(this)

        mBtnLoad = findViewById(R.id.btn_load)
        mBtnLoad.setOnClickListener(this)

        mConnectStatusTextView = findViewById(R.id.ConnectStatusTextView)

        mFrequencyText = findViewById(R.id.frequency)
        mFrequencyText.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                interval = mFrequencyText.text.toString().toLongOrNull()!!
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
            }
        })
    }

    private var prevT = 0L
    private fun initFlightController() {
        viewModel.getFlightController()?.let {
            it.rollPitchControlMode = RollPitchControlMode.VELOCITY
            it.yawControlMode = YawControlMode.ANGLE
            it.verticalControlMode = VerticalControlMode.POSITION
            it.rollPitchCoordinateSystem = FlightCoordinateSystem.GROUND
            val callback= FlightControllerState.Callback {
                droneManager.onStateChange()
            }
            it.setStateCallback(callback)
            droneManager.controller = it
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_enable_virtual_stick -> {
                viewModel.getFlightController()?.let { controller ->
                    controller.setVirtualStickModeEnabled(true) { djiError ->
                        if (djiError != null) {
                            Log.i(DEBUG, djiError.description)
                            showToast("Virtual Stick: Could not enable virtual stick")
                        } else {
                            Log.i(DEBUG, "Enable Virtual Stick Success")
                            showToast("Virtual Sticks Enabled")
                        }
                    }
                }

            }
            R.id.btn_disable_virtual_stick -> {
                viewModel.getFlightController()?.let { controller ->
                    controller.setVirtualStickModeEnabled(false) { djiError ->
                        if (djiError != null) {
                            Log.i(DEBUG, djiError.description)
                            showToast("Virtual Stick: Could not disable virtual stick")
                        } else {
                            Log.i(DEBUG, "Disable Virtual Stick Success")
                            showToast("Virtual Sticks Disabled")
                        }
                    }
                }
            }
            R.id.btn_take_off -> {
                viewModel.getFlightController()?.let { controller ->
                    controller.startTakeoff { djiError ->
                        if (djiError != null) {
                            Log.i(DEBUG, djiError.description)
                            showToast("Takeoff Error: ${djiError.description}")
                        } else {
                            Log.i(DEBUG, "Takeoff Success")
                            showToast("Takeoff Success")
                        }
                    }
                }
            }
            R.id.btn_land -> {
                viewModel.getFlightController()?.let { controller ->
                    controller.startLanding { djiError ->
                        if (djiError != null) {
                            Log.i(DEBUG, djiError.description)
                            showToast("Landing Error: ${djiError.description}")
                        } else {
                            Log.i(DEBUG, "Start Landing Success")
                            showToast("Start Landing Success")
                        }
                    }
                }
            }
            R.id.btn_load -> {
                val droneData = Klaxon().parseArray<DroneData>(resources.openRawResource(R.raw.normal))!!
            }
            R.id.btn_start_mission -> {
                schedule()
            }
            R.id.btn_start_simulation -> {
                startSimulation()
            }
            R.id.btn_record -> {
                droneManager.record = !droneManager.record
                if (droneManager.record)
                    showToast("Start Record")
                else
                    showToast("Stop Record")
            }
        }
    }
    private fun schedule()
    {
        if (mSendVirtualStickDataTimer == null) {
            mSendVirtualStickDataTask = SendVirtualStickDataTask()
            mSendVirtualStickDataTimer = Timer()
            mSendVirtualStickDataTimer?.schedule(mSendVirtualStickDataTask, 0, interval)
        }
    }

    private fun startSimulation()
    {
        viewModel.getFlightController()?.simulator?.start(
            InitializationData.createInstance(
                LocationCoordinate2D(49.2, 16.6), 10, 10
            )
        ) { djiError ->
            if (djiError != null) {
                Log.i(DEBUG, djiError.description)
                showToast("Simulator Error: ${djiError.description}")
            } else {
                Log.i(DEBUG,"Start Simulator Success")
                showToast("Start Simulator Success")
            }
        }
    }
    fun restartSimulation()
    {
        if(viewModel.getFlightController() == null) {
            Log.i(DEBUG, "isFlightController Null ")
        }
        mSendVirtualStickDataTimer?.purge()

        viewModel.getFlightController()?.simulator?.stop { djiError ->
            if (djiError != null) {
                Log.i(DEBUG, djiError.description)
                showToast("Simulator Error: ${djiError.description}")
            } else {
                Log.i(DEBUG,"Stop Simulator Success")
                showToast("Stop Simulator Success")
            }
        }

        startSimulation()
    }

    inner class SendVirtualStickDataTask : TimerTask() {
        override fun run() {
            droneManager.calculateFollowData()
        }
    }
    fun onReturn(view: View) {
        this.finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
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
}