package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.PlantAiRepository
import com.example.data.repository.FloraRepository
import com.example.model.BotanicalReport
import com.example.model.ChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AskFloraMindViewModel(
    private val aiRepository: PlantAiRepository,
    private val floraRepository: FloraRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _advancedMode = MutableStateFlow(false)
    val advancedMode: StateFlow<Boolean> = _advancedMode.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setAdvancedMode(enabled: Boolean) {
        _advancedMode.value = enabled
    }

    fun clearChat() {
        _messages.value = emptyList()
        _errorMessage.value = null
    }

    fun askQuestion(
        question: String,
        activeReport: BotanicalReport?,
        userRegion: String
    ) {
        val cleanQ = question.trim()
        if (cleanQ.isBlank() || _isStreaming.value) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = cleanQ
        )
        val assistantMsgId = UUID.randomUUID().toString()
        val historySnapshot = _messages.value

        _messages.update {
            it + userMsg + ChatMessage(id = assistantMsgId, isUser = false, text = "", isStreaming = true)
        }
        _isStreaming.value = true
        _errorMessage.value = null

        val startMs = System.currentTimeMillis()
        val accumulated = StringBuilder()

        viewModelScope.launch {
            try {
                aiRepository.streamAskFloraMind(
                    question = cleanQ,
                    activeReport = activeReport,
                    history = historySnapshot,
                    advancedBotanyMode = _advancedMode.value,
                    userRegion = userRegion
                ).collect { chunk ->
                    accumulated.append(chunk)
                    val currentText = accumulated.toString()
                    _messages.update { list ->
                        list.map { msg ->
                            if (msg.id == assistantMsgId) msg.copy(text = currentText, isStreaming = true) else msg
                        }
                    }
                }
                val finalText = accumulated.toString().ifBlank {
                    "I could not generate an answer for that question. Please try rephrasing."
                }
                _messages.update { list ->
                    list.map { msg ->
                        if (msg.id == assistantMsgId) msg.copy(text = finalText, isStreaming = false) else msg
                    }
                }
                floraRepository.logAiUsage(
                    action = "ask_floramind",
                    modelName = PlantAiRepository.MODEL_FLASH,
                    status = "success",
                    latencyMs = System.currentTimeMillis() - startMs
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ask FloraMind stream failed", e)
                _errorMessage.value = e.localizedMessage ?: "Assistant request failed."
                _messages.update { list ->
                    list.filterNot { it.id == assistantMsgId && it.text.isBlank() }
                        .map { if (it.id == assistantMsgId) it.copy(isStreaming = false) else it }
                }
                floraRepository.logAiUsage(
                    action = "ask_floramind",
                    modelName = PlantAiRepository.MODEL_FLASH,
                    status = "error",
                    latencyMs = System.currentTimeMillis() - startMs,
                    errorSummary = e.localizedMessage ?: e.javaClass.simpleName
                )
            } finally {
                _isStreaming.value = false
            }
        }
    }

    private companion object {
        const val TAG = "AskFloraMindVM"
    }
}
