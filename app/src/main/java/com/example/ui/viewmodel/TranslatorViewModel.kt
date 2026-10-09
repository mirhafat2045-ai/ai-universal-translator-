package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiTranslatorService
import com.example.data.local.AppDatabase
import com.example.data.local.TranslationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class TranslatorUiState(
    val sourceText: String = "",
    val translatedText: String = "",
    val sourceLang: String = "Auto Detect",
    val targetLang: String = "Spanish",
    val style: String = "Direct",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isDarkMode: Boolean = false
)

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).translationDao()
    private val translatorService = GeminiTranslatorService()

    val historyList: Flow<List<TranslationEntity>> = dao.getAllHistory()
    val favoritesList: Flow<List<TranslationEntity>> = dao.getFavorites()

    private val _uiState = MutableStateFlow(TranslatorUiState())
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    fun updateSourceText(text: String) {
        _uiState.update { it.copy(sourceText = text) }
    }

    fun setSourceLang(lang: String) {
        _uiState.update { it.copy(sourceLang = lang) }
    }

    fun setTargetLang(lang: String) {
        _uiState.update { it.copy(targetLang = lang) }
    }

    fun setStyle(style: String) {
        _uiState.update { it.copy(style = style) }
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun swapLanguages() {
        _uiState.update { state ->
            if (state.sourceLang == "Auto Detect") {
                state
            } else {
                state.copy(
                    sourceLang = state.targetLang,
                    targetLang = state.sourceLang,
                    sourceText = state.translatedText,
                    translatedText = state.sourceText
                )
            }
        }
    }

    fun translate() {
        val current = _uiState.value
        if (current.sourceText.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = translatorService.translateText(
                    sourceText = current.sourceText,
                    sourceLang = current.sourceLang,
                    targetLang = current.targetLang,
                    style = current.style
                )
                _uiState.update { it.copy(translatedText = result, isLoading = false) }

                // Save to history
                dao.insertTranslation(
                    TranslationEntity(
                        sourceText = current.sourceText,
                        translatedText = result,
                        sourceLang = current.sourceLang,
                        targetLang = current.targetLang,
                        style = current.style,
                        isFavorite = false
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Translation failed") }
            }
        }
    }

    fun translateImage(bitmap: Bitmap) {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

                val result = translatorService.translateImage(
                    base64Image = base64,
                    mimeType = "image/jpeg",
                    targetLang = current.targetLang
                )
                _uiState.update { it.copy(translatedText = result, isLoading = false) }

                dao.insertTranslation(
                    TranslationEntity(
                        sourceText = "[Image OCR Text Translation]",
                        translatedText = result,
                        sourceLang = "Image OCR",
                        targetLang = current.targetLang,
                        style = "Direct",
                        isFavorite = false
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Image translation failed") }
            }
        }
    }

    fun toggleFavorite(item: TranslationEntity) {
        viewModelScope.launch {
            dao.updateFavorite(item.id, !item.isFavorite)
        }
    }

    fun deleteItem(item: TranslationEntity) {
        viewModelScope.launch {
            dao.deleteTranslation(item)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.clearHistory()
        }
    }
}
