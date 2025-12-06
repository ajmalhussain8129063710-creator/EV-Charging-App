package com.evcharging.app.data

import com.evcharging.app.data.model.SupportTicket
import com.evcharging.app.data.model.TicketStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    suspend fun createTicket(message: String): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            
            // Parse for @mention
            val adminId = parseAdminMention(message)

            val ticket = SupportTicket(
                userId = userId,
                adminId = adminId,
                message = message,
                status = TicketStatus.OPEN
            )
            
            firestore.collection("support_tickets").add(ticket).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun parseAdminMention(message: String): String? {
        val regex = Regex("@(\\w+)")
        val match = regex.find(message)
        val adminName = match?.groupValues?.get(1)

        return if (adminName != null) {
            // Find admin by name
            val snapshot = firestore.collection("admins") // Assuming admins are in 'admins' or 'users' with role
                .whereEqualTo("name", adminName)
                .limit(1)
                .get()
                .await()
            
            if (!snapshot.isEmpty) {
                snapshot.documents[0].id
            } else {
                // Fallback: Check 'users' collection if admins are stored there
                 val userSnapshot = firestore.collection("users")
                    .whereEqualTo("name", adminName)
                    .limit(1)
                    .get()
                    .await()
                 if (!userSnapshot.isEmpty) {
                     userSnapshot.documents[0].id
                 } else {
                     null
                 }
            }
        } else {
            null
        }
    }

    suspend fun getTicketsForAdmin(adminId: String): Result<List<SupportTicket>> {
        return try {
            val snapshot = firestore.collection("support_tickets")
                .whereEqualTo("adminId", adminId)
                .get()
                .await()
            
            val tickets = snapshot.toObjects(SupportTicket::class.java)
            Result.success(tickets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserTickets(userId: String): Result<List<SupportTicket>> {
        return try {
            val snapshot = firestore.collection("support_tickets")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val tickets = snapshot.toObjects(SupportTicket::class.java)
            Result.success(tickets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
