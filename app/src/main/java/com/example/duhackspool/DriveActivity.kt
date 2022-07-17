package com.example.duhackspool

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.example.duhackspool.databinding.ActivityDriveBinding
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.*
import com.mapbox.navigation.ui.utils.internal.extensions.getBitmap
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

class DriveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDriveBinding

    private lateinit var mapboxMap: MapboxMap

    private var totalDistance: Float = -1F

    private val navigationLocationProvider = NavigationLocationProvider()

    private var requestIndex = -1

    private lateinit var mapboxNavigation: MapboxNavigation

    private lateinit var navigationCamera: NavigationCamera

    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    private lateinit var locationPermissionHelper: LocationPermissionHelper

    private val pxDensity = Resources.getSystem().displayMetrics.density

    private val overviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            140.0 * pxDensity,
            40.0 * pxDensity,
            120.0 * pxDensity,
            40.0 * pxDensity
        )
    }

    private val landscapeOverviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pxDensity,
            380.0 * pxDensity,
            110.0 * pxDensity,
            20.0 * pxDensity
        )
    }

    private val followingPadding: EdgeInsets by lazy {
        EdgeInsets(
            180.0 * pxDensity,
            40.0 * pxDensity,
            150.0 * pxDensity,
            40.0 * pxDensity
        )
    }

    private val landscapeFollowingPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pxDensity,
            380.0 * pxDensity,
            110.0 * pxDensity,
            40.0 * pxDensity
        )
    }

    private lateinit var cRouteProgress: RouteProgress

    private var cState: String = "toClient"

    private lateinit var routeLineApi: MapboxRouteLineApi

    private lateinit var routeLineView: MapboxRouteLineView

    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi()

    private lateinit var routeArrowView: MapboxRouteArrowView

    private lateinit var maneuverApi: MapboxManeuverApi

    private lateinit var tripProgressApi: MapboxTripProgressApi

    private val arrivalObserver = object : ArrivalObserver {
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            if (cState == "toClient") {
                cState = "waitingForClient"
                binding.maneuverView.visibility = View.INVISIBLE
                setCard(2)
                mapboxNavigation.stopTripSession()
            }
            if (cState == "toDestination") {
                cState = "arrived"
                mapboxNavigation.stopTripSession()
                val paymentAmount = (((20.0 + (totalDistance / 100.0)) * 100.0).roundToInt()) / 100.0
                Log.d("ADSFFDF", paymentAmount.toString())
                var intent = Intent(this@DriveActivity, PaymentActivity::class.java)
                intent.putExtra("paymentAmount", paymentAmount)
                intent.putExtra("isDriver", true)
                startActivity(intent)
                finish()

            }
            mapboxNavigation.stopTripSession()
        }

        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {  }

        override fun onWaypointArrival(routeProgress: RouteProgress) {  }

    }

    private val routesObserver = RoutesObserver { result ->
        if (result.navigationRoutes.isNotEmpty()) {
            val routeLines = result.navigationRoutes.map { NavigationRouteLine(it, null) }

            routeLineApi.setNavigationRouteLines(routeLines) { value ->
                mapboxMap.getStyle()?.let {
                    routeLineView.renderRouteDrawData(it, value)
                }
            }

            viewportDataSource.onRouteChanged(result.navigationRoutes.first())
            viewportDataSource.evaluate()

        } else {

            val style = mapboxMap.getStyle()

            if(style != null) {
                routeLineApi.clearRouteLine { value ->
                    routeLineView.renderClearRouteLineValue(style, value)
                }

                routeArrowView.render(style, routeArrowApi.clearArrows())
            }

            viewportDataSource.clearRouteData()
            viewportDataSource.evaluate()
        }
    }

    @SuppressLint("SetTextI18n")
    private val routeProgressObserver = RouteProgressObserver { routeProgress ->

        cRouteProgress = routeProgress

        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        val style = mapboxMap.getStyle()
        if (style != null) {
            routeArrowView.renderManeuverUpdate(style, routeArrowApi.addUpcomingManeuverArrow(routeProgress))
        }

        val maneuvers = maneuverApi.getManeuvers(routeProgress)
        maneuvers.fold(
            { error ->
                Log.e("Error", error.errorMessage.toString())
            },
            {
                binding.maneuverView.visibility = View.VISIBLE
                binding.maneuverView.renderManeuvers(maneuvers)
            }
        )
        binding.tripProgressView.render(
            tripProgressApi.getTripProgress(routeProgress)
        )
    }

    private val pointAnnotationManager by lazy {
        annotationApi.createPointAnnotationManager()
    }

    private val fDrawable by lazy {
        AppCompatResources.getDrawable(this@DriveActivity, R.drawable.red_marker)
    }

    private val humanDrawable by lazy {
        AppCompatResources.getDrawable(this@DriveActivity, android.R.drawable.presence_online)
    }

    private var pointAnnotationToRequests = mutableMapOf<PointAnnotation, Pair<Int, CarRequest>>()

    private lateinit var tmpCReq: Pair<Int, CarRequest>

    private lateinit var mainHandler: Handler

    private val locationObserver = object : LocationObserver {
        var initPos = false

        override fun onNewRawLocation(rawLocation: Location) {

        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {

            navigationLocationProvider.changePosition(
                location = locationMatcherResult.enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints
            )

            viewportDataSource.onLocationChanged(locationMatcherResult.enhancedLocation)
            viewportDataSource.evaluate()

            if (!initPos) {
                initPos = true
                navigationCamera.requestNavigationCameraToOverview(
                    stateTransitionOptions = NavigationCameraTransitionOptions.Builder()
                        .maxDuration(0)
                        .build()
                )
            }

        }
    }

    private val updatePosToClientTask = object : Runnable {
        override fun run() {
            updatePosToClient()
            mainHandler.postDelayed(this, 3000)
        }
    }

    private val updatePosToDestTask = object : Runnable {
        override fun run() {
            updatePosToDest()
            mainHandler.postDelayed(this, 3000)
        }
    }

    private lateinit var destAnnotation: PointAnnotation

    private var selectedPoint: PointAnnotation? = null

    private val annotationApi by lazy {
        binding.driverMapView.annotations
    }

    private val refreshRequestsTask = object : Runnable {
        override fun run() {
            refreshRequests()
            mainHandler.postDelayed(this, 3000)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        binding = ActivityDriveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
        locationPermissionHelper.checkPermissions {

        }

        mapboxMap = binding.driverMapView.getMapboxMap()

        binding.driverMapView.location.apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(this@DriveActivity, com.mapbox.navigation.ui.maps.R.drawable.mapbox_navigation_puck_icon)
            )
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }

        mapboxNavigation = if (MapboxNavigationProvider.isCreated()) {
            MapboxNavigationProvider.retrieve()
        } else {
            MapboxNavigationProvider.create(
                NavigationOptions.Builder(this.applicationContext)
                    .accessToken(getString(R.string.mapbox_access_token))
                    .build()
            )
        }

        maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(mapboxNavigation.navigationOptions.distanceFormatterOptions)
        )

        tripProgressApi = MapboxTripProgressApi(
            TripProgressUpdateFormatter.Builder(this)
                .distanceRemainingFormatter(DistanceRemainingFormatter(mapboxNavigation.navigationOptions.distanceFormatterOptions))
                .timeRemainingFormatter(TimeRemainingFormatter(this))
                .percentRouteTraveledFormatter(PercentDistanceTraveledFormatter())
                .estimatedTimeToArrivalFormatter(EstimatedTimeToArrivalFormatter(this, TimeFormat.NONE_SPECIFIED))
                .build()
        )

        val routeLineOptions = MapboxRouteLineOptions.Builder(this)
            .withRouteLineBelowLayerId("road-label")
            .build()

        routeLineApi = MapboxRouteLineApi(routeLineOptions)
        routeLineView = MapboxRouteLineView(routeLineOptions)

        routeArrowView = MapboxRouteArrowView(RouteArrowOptions.Builder(this).build())

        viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap)
        navigationCamera = NavigationCamera(mapboxMap, binding.driverMapView.camera, viewportDataSource)

        binding.driverMapView.camera.addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )

        if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.overviewPadding = landscapeOverviewPadding
            viewportDataSource.followingPadding = landscapeFollowingPadding
        } else {
            viewportDataSource.overviewPadding = overviewPadding
            viewportDataSource.followingPadding = followingPadding
        }

        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS)

        binding.driverMapView.gestures.pitchEnabled = false
        binding.driverMapView.compass.enabled = false


        pointAnnotationManager.addClickListener(OnPointAnnotationClickListener { annotation ->
            if (selectedPoint != null) {
                pointAnnotationManager.delete(destAnnotation)
                if (selectedPoint == annotation) {
                    selectedPoint = null
                    binding.claimButton.isEnabled = false
                    return@OnPointAnnotationClickListener true
                }
            }
            setCard(0)
            binding.claimButton.isEnabled = false
            selectedPoint = annotation

            tmpCReq = pointAnnotationToRequests[annotation]!!

            val cRequest = pointAnnotationToRequests[annotation]!!.second

            val destPoint = Point.fromLngLat(cRequest.dest[0]!!.toDouble(), cRequest.dest[1]!!.toDouble())

            val pointAnnotationOptions: PointAnnotationOptions? = fDrawable?.let { it1 ->
                PointAnnotationOptions()
                    .withPoint(destPoint)
                    .withIconImage(it1.getBitmap())
                    .withIconSize(1.25)
            }

            if (pointAnnotationOptions != null) {
                val originPos = navigationLocationProvider.lastLocation ?: return@OnPointAnnotationClickListener true
                val originPoint = Point.fromLngLat(originPos.longitude, originPos.latitude)

                val clientPoint = Point.fromLngLat(cRequest.clientPos[0]!!.toDouble(), cRequest.clientPos[1]!!.toDouble())

                mapboxNavigation.requestRoutes(
                    RouteOptions.builder()
                        .applyDefaultNavigationOptions()
                        .coordinatesList(listOf(originPoint, clientPoint, destPoint))
                        .bearingsList(
                            listOf(
                                Bearing.builder()
                                    .angle(originPos.bearing.toDouble())
                                    .degrees(45.0)
                                    .build(),
                                null,
                                null
                            )
                        )
                        .build(),
                    object : NavigationRouterCallback {

                        override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: RouterOrigin) {
                            viewportDataSource.onRouteChanged(routes[0])
                            viewportDataSource.evaluate()
                            navigationCamera.requestNavigationCameraToOverview()
                            binding.claimButton.isEnabled = true
                            binding.claimButton.setOnClickListener {
                                binding.claimButton.isEnabled = false
                                mainHandler.removeCallbacks(refreshRequestsTask)
                                pointAnnotationManager.deleteAll()
                                startNavToClient(tmpCReq)
                            }
                        }

                        override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {   }

                        override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {  }
                    }
                )
                destAnnotation = pointAnnotationManager.create(pointAnnotationOptions)
            }




            true
        })


        mapboxNavigation.startTripSession()

        mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(refreshRequestsTask)


    }

    private fun startNavToClient(data: Pair<Int, CarRequest>) {
        val indx = data.first
        val cRequest = data.second

        requestIndex = indx

        val originPos = navigationLocationProvider.lastLocation ?: return
        val originPoint = Point.fromLngLat(originPos.longitude, originPos.latitude)

        val clientPoint = Point.fromLngLat(cRequest.clientPos[0]!!.toDouble(), cRequest.clientPos[1]!!.toDouble())

        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .coordinatesList(listOf(originPoint, clientPoint))
                .bearingsList(
                    listOf(
                        Bearing.builder()
                            .angle(originPos.bearing.toDouble())
                            .degrees(45.0)
                            .build(),
                        null
                    )
                )
                .build(),
            object : NavigationRouterCallback {

                override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: RouterOrigin) {

                    mapboxNavigation.setNavigationRoutes(routes)

                    navigationCamera.requestNavigationCameraToFollowing()

                    mainHandler.postDelayed(updatePosToClientTask, 3000)

                    setCard(1)


                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {   }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {  }
            }
        )
    }

    @SuppressLint("MissingPermission")
    private fun updatePosToClient() {
        val pos = navigationLocationProvider.lastLocation ?: return
        // Use account information for driver name
        val httpAsync = "http://192.168.0.122:8000/claim_request/${requestIndex}/${cRouteProgress.distanceRemaining}/${cRouteProgress.durationRemaining}/${pos.longitude}/${pos.latitude}/Raviish/${cState}/${pos.bearing}/-1"
            .httpGet()
            .responseJson { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        Log.e("Error", result.getException().toString())
                    }
                    is Result.Success -> {
                        val carRequest: CarRequest = Gson().fromJson(result.get().obj().toString(), CarRequest::class.java)
                        if (carRequest.state == "toDestination") {
                            mainHandler.removeCallbacks(updatePosToClientTask)
                            mapboxNavigation.startTripSession()
                            startNavToDest(carRequest)
                        }
                    }
                }
            }
        httpAsync.join()
    }

    private fun startNavToDest(cReq: CarRequest) {
        val originPos = navigationLocationProvider.lastLocation ?: return
        val originPoint = Point.fromLngLat(originPos.longitude, originPos.latitude)

        val destPoint = Point.fromLngLat(cReq.dest[0]!!.toDouble(), cReq.dest[1]!!.toDouble())

        mainHandler.removeCallbacks(updatePosToClientTask)

        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .coordinatesList(listOf(originPoint, destPoint))
                .bearingsList(
                    listOf(
                        Bearing.builder()
                            .angle(originPos.bearing.toDouble())
                            .degrees(45.0)
                            .build(),
                        null
                    )
                )
                .build(),
            object : NavigationRouterCallback {

                override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: RouterOrigin) {

                    cState = "toDestination"

                    mapboxNavigation.setNavigationRoutes(routes)

                    navigationCamera.requestNavigationCameraToFollowing()

                    totalDistance = routes[0].directionsRoute.distance().toFloat()

                    setCard(1)

                    mainHandler.removeCallbacks(updatePosToClientTask)
                    mainHandler.postDelayed(updatePosToDestTask, 3000)


                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {   }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {  }
            }
        )
    }

    private fun updatePosToDest() {
        val pos = navigationLocationProvider.lastLocation ?: return
        // Use account information for driver name
        val httpAsync = "http://192.168.0.122:8000/claim_request/${requestIndex}/${cRouteProgress.distanceRemaining}/${cRouteProgress.durationRemaining}/${pos.longitude}/${pos.latitude}/Raviish/${cState}/${pos.bearing}/${totalDistance}"
            .httpGet()
            .responseJson { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        Log.e("Error", result.getException().toString())
                    }
                    is Result.Success -> {
                        val carRequest: CarRequest = Gson().fromJson(result.get().obj().toString(), CarRequest::class.java)
                        if (carRequest.state == "arrived") {
                            mainHandler.removeCallbacks(updatePosToDestTask)
                        } else if (carRequest.state == "waitingForClient") {
                            cState = "toDestination"
                        }
                    }
                }
            }
        httpAsync.join()
    }


    private fun setCard(indx: Int) {
        for (card in listOf(binding.pointView, binding.tripProgressCard, binding.waitingView)) {
            card.visibility = View.GONE
        }
        when (indx) {
            0 -> binding.pointView.visibility = View.VISIBLE
            1 -> binding.tripProgressCard.visibility = View.VISIBLE
            2 -> binding.waitingView.visibility = View.VISIBLE
        }
    }


    private fun refreshRequests() {
        val httpAsync = "http://192.168.0.122:8000/get_requests"
            .httpGet()
            .responseJson { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        Log.d("Err",result.getException().toString())
                    }
                    is Result.Success -> {
                        val tmpAnnToReq = mutableMapOf<PointAnnotation, Pair<Int, CarRequest>>()
                        val jsonObj = result.get().obj()
                        for (i in pointAnnotationToRequests.keys) {
                            pointAnnotationManager.delete(i)
                        }
                        for (i in jsonObj.keys()) {
                            val carRequest: CarRequest = Gson().fromJson(jsonObj[i].toString(), CarRequest::class.java)

                            val pointAnnotationOptions: PointAnnotationOptions? = humanDrawable?.let { it1 ->
                                PointAnnotationOptions()
                                    .withPoint(Point.fromLngLat(carRequest.clientPos[0]!!.toDouble(),
                                        carRequest.clientPos[1]!!.toDouble()
                                    ))
                                    .withIconImage(it1.getBitmap())
                                    .withIconSize(2.25)
                            }

                            if (pointAnnotationOptions != null) {
                                val pao = pointAnnotationManager.create(pointAnnotationOptions)
                                tmpAnnToReq[pao] = Pair(i.toInt(), carRequest)
                            }
                        }
                        pointAnnotationToRequests = tmpAnnToReq
                    }
                }
            }
        httpAsync.join()
    }


    override fun onStart() {
        super.onStart()
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerArrivalObserver(arrivalObserver)
    }

    override fun onStop() {
        super.onStop()
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterArrivalObserver(arrivalObserver)

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