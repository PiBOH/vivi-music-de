package com.music.vivi.desktop.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.data.diagnostics.DiagnosticExport
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_outlined_file_upload
import com.music.vivi.desktop.utils.DesktopPathProvider
import com.music.vivi.desktop.utils.DirectoryPicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource

@Composable
fun DiagnosticsCard() {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    SettingsSubsection(title = "DIAGNOSTICS") {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Diagnostic package",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = "Version, system, settings, log files and library counts in one zip. " +
                        "Tokens, passwords and cookie values are removed, and the library itself is not included.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Row {
                    Button(
                        enabled = !busy,
                        onClick = {
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            DirectoryPicker.saveDiagnosticsFile(
                                defaultFileName = "VIVIMusicDE_Diagnostics_$stamp.zip"
                            ) { target ->
                                busy = true
                                message = null
                                scope.launch {
                                    val outcome = withContext(Dispatchers.IO) {
                                        DiagnosticExport.export(
                                            target = target,
                                            settingsFile = DesktopPathProvider.settingsFile,
                                            storeFile = DesktopPathProvider.syncStateFile
                                        )
                                    }
                                    busy = false
                                    message = outcome.fold(
                                        onSuccess = { true to "Package written: ${it.name}" },
                                        onFailure = { false to (it.message ?: "The package could not be written.") }
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_outlined_file_upload),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Export diagnostics")
                    }
                }
                message?.let { (success, text) ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (success) colors.primary else colors.error
                    )
                }
            }
        }
    }
}
