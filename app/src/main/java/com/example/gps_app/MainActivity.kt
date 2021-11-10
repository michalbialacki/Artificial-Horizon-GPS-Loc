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
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener{
    private lateinit var mMap: GoogleMap
    private val NS2S = 1.0f / 1000000000.0f
    private val deltaRotationVector = FloatArray(4) { 0f }
    private var timestamp: Float = 0f
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationrequest: LocationRequest
    private lateinit var sensorManager : SensorManager
    private lateinit var horizonBackground : ImageView
    private lateinit var yawIndicator : ImageView
    private lateinit var locViewModel : LocationViewModel


    private fun setUpSensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this,
                it,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_FASTEST)

        }
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER){
            var yaw = event?.values[0]
            var pitch = (event?.values[1]-9.81f)
            var thirdAccelerometerValue = (event.values[2])

            horizonBackground.apply {

//                if (pitch>0f && thirdAccelerometerValue >0f ){
//                    rotationX = (pitch)
//                    //rotationY = (yaw)
//                    //rotation = (-yaw)
//                    //translationX = (yaw * -10)
//                    translationY = ((pitch) * -12f)
//                    Log.e(TAG, "onSensorChanged: ${pitch}", )
//                }
                if ((thirdAccelerometerValue >=0f) ){
                    rotationX = (pitch)
                    //rotationY = (yaw)
                    //rotation = (-yaw)
                    //translationX = (yaw * -10)
                    translationY = 9.81f-((-pitch) * 12f)
                    Log.e(TAG, "onSensorChanged: ${thirdAccelerometerValue}", )
                }
                else{
                    rotationX = (pitch)
                    //rotationY = (yaw)
                    //rotation = (-yaw)
                    //translationX = (yaw * -10)
                    translationY = ((-pitch) * 40f)
                    Log.e(TAG, "onSensorChanged: ${thirdAccelerometerValue}", )
                }


            }

            yawIndicator.apply {
                rotationY = (yaw)
                rotation = (-yaw)
                translationX = (yaw * -15)

            }

        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.


        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)



        horizonBackground=findViewById(R.id.img_backg)
        yawIndicator = findViewById(R.id.img_yawIndicator)
        // Get the SupportMapFragment and request notification when the map is ready to be used.



        setUpSensor()
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
        var latitude : Double = 0.0
        var longtitude : Double = 0.0
        locViewModel = ViewModelProvider(this).get(LocationViewModel::class.java)
        localizationDeclaration()
        updateCurrentLoc()
        //getUserLocation()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        locViewModel.getLocationData().observe(this, {
            mMap.clear()
            val airVehiclePosition = LatLng(it.latitude, it.longitude)
            mMap.addMarker(MarkerOptions().position(airVehiclePosition).title("Your Aircraft"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(airVehiclePosition,10f))

        })

        // Add a marker in Sydney and move the camera

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

    private fun getAddress (Lat : Double, Long: Double) : String {
        var cityName = ""
        var geocoder = Geocoder(applicationContext, Locale.getDefault())
        var address : MutableList<Address> = geocoder.getFromLocation(Lat,Long,1)
        cityName = address.get(0).getAddressLine(0)
        return cityName
    }
}