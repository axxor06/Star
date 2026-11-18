package com.star.menugenerator.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** One saved menu. `menu_data` holds JSON { settings, meals } in the same shape as the backup files. */
@Entity(
    tableName = "menus",
    indices = [Index(value = ["updated_at"]), Index(value = ["created_at"])],
)
data class MenuEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "menu_type") val menuType: String,
    @ColumnInfo(name = "menu_data") val menuData: String,
    @ColumnInfo(name = "entry_count") val entryCount: Int,
    @ColumnInfo(name = "total_paise") val totalPaise: Long,
    @ColumnInfo(name = "is_complete") val isComplete: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
)

/** Light-weight row for lists (does not load the big JSON column). */
data class MenuRow(
    val id: String,
    val title: String,
    @ColumnInfo(name = "menu_type") val menuType: String,
    @ColumnInfo(name = "entry_count") val entryCount: Int,
    @ColumnInfo(name = "total_paise") val totalPaise: Long,
    @ColumnInfo(name = "is_complete") val isComplete: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
)

private const val ROW_COLUMNS =
    "id, title, menu_type, entry_count, total_paise, is_complete, created_at, updated_at"

@Dao
interface MenuDao {
    @Query("SELECT $ROW_COLUMNS FROM menus WHERE title LIKE :pattern ESCAPE '\\' ORDER BY updated_at DESC, id")
    fun observeByUpdated(pattern: String): Flow<List<MenuRow>>

    @Query("SELECT $ROW_COLUMNS FROM menus WHERE title LIKE :pattern ESCAPE '\\' ORDER BY created_at DESC, id")
    fun observeByCreated(pattern: String): Flow<List<MenuRow>>

    @Query("SELECT $ROW_COLUMNS FROM menus WHERE title LIKE :pattern ESCAPE '\\' ORDER BY title COLLATE NOCASE ASC, id")
    fun observeByTitle(pattern: String): Flow<List<MenuRow>>

    @Query("SELECT $ROW_COLUMNS FROM menus ORDER BY updated_at DESC, id LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MenuRow>>

    @Query("SELECT COUNT(*) FROM menus")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM menus")
    suspend fun count(): Int

    @Query("SELECT * FROM menus WHERE id = :id")
    suspend fun get(id: String): MenuEntity?

    @Query("SELECT * FROM menus ORDER BY created_at")
    suspend fun all(): List<MenuEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: MenuEntity)

    @Update
    suspend fun update(entity: MenuEntity): Int

    @Query("DELETE FROM menus WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("DELETE FROM menus")
    suspend fun deleteAll(): Int
}

@Database(entities = [MenuEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun menuDao(): MenuDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "star_menus.db").build()
    }
}
