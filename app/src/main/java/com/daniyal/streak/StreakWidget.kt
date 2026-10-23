package com.daniyal.streak

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class StreakWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, buildViews(context))
        scheduleMidnight(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelMidnight(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_DONE -> {
                StreakStore.markDone(context)
                updateAll(context)
            }
            ACTION_MIDNIGHT, Intent.ACTION_DATE_CHANGED -> updateAll(context)
        }
    }

    companion object {
        const val ACTION_DONE = "com.daniyal.streak.ACTION_DONE"
        const val ACTION_MIDNIGHT = "com.daniyal.streak.ACTION_MIDNIGHT"

        private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        /** Refreshes every widget on the home screen and re-arms the midnight alarm. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, StreakWidget::class.java))
            for (id in ids) manager.updateAppWidget(id, buildViews(context))
            if (ids.isNotEmpty()) scheduleMidnight(context) else cancelMidnight(context)
        }

        private fun midnightPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, StreakWidget::class.java).setAction(ACTION_MIDNIGHT)
            return PendingIntent.getBroadcast(context, 2, intent, FLAGS)
        }

        /**
         * Inexact alarm just after local midnight so a broken streak shows 0 on the widget.
         * (Exact alarms need a special permission; Android may fire this a few minutes late.)
         */
        private fun scheduleMidnight(context: Context) {
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarms.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                StreakStore.nextMidnightMillis() + 5_000L,
                midnightPendingIntent(context)
            )
        }

        private fun cancelMidnight(context: Context) {
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarms.cancel(midnightPendingIntent(context))
        }

        private fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_streak)
            val count = StreakStore.current(context)
            val done = StreakStore.isDoneToday(context)

            views.setTextViewText(R.id.widget_count, count.toString())
            views.setTextViewText(R.id.widget_button, if (done) "Done ✓" else "Mark done")
            views.setBoolean(R.id.widget_button, "setEnabled", !done)

            val doneIntent = Intent(context, StreakWidget::class.java).setAction(ACTION_DONE)
            views.setOnClickPendingIntent(
                R.id.widget_button,
                PendingIntent.getBroadcast(context, 0, doneIntent, FLAGS)
            )

            val openIntent = Intent(context, MainActivity::class.java)
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(context, 1, openIntent, FLAGS)
            )

            return views
        }
    }
}
