package com.omastro.app.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.data.model.rashiGlyph
import com.omastro.app.ui.theme.*

/** One planet placed in a house: display name + Jyotish glyph (☉ ☽ ♂ ☿ ♃ ♀ ♄ ☊ ☋). */
data class PlanetMark(val name: String, val symbol: String)

/**
 * Authentic North Indian Vedic Kundali diamond chart drawn on Compose Canvas.
 *
 * Gold lines on a dark cosmic background; each of the 12 bhavas shows its
 * rashi glyph (♈..♓) and the occupying planets' Jyotish glyphs. House 1 is
 * always the Lagna — marked with a gold "Lg" badge.
 */
@Composable
fun VedicDiamondChart(
    lagnaSign: Int = 1,
    housePlanets: Map<Int, List<PlanetMark>> = emptyMap(),
    chartTitle: String = "Lagna Kundali (D1)",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CosmicSurface, RoundedCornerShape(16.dp))
            .border(1.dp, CosmicBorderGold, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = chartTitle,
            color = SacredGold,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(CosmicCardBg, RoundedCornerShape(8.dp))
                .border(1.5.dp, SacredGold, RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val lineColor = SacredGold
                val strokeWidth = 3.5f
                val thinWidth = 2f

                // Outer boundary
                drawRect(
                    color = lineColor,
                    topLeft = Offset(0f, 0f),
                    size = size,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )

                // Two diagonals corner-to-corner
                drawLine(color = lineColor, start = Offset(0f, 0f), end = Offset(w, h), strokeWidth = strokeWidth)
                drawLine(color = lineColor, start = Offset(w, 0f), end = Offset(0f, h), strokeWidth = strokeWidth)

                // Central diamond through edge midpoints
                val midTop = Offset(w / 2f, 0f)
                val midRight = Offset(w, h / 2f)
                val midBottom = Offset(w / 2f, h)
                val midLeft = Offset(0f, h / 2f)
                drawLine(color = lineColor, start = midTop, end = midRight, strokeWidth = strokeWidth)
                drawLine(color = lineColor, start = midRight, end = midBottom, strokeWidth = strokeWidth)
                drawLine(color = lineColor, start = midBottom, end = midLeft, strokeWidth = strokeWidth)
                drawLine(color = lineColor, start = midLeft, end = midTop, strokeWidth = strokeWidth)

                // House number anchors (small, dim)
                val houseNumberPaint = Paint().apply {
                    color = TextSecondary.copy(alpha = 0.55f).toArgb()
                    textSize = 26f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                // Rashi glyph paint — large, gold
                val rashiPaint = Paint().apply {
                    color = SacredGoldLight.toArgb()
                    textSize = 52f
                    isFakeBoldText = true
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                // Planet glyph paint — white-gold
                val planetPaint = Paint().apply {
                    color = Color.White.toArgb()
                    textSize = 40f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                // Lagna badge paint
                val lagnaPaint = Paint().apply {
                    color = SacredGold.toArgb()
                    textSize = 30f
                    isFakeBoldText = true
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }

                // House centers in the North-Indian layout:
                // 1 = top diamond (Lagna), 4/7/10 = side/bottom diamonds,
                // 2,3,5,6,8,9,11,12 = corner triangles.
                val houseCenters = mapOf(
                    1 to Offset(w * 0.50f, h * 0.26f),
                    2 to Offset(w * 0.25f, h * 0.11f),
                    3 to Offset(w * 0.11f, h * 0.25f),
                    4 to Offset(w * 0.26f, h * 0.50f),
                    5 to Offset(w * 0.11f, h * 0.75f),
                    6 to Offset(w * 0.25f, h * 0.89f),
                    7 to Offset(w * 0.50f, h * 0.74f),
                    8 to Offset(w * 0.75f, h * 0.89f),
                    9 to Offset(w * 0.89f, h * 0.75f),
                    10 to Offset(w * 0.74f, h * 0.50f),
                    11 to Offset(w * 0.89f, h * 0.25f),
                    12 to Offset(w * 0.75f, h * 0.11f)
                )
                // Small house-number offsets (toward each cell's corner)
                val numberOffsets = mapOf(
                    1 to Offset(0f, -52f), 2 to Offset(-34f, -30f), 3 to Offset(-44f, 0f),
                    4 to Offset(-52f, 0f), 5 to Offset(-44f, 0f), 6 to Offset(-34f, 30f),
                    7 to Offset(0f, 52f), 8 to Offset(34f, 30f), 9 to Offset(44f, 0f),
                    10 to Offset(52f, 0f), 11 to Offset(44f, 0f), 12 to Offset(34f, -30f)
                )

                for (house in 1..12) {
                    val center = houseCenters[house] ?: continue
                    val rashi = ((lagnaSign - 1 + (house - 1)) % 12) + 1
                    val marks = housePlanets[house] ?: emptyList()
                    val numOff = numberOffsets[house] ?: Offset.Zero

                    drawContext.canvas.nativeCanvas.apply {
                        // House number (tiny, dim)
                        drawText("$house", center.x + numOff.x, center.y + numOff.y, houseNumberPaint)
                        // Rashi glyph (large, gold)
                        drawText(rashiGlyph(rashi), center.x, center.y - 6f, rashiPaint)
                        // Lagna badge on house 1
                        if (house == 1) {
                            drawText("Lg", center.x, center.y - 52f, lagnaPaint)
                        }
                        // Planet glyphs
                        if (marks.isNotEmpty()) {
                            val glyphs = marks.joinToString(" ") { it.symbol.ifBlank { it.name.take(2) } }
                            drawText(glyphs, center.x, center.y + 52f, planetPaint)
                        }
                    }
                }

                // Soft divider emphasis between the two diagonal axes
                drawCircle(
                    color = lineColor.copy(alpha = 0.25f),
                    radius = 4f,
                    center = Offset(w / 2f, h / 2f)
                )
                // Silence unused-warning clarity: thinWidth reserved for future grid lines
                @Suppress("UNUSED_EXPRESSION") thinWidth
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Lg = Lagna  •  glyphs: ☉ ☽ ♂ ☿ ♃ ♀ ♄ ☊ ☋",
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}
