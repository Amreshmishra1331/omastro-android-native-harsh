package com.omastro.app.data.repository

import com.omastro.app.data.model.AppNotification
import com.omastro.app.data.remote.OmAstroApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Website notification center (bell icon + list + mark-read + clear-all).
 * Server is the source of truth; unread count drives the header badge.
 */
class NotificationRepository(private val api: OmAstroApiService) {

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun refresh(): Result<Unit> {
        _isLoading.value = true
        return try {
            val res = api.getNotifications()
            val data = res.body()?.data
            if (res.isSuccessful && data != null) {
                _notifications.value = data.notifications
                _unreadCount.value = data.unread
                Result.success(Unit)
            } else {
                Result.failure(Exception("Could not load notifications (HTTP ${res.code()})."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /** Marks one notification read (ids=[id]) or all when ids is null. */
    suspend fun markRead(ids: List<Int>? = null): Result<Unit> {
        return try {
            val body: Map<String, Any?> = if (ids == null) emptyMap() else mapOf("ids" to ids)
            val res = api.markNotificationsRead(body)
            if (res.isSuccessful) {
                if (ids == null) {
                    _notifications.value = _notifications.value.map { it.copy(isRead = true) }
                    _unreadCount.value = 0
                } else {
                    val set = ids.toSet()
                    _notifications.value = _notifications.value.map {
                        if (it.id in set) it.copy(isRead = true) else it
                    }
                    _unreadCount.value = _notifications.value.count { !it.isRead }
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Mark-read failed (HTTP ${res.code()})."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAll(): Result<Unit> {
        return try {
            val res = api.clearNotifications()
            if (res.isSuccessful) {
                _notifications.value = emptyList()
                _unreadCount.value = 0
                Result.success(Unit)
            } else {
                Result.failure(Exception("Clear failed (HTTP ${res.code()})."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
