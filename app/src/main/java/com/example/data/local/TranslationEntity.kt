package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translations")
data class TranslationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sourceText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val style: String = "Direct",
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
