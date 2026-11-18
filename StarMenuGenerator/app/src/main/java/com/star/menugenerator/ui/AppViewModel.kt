package com.star.menugenerator.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.star.menugenerator.StarApp
import com.star.menugenerator.data.BackupCodec
import com.star.menugenerator.data.BackupFile
import com.star.menugenerator.data.MenuRepository
import com.star.menugenerator.data.MenuRow
import com.star.menugenerator.data.MenuSort
import com.star.menugenerator.data.ParsedBackup
import com.star.menugenerator.data.RestoreMode
import com.star.menugenerator.data.RestoreResult
import com.star.menugenerator.domain.ApiMenu
import com.star.menugenerator.domain.AppJson
import com.star.menugenerator.domain.Menu
import com.star.menugenerator.domain.blankMenu
import com.star.menugenerator.domain.toApi
import com.star.menugenerator.domain.validateMenu
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Splash : Screen
    data object Home : Screen
    data object MyMenus : Screen
    data object Editor : Screen
    data object Templates : Screen
    data object Preview : Screen
    data object Backup : Screen
}

sealed interface SaveStatus {
    data object Idle : SaveStatus
    data object Saving : SaveStatus
    data object Saved : SaveStatus
    /** Menu has invalid fields, so nothing is written (only valid menus are ever saved). */
    data object Invalid : SaveStatus
    data class Failed(val message: String) : SaveStatus
}

/**
 * Holds navigation, the menu being edited, and the autosave. Editor state lives in Compose snapshot
 * state so text fields update synchronously (no cursor jumps). Autosave waits 1 s after the last
 * edit, never writes an invalid menu, and `flush()` saves immediately when leaving the screen / app.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: MenuRepository = (app as StarApp).repository

    // ---- navigation ----
    val stack = mutableStateListOf<Screen>(Screen.Splash)
    val screen: Screen get() = stack.last()

    fun go(s: Screen) { stack.add(s) }

    fun replaceAll(s: Screen) {
        stack.clear()
        stack.add(s)
    }

    fun popTo(s: Screen) {
        while (stack.size > 1 && stack.last() != s) stack.removeAt(stack.lastIndex)
    }

    /** Returns false when already at the root (so the system can close the app). */
    fun back(): Boolean {
        if (stack.size <= 1) return false
        if (screen == Screen.Editor || screen == Screen.Templates || screen == Screen.Preview) flush()
        stack.removeAt(stack.lastIndex)
        return true
    }

    // ---- editor state ----
    var menu: Menu by mutableStateOf(blankMenu()); private set
    var templateId: String by mutableStateOf("purple"); private set
    var menuId: String? = null; private set
    var saveStatus: SaveStatus by mutableStateOf(SaveStatus.Idle); private set

    private var touched = false
    private var savedJson: String? = null
    private val saveMutex = Mutex()

    fun editMenu(transform: (Menu) -> Menu) {
        menu = transform(menu)
        touched = true
    }

    fun selectTemplate(id: String) {
        templateId = id
        touched = true
    }

    private fun resetEditor() {
        menu = blankMenu()
        templateId = "purple"
        menuId = null
        touched = false
        savedJson = null
        saveStatus = SaveStatus.Idle
    }

    fun newMenu() {
        flush()
        resetEditor()
        go(Screen.Editor)
    }

    /** Loads a saved menu into the editor. [onError] gets a plain-language message. */
    fun openMenu(id: String, onError: (String) -> Unit) {
        flush()
        viewModelScope.launch {
            try {
                val stored = repo.load(id) ?: throw IllegalStateException("That menu no longer exists.")
                menu = stored.menu
                templateId = stored.templateId
                menuId = stored.id
                savedJson = AppJson.encodeToString(ApiMenu.serializer(), toApi(stored.menu, stored.templateId))
                touched = true
                saveStatus = SaveStatus.Saved
                go(Screen.Editor)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e.message ?: "Could not open the menu")
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun startAutosave() {
        viewModelScope.launch {
            snapshotFlow { menu to templateId }
                .debounce(1000)
                .collect { saveNow() }
        }
    }

    init {
        startAutosave()
    }

    fun flush() {
        viewModelScope.launch { saveNow() }
    }

    fun retrySave() = flush()

    private suspend fun saveNow() {
        if (!touched) return
        val m = menu
        val t = templateId
        val api = toApi(m, t)
        if (validateMenu(api).errors.isNotEmpty()) {
            saveStatus = SaveStatus.Invalid
            return
        }
        val json = AppJson.encodeToString(ApiMenu.serializer(), api)
        saveMutex.withLock {
            if (json == savedJson) {
                if (saveStatus is SaveStatus.Idle || saveStatus is SaveStatus.Invalid || saveStatus is SaveStatus.Saving) {
                    saveStatus = SaveStatus.Saved
                }
                return
            }
            saveStatus = SaveStatus.Saving
            try {
                val id = menuId
                if (id == null) menuId = repo.create(m, t) else repo.update(id, m, t)
                savedJson = json
                saveStatus = SaveStatus.Saved
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                saveStatus = SaveStatus.Failed(e.message ?: "Could not save")
            }
        }
    }

    // ---- lists ----
    val menuCount: Flow<Int> = repo.observeCount()
    val recentMenus: Flow<List<MenuRow>> = repo.observeRecent(3)
    fun observeMenus(query: String, sort: MenuSort): Flow<List<MenuRow>> = repo.observeMenus(query, sort)

    suspend fun duplicateMenu(id: String) { repo.duplicate(id) }

    suspend fun deleteMenu(id: String) {
        repo.delete(id)
        if (menuId == id) resetEditor()
    }

    // ---- backup / restore ----
    suspend fun createBackup(): BackupFile = repo.exportBackup()

    suspend fun parseBackup(text: String): ParsedBackup =
        withContext(Dispatchers.Default) { BackupCodec.parse(text) }

    suspend fun restoreBackup(parsed: ParsedBackup, mode: RestoreMode): RestoreResult {
        val result = repo.restore(parsed, mode)
        if (mode == RestoreMode.REPLACE) resetEditor()
        return result
    }
}
