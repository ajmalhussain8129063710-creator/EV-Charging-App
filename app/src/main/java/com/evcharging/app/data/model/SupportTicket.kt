package com.evcharging.app.data.model

import com.google.firebase.Timestamp

data class SupportTicket(
    val id: String = "",
    val userId: String = "",
    val adminId: String? = null, // Null if not assigned/mentioned
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val status: TicketStatus = TicketStatus.OPEN
)

enum class TicketStatus {
    OPEN, IN_PROGRESS, RESOLVED, CLOSED
}
