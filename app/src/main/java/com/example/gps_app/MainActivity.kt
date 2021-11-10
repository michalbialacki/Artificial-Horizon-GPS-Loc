package com.example.gps_app

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.*


import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.lang.Math.cos
import java.lang.Math.sin
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.sign

class MainActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener{
    private lateinit var mMap: GoogleMap
    private val NS2S = 1.0f / 1000000000.0f
    private val deltaRotationVector = FloatArray(4) { 0f }
    private var timestamp: Float = 0f
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationrequest: LocationRequest
    private lateinit var sensorManagerGyro : SensorManager
    private lateinit var sensorManagerAccelerometer : SensorManager
    private lateinit var horizonBackground : ImageView
    private lateinit var rollIndicator : ImageView
    private lateinit var xAcclValue : TextView
    private lateinit var yAcclValue : TextView
    private lateinit var zAcclValue : TextView

    private lateinit var locViewModel : LocationViewModel
    var gyroVals = mutableListOf<Double>(.0,.0,.0)
    var acclVals = mutableListOf<Double>(.0,.0,.0)


    private fun setupSensors() {
        sensorManagerGyro = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManagerAccelerometer = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManagerGyro.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also {
            sensorManagerGyro.registerListener(this,
                it,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_FASTEST)

        }
        sensorManagerAccelerometer.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManagerAccelerometer.registerListener(this,it,
            SensorManager.SENSOR_DELAY_FASTEST,
            SensorManager.SENSOR_DELAY_FASTEST)
        }

    }


    override fun onSensorChanged(event: SensorEvent?) {

        if(event?.sensor?.type == Sensor.TYPE_GYROSCOPE){
            /*
                0 -> pitch
                1 -> roll
                2 -> yaw
             */
            var gyroPitch = Math.round(event.values[0]*10.0) / 10.0
            var gyroRoll = Math.round(event.values[1]*10.0) / 10.0
            var gyroYaw = Math.round(event.values[2]*10.0) / 10.0

            gyroVals[0]="%.1f".format(gyroPitch).toDouble()
            gyroVals[1]="%.1f".format(gyroRoll).toDouble()
            gyroVals[2]="%.1f".format(gyroYaw).toDouble()

//            indicatorPos = (indicatorPos + (gyroRoll)*0.1f)
//            backgroundPos = (backgroundPos + (gyroPitch)*0.08f)
            gyroVals


        }

            if(event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                /*
                    0->yaw
                    1->pitch
                    2->roll

                 */


                var accelPitch = (Math.round(event.values[1] * 10.0) / 10.0)
                var accelRoll = (Math.round(event.values[0]*10.0) / 10.0)
                var accelYaw = (Math.round(event.values[2]*10.0) / 10.0)
                acclVals[0] = accelPitch
                acclVals[1] = accelRoll
                acclVals[2] = accelYaw

                yAcclValue.text = "${accelPitch}"
                xAcclValue.text = "${accelYaw}"
                zAcclValue.text = "${accelRoll}"
                acclVals


    }
        rollIndicator.apply {
            rotation = (-acclVals[1] - (-acclVals[1] * sin(gyroVals[1]))).toFloat() *7.5f
            translationX = acclVals[1].toFloat()
        }
        horizonBackground.apply {
            rotationX = (acclVals[2]).toFloat()
            translationY = -(acclVals[2]).toFloat()*20f
        }

        gyroVals
        acclVals

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    override fun onDestroy() {
        sensorManagerGyro.unregisterListener(this)
        super.onDestroy()
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.


        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        xAcclValue = findViewById(R.id.tv_xAcclValue)
        yAcclValue = findViewById(R.id.tv_yAcclValue)
        zAcclValue = findViewById(R.id.tv_zAcclValue)
        horizonBackground=findViewById(R.id.img_backg)
        rollIndicator = findViewById(R.id.img_yawIndicator)
        // Get the SupportMapFragment and request notification when the map is ready to be used.




        setupSensors()

//        indicatorPos
//        backgroundPos
    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        locViewModel = ViewModelProvider(this).get(LocationViewModel::class.java)
        localizationDeclaration()
        updateCurrentLoc()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        locViewModel.getLocationData().observe(this, {
            mMap.clear()
            val airVehiclePosition = LatLng(it.latitude, it.longitude)
            mMap.addMarker(MarkerOptions().position(airVehiclePosition).title("Your Aircraft"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(airVehiclePosition,10f))

        })

    }


    //////////////////////////function space//////////////////


    private fun localizationDeclaration(){
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(applicationContext)
        locationrequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 500
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }

        locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                }
            }
        }
    }


    private fun checkPermits() {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat
                .checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
            return
        }
    }



    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        checkPermits()
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            Toast.makeText(applicationContext,"Dziala",Toast.LENGTH_SHORT).show()
        }

    }


    @SuppressLint("MissingPermission")
    private fun updateCurrentLoc() {
        checkPermits()

        fusedLocationProviderClient.requestLocationUpdates(
            locationrequest,
            locationCallback,
            Looper.getMainLooper()
        )


    }
}