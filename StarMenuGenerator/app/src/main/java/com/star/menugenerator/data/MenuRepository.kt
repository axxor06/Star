package com.star.menugenerator.data

import androidx.room.withTransaction
import com.star.menugenerator.domain.ApiMenu
import com.star.menugenerator.domain.ApiMeal
import com.star.menugenerator.domain.AppJson
import com.star.menugenerator.domain.Menu
import com.star.menugenerator.domain.PreparedMenu
import com.star.menugenerator.domain.fromApi
import com.star.menugenerator.domain.newId
import com.star.menugenerator.domain.nowIso
import com.star.menugenerator.domain.prepareMenu
import com.star.menugenerator.domain.toApi
import com.star.menugenerator.domain.withNewIds
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

enum class MenuSort { UPDATED, CREATED, TITLE }

class MenuNotFoundException : Exception("Menu not found")

/** A saved menu loaded back into the editor shape. */
data class StoredMenu(
    val id: String,
    val templateId: String,
    val menu: Menu,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
private data class MenuData(
    val settings: JsonObject = JsonObject(emptyMap()),
    val meals: List<ApiMeal> = emptyList(),
)

private fun likeEscape(s: String): String = s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

/**
 * All persistence goes through here. Every menu is validated and its totals recomputed before it is
 * written, so the list screen's totals can never disagree with the editor.
 */
class MenuRepository(private val db: AppDatabase) {
    private val dao get() = db.menuDao()

    fun observeMenus(query: String, sort: MenuSort): Flow<List<MenuRow>> {
        val pattern = "%" + likeEscape(query.trim().take(100)) + "%"
        return when (sort) {
            MenuSort.UPDATED -> dao.observeByUpdated(pattern)
            MenuSort.CREATED -> dao.observeByCreated(pattern)
            MenuSort.TITLE -> dao.observeByTitle(pattern)
        }
    }

    fun observeRecent(limit: Int = 3): Flow<List<MenuRow>> = dao.observeRecent(limit)
    fun observeCount(): Flow<Int> = dao.observeCount()
    suspend fun count(): Int = dao.count()

    private fun entityOf(id: String, m: PreparedMenu, createdAt: String, updatedAt: String) = MenuEntity(
        id = id,
        title = m.title,
        menuType = m.type,
        menuData = AppJson.encodeToString(MenuData.serializer(), MenuData(m.settings, m.meals)),
        entryCount = m.entryCount,
        totalPaise = m.total,
        isComplete = m.complete,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun storedOf(e: MenuEntity): StoredMenu {
        val data = AppJson.decodeFromString(MenuData.serializer(), e.menuData)
        val loaded = fromApi(ApiMenu(title = e.title, settings = data.settings, meals = data.meals))
        return StoredMenu(e.id, loaded.templateId, loaded.menu, e.createdAt, e.updatedAt)
    }

    suspend fun load(id: String): StoredMenu? = dao.get(id)?.let(::storedOf)

    /** Validates and inserts. Throws MenuValidationException if any field is invalid. */
    suspend fun create(menu: Menu, templateId: String): String {
        val prepared = prepareMenu(toApi(menu, templateId))
        val id = newId()
        val now = nowIso()
        dao.insert(entityOf(id, prepared, now, now))
        return id
    }

    suspend fun update(id: String, menu: Menu, templateId: String) {
        val prepared = prepareMenu(toApi(menu, templateId))
        val existing = dao.get(id) ?: throw MenuNotFoundException()
        if (dao.update(entityOf(id, prepared, existing.createdAt, nowIso())) == 0) throw MenuNotFoundException()
    }

    suspend fun delete(id: String) {
        if (dao.delete(id) == 0) throw MenuNotFoundException()
    }

    /** Copies a menu with fresh ids and "(copy)" appended to its title (max 120 characters). */
    suspend fun duplicate(id: String): String {
        val src = load(id) ?: throw MenuNotFoundException()
        val copy = Menu(
            title = "${src.menu.title} (copy)".take(120),
            meals = src.menu.meals.map { it.withNewIds() },
            settings = src.menu.settings,
        )
        return create(copy, src.templateId)
    }

    // ---- backup / restore ----

    suspend fun exportBackup(): BackupFile {
        val rows = dao.all().map { e ->
            val data = AppJson.decodeFromString(MenuData.serializer(), e.menuData)
            BackupMenu(e.id, e.title, e.createdAt, e.updatedAt, data.settings, data.meals)
        }
        return BackupCodec.export(rows)
    }

    /** One transaction: all or nothing. MERGE keeps mine and adds/updates; REPLACE deletes mine first. */
    suspend fun restore(parsed: ParsedBackup, mode: RestoreMode): RestoreResult = db.withTransaction {
        var added = 0
        var updated = 0
        var skipped = 0
        var removed = 0
        if (mode == RestoreMode.REPLACE) removed = dao.deleteAll()
        for (m in parsed.menus) {
            val existing = dao.get(m.id)
            if (existing == null) {
                dao.insert(entityOf(m.id, m.prepared, m.createdAt, m.updatedAt))
                added++
            } else if (m.updatedAt > existing.updatedAt) {
                dao.update(entityOf(m.id, m.prepared, existing.createdAt, m.updatedAt))
                updated++
            } else {
                skipped++
            }
        }
        RestoreResult(added, updated, skipped, removed)
    }
}
