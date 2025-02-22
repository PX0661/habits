package com.willbsp.habits.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.willbsp.habits.R
import com.willbsp.habits.data.model.HabitFrequency
import com.willbsp.habits.data.repository.EntryRepository
import com.willbsp.habits.data.repository.HabitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class HabitsWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var habitRepository: HabitRepository

    @Inject
    lateinit var entryRepository: EntryRepository

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Schedule periodic updates
        val updateRequest = androidx.work.PeriodicWorkRequestBuilder<HabitsWidgetWorker>(30, java.util.concurrent.TimeUnit.MINUTES)
            .build()
        androidx.work.WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                HabitsWidgetWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                updateRequest
            )

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.habits_widget)
            
            // Set up list adapter
            val intent = Intent(context, HabitsWidgetService::class.java)
            views.setRemoteAdapter(R.id.widget_list, intent)
            
            // Handle item clicks
            val clickIntent = Intent(context, HabitsWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_HABIT
            }
            val clickPendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                0,
                clickIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE_HABIT) {
            val habitId = intent.getIntExtra(EXTRA_HABIT_ID, -1)
            if (habitId != -1) {
                CoroutineScope(Dispatchers.IO).launch {
                    entryRepository.toggleEntry(habitId, LocalDate.now())
                    // Trigger widget update after habit completion
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(
                        android.content.ComponentName(context, HabitsWidgetProvider::class.java)
                    )
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
                }
            }
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_TOGGLE_HABIT = "com.willbsp.habits.ACTION_TOGGLE_HABIT"
        const val EXTRA_HABIT_ID = "com.willbsp.habits.EXTRA_HABIT_ID"
    }
}
