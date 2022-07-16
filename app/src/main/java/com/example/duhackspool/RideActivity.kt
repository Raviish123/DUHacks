package com.example.duhackspool

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import com.example.duhackspool.databinding.ActivityRideBinding
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.ui.utils.internal.extensions.getBitmap
import java.lang.ref.WeakReference

class RideActivity : AppCompatActivity() {

    private var requestIndex = -1

    private var isFirstPos: Boolean = true

    private lateinit var binding: ActivityRideBinding

    private lateinit var mapboxMap: MapboxMap

    private lateinit var destination: Point

    private val annotationApi by lazy {
        binding.mapView.annotations
    }

    private val pointAnnotationManager by lazy {
        annotationApi.createPointAnnotationManager()
    }

    private val pointDrawable by lazy {
        AppCompatResources.getDrawable(this@RideActivity, R.drawable.red_marker)
    }

    private lateinit var locationPermissionHelper: LocationPermissionHelper

    private lateinit var clientInitPos: Point

    private var mapClickListener = OnMapLongClickListener { point ->
        destination = point
        binding.setDest.isEnabled = true
        val pointAnnotationOptions: PointAnnotationOptions? = pointDrawable?.let { it1 ->
            PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(it1.getBitmap())
        }
        pointAnnotationManager.deleteAll()
        if (pointAnnotationOptions != null) {
            pointAnnotationManager.create(pointAnnotationOptions)
        }
        true
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        if (isFirstPos) {
            isFirstPos = false
            clientInitPos = it
            Log.d("asdf",clientInitPos.toString())
            mapboxMap.setCamera(CameraOptions.Builder().center(it).zoom(17.0).build())
            binding.setDest.setOnClickListener {
                if (this::destination.isInitialized) {
                    if (destination != null) {
                        binding.setDest.isEnabled = false
                        binding.mapView.gestures.removeOnMapLongClickListener(mapClickListener)
                        val httpAsync = "http://192.168.0.122:8000/request/${destination.longitude()}/${destination.latitude()}/${clientInitPos.longitude()}/${clientInitPos.latitude()}"
                            .httpGet()
                            .responseString { _, _, result ->
                                when (result) {
                                    is Result.Failure -> {
                                        Log.d("Err",result.getException().toString())
                                    }
                                    is Result.Success -> {
                                        requestIndex = result.get().toInt()
                                        setLayout(1)
                                    }
                                }
                            }
                        httpAsync.join()
                    }

                }

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

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
        binding.setDest.isEnabled = false

        setLayout(0)

        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
            binding.mapView.location.updateSettings {
                enabled = true
                pulsingEnabled = true
            }
            binding.mapView.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            binding.mapView.gestures.addOnMapLongClickListener(mapClickListener)

        }

    }

    private fun setLayout(indx: Int) {
        for (layout in listOf(binding.chooseDestLayout, binding.requestSentLayout)) {
            layout.visibility = View.GONE
        }
        when (indx) {
            0 -> binding.chooseDestLayout.visibility = View.VISIBLE
            1 -> binding.requestSentLayout.visibility = View.VISIBLE
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