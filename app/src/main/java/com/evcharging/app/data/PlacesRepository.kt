package com.evcharging.app.data

import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlacesRepository @Inject constructor(
    private val placesClient: PlacesClient
) {
    suspend fun searchPlaces(query: String, location: LatLng? = null): List<PlacePrediction> {
        val token = AutocompleteSessionToken.newInstance()
        val requestBuilder = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query)

        if (location != null) {
            // Bias to ~5km radius
            val bias = RectangularBounds.newInstance(
                LatLng(location.latitude - 0.05, location.longitude - 0.05),
                LatLng(location.latitude + 0.05, location.longitude + 0.05)
            )
            requestBuilder.setLocationBias(bias)
        }

        return try {
            val response = placesClient.findAutocompletePredictions(requestBuilder.build()).await()
            response.autocompletePredictions.map { 
                PlacePrediction(it.placeId, it.getPrimaryText(null).toString(), it.getSecondaryText(null).toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getPlaceDetails(placeId: String): Place? {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)
        return try {
            val response = placesClient.fetchPlace(request).await()
            response.place
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class PlacePrediction(val placeId: String, val primaryText: String, val secondaryText: String)
