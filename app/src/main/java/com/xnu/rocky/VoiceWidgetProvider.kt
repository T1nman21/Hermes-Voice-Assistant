//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//

package com.xnu.rocky

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Home screen widget — a 2x2 tile with a mic. Tapping it launches Rocky directly into a voice
 * session (same deep-link as the Quick Settings tile and the "Voice" launcher shortcut).
 *
 * Android widgets are a system signature iOS widgets cannot match: interactive, resizable,
 * stacked, and they live on the home screen, not a separate widget page.
 */
class VoiceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_voice)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_START_VOICE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_VOICE,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    private companion object {
        const val REQUEST_CODE_VOICE = 1001
    }
}
