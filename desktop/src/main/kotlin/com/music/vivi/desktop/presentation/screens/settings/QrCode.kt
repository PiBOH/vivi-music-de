package com.music.vivi.desktop.presentation.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Renders [text] as a QR code using ZXing, drawn directly with a Compose
 * [Canvas] (no AWT image round-trip), exactly as 1.55.0 did. A small white
 * quiet zone is kept around the code so phone scanners can read it reliably.
 */
@Composable
fun QrCode(text: String, size: Dp = 180.dp) {
    val matrix: BitMatrix? = remember(text) {
        runCatching {
            QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 512, 512)
        }.getOrNull()
    }
    if (matrix == null) return

    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val dim = matrix.width
            val margin = size.toPx() * 0.04f
            val qrSize = size.toPx() - 2 * margin
            val cell = qrSize / dim
            for (y in 0 until dim) {
                for (x in 0 until dim) {
                    if (matrix.get(x, y)) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(margin + x * cell, margin + y * cell),
                            size = Size(cell + 0.5f, cell + 0.5f),
                        )
                    }
                }
            }
        }
    }
}
