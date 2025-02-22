package com.willbsp.habits.widget

import android.content.Intent
import android.widget.RemoteViewsService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HabitsWidgetService : RemoteViewsService() {
    
    @Inject
    lateinit var factory: HabitsWidgetFactory

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return factory
    }
}
