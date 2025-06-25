package de.theiling.neatlauncher

import android.app.Notification.FLAG_FOREGROUND_SERVICE
import android.app.Notification.FLAG_GROUP_SUMMARY
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

object notificationData {
    var map = mapOf<String,Int>()
    var onChange: ()->Unit = {}
    fun count(i: Item) = if (i.type == ITEM_TYPE_APP) map.getOrDefault(i.pack, 0) else 0
}

class NotificationService: NotificationListenerService() {
    companion object {
        val BADGING_URI: Uri = Settings.Secure.getUriFor("notification_badging")
    }

    private var connected = false
    private var enabled = false

    private val observeSettings = object: ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = onSettingsChanged()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int) = START_STICKY

    override fun onCreate() {
        super.onCreate()
        contentResolver.registerContentObserver(BADGING_URI, false, observeSettings)
        onSettingsChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(observeSettings)
        enabled = false
        update(false)
    }

    override fun onListenerConnected()    { connected = true;  update(false) }
    override fun onListenerDisconnected() { connected = false; update(false) }
    override fun onNotificationPosted (noti: StatusBarNotification) = update(true)
    override fun onNotificationRemoved(noti: StatusBarNotification) = update(true)

    fun onSettingsChanged() {
        val wasEnabled = enabled
        enabled = Settings.Secure.getInt(contentResolver, BADGING_URI.lastPathSegment,1) == 1
        if (wasEnabled == enabled) return
        when {
            !enabled && connected -> requestUnbind()
            else -> Unit
        }
        update(!enabled)
    }

    private fun notify(snotis: Array<StatusBarNotification>) {
        val newMap = mutableMapOf<String,Int>()
        for (s in snotis) {
            if (!s.isClearable) continue
            val n = s.notification
            if ((n.flags and FLAG_FOREGROUND_SERVICE) == FLAG_FOREGROUND_SERVICE) continue
            if ((n.flags and FLAG_GROUP_SUMMARY) == FLAG_GROUP_SUMMARY) continue
            val count = n.number.notIf{ it <= 0 } ?: 1
            newMap.compute(s.packageName) { _, pre -> (pre ?: 0) + count }
        }

        if (newMap != notificationData.map) {
            notificationData.map = newMap
            notificationData.onChange()
        }
    }

    private fun update(doClear: Boolean) = when {
        enabled && connected -> notify(activeNotifications)
        doClear -> notify(arrayOf())
        else -> Unit
    }
}

object notificationDot {
    val DOT = arrayOf(
        // CIRCLED 0
        "\u24EA",
        // CIRCLED 1..20
        "\u2460", "\u2461", "\u2462", "\u2463", "\u2464",
        "\u2465", "\u2466", "\u2467", "\u2468", "\u2469",
        "\u246A", "\u246B", "\u246C", "\u246D", "\u246E",
        "\u246F", "\u2470", "\u2471", "\u2472", "\u2473",
        // CIRCLED 21...50 (CJK)
        "\u3251", "\u3252", "\u3253", "\u3254", "\u3255",
        "\u3256", "\u3257", "\u3258", "\u3259", "\u325A",
        "\u325B", "\u325C", "\u325D", "\u325E", "\u325F",
        "\u32B1", "\u32B2", "\u32B3", "\u32B4", "\u32B5",
        "\u32B6", "\u32B7", "\u32B8", "\u32B9", "\u32BA",
        "\u32BB", "\u32BC", "\u32BD", "\u32BE", "\u32BF")

    const val DOT_INF = "\u267E"
}

fun Int.toDot() = notificationDot.DOT.getOrNull(this) ?: notificationDot.DOT_INF
