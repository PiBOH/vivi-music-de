package com.music.vivi.desktop.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.data.store.LibraryImportRecord
import com.music.vivi.desktop.data.store.LocalLibraryStore
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_outlined_folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The one-time import from `vivimusicde.db`, made visible: where the library came
 * from, when, and a way to run the import again. The old database is only read;
 * the current library is rotated into `device-sync.json.bak` before it is replaced.
 */
@Composable
fun LegacyLibraryCard(
    store: LocalLibraryStore = koinInject(),
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var record by remember { mutableStateOf(store.importRecord()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val subtitle = when {
        busy -> "Importing the old library..."
        record != null -> {
            val imported = record!!
            "${imported.songs} songs, ${imported.playlists} playlists, ${imported.favorites} favorites\n" +
                "${imported.sourcePath}\nImported ${dateFormat.format(Date(imported.importedAt))}"
        }
        else -> "No import from the old library has been recorded yet."
    }

    SettingsSubsection(title = "LEGACY LIBRARY") {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceContainer,
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_outlined_folder),
                        contentDescription = null,
                        tint = colors.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Import from the old library",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(
                        onClick = { confirm = true },
                        enabled = !busy,
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                    ) {
                        Text("Reimport")
                    }
                }

                Text(
                    text = "Reimporting replaces the current library with the old database. " +
                        "The current file is kept as device-sync.json.bak.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                message?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.primary
                    )
                }
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Reimport the old library?") },
            text = {
                Text(
                    "The current library will be replaced with the contents of the old database. " +
                        "The current file is kept next to it as device-sync.json.bak."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = false
                        busy = true
                        message = null
                        scope.launch {
                            val outcome = withContext(Dispatchers.IO) { store.reimportFromLegacy() }
                            busy = false
                            outcome.onSuccess { imported ->
                                record = imported
                                message = "Imported ${imported.songs} songs and ${imported.playlists} playlists."
                            }.onFailure { error ->
                                message = error.message ?: "The import failed."
                            }
                        }
                    }
                ) { Text("Reimport", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Cancel") }
            }
        )
    }
}
