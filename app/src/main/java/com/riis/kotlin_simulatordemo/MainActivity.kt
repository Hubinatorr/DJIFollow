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
    private lateinit var ws: TextView

    private lateinit var mBtnTakeOff: Button
    private lateinit var mBtnLand: Button

    private lateinit var mBtnRecord: Button
    private lateinit var mBtnInitKalman: Button
    private lateinit var mBtnStartSimulator: Button

    private lateinit var webSocketClient: WebSocketClient

    private var receivedVideoDataListener: VideoFeeder.VideoDataListener? = null
    private var codecManager: DJICodecManager? = null //handles the encoding and decoding of video data
    private lateinit var videoSurface: TextureView //Used to display the DJI product's camera video stream

    private val viewModel by viewModels<MainViewModel>()

    private var kalman = Kalman()
    private lateinit var GPS : DroneData
    private var record = true

    companion object {
        const val DEBUG = "drone_debug"
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

    private fun createWebSocketClient() {
        val uri: URI = try {
            URI("ws://147.229.193.119:8000")
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            return
        }
        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(serverHandshake: ServerHandshake) {
                ws.text = "ws connected"
            }

            override fun onMessage(s: String) {

            }

            override fun onClose(i: Int, s: String, b: Boolean) {
                ws.text = "ws disconnected"
            }

            override fun onError(e: Exception) {
                ws.text = "ws error connection"
            }
        }
        webSocketClient.connect()
    }


    private fun initUi() {
        mBtnTakeOff = findViewById(R.id.btn_take_off)
        mBtnTakeOff.setOnClickListener(this)

        mBtnLand = findViewById(R.id.btn_land)
        mBtnLand.setOnClickListener(this)

        mBtnRecord = findViewById(R.id.btn_record)
        mBtnRecord.setOnClickListener(this)

        mBtnInitKalman = findViewById(R.id.btn_init_kalman)
        mBtnInitKalman.setOnClickListener(this)

        mBtnStartSimulator = findViewById(R.id.btn_start_simulation)
        mBtnStartSimulator.setOnClickListener(this)

        mConnectStatusTextView = findViewById(R.id.ConnectStatusTextView)

        videoSurface = findViewById(R.id.video_previewer_surface)
        videoSurface.surfaceTextureListener = this

        x = findViewById(R.id.x)
        y = findViewById(R.id.y)
        z = findViewById(R.id.z)
        ws = findViewById(R.id.ws)
    }

    private fun initFlightController() {
        viewModel.getFlightController()?.let {
            it.rollPitchControlMode = RollPitchControlMode.VELOCITY
            it.yawControlMode = YawControlMode.ANGULAR_VELOCITY
            it.verticalControlMode = VerticalControlMode.VELOCITY
            it.rollPitchCoordinateSystem = FlightCoordinateSystem.GROUND
            val callback = FlightControllerState.Callback { state ->
                val newGPS = getDroneState(state)
                if (kalman.initialized) {
                    val dt = (newGPS.t - GPS.t) / 1000.0
                    kalman.predict(dt)
                    kalman.update(dt, newGPS)
                    GPS = newGPS
                    GPS.x = kalman.state[0]
                    GPS.y = kalman.state[1]
                    GPS.z = kalman.state[2]
                    GPS.vX = kalman.state[3]
                    GPS.vY = kalman.state[4]
                    GPS.vZ = kalman.state[5]
                    x.text = "K: ${GPS.x}"
                    y.text = "K: ${GPS.y}"
                    z.text = "K: ${GPS.z}"
                    if (record) {
                        webSocketClient.send(Json.encodeToString(GPS))
                    }

                } else {
                    GPS = newGPS
                    x.text = GPS.x.toString()
                    y.text = GPS.y.toString()
                    z.text = GPS.z.toString()
                }

            }
            it.setStateCallback(callback)
        }
    }

    private fun getDroneState(state: FlightControllerState): DroneData {
        return DroneData(
            System.currentTimeMillis(),
            "Target",
            Deg2UTM(
                state.aircraftLocation.latitude,
                state.aircraftLocation.longitude
            ).Northing,
            Deg2UTM(
                state.aircraftLocation.latitude,
                state.aircraftLocation.longitude
            ).Easting,
            state.aircraftLocation.altitude.toDouble(),
            state.velocityX.toDouble(),
            state.velocityY.toDouble(),
            state.velocityZ.toDouble(),
            state.attitude.roll,
            state.attitude.pitch,
            state.attitude.yaw,
        )
    }

    override fun onClick(v: View?) {
        when (v?.id) {
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
            R.id.btn_init_kalman -> {
                kalman = Kalman()
                kalman.init(mk.ndarray(mk[GPS.x, GPS.y, GPS.z, GPS.vX, GPS.vY, GPS.vZ, 0.0, 0.0, 0.0]))
            }
            R.id.btn_start_simulation -> {
                startSimulation()
            }
            R.id.btn_record -> {
                record = !record

                if (record) {
                    showToast("Record started")
                } else {
                    showToast("Record stopped")
                }
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

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
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
                    if ((it as Aircraft?)?.remoteController != null && it.remoteController.isConnected) {
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

    private fun initPreviewer() {
        val product: BaseProduct = getProductInstance() ?: return

        if (!product.isConnected) {
            showToast("Disconnected");
        } else {
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

    private fun uninitPreviewer() {
        val camera: Camera = getCameraInstance() ?: return
    }

    override fun onResume() {
        super.onResume()
        initPreviewer()
    }

    override fun onPause() {
        uninitPreviewer()
        super.onPause()
    }

    override fun onDestroy() {
        initPreviewer()
        super.onDestroy()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (codecManager == null) {
            codecManager = DJICodecManager(this, surface, width, height)
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        codecManager?.cleanSurface()
        codecManager = null
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    private fun getProductInstance(): BaseProduct? {
        return DJISDKManager.getInstance().product
    }

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
    private fun isProductModuleAvailable(): Boolean {
        return (getProductInstance() != null)
    }

    private fun isCameraModuleAvailable(): Boolean {
        return isProductModuleAvailable() && (getProductInstance()?.camera != null)
    }

    private fun isPlaybackAvailable(): Boolean {
        return isCameraModuleAvailable() && (getProductInstance()?.camera?.playbackManager != null)
    }

}