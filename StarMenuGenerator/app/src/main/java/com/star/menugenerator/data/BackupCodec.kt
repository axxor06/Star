package com.star.menugenerator.data

import com.star.menugenerator.domain.ApiMenu
import com.star.menugenerator.domain.ApiMeal
import com.star.menugenerator.domain.AppJson
import com.star.menugenerator.domain.MenuValidationException
import com.star.menugenerator.domain.PreparedMenu
import com.star.menugenerator.domain.newId
import com.star.menugenerator.domain.normalizeIso
import com.star.menugenerator.domain.nowIso
import com.star.menugenerator.domain.prepareMenu
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import java.security.MessageDigest

// ---------------------------------------------------------------------------------------------
// JSON backup file. Same format as the original project ("star-menu-backup", version 1, SHA-256
// checksum over the menus array), so backups made by the web version can be restored here too.
// Everything is validated BEFORE anything in the database is touched.
// ---------------------------------------------------------------------------------------------

const val BACKUP_FORMAT = "star-menu-backup"
const val BACKUP_VERSION = 1
const val BACKUP_APP_ID = "com.star.menugenerator"
private const val MAX_BACKUP_CHARS = 25 * 1024 * 1024
private const val MAX_BACKUP_MENUS = 5000
private val BACKUP_ID = Regex("[A-Za-z0-9_-]{1,64}")

class BackupException(message: String) : Exception(message)

@Serializable
data class BackupMenu(
    val id: String,
    val title: String,
    val createdAt: String,
    val updatedAt: String,
    val settings: JsonObject,
    val meals: List<ApiMeal>,
)

data class BackupFile(val filename: String, val text: String, val count: Int)

data class ParsedMenu(
    val id: String,
    val prepared: PreparedMenu,
    val createdAt: String,
    val updatedAt: String,
)

data class ParsedBackup(val menus: List<ParsedMenu>, val exportedAt: String?)

enum class RestoreMode { MERGE, REPLACE }

data class RestoreResult(val added: Int, val updated: Int, val skipped: Int, val removed: Int)

object BackupCodec {
    private val compact = Json { encodeDefaults = false; explicitNulls = false }
    private val pretty = Json { prettyPrint = true; encodeDefaults = false; explicitNulls = false }

    fun sha256Hex(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun checksumOf(menus: JsonArray): String =
        sha256Hex(compact.encodeToString(JsonElement.serializer(), menus))

    fun export(menus: List<BackupMenu>, exportedAt: String = nowIso()): BackupFile {
        val arr = AppJson.encodeToJsonElement(ListSerializer(BackupMenu.serializer()), menus) as JsonArray
        val doc = buildJsonObject {
            put("format", BACKUP_FORMAT)
            put("version", BACKUP_VERSION)
            put("app", BACKUP_APP_ID)
            put("exportedAt", exportedAt)
            put("menuCount", menus.size)
            put("checksum", checksumOf(arr))
            put("menus", arr)
        }
        return BackupFile(
            filename = "STAR-menus-backup-${exportedAt.take(10)}.json",
            text = pretty.encodeToString(JsonElement.serializer(), doc),
            count = menus.size,
        )
    }

    /** Throws [BackupException] with a plain-language reason if the file cannot be trusted. */
    fun parse(text: String, genId: () -> String = ::newId, now: String = nowIso()): ParsedBackup {
        if (text.isBlank()) throw BackupException("That file is empty.")
        if (text.length > MAX_BACKUP_CHARS) throw BackupException("That file is too large to be a STAR backup.")
        val root: JsonElement = try {
            Json.parseToJsonElement(text)
        } catch (e: Exception) {
            throw BackupException("That is not a valid backup file (it is not readable JSON).")
        }
        val doc = root as? JsonObject
        if (doc == null || (doc["format"] as? JsonPrimitive)?.contentOrNull != BACKUP_FORMAT) {
            throw BackupException("That file is not a STAR Menu Generator backup.")
        }
        val versionPrimitive = doc["version"] as? JsonPrimitive
        if (versionPrimitive?.intOrNull != BACKUP_VERSION) {
            throw BackupException("This backup uses version ${versionPrimitive?.contentOrNull ?: "unknown"}, which this app cannot read.")
        }
        val arr = doc["menus"] as? JsonArray
        if (arr == null || arr.size > MAX_BACKUP_MENUS) {
            throw BackupException("The backup does not contain a valid list of menus.")
        }
        if ((doc["checksum"] as? JsonPrimitive)?.contentOrNull != checksumOf(arr)) {
            throw BackupException("The backup file is damaged or was edited (checksum does not match). Nothing was changed.")
        }
        val seen = HashSet<String>()
        val menus = ArrayList<ParsedMenu>(arr.size)
        arr.forEachIndexed { i, el ->
            val obj = el as? JsonObject ?: throw BackupException("Menu ${i + 1} is not a valid menu.")
            val shownTitle = (obj["title"] as? JsonPrimitive)?.contentOrNull.orEmpty().take(40)
            val prepared = try {
                prepareMenu(AppJson.decodeFromJsonElement(ApiMenu.serializer(), obj), genId)
            } catch (e: MenuValidationException) {
                throw BackupException("Menu ${i + 1} (“$shownTitle”) is invalid: ${e.errors.firstOrNull()?.message ?: e.message}")
            } catch (e: Exception) {
                throw BackupException("Menu ${i + 1} (“$shownTitle”) is invalid: ${e.message ?: "unreadable data"}")
            }
            val rawId = (obj["id"] as? JsonPrimitive)?.contentOrNull
            val id = if (rawId != null && BACKUP_ID.matches(rawId)) rawId else genId()
            if (!seen.add(id)) throw BackupException("The backup contains the same menu twice.")
            menus += ParsedMenu(
                id = id,
                prepared = prepared,
                createdAt = normalizeIso((obj["createdAt"] as? JsonPrimitive)?.contentOrNull, now),
                updatedAt = normalizeIso((obj["updatedAt"] as? JsonPrimitive)?.contentOrNull, now),
            )
        }
        return ParsedBackup(menus, (doc["exportedAt"] as? JsonPrimitive)?.contentOrNull)
    }
}
