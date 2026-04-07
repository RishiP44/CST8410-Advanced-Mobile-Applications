package com.example.lab09

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = MessageDatabase.getDatabase(application).messageDao()

    val allMessages: StateFlow<List<ChatMessage>> = dao.getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun insertMessage(text: String, type: String) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return

        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        val message = ChatMessage(
            text = trimmedText,
            type = type,
            time = currentTime
        )

        viewModelScope.launch {
            dao.insertMessage(message)
        }
    }

    suspend fun getMessageById(messageId: Int): ChatMessage? {
        return dao.getMessageById(messageId)
    }

    fun deleteMessage(message: ChatMessage) {
        viewModelScope.launch {
            dao.deleteMessage(message)
        }
    }
}