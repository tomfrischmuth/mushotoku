/*
 * Mushotoku — a privacy-focused, offline productivity app.
 * Copyright (C) 2026 Tom Frischmuth
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mushotoku.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mushotoku.app.data.*
import com.mushotoku.app.repository.AppRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val db   = AppDatabase.getInstance(app)
    private val repo = AppRepository(db, db.taskDao(), db.noteDao(), db.expenseDao(), db.categoryDao(), db.appSettingsDao(), db.habitDao(), db.recurringCostHistoryDao(), db.additionalIncomeDao())

    val notes: StateFlow<ImmutableList<Note>> = repo.getAllNotes()
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    val deletedNotes: StateFlow<ImmutableList<Note>> = repo.getDeletedNotes()
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    /** Insert a new note and hand the persisted row (with its assigned id) back,
     *  so the editor can switch from create- to update-mode and avoid duplicate
     *  inserts on subsequent autosaves. */
    fun createNote(title: String, content: String, type: NoteType, callback: (Note) -> Unit) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val note = Note(title = title.trim(), content = content, type = type, createdAt = now, updatedAt = now)
        callback(note.copy(id = repo.addNote(note)))
    }

    fun addNoteForLinking(title: String, type: NoteType, callback: (Long) -> Unit) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val id = repo.addNote(Note(title = "# ${title.trim()}", content = "", type = type, createdAt = now, updatedAt = now))
        callback(id)
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        repo.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
    }

    fun pinNote(note: Note) = viewModelScope.launch {
        repo.updateNote(note.copy(isPinned = !note.isPinned))
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        repo.inTransaction {
            repo.clearLinkedNoteId(note.id)
            repo.softDeleteNote(note)
        }
    }

    fun restoreNote(note: Note) = viewModelScope.launch { repo.restoreNote(note) }

    fun permanentlyDeleteNote(note: Note) = viewModelScope.launch {
        repo.inTransaction {
            repo.clearLinkedNoteId(note.id)
            repo.permanentlyDeleteNote(note)
        }
    }

    fun permanentlyDeleteAllTrash() = viewModelScope.launch { repo.permanentlyDeleteAllTrash() }

    fun deleteAllNotes() = viewModelScope.launch { repo.deleteAllNotes() }

    fun getAllNotesForExport(callback: (List<Note>) -> Unit) = viewModelScope.launch {
        callback(repo.getAllNotesOnce())
    }
}
