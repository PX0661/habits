package com.willbsp.habits.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.willbsp.habits.R
import com.willbsp.habits.data.model.HabitFrequency
import com.willbsp.habits.data.repository.EntryRepository
import com.willbsp.habits.data.repository.HabitRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import javax.inject.Inject

class HabitsWidgetFactory @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val habitRepository: HabitRepository,
    private val entryRepository: EntryRepository
) : RemoteViewsFactory {

    private var weeklyHabits = listOf<WeeklyHabitItem>()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            weeklyHabits = habitRepository.getAllHabitsStream().first()
                .filter { habit -> habit.frequency == HabitFrequency.WEEKLY }
                .map { habit ->
                    val completed = entryRepository.getEntry(LocalDate.now(), habit.id) != null
                    WeeklyHabitItem(habit.id, habit.name, completed)
                }
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = weeklyHabits.size

    override fun getViewAt(position: Int): RemoteViews {
        val habit = weeklyHabits[position]
        return RemoteViews(context.packageName, R.layout.habits_widget_item).apply {
            setTextViewText(R.id.habit_name, habit.name)
            setBoolean(R.id.habit_completed, "setChecked", habit.completed)
            
            val fillInIntent = Intent().apply {
                putExtra(HabitsWidgetProvider.EXTRA_HABIT_ID, habit.id)
            }
            setOnClickFillInIntent(R.id.habit_completed, fillInIntent)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = weeklyHabits[position].id.toLong()

    override fun hasStableIds(): Boolean = true

    data class WeeklyHabitItem(
        val id: Int,
        val name: String,
        val completed: Boolean
    )
}
