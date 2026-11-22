package com.music.vivi.desktop.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music.vivi.desktop.data.backup.AutoBackupScheduler
import com.music.vivi.desktop.domain.repository.SettingsRepository
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_outlined_file_download
import com.music.vivi.desktop.generated.resources.ic_rounded_settings_backup_restore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
fun AutoBackupCard(
    settingsRepository: SettingsRepository = koinInject(),
    scheduler: AutoBackupScheduler = koinInject()
) {
    val settings by settingsRepository.settingsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    SettingsSubsection(title = "AUTOMATIC BACKUP") {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = colors.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_settings_backup_restore),
                        contentDescription = null,
                        tint = colors.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Weekly backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Text(
                            text = if (settings.lastAutoBackupAt > 0L) {
                                "A full backup was last written on ${dateFormat.format(Date(settings.lastAutoBackupAt))}."
                            } else {
                                "A full backup is written while the app is open and a week has passed."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.autoBackupEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { settingsRepository.setAutoBackupEnabled(enabled) }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(Modifier.width(38.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Before updating",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Keep a restore point before a new version is downloaded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.backupBeforeUpdate,
                        onCheckedChange = { enabled ->
                            scope.launch { settingsRepository.setBackupBeforeUpdate(enabled) }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        enabled = !busy,
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.primary),
                        onClick = {
                            busy = true
                            message = null
                            scope.launch {
                                val outcome = withContext(Dispatchers.IO) { scheduler.backupNow() }
                                busy = false
                                message = outcome.fold(
                                    onSuccess = { "Backup written: ${it.name}" },
                                    onFailure = { it.message ?: "The backup failed." }
                                )
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_outlined_file_download),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Back up now")
                    }
                    Text(
                        text = "Five automatic copies are kept.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }

                message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.primary
                    )
                }
            }
        }
    }
}
