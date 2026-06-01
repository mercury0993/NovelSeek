package com.aichat.novel.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,      // UUID
    val title: String,               // First message preview (20 chars)
    val createdAt: Long,             // System.currentTimeMillis()
    val updatedAt: Long
)
