package com.example.cloud

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CareNotesRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException(
        "Anmeldung mit Google erforderlich, bevor Cloud-Daten genutzt werden."
      )

  private fun notesCollection(userId: String) =
    db.collection("users").document(userId).collection("care_notes")

  fun observeCareNotes(userId: String): Flow<List<CareNote>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Benutzerwechsel erkannt; Abbruch alter Abfrage." }
    val path = "users/$uid/care_notes"
    emitAll(
      notesCollection(uid)
        .orderBy("updatedAt", Query.Direction.DESCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            CareNote::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun syncUserProfile(
    userId: String,
    displayName: String,
    photoUrl: String?,
  ): Result<Unit> {
    var docPath = "users/$userId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Abweichende Benutzer-ID." }
      val cleanName =
        displayName.trim().ifEmpty { "Gerätenutzer" }.take(CloudValidationBounds.MAX_DISPLAY_NAME_LENGTH)
      val cleanPhoto =
        photoUrl?.trim()?.takeIf { it.isNotEmpty() }?.take(CloudValidationBounds.MAX_PHOTO_URL_LENGTH)

      val docRef = db.collection("users").document(uid)
      docPath = docRef.path
      val existingSnapshot = docRef.get().await()
      if (!existingSnapshot.exists()) {
        val createPayload =
          mapOf(
            "userId" to uid,
            "displayName" to cleanName,
            "photoUrl" to cleanPhoto,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        docRef.set(createPayload).await()
      } else {
        val updatePayload =
          mapOf(
            "displayName" to cleanName,
            "photoUrl" to cleanPhoto,
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        docRef.update(updatePayload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun createCareNote(
    userId: String,
    title: String,
    note: String,
    category: CareNoteCategory,
  ): Result<Unit> {
    var docPath = "users/$userId/care_notes"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Abweichende Benutzer-ID." }
      val trimmedTitle = title.trim()
      val trimmedNote = note.trim()
      require(trimmedTitle.length in 1..CloudValidationBounds.MAX_TITLE_LENGTH) {
        "Titel muss zwischen 1 und ${CloudValidationBounds.MAX_TITLE_LENGTH} Zeichen lang sein."
      }
      require(trimmedNote.length <= CloudValidationBounds.MAX_NOTE_LENGTH) {
        "Notiz darf maximal ${CloudValidationBounds.MAX_NOTE_LENGTH} Zeichen enthalten."
      }

      val docRef = notesCollection(uid).document()
      docPath = docRef.path
      val payload =
        mapOf(
          "userId" to uid,
          "title" to trimmedTitle,
          "note" to trimmedNote,
          "category" to category.code,
          "resolved" to false,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun toggleCareNoteResolved(
    userId: String,
    noteId: String,
    resolved: Boolean,
  ): Result<Unit> {
    var docPath = "users/$userId/care_notes/$noteId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Abweichende Benutzer-ID." }
      require(noteId.isNotBlank()) { "Notiz-ID fehlt." }
      val docRef = notesCollection(uid).document(noteId)
      docPath = docRef.path
      docRef
        .update(
          mapOf(
            "resolved" to resolved,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateCareNote(
    userId: String,
    noteId: String,
    title: String,
    note: String,
    category: CareNoteCategory,
    resolved: Boolean,
  ): Result<Unit> {
    var docPath = "users/$userId/care_notes/$noteId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Abweichende Benutzer-ID." }
      require(noteId.isNotBlank()) { "Notiz-ID fehlt." }
      val trimmedTitle = title.trim()
      val trimmedNote = note.trim()
      require(trimmedTitle.length in 1..CloudValidationBounds.MAX_TITLE_LENGTH) {
        "Titel muss zwischen 1 und ${CloudValidationBounds.MAX_TITLE_LENGTH} Zeichen lang sein."
      }
      require(trimmedNote.length <= CloudValidationBounds.MAX_NOTE_LENGTH) {
        "Notiz darf maximal ${CloudValidationBounds.MAX_NOTE_LENGTH} Zeichen enthalten."
      }
      val docRef = notesCollection(uid).document(noteId)
      docPath = docRef.path
      docRef
        .update(
          mapOf(
            "title" to trimmedTitle,
            "note" to trimmedNote,
            "category" to category.code,
            "resolved" to resolved,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteCareNote(userId: String, noteId: String): Result<Unit> {
    var docPath = "users/$userId/care_notes/$noteId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Abweichende Benutzer-ID." }
      require(noteId.isNotBlank()) { "Notiz-ID fehlt." }
      val docRef = notesCollection(uid).document(noteId)
      docPath = docRef.path
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }
}
