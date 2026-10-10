package com.omastro.app.ui.components

import android.graphics.Paint
import android.graphics.Path as AndroidPath
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.data.model.rashiGlyph
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.theme.*

/** One planet placed in a house: display name + Jyotish glyph (☉ ☽ ♂ ☿ ♃ ♀ ♄ ☊ ☋). */
data class PlanetMark(val name: String, val symbol: String)

/** Content anchor per house: center + max content width (fractions of canvas). */
private data class HouseAnchor(val cx: Float, val cy: Float, val maxW: Float, val diamond: Boolean)

// Diamond houses get wide regions; corner triangles are narrow.
private val HOUSE_ANCHORS = mapOf(
    1 to HouseAnchor(0.50f, 0.245f, 0.34f, true),
    2 to HouseAnchor(0.25f, 0.095f, 0.30f, false),
    3 to HouseAnchor(0.095f, 0.25f, 0.17f, false),
    4 to HouseAnchor(0.25f, 0.50f, 0.34f, true),
    5 to HouseAnchor(0.095f, 0.75f, 0.17f, false),
    6 to HouseAnchor(0.25f, 0.905f, 0.30f, false),
    7 to HouseAnchor(0.50f, 0.755f, 0.34f, true),
    8 to HouseAnchor(0.75f, 0.905f, 0.30f, false),
    9 to HouseAnchor(0.905f, 0.75f, 0.17f, false),
    10 to HouseAnchor(0.75f, 0.50f, 0.34f, true),
    11 to HouseAnchor(0.905f, 0.25f, 0.17f, false),
    12 to HouseAnchor(0.75f, 0.095f, 0.30f, false)
)

// Cell polygons (fraction points) for the soft alternating tints.
private val CELL_POLYGONS = mapOf(
    1 to listOf(0.5f to 0f, 0.75f to 0.25f, 0.5f to 0.5f, 0.25f to 0.25f),
    2 to listOf(0f to 0f, 0.5f to 0f, 0.25f to 0.25f),
    3 to listOf(0f to 0f, 0.25f to 0.25f, 0f to 0.5f),
    4 to listOf(0f to 0.5f, 0.25f to 0.25f, 0.5f to 0.5f, 0.25f to 0.75f),
    5 to listOf(0f to 0.5f, 0.25f to 0.75f, 0f to 1f),
    6 to listOf(0f to 1f, 0.25f to 0.75f, 0.5f to 1f),
    7 to listOf(0.5f to 1f, 0.75f to 0.75f, 0.5f to 0.5f, 0.25f to 0.75f),
    8 to listOf(1f to 1f, 0.75f to 0.75f, 0.5f to 1f),
    9 to listOf(1f to 1f, 1f to 0.5f, 0.75f to 0.75f),
    10 to listOf(1f to 0.5f, 0.75f to 0.25f, 0.5f to 0.5f, 0.75f to 0.75f),
    11 to listOf(1f to 0f, 1f to 0.5f, 0.75f to 0.25f),
    12 to listOf(1f to 0f, 0.75f to 0.25f, 0.5f to 0f)
)

/**
 * Premium North Indian Vedic Kundali diamond chart drawn on Compose Canvas.
 *
 * Saffron/gold on deep cosmic navy. The 12 bhavas are filled with subtle
 * alternating gold tints over crisp anti-aliased lines; each house shows its
 * rashi glyph and its planets laid out as a measured flow (rows of up to 3,
 * auto-shrinking when crowded) so planet labels never overlap. House 1 is
 * the Lagna — marked with a gold "Lg" badge. Sizes scale with the canvas
 * width, so the chart stays sharp on any screen.
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(CosmicSurface, CosmicCardBg)
                ),
                RoundedCornerShape(18.dp)
            )
            .border(1.dp, CosmicBorderGold, RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = chartTitle,
            color = SacredGold,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                // Soft radial gold glow behind the chart.
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SacredGold.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        radius = 900f
                    ),
                    RoundedCornerShape(10.dp)
                )
                .background(CosmicCardBg.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                .border(1.5.dp, SacredGold, RoundedCornerShape(10.dp))
                .padding(8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                // Scale factor: all text sizes derive from the canvas width so
                // the chart renders crisply at any screen size.
                val k = w / 1000f

                val gold = SacredGold
                val goldSoft = SacredGold.copy(alpha = 0.85f)

                // ---- Cell polygons for the soft alternating tints ----
                val tintPaint = Paint().apply { isAntiAlias = true }
                val native = drawContext.canvas.nativeCanvas
                for (house in 1..12) {
                    val pts = CELL_POLYGONS[house] ?: continue
                    val path = AndroidPath().apply {
                        pts.forEachIndexed { i, (fx, fy) ->
                            if (i == 0) moveTo(fx * w, fy * h) else lineTo(fx * w, fy * h)
                        }
                        close()
                    }
                    tintPaint.color = gold.copy(
                        alpha = if (house % 2 == 1) 0.055f else 0.028f
                    ).toArgb()
                    native.drawPath(path, tintPaint)
                }

                // ---- Chart lines: crisp, layered ----
                val outerW = 4f * k
                val innerW = 2.6f * k
                drawRect(
                    color = gold,
                    topLeft = Offset(0f, 0f),
                    size = size,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = outerW)
                )
                // Hairline inner frame for a premium double-border feel.
                val frameInset = 9f * k
                drawRect(
                    color = gold.copy(alpha = 0.28f),
                    topLeft = Offset(frameInset, frameInset),
                    size = androidx.compose.ui.geometry.Size(
                        w - frameInset * 2, h - frameInset * 2
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4f * k)
                )
                // Two diagonals corner-to-corner.
                drawLine(color = goldSoft, start = Offset(0f, 0f), end = Offset(w, h), strokeWidth = innerW)
                drawLine(color = goldSoft, start = Offset(w, 0f), end = Offset(0f, h), strokeWidth = innerW)
                // Central diamond through edge midpoints.
                val midTop = Offset(w / 2f, 0f)
                val midRight = Offset(w, h / 2f)
                val midBottom = Offset(w / 2f, h)
                val midLeft = Offset(0f, h / 2f)
                drawLine(color = goldSoft, start = midTop, end = midRight, strokeWidth = innerW)
                drawLine(color = goldSoft, start = midRight, end = midBottom, strokeWidth = innerW)
                drawLine(color = goldSoft, start = midBottom, end = midLeft, strokeWidth = innerW)
                drawLine(color = goldSoft, start = midLeft, end = midTop, strokeWidth = innerW)

                // Center emblem: small filled gold diamond.
                val er = 11f * k
                val cx = w / 2f
                val cy = h / 2f
                drawContext.canvas.nativeCanvas.drawPath(
                    AndroidPath().apply {
                        moveTo(cx, cy - er)
                        lineTo(cx + er, cy)
                        lineTo(cx, cy + er)
                        lineTo(cx - er, cy)
                        close()
                    },
                    Paint().apply { color = gold.toArgb(); isAntiAlias = true }
                )

                // ---- Paints ----
                val houseNumberPaint = Paint().apply {
                    color = TextSecondary.copy(alpha = 0.6f).toArgb()
                    textSize = 23f * k
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                val rashiDiamondPaint = Paint().apply {
                    color = SacredGoldLight.toArgb()
                    textSize = 56f * k
                    isFakeBoldText = true
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                val rashiTriPaint = Paint().apply {
                    color = SacredGoldLight.toArgb()
                    textSize = 42f * k
                    isFakeBoldText = true
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                val planetPaint = Paint().apply {
                    color = Color.White.toArgb()
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                val lagnaTextPaint = Paint().apply {
                    color = CosmicDeepBlue.toArgb()
                    textSize = 21f * k
                    isFakeBoldText = true
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                val lagnaBgPaint = Paint().apply {
                    color = gold.toArgb()
                    isAntiAlias = true
                }

                // Content anchors live at file level (HOUSE_ANCHORS) —
                // see top of file.
                val glyphGap = 9f * k

                for (house in 1..12) {
                    val anchor = HOUSE_ANCHORS[house] ?: continue
                    val cxPx = anchor.cx * w
                    val cyPx = anchor.cy * h
                    val maxWpx = anchor.maxW * w

                    val rashi = ((lagnaSign - 1 + (house - 1)) % 12) + 1
                    val marks = housePlanets[house] ?: emptyList()
                    val glyphs = marks.map { it.symbol.ifBlank { it.name.take(2) } }

                    // Smart planet layout: rows of up to 3 (2 for narrow
                    // triangle cells), measured with the real Paint, shrinking
                    // the font until every row fits the cell — never overlaps.
                    val perRow = if (anchor.diamond) 3 else 2
                    var planetSize = when {
                        glyphs.size <= 2 -> 42f * k
                        glyphs.size <= 4 -> 34f * k
                        else -> 28f * k
                    }
                    val rowWidth: (List<String>) -> Float = { row ->
                        row.sumOf { planetPaint.measureText(it).toDouble() }.toFloat() +
                            glyphGap * (row.size - 1).coerceAtLeast(0)
                    }
                    val rows: List<List<String>> = glyphs.chunked(perRow)
                    planetPaint.textSize = planetSize
                    while (rows.any { rowWidth(it) > maxWpx - 8f * k } && planetSize > 17f * k) {
                        planetSize -= 2f * k
                        planetPaint.textSize = planetSize
                    }

                    val rashiPaint = if (anchor.diamond) rashiDiamondPaint else rashiTriPaint
                    val rashiH = rashiPaint.textSize * 0.95f
                    val badgeH = if (house == 1) 30f * k else 0f
                    val rowH = planetSize * 1.22f
                    val stackH = rashiH + 8f * k + badgeH + rows.size * rowH
                    val stackTop = cyPx - stackH / 2f
                    var y = stackTop

                    native.apply {
                        // House number (tiny, dim): top-left of the content
                        // stack — always inside the cell, never on a line.
                        drawText(
                            "$house",
                            cxPx - maxWpx / 2f + 14f * k,
                            stackTop + 20f * k,
                            houseNumberPaint
                        )

                        // Rashi glyph (large, gold).
                        y += rashiH
                        drawText(rashiGlyph(rashi), cxPx, y, rashiPaint)
                        y += 8f * k

                        // Lagna badge on house 1: gold pill with dark "Lg".
                        if (house == 1) {
                            val bw = 50f * k
                            val bh = 26f * k
                            drawRoundRect(
                                cxPx - bw / 2f, y, cxPx + bw / 2f, y + bh,
                                13f * k, 13f * k, lagnaBgPaint
                            )
                            drawText("Lg", cxPx, y + bh - 7f * k, lagnaTextPaint)
                            y += bh
                        }

                        // Planet glyph rows, centered.
                        rows.forEach { row ->
                            y += rowH
                            val rw = rowWidth(row)
                            var x = cxPx - rw / 2f
                            row.forEach { g ->
                                val gw = planetPaint.measureText(g)
                                drawText(g, x + gw / 2f, y, planetPaint)
                                x += gw + glyphGap
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = W.t("kundali_chart_legend"),
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}
