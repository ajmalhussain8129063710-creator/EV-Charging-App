package com.evcharging.app.ui.components

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evcharging.app.data.BookingRepository
import com.evcharging.app.data.StationRepository
import com.evcharging.app.data.model.Station
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class VoiceAssistantViewModel @Inject constructor(
    application: Application,
    private val stationRepository: StationRepository,
    private val bookingRepository: BookingRepository,
    private val placesClient: PlacesClient
) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val _voiceResponse = MutableStateFlow<String?>(null)
    val voiceResponse: StateFlow<String?> = _voiceResponse.asStateFlow()

    // Conversation State
    private enum class ConversationState {
        IDLE,
        TRIP_PLANNING_CONFIRMATION,
        SELECTING_STATION,
        CONFIRMING_BOOKING,
        ENTERING_PIN
    }

    private var currentState = ConversationState.IDLE
    private var lastFoundStations: List<Station> = emptyList()
    private var selectedStation: Station? = null
    private var tempTripDetails: Pair<String, String>? = null // Source, Destination

    init {
        tts = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    fun processCommand(command: String) {
        val lowerCommand = command.lowercase()
        viewModelScope.launch {
            when (currentState) {
                ConversationState.IDLE -> handleIdleCommand(lowerCommand, command)
                ConversationState.SELECTING_STATION -> handleStationSelection(lowerCommand)
                ConversationState.CONFIRMING_BOOKING -> handleBookingConfirmation(lowerCommand)
                ConversationState.ENTERING_PIN -> handlePinEntry(lowerCommand)
                ConversationState.TRIP_PLANNING_CONFIRMATION -> handleTripConfirmation(lowerCommand)
            }
        }
    }

    private suspend fun handleIdleCommand(lowerCommand: String, originalCommand: String) {
        when {
            // Case 1: Trip Planning "Book Calicut to Delhi"
            lowerCommand.contains("book") && lowerCommand.contains("to") -> {
                // simple regex extraction
                val parts = originalCommand.split(" to ", ignoreCase = true)
                if (parts.size == 2) {
                    val source = parts[0].replace("book", "", true).trim()
                    val destination = parts[1].trim()
                    tempTripDetails = source to destination
                    speak("I found a route from $source to $destination. Should I map this trip for you?")
                    currentState = ConversationState.TRIP_PLANNING_CONFIRMATION
                } else {
                    speak("I didn't quite catch the locations. Please say 'Book location A to location B'.")
                }
            }
            // Case 2: "Book nearest charging station"
            lowerCommand.contains("nearest") && lowerCommand.contains("station") -> {
                speak("Searching for nearest charging stations...")
                val result = stationRepository.getStationsNear(21.1458, 79.0882, 10.0) // Mock location
                if (result.isSuccess) {
                    val stations = result.getOrDefault(emptyList()).take(3)
                    if (stations.isNotEmpty()) {
                        lastFoundStations = stations
                        val stationNames = stations.mapIndexed { index, s -> "${index + 1}: ${s.name}" }.joinToString(", ")
                        speak("I found specific stations nearby: $stationNames. Which one would you like to book?")
                        currentState = ConversationState.SELECTING_STATION
                    } else {
                        speak("I couldn't find any stations nearby.")
                    }
                } else {
                    speak("There was an error finding stations.")
                }
            }
            else -> speak("I mostly understand trip planning and booking commands. Try saying 'Book Calicut to Delhi'.")
        }
    }

    private fun handleTripConfirmation(command: String) {
        if (command.contains("yes") || command.contains("sure") || command.contains("fix") || command.contains("map")) {
            speak("Okay, mapping the route from ${tempTripDetails?.first} to ${tempTripDetails?.second}. Showing compatible charging stations along the way.")
            // Trigger Navigation Event (Screen should observe this)
             _voiceResponse.value = "NAVIGATE_TRIP:${tempTripDetails?.first}:${tempTripDetails?.second}"
            currentState = ConversationState.IDLE
        } else {
            speak("Okay, trip planning cancelled.")
            currentState = ConversationState.IDLE
        }
    }

    private fun handleStationSelection(command: String) {
        // "The first one", "number one", "Orchard"
        var index = -1
        if (command.contains("first") || command.contains("one") || command.contains("1")) index = 0
        if (command.contains("second") || command.contains("two") || command.contains("2")) index = 1
        if (command.contains("third") || command.contains("three") || command.contains("3")) index = 2

        if (index != -1 && index < lastFoundStations.size) {
            selectedStation = lastFoundStations[index]
            speak("Selected ${selectedStation?.name}. Do you want to pay with your saved Card ending in 8899?")
            currentState = ConversationState.CONFIRMING_BOOKING
        } else {
            speak("Please say 'the first one', 'second one', or the station name.")
        }
    }

    private fun handleBookingConfirmation(command: String) {
         if (command.contains("yes") || command.contains("sure")) {
             speak("Please enter your 4-digit security PIN to confirm.")
             currentState = ConversationState.ENTERING_PIN
         } else if (command.contains("card") || command.contains("upi")) {
             speak("Okay, using that method. Please say your security PIN.")
             currentState = ConversationState.ENTERING_PIN
         } else {
             speak("Booking cancelled.")
             currentState = ConversationState.IDLE
         }
    }

    private suspend fun handlePinEntry(command: String) {
        // Extract digits
        val pin = command.filter { it.isDigit() }
        if (pin.length == 4) {
            speak("Verifying...")
            // Mock Booking
            val result = bookingRepository.createBooking(
                stationName = selectedStation!!.name,
                amount = "25.0",
                paymentMethod = "Wallet", 
                bookingDate = System.currentTimeMillis() + 3600000
            )
            if (result.isSuccess) {
                speak("Booking successful for ${selectedStation?.name}. I've sent the details to your wallet.")
                currentState = ConversationState.IDLE
            } else {
                speak("Payment failed. Please try again.")
                currentState = ConversationState.IDLE
            }
        } else {
            speak("That didn't sound like a 4-digit PIN. Please try again.")
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        _voiceResponse.value = "SPEAK:$text" // For UI to display if needed
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }
}
