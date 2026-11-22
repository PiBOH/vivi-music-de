package com.music.vivi.desktop.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.utils.DesktopPathProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * What the desktop actually knows about sync.
 *
 * This build has no relay client: the phone writes the pairing identity into
 * `device-sync.json`, and the desktop preserves those keys. The card therefore
 * reports the file that holds them, its age and whether the rotated copy is
 * older, instead of pretending to show a live connection.
 */
@Composable
fun SyncStatusCard() {
    val colors = MaterialTheme.colorScheme
    var revision by remember { mutableIntStateOf(0) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val storeFile = DesktopPathProvider.syncStateFile
    val backupFile = remember { File(storeFile.absoluteFile.parentFile, "${storeFile.name}.bak") }
    val details = remember(revision) { readStoreState(storeFile, backupFile) }

    SettingsSubsection(title = "SYNC AND STORE STATUS") {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "device-sync.json",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                StatusLine("Store", details.storeLine)
                StatusLine("Rotated copy", details.backupLine)
                StatusLine("Sync identity", details.identityLine)
                Text(
                    text = "This build reads and keeps the sync identity in this file, but it does not " +
                        "open a live connection to the phone relay. Use Backup & Restore to move the " +
                        "library between devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.primary),
                        onClick = { revision++ }
                    ) {
                        Text("Refresh")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.65f)
        )
    }
}

private data class StoreState(
    val storeLine: String,
    val backupLine: String,
    val identityLine: String
)

private fun readStoreState(storeFile: File, backupFile: File): StoreState {
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val storeLine = if (storeFile.isFile) {
        "${storeFile.absolutePath} (${storeFile.length()} bytes, ${format.format(Date(storeFile.lastModified()))})"
    } else {
        "Not written yet at ${storeFile.absolutePath}"
    }
    val backupLine = if (backupFile.isFile) {
        "device-sync.json.bak (${backupFile.length()} bytes, ${format.format(Date(backupFile.lastModified()))})"
    } else {
        "No rotated copy yet"
    }
    val identity = if (!storeFile.isFile) {
        "No store to read."
    } else {
        runCatching {
            val root = Json { ignoreUnknownKeys = true }
                .parseToJsonElement(storeFile.readText())
                .jsonObject
            val keys = root.keys.filter { it != "localLibrary" && it != "libraryImport" }.sorted()
            if (keys.isEmpty()) {
                "No pairing or device keys are present."
            } else {
                "Preserved keys: ${keys.joinToString(", ")}"
            }
        }.getOrElse { "The store cannot be parsed: ${it.message}" }
    }
    return StoreState(storeLine, backupLine, identity)
}
