package com.aichat.novel

import android.app.Application
import androidx.room.Room
import com.aichat.novel.data.db.AppDatabase

class NovelSeekApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "novelseek_db"
        )
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()
    }

    companion object {
        lateinit var instance: NovelSeekApp
            private set
    }
}
