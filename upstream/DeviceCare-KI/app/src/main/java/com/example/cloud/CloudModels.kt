package com.example.cloud

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class CareNoteCategory(val code: String, val label: String) {
  GENERAL("GENERAL", "Allgemein"),
  BATTERY("BATTERY", "Akku"),
  PERMISSIONS("PERMISSIONS", "Berechtigungen"),
  QA("QA", "Android 15 QA");

  companion object {
    fun fromCode(raw: String?): CareNoteCategory =
      entries.firstOrNull { it.code == raw } ?: GENERAL
  }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
  val userId: String = "",
  val displayName: String = "",
  val photoUrl: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CareNote(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val note: String = "",
  val category: String = CareNoteCategory.GENERAL.code,
  val resolved: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val categoryEnum: CareNoteCategory
    get() = CareNoteCategory.fromCode(category)
}

object CloudValidationBounds {
  const val MAX_DISPLAY_NAME_LENGTH = 80
  const val MAX_PHOTO_URL_LENGTH = 500
  const val MAX_TITLE_LENGTH = 120
  const val MAX_NOTE_LENGTH = 1000
}
