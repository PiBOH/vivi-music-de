package com.music.vivi.desktop.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.music.vivi.desktop.generated.resources.Res
import com.music.vivi.desktop.generated.resources.vivi_app_logo
import org.jetbrains.compose.resources.painterResource

/**
 * The official VIVI Music DE mark.
 *
 * The raster source is the repository's official 1024 px icon, which already
 * carries a transparent circle around the mark, so it sits on any surface
 * without a white square behind it and keeps its detail at small sizes.
 * A caller controls the size; the logo is never tinted, because its blue-violet
 * ring is part of the brand.
 */
@Composable
fun ViviLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(Res.drawable.vivi_app_logo),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(CircleShape),
    )
}
