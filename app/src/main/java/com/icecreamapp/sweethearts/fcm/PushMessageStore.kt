package com.icecreamapp.sweethearts.fcm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the last push notification (title + body) for in-app display.
 * Updated by [IceCreamFirebaseMessagingService] when a message is received.
 */
object PushMessageStore {

    private val _lastMessage = MutableStateFlow<Pair<String, String>?>(null)
    val lastMessage: StateFlow<Pair<String, String>?> = _lastMessage.asStateFlow()

    fun set(title: String, body: String) {
        _lastMessage.value = title to body
    }

    fun clear() {
        _lastMessage.value = null
    }
}
