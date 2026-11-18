package com.star.menugenerator

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.star.menugenerator.data.AppDatabase
import com.star.menugenerator.data.BackupCodec
import com.star.menugenerator.data.MenuNotFoundException
import com.star.menugenerator.data.MenuRepository
import com.star.menugenerator.data.RestoreMode
import com.star.menugenerator.domain.Entry
import com.star.menugenerator.domain.EntryType
import com.star.menugenerator.domain.Meal
import com.star.menugenerator.domain.Menu
import com.star.menugenerator.domain.MenuValidationException
import com.star.menugenerator.domain.Section
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Room + repository tests. Run on a device/emulator: Android Studio > right-click > Run (connectedAndroidTest). */
@RunWith(AndroidJUnit4::class)
class MenuRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: MenuRepository

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = MenuRepository(db)
    }

    @After fun tearDown() { db.close() }

    private fun sample(title: String = "Wedding") = Menu(
        title,
        listOf(
            Meal(
                "meal-0001", "Lunch",
                listOf(
                    Entry("entry-0001", EntryType.INDIVIDUAL, name = "Dosa", quantity = "100", unitPrice = "5"),
                    Entry("entry-0002", EntryType.PACKAGE, name = "Sadhya", itemsText = "Rice, Sambar", pricePerPlate = "250", plateCount = "100"),
                    Entry("entry-0003", EntryType.BUFFET, name = "Grand", pricePerPlate = "450", plateCount = "10",
                        sections = listOf(Section("sect-0001", "Starters", "A, B"))),
                ),
            ),
        ),
    )

    @Test fun create_load_update_delete() = runBlocking {
        val id = repo.create(sample(), "green")
        val loaded = repo.load(id)!!
        assertEquals("Wedding", loaded.menu.title)
        assertEquals("green", loaded.templateId)
        assertEquals("A, B", loaded.menu.meals[0].entries[2].sections[0].itemsText)
        assertEquals(1, repo.count())

        repo.update(id, sample("Renamed"), "purple")
        assertEquals("Renamed", repo.load(id)!!.menu.title)
        assertEquals("purple", repo.load(id)!!.templateId)
        assertEquals(loaded.createdAt, repo.load(id)!!.createdAt)

        repo.delete(id)
        assertEquals(0, repo.count())
        try { repo.delete(id); fail() } catch (e: MenuNotFoundException) { }
    }

    @Test fun invalid_menu_is_never_written() = runBlocking {
        val bad = sample().let { it.copy(meals = listOf(it.meals[0].copy(entries = listOf(Entry("entry-0009", EntryType.INDIVIDUAL, name = "Tea", quantity = "-1", unitPrice = "5"))))) }
        try { repo.create(bad, "purple"); fail() } catch (e: MenuValidationException) { }
        assertEquals(0, repo.count())
    }

    @Test fun stored_totals_match_calculation() = runBlocking {
        repo.create(sample(), "purple")
        val rows = db.menuDao().all()
        assertEquals(50000L + 2500000L + 450000L, rows[0].totalPaise) // 500 + 25,000 + 4,500 rupees in paise
        assertTrue(rows[0].isComplete)
        assertEquals(3, rows[0].entryCount)
        assertEquals("MIXED", rows[0].menuType)
    }

    @Test fun duplicate_gets_new_ids_and_copy_title() = runBlocking {
        val id = repo.create(sample(), "green")
        val copyId = repo.duplicate(id)
        assertNotEquals(id, copyId)
        val copy = repo.load(copyId)!!
        assertEquals("Wedding (copy)", copy.menu.title)
        assertNotEquals("meal-0001", copy.menu.meals[0].id)
        assertEquals(2, repo.count())
    }

    @Test fun backup_and_restore_merge_and_replace() = runBlocking {
        val a = repo.create(sample("A"), "purple")
        repo.create(sample("B"), "green")
        val backup = repo.exportBackup()
        assertEquals(2, backup.count)
        val parsed = BackupCodec.parse(backup.text)

        repo.delete(a)
        assertEquals(1, repo.count())
        val merged = repo.restore(parsed, RestoreMode.MERGE)
        assertEquals(1, merged.added)       // A comes back
        assertEquals(0, merged.updated)
        assertEquals(1, merged.skipped)     // B unchanged
        assertEquals(2, repo.count())

        repo.create(sample("C"), "purple")
        assertEquals(3, repo.count())
        val replaced = repo.restore(parsed, RestoreMode.REPLACE)
        assertEquals(3, replaced.removed)
        assertEquals(2, repo.count())
        assertFalse(db.menuDao().all().any { it.title == "C" })
    }
}
