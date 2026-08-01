package com.jerrywolff.androidpeoplesearch.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jerrywolff.androidpeoplesearch.ai.LocalHeuristicAssistant
import com.jerrywolff.androidpeoplesearch.ai.MlKitGeminiNanoAssistant
import com.jerrywolff.androidpeoplesearch.data.SettingsRepository
import com.jerrywolff.androidpeoplesearch.model.AiSummary
import com.jerrywolff.androidpeoplesearch.model.AppSettings
import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult
import com.jerrywolff.androidpeoplesearch.model.SearchSession
import com.jerrywolff.androidpeoplesearch.model.SourceCategory
import com.jerrywolff.androidpeoplesearch.search.PeopleSearchEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: PeopleSearchQuery = PeopleSearchQuery(),
    val results: List<PersonSearchResult> = emptyList(),
    val selectedResult: PersonSearchResult? = null,
    val summary: AiSummary? = null,
    val settings: AppSettings = AppSettings(),
    val isSearching: Boolean = false,
    val localAiAvailable: Boolean = false,
    val message: String? = null,
)

class PeopleSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = PeopleSearchEngine()
    private val assistant = MlKitGeminiNanoAssistant(LocalHeuristicAssistant())
    private val settingsRepository = SettingsRepository(application)
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { settingsRepository.settings.collect { settings -> _state.update { it.copy(settings = settings) } } }
        viewModelScope.launch { _state.update { it.copy(localAiAvailable = assistant.isAvailable()) } }
    }

    fun updateQuery(query: PeopleSearchQuery) = _state.update { it.copy(query = query, message = null) }
    fun clearQuery() = _state.update { it.copy(query = PeopleSearchQuery(), results = emptyList(), summary = null) }

    fun search(onComplete: () -> Unit) {
        val query = state.value.query.normalized()
        if (query.isBlank()) {
            _state.update { it.copy(message = "Enter a name, username, domain, or free-text query.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, message = null) }
            val results = engine.search(query, state.value.settings.enabledCategories)
            settingsRepository.record(SearchSession(query, results.size, System.currentTimeMillis()))
            _state.update { it.copy(query = query, results = results, isSearching = false, summary = null) }
            onComplete()
        }
    }

    fun select(result: PersonSearchResult) = _state.update { it.copy(selectedResult = result) }
    fun summarize(onComplete: () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(summary = assistant.summarizeResults(it.results)) }
        onComplete()
    }

    fun explain(result: PersonSearchResult, onComplete: (String) -> Unit) = viewModelScope.launch {
        onComplete(assistant.explainMatch(state.value.query, result))
    }

    fun setCategory(category: SourceCategory, enabled: Boolean) = viewModelScope.launch { settingsRepository.setCategory(category, enabled) }
    fun setHistory(enabled: Boolean) = viewModelScope.launch { settingsRepository.setHistoryEnabled(enabled) }
    fun setPrivacyMode(enabled: Boolean) = viewModelScope.launch { settingsRepository.setPrivacyMode(enabled) }
    fun clearLocalData() = viewModelScope.launch {
        settingsRepository.clearLocalData()
        _state.update { SearchUiState(message = "Local settings and history cleared.") }
    }
}
