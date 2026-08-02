package com.jerrywolff.androidpeoplesearch.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Launch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jerrywolff.androidpeoplesearch.model.AiSummary
import com.jerrywolff.androidpeoplesearch.model.ConfidenceLevel
import com.jerrywolff.androidpeoplesearch.model.PeopleSearchQuery
import com.jerrywolff.androidpeoplesearch.model.PersonSearchResult
import com.jerrywolff.androidpeoplesearch.model.SourceCategory
import com.jerrywolff.androidpeoplesearch.search.usStateNames
import com.jerrywolff.androidpeoplesearch.ui.theme.AndroidPeopleSearchTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: SearchUiState, update: (PeopleSearchQuery) -> Unit, selectState: (String) -> Unit, refine: (String) -> Unit, clear: () -> Unit, search: () -> Unit) {
    val query = state.query
    var chatInput by rememberSaveable { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            NoticeCard()
            Spacer(Modifier.height(8.dp))
            Text("Search public sources", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("One search automatically checks every compliant public source and labels where each result was found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { FormField("Full name", query.fullName) { update(query.copy(fullName = it)) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormField("First name", query.firstName, Modifier.weight(1f)) { update(query.copy(firstName = it)) }
                FormField("Last name", query.lastName, Modifier.weight(1f)) { update(query.copy(lastName = it)) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormField("City", query.city, Modifier.weight(1f)) { update(query.copy(city = it)) }
                StateDropdown(query.state, Modifier.weight(1f), selectState)
            }
        }
        item {
            CountyDropdown(
                value = query.county,
                counties = state.countyOptions,
                isLoading = state.isLoadingCounties,
                loadFailed = state.countyLoadFailed,
            ) { update(query.copy(county = it)) }
        }
        item { CountryDropdown(query.country) { update(query.copy(country = it)) } }
        item { FormField("Company", query.company) { update(query.copy(company = it)) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FormField("Email domain", query.emailDomain, Modifier.weight(1f)) { update(query.copy(emailDomain = it)) }
                FormField("Username", query.username, Modifier.weight(1f)) { update(query.copy(username = it)) }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Search assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                state.searchChat.takeLast(4).forEach { message ->
                    Text(
                        "${if (message.fromAssistant) "Assistant" else "You"}: ${message.text}",
                        color = if (message.fromAssistant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Message search assistant") },
                    minLines = 2,
                    maxLines = 4,
                    trailingIcon = {
                        IconButton(
                            onClick = { refine(chatInput); chatInput = "" },
                            enabled = chatInput.isNotBlank(),
                        ) { Icon(Icons.Outlined.Send, "Send details") }
                    },
                )
            }
        }
        item { Text("All public source categories are searched automatically.", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
        state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(search, Modifier.weight(1f), enabled = !state.isSearching) { Icon(Icons.Outlined.Search, null); Text(if (state.isSearching) "Searching..." else "Search public sources") }
                OutlinedButton(clear) { Text("Clear") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FormField(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(), update: (String) -> Unit) {
    OutlinedTextField(value, update, modifier, label = { Text(label) }, singleLine = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StateDropdown(value: String, modifier: Modifier = Modifier.fillMaxWidth(), update: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value.ifBlank { "Any state" },
            onValueChange = {},
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            readOnly = true,
            label = { Text("State") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Any state") }, onClick = { update(""); expanded = false })
            usStateNames().forEach { state ->
                DropdownMenuItem(text = { Text(state) }, onClick = { update(state); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountyDropdown(
    value: String,
    counties: List<String>,
    isLoading: Boolean,
    loadFailed: Boolean,
    update: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded && counties.isNotEmpty(),
            onExpandedChange = { if (counties.isNotEmpty()) expanded = it },
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = update,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                label = { Text(if (isLoading) "Loading counties..." else "County") },
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded && counties.isNotEmpty()) },
            )
            ExposedDropdownMenu(
                expanded = expanded && counties.isNotEmpty(),
                onDismissRequest = { expanded = false },
            ) {
                counties.forEach { county ->
                    DropdownMenuItem(text = { Text(county) }, onClick = { update(county); expanded = false })
                }
            }
        }
        if (loadFailed) {
            Text("County list unavailable. Enter the county manually.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryDropdown(value: String, update: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val countries = remember {
        Locale.getISOCountries()
            .map { Locale("", it).displayCountry }
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value.ifBlank { "Any country" },
            onValueChange = {},
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            readOnly = true,
            label = { Text("Country") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Any country") },
                onClick = { update(""); expanded = false },
            )
            countries.forEach { country ->
                DropdownMenuItem(
                    text = { Text(country) },
                    onClick = { update(country); expanded = false },
                )
            }
        }
    }
}

@Composable
fun ResultsScreen(state: SearchUiState, onBack: () -> Unit, onSelect: (PersonSearchResult) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BackHeader("Results", onBack); NoticeCard() }
        if (state.sourceCounts.isNotEmpty()) {
            item { Text("Records found by source", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(state.sourceCounts, key = { it.providerName }) { source ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(source.providerName, Modifier.weight(1f))
                    Text(
                        if (source.failed) source.failureReason ?: "Unavailable" else "${source.recordCount} found",
                        color = if (source.failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        if (state.results.isEmpty()) {
            item {
                Text(
                    if (state.failedProviders.isEmpty()) {
                        "No public profiles or records matched this query."
                    } else {
                        "No records were returned by the sources that responded."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        state.summary?.let { review ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Automatic review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(review.headline, fontWeight = FontWeight.SemiBold)
                    Text(review.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    review.caveats.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        if (state.results.isNotEmpty()) item { Text("Records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(state.results, key = { it.id }) { ResultCard(it, onSelect) }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun ResultCard(result: PersonSearchResult, onSelect: (PersonSearchResult) -> Unit) {
    val context = LocalContext.current
    Card(onClick = { onSelect(result) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(result.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ConfidenceChip(result.confidence, result.confidenceScore)
            }
            Text(
                "Found via ${result.sourceName} · ${result.category.label} · " +
                    when {
                        result.providerId == "bing_linkedin" -> "Public LinkedIn reference"
                        result.providerId == "bing_web" -> "Public web index"
                        result.providerId == "county_official_references" -> "Official government index"
                        result.providerId == "king_county_employees" -> "Official county directory"
                        else -> "Direct API"
                    },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(result.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(result.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip({ openUrl(context, result.url) }, { Text("Open source") }, leadingIcon = { Icon(Icons.Outlined.Launch, null) })
                AssistChip({ copyUrl(context, result.url) }, { Text("Copy link") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) })
            }
        }
    }
}

@Composable
fun DetailScreen(query: PeopleSearchQuery, result: PersonSearchResult, explain: (PersonSearchResult, (String) -> Unit) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var explanation by remember { mutableStateOf("Preparing a local explanation...") }
    LaunchedEffect(result.id) { explain(result) { explanation = it } }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BackHeader("Result detail", onBack) }
        item {
            Text(result.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Found via ${result.sourceName} · ${result.category.label}", color = MaterialTheme.colorScheme.primary)
            ConfidenceChip(result.confidence, result.confidenceScore)
        }
        if (result.confidence == ConfidenceLevel.LOW) item { Text("Low confidence: treat this only as a lead and verify every detail.", color = MaterialTheme.colorScheme.error) }
        item { DetailBlock("Why this may match", explanation) }
        item { DetailBlock("Extracted query signals", result.extractedSignals.ifEmpty { listOf("No corroborating signals") }.joinToString()) }
        item { DetailBlock("Source URL", result.url) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ openUrl(context, result.url) }) { Icon(Icons.Outlined.Launch, null); Text("Open source") }
                OutlinedButton({ copyUrl(context, result.url) }) { Icon(Icons.Outlined.ContentCopy, null); Text("Copy") }
            }
        }
        item { Text("Query: ${query.searchText()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun SettingsScreen(state: SearchUiState, setPageDepth: (Int) -> Unit, setHistory: (Boolean) -> Unit, setPrivacy: (Boolean) -> Unit, clearData: () -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            BackHeader("Sources & settings", onBack)
            DetailBlock("Automatic source coverage", "Every compliant provider and category runs for each search. Results identify the source and category individually.")
        }
        item {
            Text("Pages per source", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { setPageDepth(state.settings.pageDepth - 1) },
                    enabled = state.settings.pageDepth > 1,
                ) { Text("−") }
                Text("${state.settings.pageDepth}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = { setPageDepth(state.settings.pageDepth + 1) },
                    enabled = state.settings.pageDepth < 5,
                ) { Text("+") }
            }
            Text("Each direct public API retrieves up to ${state.settings.pageDepth * 20} records. Range: 1–5 pages.", style = MaterialTheme.typography.bodySmall)
        }
        item { Divider(); SettingToggle("Save local search history", state.settings.historyEnabled, setHistory); Text("Off by default. Disabling it deletes saved history.", style = MaterialTheme.typography.bodySmall) }
        item { SettingToggle("Privacy mode", state.settings.privacyMode, setPrivacy); Text("Keeps local persistence minimal.", style = MaterialTheme.typography.bodySmall) }
        item { DetailBlock("AI status", if (state.localAiAvailable) "Gemini Nano available on this device" else "Deterministic local fallback active") }
        item { OutlinedButton(clearData) { Icon(Icons.Outlined.DeleteSweep, null); Text("Clear local data") }; state.message?.let { Text(it) } }
    }
}

@Composable
fun SummaryScreen(summary: AiSummary?, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BackHeader("AI summary", onBack)
        Text(summary?.headline ?: "No summary", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(summary?.summary.orEmpty())
        summary?.caveats?.forEach { Text("• $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("Generated entirely on-device using the deterministic fallback unless Gemini Nano is explicitly available.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) = InfoScreen("Privacy & safety", onBack, listOf(
    "Use results only for lawful, respectful purposes.",
    "Never use this app for stalking, harassment, doxxing, credential discovery, or private-address discovery.",
    "The app generates public source links and does not bypass authentication, paywalls, CAPTCHA, robots.txt, or rate limits.",
    "Results may be incomplete, outdated, or inaccurate. Confidence labels are leads, not identity facts.",
    "Search history is disabled by default and can be cleared from Settings.",
))

@Composable
fun AboutScreen(onBack: () -> Unit) = InfoScreen("About", onBack, listOf(
    "Android People Search 1.0.0",
    "A privacy-conscious public-source research tool built with Kotlin and Jetpack Compose.",
    "No API keys, paid result providers, analytics, background tracking, or cloud AI are required.",
    "Each result opens its attributed source in a browser Custom Tab.",
))

@Composable
private fun InfoScreen(title: String, onBack: () -> Unit, paragraphs: List<String>) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BackHeader(title, onBack)
        paragraphs.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable
private fun NoticeCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Text("Public-source notice: links may be incomplete or inaccurate. Verify identity independently; confidence labels are not facts.", Modifier.padding(14.dp))
    }
}

@Composable
private fun BackHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, update: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, update)
    }
}

@Composable
private fun ConfidenceChip(level: ConfidenceLevel, score: Int) {
    AssistChip({}, { Text("${level.label} $score") })
}

@Composable
private fun DetailBlock(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value)
    }
}

private fun openUrl(context: Context, url: String) = CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
private fun copyUrl(context: Context, url: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Public source link", url))
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() = AndroidPeopleSearchTheme { HomeScreen(SearchUiState(), {}, {}, {}, {}, {}) }
