package com.evcharging.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): com.google.firebase.storage.FirebaseStorage = com.google.firebase.storage.FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun providePlacesClient(@dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context): com.google.android.libraries.places.api.net.PlacesClient {
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
             try {
                val appInfo = context.packageManager.getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
                val apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY")
                if (!apiKey.isNullOrEmpty()) {
                    com.google.android.libraries.places.api.Places.initialize(context, apiKey)
                }
             } catch (e: Exception) {
                 e.printStackTrace()
             }
        }
        
        // Fallback: If still not initialized, try hardcoded key (mirrors EVChargingApp)
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            try {
                com.google.android.libraries.places.api.Places.initialize(context, "AIzaSyCuSXXGnpz5AP4XQZOl_udZIyiRUs1KGDs")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return com.google.android.libraries.places.api.Places.createClient(context)
    }
}
