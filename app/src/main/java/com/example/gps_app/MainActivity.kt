package com.example.gps_app

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
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
import java.lang.Math.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener{
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationrequest: LocationRequest
    private lateinit var sensorManagerGyro : SensorManager
    private lateinit var sensorManagerAccelerometer : SensorManager
    private lateinit var sensorManagerMagnetometer : SensorManager
    private lateinit var horizonBackground : ImageView
    private lateinit var rollIndicator : ImageView
    private lateinit var xAcclValue : TextView
    private lateinit var yAcclValue : TextView
    private lateinit var zAcclValue : TextView
    private lateinit var locViewModel : LocationViewModel
    var gyroVals = mutableListOf<Double>(.0,.0,.0)
    var acclVals = mutableListOf<Double>(.0,.0,.0)
    var magVals = mutableListOf<Double>(.0,.0,.0)

    private fun setupSensors() {
        sensorManagerGyro = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManagerMagnetometer = getSystemService(SENSOR_SERVICE) as SensorManager
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
        sensorManagerMagnetometer.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
            sensorManagerMagnetometer.registerListener(this,it,
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
            var gyroPitch = round(event.values[0]*10.0) / 10.0
            var gyroRoll = round(event.values[1]*10.0) / 10.0
            var gyroYaw = round(event.values[2]*10.0) / 10.0
            gyroVals
        }
        if(event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD){
            var magPitch = (round(event.values[1] * 10.0) / 10.0)
            var magRoll = (round(event.values[2]*10.0) / 10.0)
            var magYaw = (round(event.values[0]*10.0) / 10.0)
            magVals[0]=magYaw
            magVals[1]=magRoll
            magVals[2]=magPitch
            magVals
        }

            if(event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                /*
                    0->yaw
                    1->roll
                    2->pitch

                 */


                var accelPitch = (round(event.values[2] * 10.0) / 10.0)
                var accelRoll = (round(event.values[1]*10.0) / 10.0)
                var accelYaw = (round(event.values[0]*10.0) / 10.0)
                acclVals[0] = accelPitch
                acclVals[1] = accelRoll
                acclVals[2] = accelYaw
                var angles = AngleTransform(acclVals,gyroVals,magVals)
                angles

                rollIndicator.apply {
                    rotation = angles.roll.toFloat()
                    translationX = angles.roll.toFloat()


                }
                    horizonBackground.apply {
                    rotationX = (angles.pitch).toFloat()*0.1f
                    translationY = -(angles.pitch).toFloat()*2.0f

        }
                angles


                xAcclValue.text = (round(angles.pitch*10.0)/10.0).toString()
                yAcclValue.text = (round(angles.calculatedRoll*10.0)/10.0).toString()
                zAcclValue.text = (round(angles.magneticYaw*10.0)/10.0).toString()
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
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        xAcclValue = findViewById(R.id.tv_xAcclValue)
        yAcclValue = findViewById(R.id.tv_yAcclValue)
        zAcclValue = findViewById(R.id.tv_zAcclValue)

        horizonBackground=findViewById(R.id.img_backg)
        rollIndicator = findViewById(R.id.img_yawIndicator)



        setupSensors()

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