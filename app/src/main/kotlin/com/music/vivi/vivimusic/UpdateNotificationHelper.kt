package com.music.vivi.vivimusic

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.music.vivi.BuildConfig
import com.music.vivi.R
import com.music.vivi.constants.EnableNotificationsKey
import com.music.vivi.utils.dataStore
import com.music.vivi.utils.get
import com.music.vivi.vivimusic.updater.APK_LATEST_BASE
import com.music.vivi.vivimusic.updater.UPDATE_SOURCE_FORK
import com.music.vivi.vivimusic.updater.getUpdateSource
import com.music.vivi.vivimusic.updater.updateRepo

object UpdateNotificationHelper {
    private const val CHANNEL_ID = "updates"
    private const val NOTIFICATION_ID = 1001

    fun showUpdateNotification(context: Context, versionName: String) {
        val notificationsEnabled = context.dataStore.get(EnableNotificationsKey, true)
        if (!notificationsEnabled) return

        val nm = context.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.app_updates_title),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }

        // Where the notification sends the user to download the build.
        //
        // Our own source publishes the companion APK on the `apk-latest` branch
        // (`.releases/apk/latest`), never as a release asset — our releases
        // carry the desktop installers only — so the old
        // `releases/download/<tag>/vivi.apk` URL 404s on this repository, and
        // the nightly CI only exists upstream. The click therefore opens the
        // fixed channel URL that the in-app updater itself downloads from.
        val apkUrl = when {
            getUpdateSource(context) == UPDATE_SOURCE_FORK ->
                if (BuildConfig.CAST_AVAILABLE) "$APK_LATEST_BASE/vivi-gms.apk"
                else "$APK_LATEST_BASE/vivi-foss.apk"
            versionName.contains("nightly", ignoreCase = true) ->
                "https://nightly.link/${updateRepo(context)}/workflows/nightly.yml/main/vivi-music-gms-nightly.zip"
            else ->
                "https://github.com/${updateRepo(context)}/releases/download/$versionName/vivi.apk"
        }
        val intent = Intent(Intent.ACTION_VIEW, apkUrl.toUri())

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pending = PendingIntent.getActivity(context, NOTIFICATION_ID, intent, flags)

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.vivimusicnotification)
            .setContentTitle(context.getString(R.string.update_available_title))
            .setContentText(versionName)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notif)
        }
    }
}
