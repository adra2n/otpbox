package com.otpbox.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.otpbox.data.local.OtpDatabase
import com.otpbox.data.local.PasswordDao
import com.otpbox.domain.model.PasswordEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PasswordRepositoryImplTest {

    private lateinit var database: OtpDatabase
    private lateinit var dao: PasswordDao
    private lateinit var repository: PasswordRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OtpDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.passwordDao()
        repository = PasswordRepositoryImpl(dao)
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun sampleEntry(id: String, title: String, sortOrder: Int = 0) = PasswordEntry(
        id = id,
        title = title,
        username = "user_$id",
        password = "secret_$id",
        url = "https://example.com/$id",
        note = "note_$id",
        sortOrder = sortOrder
    )

    @Test
    fun add_and_observe() = runBlocking {
        val entry = sampleEntry("a", "Alpha")
        repository.add(entry)

        val observed = repository.observeEntries().first()
        assertEquals(1, observed.size)
        assertEquals("Alpha", observed.first().title)
        assertEquals("user_a", observed.first().username)
    }

    @Test
    fun soft_delete_marks_deleted() = runBlocking {
        val entry = sampleEntry("a", "Alpha")
        repository.add(entry)

        repository.delete("a")

        assertTrue(repository.getActiveEntries().isEmpty())

        val all = repository.getAllIncludingDeleted()
        assertEquals(1, all.size)
        assertTrue(all.first().deleted)
    }

    @Test
    fun update_changes_title_and_updatedAt() = runBlocking {
        val entry = sampleEntry("a", "Alpha")
        repository.add(entry)

        val before = repository.getById("a")!!
        val updated = before.copy(title = "Beta")
        repository.update(updated)

        val after = repository.getById("a")!!
        assertEquals("Beta", after.title)
        assertTrue(after.updatedAt >= before.updatedAt)
    }

    @Test
    fun observeActive_orders_by_sortOrder() = runBlocking {
        repository.add(sampleEntry("b", "Second", sortOrder = 2))
        repository.add(sampleEntry("a", "First", sortOrder = 1))

        val observed = repository.observeEntries().first()
        assertEquals(2, observed.size)
        assertEquals("First", observed[0].title)
        assertEquals("Second", observed[1].title)
        assertNull(repository.getById("missing"))
    }
}
