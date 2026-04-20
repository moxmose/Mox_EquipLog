package com.moxmose.moxequiplog.utils

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

class CalendarManager(private val context: Context) {

    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val transport = NetHttpTransport()

    fun getCredential(accountName: String): GoogleAccountCredential {
        return GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(CalendarScopes.CALENDAR_EVENTS)
        ).apply {
            selectedAccountName = accountName
        }
    }

    private fun getCalendarService(credential: GoogleAccountCredential): Calendar {
        return Calendar.Builder(transport, jsonFactory, credential)
            .setApplicationName("Mox EquipLog")
            .build()
    }

    suspend fun addEvent(
        credential: GoogleAccountCredential,
        title: String,
        description: String,
        startTimeMillis: Long,
        endTimeMillis: Long
    ): String? = withContext(Dispatchers.IO) {
        try {
            val service = getCalendarService(credential)
            val event = Event().apply {
                summary = title
                this.description = description
                start = EventDateTime().setDateTime(com.google.api.client.util.DateTime(startTimeMillis))
                end = EventDateTime().setDateTime(com.google.api.client.util.DateTime(endTimeMillis))
            }
            val createdEvent = service.events().insert("primary", event).execute()
            createdEvent.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteEvent(credential: GoogleAccountCredential, eventId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = getCalendarService(credential)
            service.events().delete("primary", eventId).execute()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateEvent(
        credential: GoogleAccountCredential,
        eventId: String,
        title: String,
        description: String,
        startTimeMillis: Long,
        endTimeMillis: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = getCalendarService(credential)
            val event = service.events().get("primary", eventId).execute().apply {
                summary = title
                this.description = description
                start = EventDateTime().setDateTime(com.google.api.client.util.DateTime(startTimeMillis))
                end = EventDateTime().setDateTime(com.google.api.client.util.DateTime(endTimeMillis))
            }
            service.events().update("primary", eventId, event).execute()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
