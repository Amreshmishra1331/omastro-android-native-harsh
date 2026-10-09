package com.omastro.app.ui.screens.vastu

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/**
 * Live compass using the device magnetometer + accelerometer (tilt-compensated).
 *
 * Exposes [heading] (0–359 degrees, 0 = North, clockwise) and [accuracy].
 * If the device lacks either sensor, [available] is false and the UI must
 * show an honest "compass not available" state instead of fake values.
 */
class CompassSensorManager(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    /** False when the device has no magnetometer or no accelerometer. */
    val available: Boolean = accelerometer != null && magnetometer != null

    private val _heading = MutableStateFlow(0f)
    val heading: StateFlow<Float> = _heading

    /** Mirrors SensorManager accuracy constants for the magnetic-field sensor. */
    private val _accuracy = MutableStateFlow(SensorManager.SENSOR_STATUS_UNRELIABLE)
    val accuracy: StateFlow<Int> = _accuracy

    /** True once a tilt-compensated heading has been computed at least once. */
    private val _hasReading = MutableStateFlow(false)
    val hasReading: StateFlow<Boolean> = _hasReading

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    fun start() {
        if (!available) return
        hasGravity = false
        hasGeomagnetic = false
        sensorManager.registerListener(
            this, accelerometer, SensorManager.SENSOR_DELAY_UI
        )
        sensorManager.registerListener(
            this, magnetometer, SensorManager.SENSOR_DELAY_UI
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravity, 0, 3)
                hasGravity = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                hasGeomagnetic = true
            }
        }
        if (!hasGravity || !hasGeomagnetic) return
        val r = FloatArray(9)
        val i = FloatArray(9)
        if (!SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) return
        val orientation = FloatArray(3)
        SensorManager.getOrientation(r, orientation)
        var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
        azimuthDeg = (azimuthDeg + 360f) % 360f
        _heading.value = azimuthDeg
        _hasReading.value = true
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            _accuracy.value = accuracy
        }
    }

    companion object {
        /** 8-sector Vastu direction code for a compass heading. */
        fun directionCode(heading: Float): String {
            val h = ((heading % 360f) + 360f) % 360f
            return when {
                h < 22.5f || h >= 337.5f -> "N"
                h < 67.5f -> "NE"
                h < 112.5f -> "E"
                h < 157.5f -> "SE"
                h < 202.5f -> "S"
                h < 247.5f -> "SW"
                h < 292.5f -> "W"
                else -> "NW"
            }
        }

        /** Rounded whole degrees for display. */
        fun roundedDegrees(heading: Float): Int =
            (((heading % 360f) + 360f) % 360f).roundToInt() % 360
    }
}
