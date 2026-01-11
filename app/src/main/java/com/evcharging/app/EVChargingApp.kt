package com.evcharging.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import com.google.android.libraries.places.api.Places

@HiltAndroidApp
class EVChargingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize the SDK
        if (!Places.isInitialized()) {
            try {
                Places.initialize(applicationContext, "AIzaSyCuSXXGnpz5AP4XQZOl_udZIyiRUs1KGDs")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
