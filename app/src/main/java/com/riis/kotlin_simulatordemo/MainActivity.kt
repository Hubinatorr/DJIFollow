package com.riis.kotlin_simulatordemo

import android.Manifest
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.TextureView
import android.widget.Button
import android.widget.EditText
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
import dji.sdk.flightcontroller.FlightController
import dji.sdk.products.Aircraft
import dji.sdk.products.HandHeld
import dji.sdk.sdkmanager.DJISDKManager
import dji.thirdparty.org.java_websocket.client.WebSocketClient
import dji.thirdparty.org.java_websocket.handshake.ServerHandshake
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import java.net.URI
import java.net.URISyntaxException


class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {
    private lateinit var tConnectStatusTextView: TextView
    private lateinit var tX: TextView
    private lateinit var tY: TextView
    private lateinit var tZ: TextView
    private lateinit var tWs: TextView

    private val viewModel by viewModels<MainViewModel>()
    private lateinit var flightController: FlightController
    private lateinit var webSocketClient: WebSocketClient

    private var pidController = PIDController(1.5, 0.5, 0.0)
    private var kalman = KalmanFilter()

    private lateinit var GPS: DroneData
    private lateinit var target: DroneData
    private lateinit var targets: MutableList<DroneData>

    private var follow = false
    private var record = false
    private var startTest = false
    private var wsConnected = false

    private var URL = "ws://147.229.193.119:8000"

    private var receivedVideoDataListener: VideoFeeder.VideoDataListener? = null
    private var codecManager: DJICodecManager? = null
    private lateinit var videoSurface: TextureView

    companion object {
        const val DEBUG = "drone_debug"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getPermissions()
        viewModel.startSdkRegistration(this)
        initObservers()
        receivedVideoDataListener = VideoFeeder.VideoDataListener { videoBuffer, size ->
            codecManager?.sendDataToDecoder(videoBuffer, size)
        }
        initUi()
    }

    private fun createWebSocketClient() {
        val uri: URI = try {
            URI(URL)
        } catch (e: URISyntaxException) {
            showToast("Bad Url")
            e.printStackTrace()
            return
        }
        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(serverHandshake: ServerHandshake) {
                tWs.text = "Connected"
                tWs.setTextColor(Color.GREEN)
                wsConnected = true
            }

            override fun onMessage(s: String) {
                if (s[0] == '{') {
                    target = Json.decodeFromString(s)
                }
            }

            override fun onClose(i: Int, s: String, b: Boolean) {
                tWs.text = "Disconnected"
                wsConnected = false
            }

            override fun onError(e: Exception) {
                tWs.text = "Connection error"
                tWs.setTextColor(Color.RED)
                wsConnected = false
            }
        }
        webSocketClient.connect()
    }

    private fun initUi() {
        findViewById<Button>(R.id.btn_enable_virtual_stick).setOnClickListener {
            if (setVirtualSticksEnabled(true)) {
                follow = true
                setOffset()
            }
        }
        findViewById<Button>(R.id.btn_disable_virtual_stick).setOnClickListener {
            setVirtualSticksEnabled(false)
        }
        findViewById<Button>(R.id.btn_take_off).setOnClickListener {
            flightController.startTakeoff { djiError ->
                showToast(if (djiError != null) "Takeoff Error: ${djiError.description}" else "Takeoff Success")
            }
        }
        findViewById<Button>(R.id.btn_land).setOnClickListener {
            flightController.startLanding { djiError ->
                showToast(if (djiError != null) "Landing Error: ${djiError.description}" else "Start Landing Success")
            }
        }
        findViewById<Button>(R.id.btn_connect_ws).setOnClickListener { createWebSocketClient() }
        findViewById<Button>(R.id.btn_init_kalman).setOnClickListener {
            kalman = KalmanFilter()
            kalman.prevData = GPS
            kalman.init(mk.ndarray(mk[GPS.x, GPS.y, GPS.z, GPS.vX, GPS.vY, GPS.vZ, 0.0, 0.0, 0.0]))
        }
        findViewById<Button>(R.id.btn_start_simulation).setOnClickListener {
            flightController.simulator?.start(
                InitializationData.createInstance(
                    LocationCoordinate2D(49.2, 16.6), 10, 10
                )
            ) { djiError ->
                if (djiError != null) {
                    showToast("Simulator Error: ${djiError.description}")
                } else {
                    showToast("Start Simulator Success")
                }
            }
        }
        findViewById<Button>(R.id.btn_record).setOnClickListener {
            record = !record
            if (record) {
                showToast(if (record) "Record stated" else "Record stopped")
            }
        }
        findViewById<Button>(R.id.btn_start_test).setOnClickListener {
            targets = Json.decodeFromStream(resources.openRawResource(R.raw.fast))

            if (setVirtualSticksEnabled(true)) {
                target = targets[0]
                setOffset()
                startTest = true
            }
        }

        tConnectStatusTextView = findViewById(R.id.ConnectStatusTextView)

        videoSurface = findViewById(R.id.video_previewer_surface)
        videoSurface.surfaceTextureListener = this

        findViewById<EditText>(R.id.kd).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().toDoubleOrNull() == null) {
                    showToast("Error changing kd"); return
                }
                pidController.Kd = s.toString().toDouble()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<EditText>(R.id.kp).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().toDoubleOrNull() == null) {
                    showToast("Error changing kp"); return
                }
                pidController.Kp = s.toString().toDouble()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<EditText>(R.id.ki).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().toDoubleOrNull() == null) {
                    showToast("Error changing kp"); return
                }
                pidController.Kp = s.toString().toDouble()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<EditText>(R.id.websocket_url).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                URL = s.toString()
                showToast("Changed url")
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        tX = findViewById(R.id.x)
        tY = findViewById(R.id.y)
        tZ = findViewById(R.id.z)
        tWs = findViewById(R.id.ws)
    }

    private fun initFlightController() {
        viewModel.getFlightController()?.let {
            it.rollPitchControlMode = RollPitchControlMode.VELOCITY
            it.yawControlMode = YawControlMode.ANGULAR_VELOCITY
            it.verticalControlMode = VerticalControlMode.VELOCITY
            it.rollPitchCoordinateSystem = FlightCoordinateSystem.GROUND

            val callback = FlightControllerState.Callback { state ->
                val newGPS = getDroneState(state)
                newGPS.vZ = -newGPS.vZ

                if (kalman.initialized) {
                    val dt = (newGPS.t - GPS.t) / 1000.0
                    kalman.predict(dt)
                    GPS = kalman.update(dt, newGPS)
                } else {
                    GPS = newGPS
                }

                tX.text = "${if (kalman.initialized) "k_x" else "x"}: ${GPS.x}"
                tY.text = "${if (kalman.initialized) "k_y" else "y"}: ${GPS.y}"
                tZ.text = "${if (kalman.initialized) "k_z" else "z"}: ${GPS.z}"

                if (follow) {
                    calculateFollowData()
                }

                if (startTest) {
                    if (targets.isNotEmpty()) {
                        target = targets[0]
                        calculateFollowData()
                        webSocketClient.send(Json.encodeToString(GPS))
                        targets.removeAt(0)
                    } else {
                        showToast("Test Stopped")
                        startTest = false
                    }
                }

                if (record && wsConnected) {
                    try {
                        webSocketClient.send(Json.encodeToString(GPS))
                    } catch (e: Exception) {
                        showToast(e.message!!)
                    }
                }
            }

            it.setStateCallback(callback)
            flightController = it
        }
    }

    private fun calculateFollowData() {
        val (roll, pitch, throttle) = pidController.compute(GPS, target)
        flightController.sendVirtualStickFlightControlData(
            FlightControlData(
                pitch.coerceIn(-10.0, 10.0).toFloat(),
                roll.coerceIn(-10.0, 10.0).toFloat(),
                0f,
                throttle.coerceIn(-4.0, 4.0).toFloat()
            )
        ) { djiError ->
            if (djiError != null) {
                showToast("Couldn't sent VirtualStick Commands")
            }
        }
    }

    private fun getDroneState(state: FlightControllerState): DroneData {
        return DroneData(
            System.currentTimeMillis(), "Target",
            Deg2UTM(state.aircraftLocation.latitude, state.aircraftLocation.longitude).Northing,
            Deg2UTM(state.aircraftLocation.latitude, state.aircraftLocation.longitude).Easting,
            state.aircraftLocation.altitude.toDouble(),
            state.velocityX.toDouble(), state.velocityY.toDouble(), state.velocityZ.toDouble()
        )
    }

    private fun setOffset() {
        pidController.offsetX = target.x - GPS.x
        pidController.offsetY = target.y - GPS.y
        pidController.offsetZ = target.z - GPS.z
    }

    private fun setVirtualSticksEnabled(enabled: Boolean): Boolean {
        var ret = true
        flightController.setVirtualStickModeEnabled(enabled) { djiError ->
            if (djiError != null) {
                showToast("Could not ${if (enabled) "enable" else "disable"} virtual stick")
                ret = false
            } else {
                showToast("${if (enabled) "Enabled" else "Disabled"} virtual stick")
            }
        }
        return ret
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun getPermissions () {
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
    }

    private fun initObservers() {
        viewModel.connectionStatus.observe(this, androidx.lifecycle.Observer<Boolean> {
            initFlightController()
            var ret = false
            viewModel.product?.let {
                if (it.isConnected) {
                    tConnectStatusTextView.text = it.model.toString() + " Connected"
                    ret = true
                } else {
                    if ((it as Aircraft?)?.remoteController != null && it.remoteController.isConnected) {
                        tConnectStatusTextView.text = "only RC Connected"
                        ret = true
                    }
                }
            }

            if (!ret) {
                tConnectStatusTextView.text = "Disconnected"
            }
        })
    }

    private fun initPreviewer() {
        val product: BaseProduct = getProductInstance() ?: return

        if (!product.isConnected) {
            showToast("Disconnected")
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
}