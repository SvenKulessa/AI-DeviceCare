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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.AiAdvisorUiState
import com.example.ai.AiChatMessage
import com.example.ai.AiUiState
import com.example.ui.theme.MonoTokenTextStyle

private data class QuickPromptSuggestion(
  val label: String,
  val prompt: String,
)

private val quickPromptSuggestions =
  listOf(
    QuickPromptSuggestion(
      label = "Kamera & Mikrofon Risiko",
      prompt =
        "Welche Risiken entstehen, wenn Apps dauerhaft Kamera-, Standort- oder Mikrofonrechte besitzen, und wie prüfe ich das sinnvoll in Android?",
    ),
    QuickPromptSuggestion(
      label = "Akku schonend laden",
      prompt =
        "Gib mir 4 praxisnahe Tipps für eine lange Akkulebensdauer bei aktuellen Android-Smartphones.",
    ),
    QuickPromptSuggestion(
      label = "Rechte-Checkliste",
      prompt =
        "Erstelle eine kompakte Checkliste, welche Android-Laufzeitberechtigungen man regelmäßig überprüfen sollte.",
    ),
  )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiAdvisorScreen(
  aiState: AiAdvisorUiState,
  hasOptionalContextAvailable: Boolean,
  onBack: () -> Unit,
  onSendPrompt: (String) -> Unit,
  onToggleContext: () -> Unit,
  onClearChat: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  var promptDraft by rememberSaveable { mutableStateOf("") }
  val listState = rememberLazyListState()

  val streamingText = (aiState.status as? AiUiState.Streaming)?.text

  LaunchedEffect(aiState.messages.size, streamingText?.length) {
    val targetIndex =
      aiState.messages.size + (if (aiState.status is AiUiState.Streaming || aiState.status is AiUiState.Loading) 1 else 0)
    if (targetIndex > 0) {
      listState.animateScrollToItem(targetIndex)
    }
  }

  fun submitCurrentPrompt() {
    val trimmed = promptDraft.trim()
    if (trimmed.isNotEmpty() && !aiState.isGenerating) {
      promptDraft = ""
      onSendPrompt(trimmed)
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize().imePadding().testTag("ai_advisor_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "KI-Berater",
              style = MaterialTheme.typography.titleLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "Gemini 3.8 Flash · Firebase AI Logic",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("ai_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Zurück",
            )
          }
        },
        actions = {
          if (aiState.messages.isNotEmpty() && !aiState.isGenerating) {
            IconButton(
              onClick = onClearChat,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("ai_clear_chat_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.DeleteSweep,
                contentDescription = "Chat leeren",
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
    bottomBar = {
      Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedTextField(
              value = promptDraft,
              onValueChange = { promptDraft = it },
              placeholder = { Text("Frage zu Akku oder App-Rechten stellen…") },
              singleLine = false,
              maxLines = 3,
              enabled = !aiState.isGenerating,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
              keyboardActions = KeyboardActions(onSend = { submitCurrentPrompt() }),
              modifier = Modifier.weight(1f).testTag("ai_prompt_input"),
            )
            FilledIconButton(
              onClick = { submitCurrentPrompt() },
              enabled = promptDraft.isNotBlank() && !aiState.isGenerating,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("ai_send_button"),
            ) {
              if (aiState.isGenerating) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
              } else {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Send,
                  contentDescription = "An Gemini senden",
                )
              }
            }
          }
        }
      }
    },
  ) { innerPadding ->
    LazyColumn(
      state = listState,
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .semantics { liveRegion = LiveRegionMode.Polite },
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
          shape = MaterialTheme.shapes.large,
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                  imageVector = Icons.Filled.AutoAwesome,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(8.dp).size(18.dp),
                )
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Datenschutz-gesteuerte Gemini-Hilfe",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.semantics { heading() },
                )
                Text(
                  text = "Nur auf explizite Eingabe. Kein automatischer Hintergrund-Upload.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.PrivacyTip,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
                Column {
                  Text(
                    text = "Anonymisierte Kennzahlen anhängen",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text =
                      if (hasOptionalContextAvailable)
                        "Nur Akku-% & Berechtigungs-Summen, keine Paketnamen."
                      else "Aktiviere bei Bedarf für Akku-/Rechte-Summen (keine Paketnamen).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
              Switch(
                checked = aiState.includeAnonymizedContext,
                onCheckedChange = { onToggleContext() },
                modifier = Modifier.testTag("ai_context_toggle"),
              )
            }

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              quickPromptSuggestions.forEach { suggestion ->
                AssistChip(
                  onClick = {
                    if (!aiState.isGenerating) {
                      onSendPrompt(suggestion.prompt)
                    }
                  },
                  label = { Text(suggestion.label) },
                  enabled = !aiState.isGenerating,
                )
              }
            }
          }
        }
      }

      if (aiState.messages.isEmpty() && aiState.status is AiUiState.Idle) {
        item {
          Box(
            modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp).padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text =
                "Wähle oben einen Schnell-Tipp oder stelle eine Frage zu Akkuzustand, Energiesparmodus oder App-Berechtigungen.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      items(items = aiState.messages, key = { it.id }) { msg ->
        Box(modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp)) {
          AiChatBubble(message = msg)
        }
      }

      if (aiState.status is AiUiState.Loading || aiState.status is AiUiState.Streaming) {
        item {
          val liveText = (aiState.status as? AiUiState.Streaming)?.text
          Box(modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp)) {
            Card(
              shape = MaterialTheme.shapes.large,
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                  Text(
                    text = if (liveText == null) "Gemini denkt nach…" else "Gemini antwortet…",
                    style = MonoTokenTextStyle,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                }
                if (!liveText.isNullOrBlank()) {
                  Text(
                    text = liveText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                }
              }
            }
          }
        }
      }

      if (aiState.status is AiUiState.Error) {
        item {
          Box(modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp)) {
            Card(
              modifier = Modifier.fillMaxWidth().testTag("ai_error_banner"),
              shape = MaterialTheme.shapes.large,
              colors =
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Outlined.ErrorOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                  text = aiState.status.message,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                )
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(8.dp)) }
    }
  }
}

@Composable
private fun AiChatBubble(message: AiChatMessage) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
  ) {
    Card(
      modifier = Modifier.widthIn(max = 640.dp),
      shape = MaterialTheme.shapes.large,
      colors =
        CardDefaults.cardColors(
          containerColor =
            if (message.isUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = if (message.isUser) "Du" else "Gemini KI-Berater",
          style = MonoTokenTextStyle,
          color =
            if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
            else MaterialTheme.colorScheme.primary,
        )
        Text(
          text = message.text,
          style = MaterialTheme.typography.bodyMedium,
          color =
            if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}
