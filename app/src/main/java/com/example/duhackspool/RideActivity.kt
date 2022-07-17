package com.example.duhackspool

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import com.example.duhackspool.databinding.ActivityRideBinding
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.ui.utils.internal.extensions.getBitmap
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

class RideActivity : AppCompatActivity() {

    private var requestIndex = -1

    private var isFirstPos: Boolean = true

    private lateinit var binding: ActivityRideBinding

    private lateinit var mapboxMap: MapboxMap

    private lateinit var carAnnotation: PointAnnotation

    private var totalDistance = -1F

    private lateinit var destination: Point

    private var acceptReached = 0

    private val annotationApi by lazy {
        binding.mapView.annotations
    }

    private val pointAnnotationManager by lazy {
        annotationApi.createPointAnnotationManager()
    }

    private val carDrawable by lazy {
        AppCompatResources.getDrawable(this@RideActivity, R.drawable.car)
    }

    private val pointDrawable by lazy {
        AppCompatResources.getDrawable(this@RideActivity, R.drawable.red_marker)
    }

    private lateinit var locationPermissionHelper: LocationPermissionHelper

    private lateinit var mainHandler: Handler

    private val refreshRequestsTask = object : Runnable {
        override fun run() {
            refreshRequest()
            mainHandler.postDelayed(this, 2500)
        }
    }

    private lateinit var clientInitPos: Point

    private var mapClickListener = OnMapLongClickListener { point ->
        destination = point
        binding.setDest.isEnabled = true
        val pointAnnotationOptions: PointAnnotationOptions? = pointDrawable?.let { it1 ->
            PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(it1.getBitmap())
        }

        "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/${clientInitPos.longitude()},${clientInitPos.latitude()};${destination.longitude()},${destination.latitude()}?access_token=pk.eyJ1IjoidHJ1Y2ttYXBwZXIiLCJhIjoiY2w0NTNqbXByMDR1OTNrcDUyNmoxNGR2bCJ9.WMfnWG6aDMGRJ5MbEbSJIQ"
            .httpGet()
            .responseJson { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        Log.d("Errr", result.getException().toString())
                    }
                    is Result.Success -> {
                        val dist = result.get().obj().getJSONArray("routes").getJSONObject(0).getDouble("distance")
                        binding.textView20.text = "Estimated Pool Price: ${(((20.0 + (dist / 100.0)) * 100.0).roundToInt()) / 100.0}"
                    }
                }
            }
            .join()
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
                                        mainHandler.post(refreshRequestsTask)
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

//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

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

        binding.textView20.text = ""

        setLayout(0)

        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
            binding.mapView.location.updateSettings {
                enabled = true
                pulsingEnabled = true
            }
            binding.mapView.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            binding.mapView.gestures.addOnMapLongClickListener(mapClickListener)

        }

        mainHandler = Handler(Looper.getMainLooper())

    }

    private fun refreshRequest() {
        val tmpAcceptReached = acceptReached

        if (tmpAcceptReached == 1) {
            acceptReached = 0
        }

        val httpAsync = "http://192.168.0.122:8000/get_request/${requestIndex}/${tmpAcceptReached}"
            .httpGet()
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        Log.d("Err",result.getException().toString())
                    }
                    is Result.Success -> {
                        val carRequestString = result.get().toString()
                        Log.d("asdf", carRequestString)
                        evaluateRequest(carRequestString)
                    }
                }
            }
        httpAsync.join()

    }

    @SuppressLint("SetTextI18n")
    private fun evaluateRequest(request: String) {
        val carRequest = Gson().fromJson(request, CarRequest::class.java)

        when (carRequest.state) {
            "notClaimed" -> {
                setLayout(1)
            }
            "toClient" -> {
                setLayout(2)
                binding.textView6.text = "Your Driver: ${carRequest.driver}"
                binding.textView8.text = "${carRequest.duration?.div(60)!!.roundToInt()} min"
                binding.textView9.text = "${carRequest.distance?.div(10)!!.roundToInt() / 100.0} km"


                var isCarInit = false

                if (this::carAnnotation.isInitialized) {
                    isCarInit = true
                }

                if (isCarInit) {
                    if (carAnnotation.point.longitude() == carRequest.driverPos[0]!!.toDouble() && carAnnotation.point.latitude() == carRequest.driverPos[1]!!.toDouble() && carAnnotation.iconRotate == carRequest.driverBearing.toDouble()) return
                    pointAnnotationManager.delete(carAnnotation)
                }

                val pointAnnotationOptions: PointAnnotationOptions? = carDrawable?.let { it1 ->
                    PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(carRequest.driverPos[0]!!.toDouble(),
                            carRequest.driverPos[1]!!.toDouble()
                        ))
                        .withIconImage(it1.getBitmap())
                        .withIconRotate(carRequest.driverBearing.toDouble())
                        .withIconSize(0.5)
                }


                if (pointAnnotationOptions != null) {
                    carAnnotation = pointAnnotationManager.create(pointAnnotationOptions)
                }

            }
            "waitingForClient" -> {
                setLayout(3)
                binding.confirmBtn.setOnClickListener {
                    acceptReached = 1
                    binding.confirmBtn.isEnabled = false
                }
            }
            "toDestination" -> {
                setLayout(4)
                binding.textView18.text = "${carRequest.duration?.div(60)!!.roundToInt()} min"
                binding.textView19.text = "${carRequest.distance?.div(10)!!.roundToInt() / 100.0} km"
                binding.mapView.location.updateSettings {
                    enabled = false
                    pulsingEnabled = false
                }
                binding.mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
                if (totalDistance == -1F) totalDistance = carRequest.totalDistance!!

                if (carAnnotation.point.longitude() == carRequest.driverPos[0]!!.toDouble() && carAnnotation.point.latitude() == carRequest.driverPos[1]!!.toDouble() && carAnnotation.iconRotate == carRequest.driverBearing.toDouble()) return
                pointAnnotationManager.delete(carAnnotation)

                val pointAnnotationOptions: PointAnnotationOptions? = carDrawable?.let { it1 ->
                    PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(carRequest.driverPos[0]!!.toDouble(),
                            carRequest.driverPos[1]!!.toDouble()
                        ))
                        .withIconImage(it1.getBitmap())
                        .withIconRotate(carRequest.driverBearing.toDouble())
                        .withIconSize(0.5)
                }


                if (pointAnnotationOptions != null) {
                    carAnnotation = pointAnnotationManager.create(pointAnnotationOptions)
                }
            }
            "arrived" -> {
                mainHandler.removeCallbacks(refreshRequestsTask)
                if (totalDistance == -1F) totalDistance = carRequest.totalDistance!!
                val paymentAmount = (((20.0 + (totalDistance / 100.0)) * 100.0).roundToInt()) / 100.0
                Log.d("ADSFFDF", paymentAmount.toString())
                var intent = Intent(this@RideActivity, PaymentActivity::class.java)
                intent.putExtra("paymentAmount", paymentAmount)
                intent.putExtra("isDriver", false)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()


    }

    override fun onStop() {
        super.onStop()

        mainHandler.removeCallbacks(refreshRequestsTask)
    }


    override fun onPause() {
        super.onPause()

        "http://192.168.0.122:8000/deleteRequest/${requestIndex}"
            .httpGet()
    }


    private fun setLayout(indx: Int) {
        for (layout in listOf(binding.chooseDestLayout, binding.requestSentLayout, binding.driverComingLayout, binding.waitingForClientLayout, binding.toDestLayout)) {
            layout.visibility = View.GONE
        }
        when (indx) {
            0 -> binding.chooseDestLayout.visibility = View.VISIBLE
            1 -> binding.requestSentLayout.visibility = View.VISIBLE
            2 -> binding.driverComingLayout.visibility = View.VISIBLE
            3 -> binding.waitingForClientLayout.visibility = View.VISIBLE
            4 -> binding.toDestLayout.visibility = View.VISIBLE
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}