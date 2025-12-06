package com.evcharging.admin.model

import com.google.firebase.Timestamp

data class SupportTicket(
    val id: String = "",
    val userId: String = "",
    val adminId: String? = null,
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val status: TicketStatus = TicketStatus.OPEN
)

enum class TicketStatus {
    OPEN, IN_PROGRESS, RESOLVED, CLOSED
}
