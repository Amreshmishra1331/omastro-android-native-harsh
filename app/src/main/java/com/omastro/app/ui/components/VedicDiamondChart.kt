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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.ui.theme.*

/**
 * Authentic North Indian Vedic Kundali Diamond Chart drawn natively on Jetpack Compose Canvas.
 * Displays all 12 Bhavas (Houses), Rashi signs, and located Grahas (Planets).
 */
@Composable
fun VedicDiamondChart(
    ascendantSign: Int = 1,
    housesPlanets: Map<String, List<String>> = emptyMap(),
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
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .size(320.dp)
                .background(CosmicCardBg, RoundedCornerShape(8.dp))
                .border(1.5.dp, SacredGold, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val strokeColor = SacredGoldDark
                val strokeWidth = 3f

                // Outer boundary
                drawRect(
                    color = strokeColor,
                    topLeft = Offset(0f, 0f),
                    size = size,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )

                // 1. Two diagonal cross lines from corners
                drawLine(
                    color = strokeColor,
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = strokeColor,
                    start = Offset(w, 0f),
                    end = Offset(0f, h),
                    strokeWidth = strokeWidth
                )

                // 2. Central diamond connecting midpoints of the four edges
                val midTop = Offset(w / 2f, 0f)
                val midRight = Offset(w, h / 2f)
                val midBottom = Offset(w / 2f, h)
                val midLeft = Offset(0f, h / 2f)

                drawLine(color = strokeColor, start = midTop, end = midRight, strokeWidth = strokeWidth)
                drawLine(color = strokeColor, start = midRight, end = midBottom, strokeWidth = strokeWidth)
                drawLine(color = strokeColor, start = midBottom, end = midLeft, strokeWidth = strokeWidth)
                drawLine(color = strokeColor, start = midLeft, end = midTop, strokeWidth = strokeWidth)

                // Paint for House numbers and Planets
                val numberPaint = Paint().apply {
                    color = TextSecondary.toArgb()
                    textSize = 30f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }

                val planetPaint = Paint().apply {
                    color = SacredGoldLight.toArgb()
                    textSize = 34f
                    isFakeBoldText = true
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }

                // Coordinates for 12 Bhavas (North Indian style)
                // House 1: Lagna (Center top diamond)
                // House 2: Top left upper triangle
                // House 3: Top left lower triangle
                // House 4: Left center diamond
                // House 5: Bottom left upper triangle
                // House 6: Bottom left lower triangle
                // House 7: Center bottom diamond
                // House 8: Bottom right lower triangle
                // House 9: Bottom right upper triangle
                // House 10: Right center diamond
                // House 11: Top right lower triangle
                // House 12: Top right upper triangle

                val houseCenters = mapOf(
                    1 to Offset(w * 0.50f, h * 0.28f),
                    2 to Offset(w * 0.25f, h * 0.12f),
                    3 to Offset(w * 0.12f, h * 0.25f),
                    4 to Offset(w * 0.28f, h * 0.50f),
                    5 to Offset(w * 0.12f, h * 0.75f),
                    6 to Offset(w * 0.25f, h * 0.88f),
                    7 to Offset(w * 0.50f, h * 0.72f),
                    8 to Offset(w * 0.75f, h * 0.88f),
                    9 to Offset(w * 0.88f, h * 0.75f),
                    10 to Offset(w * 0.72f, h * 0.50f),
                    11 to Offset(w * 0.88f, h * 0.25f),
                    12 to Offset(w * 0.75f, h * 0.12f)
                )

                // Draw Rashi sign numbers and planetary symbols
                for (house in 1..12) {
                    val center = houseCenters[house] ?: continue
                    val rashi = ((ascendantSign - 1 + (house - 1)) % 12) + 1
                    val planets = housesPlanets[house.toString()] ?: emptyList()

                    drawContext.canvas.nativeCanvas.apply {
                        // Draw House sign number
                        drawText(
                            "$rashi",
                            center.x,
                            center.y - 12f,
                            numberPaint
                        )

                        // Draw Planets if present
                        if (planets.isNotEmpty()) {
                            val text = planets.joinToString(" ")
                            drawText(
                                text,
                                center.x,
                                center.y + 24f,
                                planetPaint
                            )
                        }
                    }
                }
            }
        }
    }
}
