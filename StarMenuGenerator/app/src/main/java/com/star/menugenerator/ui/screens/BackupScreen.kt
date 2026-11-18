package com.star.menugenerator.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.star.menugenerator.data.BackupException
import com.star.menugenerator.data.BackupFile
import com.star.menugenerator.data.ParsedBackup
import com.star.menugenerator.data.RestoreMode
import com.star.menugenerator.ui.AppViewModel
import com.star.menugenerator.ui.components.PrimaryButton
import com.star.menugenerator.ui.components.StarCard
import com.star.menugenerator.ui.components.StarHeader
import com.star.menugenerator.ui.theme.StarDeep
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_BYTES = 25 * 1024 * 1024

private fun readText(context: Context, uri: Uri): String {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw BackupException("That file could not be opened.")
    if (bytes.size > MAX_BYTES) throw BackupException("That file is too large to be a STAR backup.")
    return String(bytes, Charsets.UTF_8)
}

/** Backup = one JSON file you choose where to save. Restore validates the whole file before touching anything. */
@Composable
fun BackupScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf(false) }
    var pendingBackup by remember { mutableStateOf<BackupFile?>(null) }
    var parsed by remember { mutableStateOf<ParsedBackup?>(null) }

    fun say(msg: String) { scope.launch { snackbar.showSnackbar(msg) } }

    val createLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val file = pendingBackup
        pendingBackup = null
        if (uri != null && file != null) {
            scope.launch {
                busy = true
                try {
                    withContext(Dispatchers.IO) {
                        val out = context.contentResolver.openOutputStream(uri, "wt") ?: throw IllegalStateException("Could not write the file")
                        out.use { it.write(file.text.toByteArray(Charsets.UTF_8)) }
                    }
                    say("Backup saved — ${file.count} menu${if (file.count == 1) "" else "s"}")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    say("Backup failed: ${e.message ?: "unknown error"}")
                }
                busy = false
            }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                try {
                    val text = withContext(Dispatchers.IO) { readText(context, uri) }
                    parsed = vm.parseBackup(text)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: BackupException) {
                    say(e.message ?: "That backup cannot be used.")
                } catch (e: Exception) {
                    say("Could not read that file: ${e.message ?: "unknown error"}")
                }
                busy = false
            }
        }
    }

    parsed?.let { p ->
        fun restore(mode: RestoreMode) {
            parsed = null
            scope.launch {
                busy = true
                try {
                    val r = vm.restoreBackup(p, mode)
                    say("Restored: ${r.added} added, ${r.updated} updated, ${r.skipped} unchanged" + (if (r.removed > 0) ", ${r.removed} removed" else ""))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    say("Restore failed — nothing was changed. ${e.message ?: ""}")
                }
                busy = false
            }
        }
        AlertDialog(
            onDismissRequest = { parsed = null },
            containerColor = Color.White,
            title = { Text("Restore ${p.menus.size} menu${if (p.menus.size == 1) "" else "s"}?") },
            text = {
                Text(
                    "Merge keeps all your current menus, adds the new ones, and updates any menu the backup has a newer copy of.\n\n" +
                        "Replace deletes every menu on this phone first, then restores the backup.",
                )
            },
            confirmButton = {
                Column {
                    TextButton(onClick = { restore(RestoreMode.MERGE) }) { Text("Merge", fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { restore(RestoreMode.REPLACE) }) {
                        Text("Replace everything", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = { TextButton(onClick = { parsed = null }) { Text("Cancel") } },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { StarHeader("Backup & Restore", onBack = { vm.back() }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            StarCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Create a backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StarDeep)
                    Text("Saves all your menus into one file that you choose where to keep (Downloads, Drive, a USB drive…). Use it before changing phones.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryButton(
                        "Create backup file", icon = Icons.Filled.Backup, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    val file = vm.createBackup()
                                    pendingBackup = file
                                    createLauncher.launch(file.filename)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    say("Backup failed: ${e.message ?: "unknown error"}")
                                }
                                busy = false
                            }
                        },
                    )
                }
            }
            StarCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Restore from a backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StarDeep)
                    Text("Choose a STAR backup file. It is fully checked first — a damaged or edited file is rejected and nothing is changed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(
                        onClick = { openLauncher.launch(arrayOf("*/*")) }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                    ) { Text("Choose backup file…") }
                }
            }
        }
    }
}
