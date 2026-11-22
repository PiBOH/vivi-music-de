package com.music.vivi.desktop.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource

import com.music.vivi.desktop.BuildConfig
import com.music.vivi.desktop.domain.model.AppReleaseInfo
import com.music.vivi.desktop.domain.model.UpdateDownloadState
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.ic_automirrored_rounded_open_in_new
import com.music.vivi.desktop.generated.resources.ic_outlined_file_download
import com.music.vivi.desktop.generated.resources.ic_rounded_refresh
import com.music.vivi.desktop.generated.resources.ic_rounded_system_update
import com.music.vivi.desktop.presentation.components.subcomps.AppTooltip

@Composable
fun AppUpdateDialog(
    release: AppReleaseInfo,
    onDismiss: (skipThisVersion: Boolean) -> Unit,
    onOpenRelease: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    downloadState: UpdateDownloadState = UpdateDownloadState.Idle,
    onDownload: (() -> Unit)? = null,
    onInstall: (() -> Unit)? = null
) {
    TrackDialogInput()
    val colors = MaterialTheme.colorScheme
    var skipThisVersion by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDismiss(skipThisVersion) },
        modifier = modifier.width(580.dp),
        icon = {
            Surface(
                shape = CircleShape,
                color = colors.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_rounded_system_update),
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Update Available",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${release.versionName} is available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Installed: v${BuildConfig.APP_VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (release.releaseNotes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceContainerHighest.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "Release notes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = release.releaseNotes.trim(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                ),
                                color = colors.onSurface
                            )
                        }
                    }
                }

                if (onDownload != null || downloadState !is UpdateDownloadState.Idle) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.secondaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (val state = downloadState) {
                                is UpdateDownloadState.Idle -> Text(
                                    text = "The installer is downloaded here and checked against the published " +
                                        "SHA-256 before it can be opened.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                                is UpdateDownloadState.Downloading -> {
                                    val total = state.total.takeIf { it > 0L }
                                    val progress = if (total != null) {
                                        (state.received.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                                    } else 0f
                                    Text(
                                        text = if (total != null) {
                                            "Downloading ${state.received / (1024 * 1024)} of ${total / (1024 * 1024)} MB"
                                        } else {
                                            "Downloading ${state.received / (1024 * 1024)} MB"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurface
                                    )
                                    if (total != null) {
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                                is UpdateDownloadState.Ready -> {
                                    Text(
                                        text = "Downloaded and verified: ${state.file.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.primary
                                    )
                                    AppTooltip("SHA-256 ${state.sha256}") {
                                        Text(
                                            text = "SHA-256 ${state.sha256}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                is UpdateDownloadState.Failed -> Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.error
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { skipThisVersion = !skipThisVersion }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = skipThisVersion,
                        onCheckedChange = { skipThisVersion = it },
                        colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Don't notify me again for this version",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            val ready = downloadState as? UpdateDownloadState.Ready
            val downloading = downloadState is UpdateDownloadState.Downloading
            Button(
                enabled = !downloading,
                onClick = {
                    when {
                        ready != null && onInstall != null -> {
                            onInstall()
                            onDismiss(skipThisVersion)
                        }
                        onDownload != null -> onDownload()
                        else -> {
                            onOpenRelease(release.htmlUrl)
                            onDismiss(skipThisVersion)
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(
                    painter = painterResource(
                        when {
                            ready != null && onInstall != null -> Res.drawable.ic_rounded_system_update
                            onDownload != null && downloadState is UpdateDownloadState.Failed ->
                                Res.drawable.ic_rounded_refresh
                            onDownload != null -> Res.drawable.ic_outlined_file_download
                            else -> Res.drawable.ic_automirrored_rounded_open_in_new
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        ready != null && onInstall != null -> "Install update"
                        downloading -> "Downloading..."
                        onDownload != null && downloadState is UpdateDownloadState.Failed -> "Try again"
                        onDownload != null -> "Download update"
                        else -> "View Release"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss(skipThisVersion) },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = "Later",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}
