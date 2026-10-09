package com.omastro.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Website notification center models.
 * Server: GET /notifications -> {success, data:{notifications:[...], unread:N}}
 * Row keys (verified in rest-api.php): id, type, title, message, icon,
 * action_url, is_read, time.
 */
data class AppNotification(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("type") val type: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("message") val message: String = "",
    @SerializedName("icon") val icon: String = "",
    @SerializedName("action_url") val actionUrl: String = "",
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("time") val time: String = ""
)

data class NotificationsData(
    @SerializedName("notifications") val notifications: List<AppNotification> = emptyList(),
    @SerializedName("unread") val unread: Int = 0
)

data class NotificationsResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: NotificationsData? = null,
    @SerializedName("message") val message: String? = null
)
