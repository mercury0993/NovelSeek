package com.aichat.novel.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        val MIGRATION_1_2 = @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE messages ADD COLUMN promptTokens INTEGER")
                database.execSQL("ALTER TABLE messages ADD COLUMN completionTokens INTEGER")
                database.execSQL("ALTER TABLE messages ADD COLUMN totalTokens INTEGER")
            }
        }
    }
}
