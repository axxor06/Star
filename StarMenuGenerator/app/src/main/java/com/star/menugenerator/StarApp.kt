package com.star.menugenerator

import android.app.Application
import com.star.menugenerator.data.AppDatabase
import com.star.menugenerator.data.MenuRepository
import com.star.menugenerator.render.Exporter
import com.star.menugenerator.render.MenuRenderer

/** Application-wide singletons (no dependency-injection framework needed for an app this size). */
class StarApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: MenuRepository by lazy { MenuRepository(database) }
    val renderer: MenuRenderer by lazy { MenuRenderer(this) }
    val exporter: Exporter by lazy { Exporter(this, renderer) }
}
