package com.jerrywolff.androidpeoplesearch.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jerrywolff.androidpeoplesearch.ai.LocalHeuristicAssistant
import com.jerrywolff.androidpeoplesearch.data.SettingsRepository
import com.jerrywolff.androidpeoplesearch.model.AiSummary
import com.jerrywolff.androidpeoplesearch.model.AppSettings
import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult
import com.jerrywolff.androidpeoplesearch.model.SearchSession
import com.jerrywolff.androidpeoplesearch.search.CountyCatalog
import com.jerrywolff.androidpeoplesearch.search.PeopleSearchEngine
import com.jerrywolff.androidpeoplesearch.search.SourceResultCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchChatMessage(val fromAssistant: Boolean, val text: String)

data class SearchUiState(
    val query: PeopleSearchQuery = PeopleSearchQuery(),
    val results: List<PersonSearchResult> = emptyList(),
    val selectedResult: PersonSearchResult? = null,
    val summary: AiSummary? = null,
    val settings: AppSettings = AppSettings(),
    val isSearching: Boolean = false,
    val failedProviders: List<String> = emptyList(),
    val sourceCounts: List<SourceResultCount> = emptyList(),
    val countyOptions: List<String> = emptyList(),
    val isLoadingCounties: Boolean = false,
    val countyLoadFailed: Boolean = false,
    val searchChat: List<SearchChatMessage> = listOf(
        SearchChatMessage(true, "Add details about the person, location, employer, domain, or username."),
    ),
    val message: String? = null,
)

class PeopleSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = PeopleSearchEngine()
    private val countyCatalog = CountyCatalog()
    private val assistant = LocalHeuristicAssistant()
    private val settingsRepository = SettingsRepository(application)
    private val _state = MutableStateFlow(SearchUiState())
    private var lastSubmittedQuery: PeopleSearchQuery? = null
    private var repeatedSearchCount = 0
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { settingsRepository.settings.collect { settings -> _state.update { it.copy(settings = settings) } } }
    }

    fun updateQuery(query: PeopleSearchQuery) = _state.update { it.copy(query = query, message = null) }
    fun selectState(state: String) {
        _state.update {
            it.copy(
                query = it.query.copy(
                    state = state,
                    county = "",
                    country = if (state.isNotBlank()) "United States" else it.query.country,
                ),
                countyOptions = emptyList(),
                message = null,
            )
        }
        loadCountyOptions(state)
    }

    private fun loadCountyOptions(state: String) {
        _state.update {
            it.copy(
                countyOptions = emptyList(),
                isLoadingCounties = state.isNotBlank(),
                countyLoadFailed = false,
            )
        }
        if (state.isBlank()) return
        viewModelScope.launch {
            val counties = runCatching { countyCatalog.countiesForState(state) }
            _state.update { current ->
                if (current.query.state != state) {
                    current
                } else {
                    current.copy(
                        countyOptions = counties.getOrDefault(emptyList()),
                        isLoadingCounties = false,
                        countyLoadFailed = counties.isFailure,
                    )
                }
            }
        }
    }
    fun clearQuery() = _state.update {
        it.copy(
            query = PeopleSearchQuery(),
            results = emptyList(),
            summary = null,
            failedProviders = emptyList(),
            sourceCounts = emptyList(),
            countyOptions = emptyList(),
            isLoadingCounties = false,
            countyLoadFailed = false,
            searchChat = listOf(SearchChatMessage(true, "Add details about the person, location, employer, domain, or username.")),
        )
    }

    fun refineSearch(message: String) = viewModelScope.launch {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return@launch
        _state.update { it.copy(searchChat = it.searchChat + SearchChatMessage(false, trimmed)) }
        val refinement = assistant.refineSearchQuery(state.value.query, trimmed)
        val stateChanged = refinement.query.state != state.value.query.state
        _state.update {
            it.copy(
                query = refinement.query,
                searchChat = it.searchChat + SearchChatMessage(true, refinement.reply),
                message = null,
            )
        }
        if (stateChanged) loadCountyOptions(refinement.query.state)
    }

    fun search(onComplete: () -> Unit) {
        val query = state.value.query.normalized()
        if (query.isBlank()) {
            _state.update { it.copy(message = "Enter a name, username, domain, or free-text query.") }
            return
        }
        repeatedSearchCount = if (query == lastSubmittedQuery) repeatedSearchCount + 1 else 0
        lastSubmittedQuery = query
        val breadthLevel = repeatedSearchCount
        viewModelScope.launch {
            _state.update {
                it.copy(
                    results = emptyList(),
                    selectedResult = null,
                    summary = null,
                    failedProviders = emptyList(),
                    sourceCounts = emptyList(),
                    isSearching = true,
                    message = null,
                )
            }
            val report = engine.searchWithDiagnostics(query, state.value.settings.pageDepth, breadthLevel)
            val review = assistant.summarizeResults(report.results)
            settingsRepository.record(SearchSession(query, report.results.size, System.currentTimeMillis()))
            _state.update {
                it.copy(
                    query = query,
                    results = report.results,
                    failedProviders = report.failures.map { failure -> failure.providerName },
                    sourceCounts = report.sourceCounts,
                    isSearching = false,
                    summary = review,
                )
            }
            onComplete()
        }
    }

    fun select(result: PersonSearchResult) = _state.update { it.copy(selectedResult = result) }
    fun explain(result: PersonSearchResult, onComplete: (String) -> Unit) = viewModelScope.launch {
        onComplete(assistant.explainMatch(state.value.query, result))
    }

    fun setHistory(enabled: Boolean) = viewModelScope.launch { settingsRepository.setHistoryEnabled(enabled) }
    fun setPrivacyMode(enabled: Boolean) = viewModelScope.launch { settingsRepository.setPrivacyMode(enabled) }
    fun setPageDepth(pageDepth: Int) = viewModelScope.launch { settingsRepository.setPageDepth(pageDepth) }
    fun clearLocalData() = viewModelScope.launch {
        settingsRepository.clearLocalData()
        _state.update { SearchUiState(message = "Local settings and history cleared.") }
    }
}
