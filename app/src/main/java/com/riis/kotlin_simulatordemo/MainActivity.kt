package com.riis.kotlin_simulatordemo

import android.Manifest
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import dji.common.camera.SettingsDefinitions
import dji.common.flightcontroller.FlightControllerState
import dji.common.flightcontroller.simulator.InitializationData
import dji.common.flightcontroller.virtualstick.*
import dji.common.model.LocationCoordinate2D
import dji.common.product.Model
import dji.sdk.base.BaseProduct
import dji.sdk.camera.Camera
import dji.sdk.camera.VideoFeeder
import dji.sdk.codec.DJICodecManager
import dji.sdk.products.Aircraft
import dji.sdk.products.HandHeld
import dji.sdk.sdkmanager.DJISDKManager
import dji.thirdparty.org.java_websocket.client.WebSocketClient
import dji.thirdparty.org.java_websocket.handshake.ServerHandshake
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.get
import java.net.URI
import java.net.URISyntaxException
import java.util.*


class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener, View.OnClickListener {
    private lateinit var mConnectStatusTextView: TextView
    private lateinit var x: TextView
    private lateinit var y: TextView
    private lateinit var z: TextView
    private lateinit var dist: TextView
    private lateinit var ws: TextView

    private lateinit var mBtnEnableVirtualStick: Button
    private lateinit var mBtnDisableVirtualStick: Button
    private lateinit var mBtnTakeOff: Button
    private lateinit var mBtnLand: Button
    private lateinit var mBtnLoad: Button
    private lateinit var mBtnRecord: Button
    private lateinit var mBtnStartMission: Button
    private lateinit var mBtnStartSimulator: Button


    private var receivedVideoDataListener: VideoFeeder.VideoDataListener? = null
    private var codecManager: DJICodecManager? = null //handles the encoding and decoding of video data
    private lateinit var videoSurface: TextureView //Used to display the DJI product's camera video stream

    private var mSendVirtualStickDataTimer: Timer? = null
    private val viewModel by viewModels<MainViewModel>()

    private var droneManager = DroneManager()
    private var kalman: Kalman = Kalman()

    private var startMission = false
    companion object {
        const val DEBUG = "drone_debug"
        const val RECORD = "drone_record"
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
        receivedVideoDataListener = VideoFeeder.VideoDataListener { videoBuffer, size ->
            codecManager?.sendDataToDecoder(videoBuffer, size)
        }
        viewModel.startSdkRegistration(this)
        initObservers()
        initUi()
    }

    private lateinit var target: DroneData
    private lateinit var prevTarget: DroneData
    private fun createWebSocketClient() {
        val uri: URI = try {
            URI("ws://147.229.193.119:8000")
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            return
        }
        droneManager.webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(serverHandshake: ServerHandshake) {
                ws.text = "ws connected"
            }

            override fun onMessage(s: String) {
                try {
                    prevTarget = if (!::prevTarget.isInitialized) {
                        Json.decodeFromString(s)
                    } else {
                        target
                    }
                    target = Json.decodeFromString(s)

                    if (startMission) {
                        val dt = (target.t - prevTarget.t) / 1000.0

                        kalman.predict(dt)
                        kalman.update(dt, target)
                        target.x = kalman.state[0]
                        target.y = kalman.state[1]
                        target.z = kalman.state[2]
                        target.vX = kalman.state[3]
                        target.vY = kalman.state[4]
                        target.vZ = -kalman.state[5]
                        target.id = "kalman"
                        droneManager.webSocketClient.send(Json.encodeToString(target))
                    }
                } catch (e: Exception) {
                    Log.i(DEBUG, e.message.toString())
                }
            }

            override fun onClose(i: Int, s: String, b: Boolean) {
                ws.text = "ws disconnected"
            }

            override fun onError(e: Exception) {
                ws.text = "ws error connection"
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
                    } else {

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

        mBtnLoad = findViewById(R.id.btn_connect_ws)
        mBtnLoad.setOnClickListener(this)

        mConnectStatusTextView = findViewById(R.id.ConnectStatusTextView)

        videoSurface = findViewById(R.id.video_previewer_surface)
        videoSurface.surfaceTextureListener = this

        x = findViewById(R.id.x)
        y = findViewById(R.id.y)
        z = findViewById(R.id.z)
        dist = findViewById(R.id.targer_distance)
        ws = findViewById(R.id.ws)
    }
    private fun initFlightController() {
        viewModel.getFlightController()?.let {
            it.rollPitchControlMode = RollPitchControlMode.VELOCITY
            it.yawControlMode = YawControlMode.ANGULAR_VELOCITY
            it.verticalControlMode = VerticalControlMode.VELOCITY
            it.rollPitchCoordinateSystem = FlightCoordinateSystem.GROUND
            val callback = FlightControllerState.Callback {
                val state = droneManager.getDroneState("target")
                x.text = state.x.toString()
                y.text = state.y.toString()
                z.text = state.z.toString()
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
            R.id.btn_connect_ws -> {
                createWebSocketClient()
            }
            R.id.btn_start_mission -> {
                if (::prevTarget.isInitialized && ::target.isInitialized) {
                    val ax = (target.vX - prevTarget.vX) / ((target.t - prevTarget.t)/1000.0)
                    val ay = (target.vY - prevTarget.vY) / ((target.t - prevTarget.t)/1000.0)
                    val az = (target.vZ - prevTarget.vZ) / ((target.t - prevTarget.t)/1000.0)

                    kalman.state = mk.ndarray(mk[
                            target.x,
                            target.y,
                            target.z,
                            target.vX,
                            target.vY,
                            target.vZ,
                            ax,
                            ay,
                            az
                    ])
                    startMission = true
                }
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

    private fun startSimulation() {
        viewModel.getFlightController()?.simulator?.start(
            InitializationData.createInstance(
                LocationCoordinate2D(49.2, 16.6), 10, 10
            )
        ) { djiError ->
            if (djiError != null) {
                Log.i(DEBUG, djiError.description)
                showToast("Simulator Error: ${djiError.description}")
            } else {
                Log.i(DEBUG, "Start Simulator Success")
                showToast("Start Simulator Success")
            }
        }
    }

    fun restartSimulation() {
        if (viewModel.getFlightController() == null) {
            Log.i(DEBUG, "isFlightController Null")
        }
        mSendVirtualStickDataTimer?.purge()

        viewModel.getFlightController()?.simulator?.stop { djiError ->
            if (djiError != null) {
                Log.i(DEBUG, djiError.description)
                showToast("Simulator Error: ${djiError.description}")
            } else {
                Log.i(DEBUG, "Stop Simulator Success")
                showToast("Stop Simulator Success")
            }
        }

        startSimulation()
    }

    fun onReturn(view: View) {
        this.finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun initPreviewer() {

        //gets an instance of the connected DJI product (null if nonexistent)
        val product: BaseProduct = getProductInstance() ?: return

        //if DJI product is disconnected, alert the user
        if (!product.isConnected) {
            showToast("Disconnected");
        } else {
            /*
            if the DJI product is connected and the aircraft model is not unknown, add the
            receivedVideoDataListener to the primary video feed.
            */
            videoSurface.surfaceTextureListener = this
            if (product.model != Model.UNKNOWN_AIRCRAFT) {
                receivedVideoDataListener?.let {
                    VideoFeeder.getInstance().primaryVideoFeed.addVideoDataListener(
                        it
                    )
                }
            }
        }
    }

    //Function that uninitializes the display for the videoSurface TextureView
    private fun uninitPreviewer() {
        val camera: Camera = getCameraInstance() ?: return
    }


    //When the MainActivity is created or resumed, initialize the video feed display
    override fun onResume() {
        super.onResume()
        initPreviewer()
    }

    //When the MainActivity is paused, clear the video feed display
    override fun onPause() {
        uninitPreviewer()
        super.onPause()
    }

    //When the MainActivity is destroyed, clear the video feed display
    override fun onDestroy() {
        uninitPreviewer()
        super.onDestroy()
    }

    //When a TextureView's SurfaceTexture is ready for use, use it to initialize the codecManager
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (codecManager == null) {
            codecManager = DJICodecManager(this, surface, width, height)
        }
    }

    //when a SurfaceTexture's size changes...
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

    //when a SurfaceTexture is about to be destroyed, uninitialize the codedManager
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        codecManager?.cleanSurface()
        codecManager = null
        return false
    }

    //When a SurfaceTexture is updated...
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}


    //Function for taking a a single photo using the DJI Product's camera
    private fun captureAction() {
        val camera: Camera = getCameraInstance() ?: return

        /*
        Setting the camera capture mode to SINGLE, and then taking a photo using the camera.
        If the resulting callback for each operation returns an error that is null, then the
        two operations are successful.
        */
        val photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE
        camera.setShootPhotoMode(photoMode) { djiError ->
            if (djiError == null) {
                lifecycleScope.launch {
                    camera.startShootPhoto { djiErrorSecond ->
                        if (djiErrorSecond == null) {
                            showToast("take photo: success")
                        } else {
                            showToast("Take Photo Failure: ${djiError?.description}")
                        }
                    }
                }
            }
        }
    }

    /*
    Function for setting the camera mode. If the resulting callback returns an error that
    is null, then the operation was successful.
    */
    private fun switchCameraMode(cameraMode: SettingsDefinitions.CameraMode) {
        val camera: Camera = getCameraInstance() ?: return

        camera.setMode(cameraMode) { error ->
            if (error == null) {
                showToast("Switch Camera Mode Succeeded")
            } else {
                showToast("Switch Camera Error: ${error.description}")
            }
        }

    }

    /*
    Note:
    Depending on the DJI product, the mobile device is either connected directly to the drone,
    or it is connected to a remote controller (RC) which is then used to control the drone.
    */

    //Function used to get the DJI product that is directly connected to the mobile device
    private fun getProductInstance(): BaseProduct? {
        return DJISDKManager.getInstance().product
    }

    /*
    Function used to get an instance of the camera in use from the DJI product
    */
    private fun getCameraInstance(): Camera? {
        if (getProductInstance() == null) return null

        return when {
            getProductInstance() is Aircraft -> {
                (getProductInstance() as Aircraft).camera
            }
            getProductInstance() is HandHeld -> {
                (getProductInstance() as HandHeld).camera
            }
            else -> null
        }
    }

    //Function that returns True if a DJI aircraft is connected
    private fun isAircraftConnected(): Boolean {
        return getProductInstance() != null && getProductInstance() is Aircraft
    }

    //Function that returns True if a DJI product is connected
    private fun isProductModuleAvailable(): Boolean {
        return (getProductInstance() != null)
    }

    //Function that returns True if a DJI product's camera is available
    private fun isCameraModuleAvailable(): Boolean {
        return isProductModuleAvailable() && (getProductInstance()?.camera != null)
    }

    //Function that returns True if a DJI camera's playback feature is available
    private fun isPlaybackAvailable(): Boolean {
        return isCameraModuleAvailable() && (getProductInstance()?.camera?.playbackManager != null)
    }

}