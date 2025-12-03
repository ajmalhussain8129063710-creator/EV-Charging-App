# Walkthrough - EV Charging Android App

## Prerequisites
-   **Android Studio**: You must have Android Studio installed.
-   **Firebase Account**: You need a Google account to access the Firebase Console.

## Setup Instructions

### 1. Open the Project
1.  Open **Android Studio**.
2.  Select **Open** and navigate to `e:\EV CHARGING APP`.
3.  Click **OK**.
4.  Android Studio will start syncing Gradle. This might take a few minutes as it downloads dependencies.

### 2. Firebase Setup (CRITICAL)
1.  Go to the [Firebase Console](https://console.firebase.google.com/).
2.  Create a new project (e.g., "EV Charging App").
3.  Add an Android App to the project:
    -   **Package Name**: `com.evcharging.app`
    -   **Debug Signing Certificate**: Optional for now.
4.  Download the `google-services.json` file.
5.  Move the `google-services.json` file into the `app/` folder of your project (`e:\EV CHARGING APP\app\google-services.json`).
6.  In Firebase Console, enable **Authentication**:
    -   Go to **Build** -> **Authentication**.
    -   Click **Get Started**.
    -   Enable **Email/Password** provider.
7.  In Firebase Console, enable **Firestore Database**:
    -   Go to **Build** -> **Firestore Database**.
    -   Click **Create Database**.
    -   Start in **Test Mode** (for development).

### 3. Run the App
1.  In Android Studio, select an Emulator or connect a physical device.
2.  Click the **Run** button (Green Play Icon).

## Verification Steps

### 1. Sign Up Flow
1.  The app should launch on the **Login Screen**.
2.  Click "Don't have an account? Sign Up".
3.  Fill in the details:
    -   **User Name**: Test User
    -   **Email**: test@example.com
    -   **Password**: password123
    -   **Car Model**: Tesla Model 3
    -   **Car Color**: Red
4.  Click **Sign Up**.
5.  **Expected Result**: You should be redirected to the Login Screen (as per your requirement).

### 2. Login Flow
1.  Enter the email (`test@example.com`) and password (`password123`).
2.  Click **Login**.
3.  **Expected Result**: You should be redirected to the **Home Screen**.
# Walkthrough - EV Charging Android App

## Prerequisites
-   **Android Studio**: You must have Android Studio installed.
-   **Firebase Account**: You need a Google account to access the Firebase Console.

## Setup Instructions

### 1. Open the Project
1.  Open **Android Studio**.
2.  Select **Open** and navigate to `e:\EV CHARGING APP`.
3.  Click **OK**.
4.  Android Studio will start syncing Gradle. This might take a few minutes as it downloads dependencies.

### 2. Firebase Setup (CRITICAL)
1.  Go to the [Firebase Console](https://console.firebase.google.com/).
2.  Create a new project (e.g., "EV Charging App").
3.  Add an Android App to the project:
    -   **Package Name**: `com.evcharging.app`
    -   **Debug Signing Certificate**: Optional for now.
4.  Download the `google-services.json` file.
5.  Move the `google-services.json` file into the `app/` folder of your project (`e:\EV CHARGING APP\app\google-services.json`).
6.  In Firebase Console, enable **Authentication**:
    -   Go to **Build** -> **Authentication**.
    -   Click **Get Started**.
    -   Enable **Email/Password** provider.
7.  In Firebase Console, enable **Firestore Database**:
    -   Go to **Build** -> **Firestore Database**.
    -   Click **Create Database**.
    -   Start in **Test Mode** (for development).

### 3. Run the App
1.  In Android Studio, select an Emulator or connect a physical device.
2.  Click the **Run** button (Green Play Icon).

## Verification Steps

### 1. Sign Up Flow
1.  The app should launch on the **Login Screen**.
2.  Click "Don't have an account? Sign Up".
3.  Fill in the details:
    -   **User Name**: Test User
    -   **Email**: test@example.com
    -   **Password**: password123
    -   **Car Model**: Tesla Model 3
    -   **Car Color**: Red
4.  Click **Sign Up**.
5.  **Expected Result**: You should be redirected to the Login Screen (as per your requirement).

### 2. Login Flow
1.  Enter the email (`test@example.com`) and password (`password123`).
2.  Click **Login**.
3.  **Expected Result**: You should be redirected to the **Home Screen**.

### 3. Navigation
1.  On the Home Screen, verify the Bottom Navigation Bar is visible.
2.  Click **Navigation**.
    -   **Expected Result**: "Navigation" screen appears.
3.  Click **Trip Planner**.
    -   Navigate between Home, Navigation, Trip Planner.
4.  Click **Home**.
    -   **Expected Result**: Back to Home Screen.

### 4. Advanced Features Verification

#### Google Maps & Charging Stations
1.  **API Key**: Ensure you have replaced `YOUR_API_KEY_HERE` in `app/src/main/AndroidManifest.xml` with a valid Google Maps API Key.
2.  Go to the **Navigation** tab.
3.  You should see a Google Map centered on Singapore (default).
4.  Look for **Red Markers** indicating "Station A", "Station B", etc.
5.  Click a marker to see "EV Charging Available".

#### Trip Planner
1.  Go to the **Trip Planner** tab.
2.  Enter "Home" as Start Location and "Office" as Destination.
3.  Click **Plan Trip**.
4.  **Expected Result**: A card appears showing:
    -   Distance: 150 km
    -   Est. Battery Usage: 45%
    -   Suggested Charging Stops.

#### AI Voice Assistant
1.  Click the **Microphone Button** (Floating Action Button).
2.  Grant **Microphone Permission** if asked.
3.  Tap the button again to start listening (Icon changes to Mic Off/Red).
4.  Say: "Plan trip to Changi Airport".
5.  **Expected Result**: The "Destination" field in Trip Planner should automatically fill with "Changi Airport" (if recognized).

## Troubleshooting
-   **Gradle Errors**: If you see Gradle errors, try **File -> Sync Project with Gradle Files**.
-   **Firebase Errors**: Ensure `google-services.json` is in the correct folder (`app/`) and the package name matches `com.evcharging.app`.
