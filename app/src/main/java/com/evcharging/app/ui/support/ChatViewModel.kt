package com.evcharging.app.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(text = "Hello! I'm your EV Assistant. How can I help you today?", isUser = false)
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // Add user message
        val userMessage = ChatMessage(text = text, isUser = true)
        _messages.value = _messages.value + userMessage

        // Simulate bot response
        viewModelScope.launch {
            _isTyping.value = true
            delay(1500) // Simulate thinking delay
            
            val responseText = getBotResponse(text)
            val botMessage = ChatMessage(text = responseText, isUser = false)
            
            _messages.value = _messages.value + botMessage
            _isTyping.value = false
        }
    }

    private fun getBotResponse(query: String): String {
        val lowerQuery = query.lowercase()
        return when {
            // Booking
            lowerQuery.contains("book") || lowerQuery.contains("reserve") -> 
                "To book a charging station:\n1. Go to the 'Trip Planner' or 'Navigation' screen.\n2. Select a station.\n3. Tap 'Book Slot'.\n4. Choose your date and time.\n5. Confirm your booking."
            lowerQuery.contains("cancel") && lowerQuery.contains("booking") ->
                "To cancel a booking, please go to your 'Charging History' (in the Profile menu), find the upcoming booking, and select 'Cancel'. Note that cancellation fees may apply if done last minute."
            
            // Charging
            lowerQuery.contains("start") && lowerQuery.contains("charg") ->
                "To start charging:\n1. Arrive at the station.\n2. Open the app and go to your active booking.\n3. Tap 'Start Charging'.\n4. Plug in the connector to your vehicle."
            lowerQuery.contains("stop") && lowerQuery.contains("charg") ->
                "To stop charging, simply tap the 'Stop Charging' button on the active charging screen in the app. The session will end, and the cost will be deducted from your wallet."
            lowerQuery.contains("slow") && lowerQuery.contains("charg") ->
                "Charging speed depends on the station type (DC Fast vs. AC) and your vehicle's battery management system. If it's unusually slow, try reconnecting or checking if the station is shared with another vehicle."

            // Navigation & Trip Planner
            lowerQuery.contains("find") || lowerQuery.contains("map") || lowerQuery.contains("locate") ->
                "You can find charging stations by tapping the 'Navigation' tab. You can filter by connector type and availability."
            lowerQuery.contains("plan") || lowerQuery.contains("trip") || lowerQuery.contains("route") ->
                "Use the 'Trip Planner' feature to plan long journeys. Enter your destination, and we'll suggest optimal charging stops along the way based on your vehicle's range."

            // Profile & Vehicle
            lowerQuery.contains("profile") || lowerQuery.contains("edit") || lowerQuery.contains("name") ->
                "You can edit your profile details (Name, Phone) by tapping the Profile icon on the Home screen and selecting 'Profile'. Then tap 'Edit Profile'."
            lowerQuery.contains("vehicle") || lowerQuery.contains("car") || lowerQuery.contains("model") ->
                "To update your vehicle details, go to your Profile page and edit the 'Vehicle Model' and 'Color'. This will also update the 3D car model on your Home screen!"
            
            // Wallet & Points
            lowerQuery.contains("wallet") || lowerQuery.contains("balance") || lowerQuery.contains("money") ->
                "You can check your wallet balance in the 'Wallet' screen. You can add money using credit/debit cards or UPI."
            lowerQuery.contains("points") || lowerQuery.contains("loyalty") || lowerQuery.contains("reward") ->
                "You earn Loyalty Points for every kWh charged! Check your points balance in the Profile menu. You can redeem these points for charging discounts or at partner dining locations."
            
            // Troubleshooting
            lowerQuery.contains("crash") || lowerQuery.contains("bug") || lowerQuery.contains("error") ->
                "I'm sorry to hear that. Please try restarting the app. If the issue persists, you can report it here by describing the error in detail, and our support team will look into it."
            lowerQuery.contains("contact") || lowerQuery.contains("human") || lowerQuery.contains("support") ->
                "You can reach our human support team at support@evapp.com or call our helpline at 1-800-EV-HELP."

            // Greetings & General
            lowerQuery.contains("hello") || lowerQuery.contains("hi") || lowerQuery.contains("hey") ->
                "Hello there! I'm your EV Assistant. Ask me anything about booking, charging, or your account."
            lowerQuery.contains("thank") ->
                "You're welcome! Happy charging! ⚡"
            
            else -> "I'm not sure I understood that. You can ask me things like:\n- 'How do I book a slot?'\n- 'How to earn points?'\n- 'My charging is slow'\n- 'Update my car model'"
        }
    }
}
