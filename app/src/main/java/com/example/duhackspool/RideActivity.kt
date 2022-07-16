package com.example.duhackspool

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.duhackspool.databinding.ActivityRideBinding
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import java.lang.ref.WeakReference

class RideActivity : AppCompatActivity() {

    private var isFirstPos: Boolean = true

    private lateinit var binding: ActivityRideBinding

    private lateinit var mapboxMap: MapboxMap

    private lateinit var locationPermissionHelper: LocationPermissionHelper

    private lateinit var clientInitPos: Point

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        if (isFirstPos) {
            isFirstPos = false
            clientInitPos = it
            Log.d("asdf",clientInitPos.toString())
            mapboxMap.setCamera(CameraOptions.Builder().center(it).zoom(17.0).build())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
        locationPermissionHelper.checkPermissions {

        }

        mapboxMap = binding.mapView.getMapboxMap()
        binding.mapView.compass.enabled = false
        binding.mapView.gestures.pitchEnabled = false

        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
            binding.mapView.location.updateSettings {
                enabled = true
                pulsingEnabled = true
            }
            binding.mapView.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionsResult(requestCode,
            permissions as Array<String>, grantResults)
    }
}