package com.star.menugenerator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.star.menugenerator.data.MenuRow
import com.star.menugenerator.data.MenuSort
import com.star.menugenerator.domain.formatPaise
import com.star.menugenerator.ui.AppViewModel
import com.star.menugenerator.ui.components.ConfirmDialog
import com.star.menugenerator.ui.components.PrimaryButton
import com.star.menugenerator.ui.components.StarCard
import com.star.menugenerator.ui.components.StarHeader
import com.star.menugenerator.ui.components.TypeBadge
import com.star.menugenerator.ui.theme.StarDeep
import com.star.menugenerator.ui.theme.StarPurple
import com.star.menugenerator.ui.theme.StarWarn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val WHEN_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

private fun whenText(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).format(WHEN_FORMAT)
} catch (e: Exception) {
    iso
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyMenusScreen(vm: AppViewModel) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var debounced by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(MenuSort.UPDATED) }
    var pendingDelete by remember { mutableStateOf<MenuRow?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        delay(250)
        debounced = query
    }
    val flow = remember(debounced, sort) { vm.observeMenus(debounced, sort) }
    val rows by flow.collectAsState(initial = null)

    fun act(id: String, okMessage: String, block: suspend () -> Unit) {
        busyId = id
        scope.launch {
            try {
                block()
                snackbar.showSnackbar(okMessage)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbar.showSnackbar(e.message ?: "Something went wrong")
            }
            busyId = null
        }
    }

    pendingDelete?.let { m ->
        ConfirmDialog(
            title = "Delete this menu?",
            message = "“${m.title}” will be permanently deleted from this phone.",
            onConfirm = { act(m.id, "Menu deleted") { vm.deleteMenu(m.id) } },
            onDismiss = { pendingDelete = null },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { StarHeader("My Menus", onBack = { vm.back() }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Search menus") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(sort == MenuSort.UPDATED, { sort = MenuSort.UPDATED }, label = { Text("Recently updated") })
                    FilterChip(sort == MenuSort.CREATED, { sort = MenuSort.CREATED }, label = { Text("Newest") })
                    FilterChip(sort == MenuSort.TITLE, { sort = MenuSort.TITLE }, label = { Text("A–Z") })
                }
            }
            val list = rows
            when {
                list == null -> Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = StarPurple)
                    Spacer(Modifier.height(8.dp))
                    Text("Loading your menus…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                list.isEmpty() && debounced.isBlank() -> Column(
                    Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("No saved menus yet", style = MaterialTheme.typography.titleLarge, color = StarDeep)
                    Text(
                        "Menus are saved on this phone automatically while you create them.",
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PrimaryButton("Create your first menu", onClick = { vm.newMenu() }, icon = Icons.Filled.Add)
                }
                list.isEmpty() -> Text(
                    "No menus match “$debounced”.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(list, key = { it.id }) { m ->
                        val busy = busyId == m.id
                        StarCard {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(m.title, Modifier.weight(1f, fill = false), fontWeight = FontWeight.Bold, color = StarDeep, maxLines = 2)
                                    if (m.entryCount == 0) TypeBadge("Empty draft", MaterialTheme.colorScheme.onSurfaceVariant)
                                    else if (!m.isComplete) TypeBadge("Incomplete total", StarWarn)
                                }
                                Text(
                                    "Updated ${whenText(m.updatedAt)} · ${m.entryCount} entr${if (m.entryCount == 1) "y" else "ies"}" +
                                        (if (m.totalPaise > 0) " · ${formatPaise(m.totalPaise)}" else ""),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PrimaryButton(
                                        "Open", enabled = !busy, icon = Icons.Filled.Edit, modifier = Modifier.weight(1f),
                                        onClick = {
                                            vm.openMenu(m.id) { msg -> scope.launch { snackbar.showSnackbar(msg) } }
                                        },
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(onClick = { act(m.id, "Menu duplicated") { vm.duplicateMenu(m.id) } }, enabled = !busy) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate ${m.title}", tint = StarPurple)
                                    }
                                    IconButton(onClick = { pendingDelete = m }, enabled = !busy) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete ${m.title}", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
