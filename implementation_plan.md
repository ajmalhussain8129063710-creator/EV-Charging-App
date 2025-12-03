# Implementation Plan - EV Charging Android App

## Goal Description
Build a corporate-level Native Android Application for EV Charging.
The app will include:
-   **Authentication**: Login and Sign Up with Firebase.
-   **Sign Up Details**: Car Model, Car Color, User Name, Password.
-   **Main Interface**: Bottom Navigation with Home, Navigation, and Trip Planner.
-   **Tech Stack**: Kotlin, Jetpack Compose, Firebase.

## User Review Required
> [!IMPORTANT]
> **Environment**: Since `java` and `gradle` are not installed in the current shell, I will generate the source code. You **MUST** open the project in **Android Studio** to build and run it. Android Studio will handle the Gradle and Java environment.

> [!WARNING]
> **Firebase Setup**: You will need to create a Firebase project in the Firebase Console, enable Authentication and Firestore, and download the `google-services.json` file to the `app/` directory. I cannot do this for you.

## Proposed Changes

### Project Structure
I will create the following directory structure and files:

#### [NEW] Project Configuration
-   `build.gradle.kts` (Project level)
-   `settings.gradle.kts`
-   `app/build.gradle.kts` (App level)
-   `app/src/main/AndroidManifest.xml`

#### [NEW] Source Code (`app/src/main/java/com/evcharging/app/`)
-   `MainActivity.kt`: Entry point, sets up Navigation.
-   `EVChargingApp.kt`: Application class (Hilt setup).
-   **DI**: `di/AppModule.kt` (Firebase injection).
-   **Theme**: `ui/theme/` (Color, Type, Theme - Corporate Design).
-   **Auth**:
    -   `ui/auth/AuthViewModel.kt`
    -   `ui/auth/LoginScreen.kt`
    -   `ui/auth/SignUpScreen.kt`
    -   `data/AuthRepository.kt`
-   **Main**:
    -   `ui/home/HomeScreen.kt`
    -   `ui/navigation/NavigationScreen.kt` (Map placeholder)
    -   `ui/tripplanner/TripPlannerScreen.kt`
    -   `ui/components/BottomNavigationBar.kt`

### Dependencies
-   Jetpack Compose (UI)
-   Firebase Auth & Firestore
-   Hilt (Dependency Injection)
-   Navigation Compose
-   Material 3

## Verification Plan

### Manual Verification
1.  **Open in Android Studio**: Open the `e:\EV CHARGING APP` folder in Android Studio.
2.  **Sync Gradle**: Allow Android Studio to download dependencies.
3.  **Add Firebase**: Place `google-services.json` in `app/`.
4.  **Run**: Run the app on an Emulator or Device.
5.  **Test Flow**:
    -   Go to Sign Up.
    -   Enter details (Car Model, Color, etc.).
    -   Click Sign Up -> Should redirect to Login (or auto-login).
    -   Login -> Go to Home.
    -   Navigate between Home, Navigation, Trip Planner.

### [NEW] Advanced Features
#### [MODIFY] Build Configuration
-   Add `com.google.maps.android:maps-compose` dependency.
-   Add `com.google.android.gms:play-services-location`.

#### [MODIFY] Manifest
-   Add `ACCESS_FINE_LOCATION`, `RECORD_AUDIO` permissions.
-   Add `com.google.android.geo.API_KEY` meta-data.

#### [MODIFY] Navigation Screen
-   Integrate Google Maps View.
-   Show mock "EV Charging Stations" markers on the map.
-   Handle location permissions.

#### [MODIFY] Trip Planner Screen
-   Add inputs for "Start Location" and "Destination".
-   Calculate (mock) distance and battery usage.
-   Show charging stops along the route.

#### [NEW] Voice Assistant
-   `ui/components/VoiceAssistantButton.kt`: Floating button for voice commands.
-   `util/VoiceRecognitionManager.kt`: Wrapper around Android `SpeechRecognizer`.
-   Handle commands: "Navigate to [Place]", "Plan trip to [Place]".
