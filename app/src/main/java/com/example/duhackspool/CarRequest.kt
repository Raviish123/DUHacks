package com.example.duhackspool

class CarRequest {
    var open: Boolean? = null
    var state: String? = null
    var driver: String? = null
    var dest: List<Float?> = listOf(null, null)
    var driverPos: List<Float?> = listOf(null, null)
    var distance: Float? = null
    var duration: Float? = null
    var clientPos: List<Float?> = listOf(null, null)
    var driverBearing: Float = 0F
    var totalDistance: Float? = null
}