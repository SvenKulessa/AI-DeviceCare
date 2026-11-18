package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.cloud.CareNote
import com.example.cloud.CareNoteCategory
import com.example.cloud.CareNotesRepository
import com.example.cloud.CloudNotesUiState
import com.example.cloud.CloudNotesViewModel
import com.example.cloud.CloudValidationBounds
import com.example.cloud.attemptAutoSignIn
import com.example.cloud.authStateFlow
import com.example.cloud.onGoogleSignInClicked
import com.example.cloud.signOutFromGoogle
import com.example.ui.theme.MonoTokenTextStyle
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
  onBack: () -> Unit,
  onShowSnackbar: (String) -> Unit,
  modifier: Modifier = Modifier,
  auth: FirebaseAuth? = null,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  // A missing google-services.json is a supported, fail-closed internal-build state.
  // Never access Firebase.auth until a genuine FirebaseApp exists.
  val configuredAuth = remember(context, auth) {
    auth ?: FirebaseApp.getApps(context).firstOrNull()?.let { FirebaseAuth.getInstance(it) }
  }
  val user = if (configuredAuth == null) {
    null
  } else {
    val currentUser by configuredAuth.authStateFlow()
      .collectAsStateWithLifecycle(initialValue = configuredAuth.currentUser)
    currentUser
  }
  var showEditorDialog by remember { mutableStateOf(false) }
  var editingNote by remember { mutableStateOf<CareNote?>(null) }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("cloud_sync_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Cloud-Sync",
              style = MaterialTheme.typography.titleLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text =
                if (user != null) "Angemeldet · Firestore Sync"
                else "Anmeldung erforderlich",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("cloud_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Zurück",
            )
          }
        },
        actions = {
          if (user != null) {
            IconButton(
              onClick = {
                signOutFromGoogle(
                  credentialManager = credentialManager,
                  scope = scope,
                  onSignOutComplete = { onShowSnackbar("Von Google abgemeldet.") },
                )
              },
              modifier =
                Modifier.minimumInteractiveComponentSize().testTag("google_sign_out_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Abmelden",
              )
            }
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
          ),
      )
    },
    floatingActionButton = {
      if (user != null) {
        FloatingActionButton(
          onClick = {
            editingNote = null
            showEditorDialog = true
          },
          modifier = Modifier.testTag("cloud_add_note_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Neue Cloud-Notiz erstellen",
          )
        }
      }
    },
  ) { innerPadding ->
    if (user == null) {
      // Strict auth gate: no Firestore ViewModel, listener, or query is created here.
      CloudAuthGateContent(
        isConfigured = configuredAuth != null,
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        onSignedIn = { onShowSnackbar("Mit Google angemeldet.") },
      )
    } else {
      key(user.uid) {
        CloudNotesAuthenticatedContent(
          user = user,
          modifier = Modifier.fillMaxSize().padding(innerPadding),
          showEditorDialog = showEditorDialog,
          editingNote = editingNote,
          onOpenEditNote = { note ->
            editingNote = note
            showEditorDialog = true
          },
          onDismissEditor = {
            showEditorDialog = false
            editingNote = null
          },
          onShowSnackbar = onShowSnackbar,
        )
      }
    }
  }
}

@Composable
private fun CloudAuthGateContent(
  isConfigured: Boolean,
  onSignedIn: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var authError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(isConfigured) {
    if (isConfigured) attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onSignedIn,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Box(
    modifier = modifier.padding(16.dp).testTag("cloud_auth_gate"),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
      shape = MaterialTheme.shapes.extraLarge,
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Icon(
            imageVector = Icons.Filled.CloudQueue,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(12.dp).size(26.dp),
          )
        }

        Text(
          text = "Gerätepflege-Notizen synchronisieren",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.semantics { heading() },
        )

        Text(
          text =
            "Melde dich mit Google an, um eigene Wartungschecklisten, Akku-Beobachtungen und QA-Notizen verschlüsselt und kontogebunden in Cloud Firestore zu sichern.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
          text = "Rohpaketlisten installierter Apps werden weiterhin niemals automatisch in die Cloud hochgeladen.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
        )

        if (!isConfigured) {
          Text(
            text = "Cloud-Sync derzeit nicht konfiguriert (NOT_PROVEN). " +
              "Die App benötigt eine gültige Firebase-Konfiguration. " +
              "Lokale Gerätedaten bleiben auf diesem Gerät.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("cloud_unconfigured_notice"),
          )
        }

        if (authError != null) {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.errorContainer,
          ) {
            Text(
              text = authError.orEmpty(),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(12.dp),
            )
          }
        }

        Button(
          onClick = {
            isLoading = true
            authError = null
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onSignedIn()
              },
              onAuthError = { message ->
                isLoading = false
                authError = message
              },
              scope = scope,
              onAuthCancelled = { isLoading = false },
            )
          },
          enabled = isConfigured && !isLoading,
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("google_sign_in_button"),
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Anmeldung läuft…")
          } else {
            Icon(
              imageVector = Icons.Filled.Login,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign in with Google")
          }
        }
      }
    }
  }
}

@Composable
private fun CloudNotesAuthenticatedContent(
  user: FirebaseUser,
  showEditorDialog: Boolean,
  editingNote: CareNote?,
  onOpenEditNote: (CareNote) -> Unit,
  onDismissEditor: () -> Unit,
  onShowSnackbar: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: CloudNotesViewModel =
    viewModel(
      key = user.uid,
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            CloudNotesViewModel(CareNotesRepository(db), user.uid)
          }
        },
    )

  val notesState by viewModel.notesState.collectAsStateWithLifecycle()
  val mutationBusy by viewModel.mutationBusy.collectAsStateWithLifecycle()
  val feedback by viewModel.feedback.collectAsStateWithLifecycle()

  LaunchedEffect(user.uid) {
    viewModel.syncProfile(
      displayName = user.displayName ?: "Google Nutzer",
      photoUrl = user.photoUrl?.toString(),
    )
  }

  LaunchedEffect(feedback) {
    val msg = feedback
    if (msg != null) {
      onShowSnackbar(msg.text)
      viewModel.clearFeedback()
    }
  }

  if (showEditorDialog) {
    CareNoteEditorDialog(
      initialNote = editingNote,
      busy = mutationBusy,
      onDismiss = onDismissEditor,
      onSave = { title, body, category ->
        if (editingNote == null) {
          viewModel.createNote(title = title, note = body, category = category)
        } else {
          viewModel.updateNote(
            noteId = editingNote.id,
            title = title,
            note = body,
            category = category,
            resolved = editingNote.resolved,
          )
        }
        onDismissEditor()
      },
    )
  }

  LazyColumn(
    modifier = modifier.fillMaxSize().testTag("cloud_notes_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
        shape = MaterialTheme.shapes.large,
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.14f),
          ) {
            Icon(
              imageVector = Icons.Filled.CloudDone,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(10.dp).size(20.dp),
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = user.displayName ?: "Google Konto",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
              text = "Eigener Firestore-Speicher aktiv · Echtzeit-Sync",
              style = MonoTokenTextStyle,
              color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
            )
          }
        }
      }
    }

    when (val state = notesState) {
      is CloudNotesUiState.Loading -> {
        item {
          Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
        }
      }
      is CloudNotesUiState.Error -> {
        item {
          Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
            shape = MaterialTheme.shapes.large,
            colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
              )
              Text(
                text = state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
            }
          }
        }
      }
      is CloudNotesUiState.Success -> {
        if (state.notes.isEmpty()) {
          item {
            Card(
              modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
              shape = MaterialTheme.shapes.large,
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
              border =
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = "Noch keine Cloud-Notizen vorhanden",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text =
                    "Tippe unten rechts auf +, um eine Wartungs- oder Prüfpunkt-Notiz kontogebunden in Cloud Firestore abzulegen.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          items(items = state.notes, key = { it.id }) { note ->
            Box(modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp)) {
              CareNoteCard(
                note = note,
                onToggleResolved = { checked -> viewModel.toggleResolved(note.id, checked) },
                onEdit = { onOpenEditNote(note) },
                onDelete = { viewModel.deleteNote(note.id) },
              )
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(72.dp)) }
  }
}

@Composable
private fun CareNoteCard(
  note: CareNote,
  onToggleResolved: (Boolean) -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val displayTs = note.updatedAt ?: note.createdAt ?: Timestamp.now()
  val formattedDate =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.GERMAN)
      .format(displayTs.toDate())

  Card(
    shape = MaterialTheme.shapes.large,
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Checkbox(
        checked = note.resolved,
        onCheckedChange = onToggleResolved,
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text = note.categoryEnum.label,
              style = MonoTokenTextStyle,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          Text(
            text = formattedDate,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = note.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          textDecoration = if (note.resolved) TextDecoration.LineThrough else null,
        )
        if (note.note.isNotBlank()) {
          Text(
            text = note.note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      IconButton(
        onClick = onEdit,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(
          imageVector = Icons.Filled.Edit,
          contentDescription = "Notiz bearbeiten",
        )
      }
      IconButton(
        onClick = onDelete,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(
          imageVector = Icons.Filled.DeleteOutline,
          contentDescription = "Notiz löschen",
          tint = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CareNoteEditorDialog(
  initialNote: CareNote?,
  busy: Boolean,
  onDismiss: () -> Unit,
  onSave: (String, String, CareNoteCategory) -> Unit,
) {
  var title by rememberSaveable { mutableStateOf(initialNote?.title.orEmpty()) }
  var body by rememberSaveable { mutableStateOf(initialNote?.note.orEmpty()) }
  var category by remember {
    mutableStateOf(initialNote?.categoryEnum ?: CareNoteCategory.GENERAL)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (initialNote == null) "Neue Cloud-Notiz" else "Cloud-Notiz bearbeiten")
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = {
            if (it.length <= CloudValidationBounds.MAX_TITLE_LENGTH) title = it
          },
          label = { Text("Titel") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = body,
          onValueChange = {
            if (it.length <= CloudValidationBounds.MAX_NOTE_LENGTH) body = it
          },
          label = { Text("Details (optional)") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          CareNoteCategory.entries.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat.label) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(title, body, category) },
        enabled = title.isNotBlank() && !busy,
      ) {
        Text("Speichern")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Abbrechen") }
    },
  )
}
