package com.star.menugenerator

import com.star.menugenerator.data.BACKUP_FORMAT
import com.star.menugenerator.data.BackupCodec
import com.star.menugenerator.data.BackupException
import com.star.menugenerator.data.BackupMenu
import com.star.menugenerator.domain.prepareMenu
import com.star.menugenerator.domain.toApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {
    private fun backupMenu(id: String, title: String, updated: String = "2026-02-02T10:00:00.000Z"): BackupMenu {
        val prepared = prepareMenu(toApi(longMenu().copy(title = title), "green"))
        return BackupMenu(id, prepared.title, "2026-01-01T00:00:00.000Z", updated, prepared.settings, prepared.meals)
    }

    private fun assertRejected(text: String, contains: String) {
        try {
            BackupCodec.parse(text)
            fail("expected BackupException containing: $contains")
        } catch (e: BackupException) {
            assertTrue("message was: ${e.message}", e.message!!.contains(contains))
        }
    }

    @Test fun round_trip_keeps_everything() {
        val file = BackupCodec.export(listOf(backupMenu("menu-0001", "Wedding"), backupMenu("menu-0002", "Reception")), "2026-03-04T05:06:07.000Z")
        assertEquals(2, file.count)
        assertEquals("STAR-menus-backup-2026-03-04.json", file.filename)
        val parsed = BackupCodec.parse(file.text)
        assertEquals(2, parsed.menus.size)
        assertEquals("menu-0001", parsed.menus[0].id)
        assertEquals("Wedding", parsed.menus[0].prepared.title)
        assertEquals(7020000L, parsed.menus[0].prepared.total)
        assertEquals("2026-02-02T10:00:00.000Z", parsed.menus[0].updatedAt)
        assertEquals("2026-03-04T05:06:07.000Z", parsed.exportedAt)
    }

    @Test fun empty_backup_is_valid() {
        val parsed = BackupCodec.parse(BackupCodec.export(emptyList()).text)
        assertEquals(0, parsed.menus.size)
    }

    @Test fun edited_file_is_rejected_by_checksum() {
        val file = BackupCodec.export(listOf(backupMenu("menu-0001", "Wedding")))
        assertTrue(file.text.contains("Onam Sadhya"))
        assertRejected(file.text.replace("Onam Sadhya", "Onam Sadhyb"), "checksum")
    }

    @Test fun wrong_files_are_rejected_with_plain_messages() {
        assertRejected("", "empty")
        assertRejected("   ", "empty")
        assertRejected("not json at all {", "not readable JSON")
        assertRejected("[1,2,3]", "not a STAR Menu Generator backup")
        assertRejected("{\"format\":\"something-else\"}", "not a STAR Menu Generator backup")
        assertRejected("{\"format\":\"$BACKUP_FORMAT\",\"version\":2,\"menus\":[]}", "version 2")
        assertRejected("{\"format\":\"$BACKUP_FORMAT\",\"version\":1}", "valid list of menus")
    }

    @Test fun invalid_menu_inside_a_valid_checksum_is_rejected_before_anything_is_changed() {
        // Build a document whose checksum is right but whose menu has a negative quantity.
        val good = backupMenu("menu-0001", "Wedding")
        val badMeals = good.meals.map { m ->
            m.copy(entries = m.entries!!.map { e -> if (e.type == "INDIVIDUAL") e.copy(quantity = "-4") else e })
        }
        val file = BackupCodec.export(listOf(good.copy(meals = badMeals)))
        assertRejected(file.text, "Menu 1")
    }

    @Test fun duplicate_ids_are_rejected() {
        val file = BackupCodec.export(listOf(backupMenu("same-id-1", "A"), backupMenu("same-id-1", "B")))
        assertRejected(file.text, "same menu twice")
    }

    @Test fun invalid_id_gets_a_new_one_and_bad_dates_fall_back() {
        val m = backupMenu("x y!", "A").copy(createdAt = "yesterday", updatedAt = "????")
        val parsed = BackupCodec.parse(BackupCodec.export(listOf(m)).text, genId = { "generated-1" }, now = "2026-05-05T00:00:00.000Z")
        assertEquals("generated-1", parsed.menus[0].id)
        assertEquals("2026-05-05T00:00:00.000Z", parsed.menus[0].createdAt)
        assertEquals("2026-05-05T00:00:00.000Z", parsed.menus[0].updatedAt)
    }

    @Test fun checksum_is_sha256_hex() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", BackupCodec.sha256Hex(""))
    }
}
