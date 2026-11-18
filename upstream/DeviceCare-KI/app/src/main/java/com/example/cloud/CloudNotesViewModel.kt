package com.example.cloud

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CloudNotesUiState {
  data object Loading : CloudNotesUiState

  data class Success(val notes: List<CareNote>) : CloudNotesUiState

  data class Error(val message: String) : CloudNotesUiState
}

data class CloudFeedbackMessage(
  val isError: Boolean,
  val text: String,
)

class CloudNotesViewModel(
  private val repository: CareNotesRepository,
  private val currentUserId: String,
) : ViewModel() {

  val notesState: StateFlow<CloudNotesUiState> =
    repository
      .observeCareNotes(currentUserId)
      .map<List<CareNote>, CloudNotesUiState> { CloudNotesUiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing care_notes", rootError)
        emit(
          CloudNotesUiState.Error(
            rootError.localizedMessage?.takeIf { it.isNotBlank() }
              ?: "Cloud-Notizen konnten nicht geladen werden."
          )
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CloudNotesUiState.Loading,
      )

  private val _mutationBusy = MutableStateFlow(false)
  val mutationBusy: StateFlow<Boolean> = _mutationBusy.asStateFlow()

  private val _feedback = MutableStateFlow<CloudFeedbackMessage?>(null)
  val feedback: StateFlow<CloudFeedbackMessage?> = _feedback.asStateFlow()

  fun clearFeedback() {
    _feedback.value = null
  }

  fun syncProfile(displayName: String, photoUrl: String?) {
    viewModelScope.launch {
      repository
        .syncUserProfile(
          userId = currentUserId,
          displayName = displayName,
          photoUrl = photoUrl,
        )
        .onFailure { error ->
          _feedback.value =
            CloudFeedbackMessage(
              isError = true,
              text = error.localizedMessage ?: "Profil-Synchronisation fehlgeschlagen.",
            )
        }
    }
  }

  fun createNote(title: String, note: String, category: CareNoteCategory) {
    val trimmedTitle = title.trim()
    val trimmedNote = note.trim()
    if (trimmedTitle.isEmpty()) {
      _feedback.value = CloudFeedbackMessage(isError = true, text = "Bitte einen Titel eingeben.")
      return
    }
    if (trimmedTitle.length > CloudValidationBounds.MAX_TITLE_LENGTH) {
      _feedback.value =
        CloudFeedbackMessage(
          isError = true,
          text = "Titel darf maximal ${CloudValidationBounds.MAX_TITLE_LENGTH} Zeichen lang sein.",
        )
      return
    }
    if (trimmedNote.length > CloudValidationBounds.MAX_NOTE_LENGTH) {
      _feedback.value =
        CloudFeedbackMessage(
          isError = true,
          text = "Notiz darf maximal ${CloudValidationBounds.MAX_NOTE_LENGTH} Zeichen enthalten.",
        )
      return
    }
    if (_mutationBusy.value) return
    _mutationBusy.value = true

    viewModelScope.launch {
      try {
        repository
          .createCareNote(
            userId = currentUserId,
            title = trimmedTitle,
            note = trimmedNote,
            category = category,
          )
          .onSuccess {
            _feedback.value =
              CloudFeedbackMessage(isError = false, text = "Cloud-Notiz gespeichert.")
          }
          .onFailure { error ->
            _feedback.value =
              CloudFeedbackMessage(
                isError = true,
                text = error.localizedMessage ?: "Notiz konnte nicht gespeichert werden.",
              )
          }
      } finally {
        _mutationBusy.value = false
      }
    }
  }

  fun toggleResolved(noteId: String, resolved: Boolean) {
    viewModelScope.launch {
      repository
        .toggleCareNoteResolved(currentUserId, noteId, resolved)
        .onFailure { error ->
          _feedback.value =
            CloudFeedbackMessage(
              isError = true,
              text = error.localizedMessage ?: "Status konnte nicht aktualisiert werden.",
            )
        }
    }
  }

  fun updateNote(
    noteId: String,
    title: String,
    note: String,
    category: CareNoteCategory,
    resolved: Boolean,
  ) {
    val trimmedTitle = title.trim()
    val trimmedNote = note.trim()
    if (trimmedTitle.isEmpty()) {
      _feedback.value = CloudFeedbackMessage(isError = true, text = "Bitte einen Titel eingeben.")
      return
    }
    if (_mutationBusy.value) return
    _mutationBusy.value = true

    viewModelScope.launch {
      try {
        repository
          .updateCareNote(
            userId = currentUserId,
            noteId = noteId,
            title = trimmedTitle,
            note = trimmedNote,
            category = category,
            resolved = resolved,
          )
          .onSuccess {
            _feedback.value =
              CloudFeedbackMessage(isError = false, text = "Cloud-Notiz aktualisiert.")
          }
          .onFailure { error ->
            _feedback.value =
              CloudFeedbackMessage(
                isError = true,
                text = error.localizedMessage ?: "Änderung fehlgeschlagen.",
              )
          }
      } finally {
        _mutationBusy.value = false
      }
    }
  }

  fun deleteNote(noteId: String) {
    viewModelScope.launch {
      repository
        .deleteCareNote(currentUserId, noteId)
        .onSuccess {
          _feedback.value =
            CloudFeedbackMessage(isError = false, text = "Cloud-Notiz gelöscht.")
        }
        .onFailure { error ->
          _feedback.value =
            CloudFeedbackMessage(
              isError = true,
              text = error.localizedMessage ?: "Löschen fehlgeschlagen.",
            )
        }
    }
  }

  private companion object {
    const val TAG = "CloudNotesVM"
    const val STOP_TIMEOUT_MILLIS = 5_000L
  }
}
