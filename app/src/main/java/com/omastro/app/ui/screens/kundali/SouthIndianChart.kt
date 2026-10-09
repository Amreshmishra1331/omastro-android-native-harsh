package com.omastro.app.ui.screens.kundali

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.data.model.PlanetInfo
import com.omastro.app.ui.theme.*

/**
 * South-Indian style fixed-sign chart (4x4 grid).
 *
 * Cells are FIXED rashis — planets are placed by their signNumber, and the
 * ascendant marker is drawn on the actual ascendant sign. Correct for every
 * lagna, unlike house-index-based placement.
 */
@Composable
fun SouthIndianChart(
    planets: List<PlanetInfo>,
    ascendantSign: Int,
    chartTitle: String = "South Indian Chart",
    modifier: Modifier = Modifier
) {
    // (row, col) for each sign number 1..12 in the classic South-Indian layout.
    val signCells = mapOf(
        12 to (0 to 0), 1 to (0 to 1), 2 to (0 to 2), 3 to (0 to 3),
        11 to (1 to 0), 4 to (1 to 3),
        10 to (2 to 0), 5 to (2 to 3),
        9 to (3 to 0), 8 to (3 to 1), 7 to (3 to 2), 6 to (3 to 3)
    )

    val planetsBySign: Map<Int, List<PlanetInfo>> = planets.groupBy { it.signNumber }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CosmicSurface, RoundedCornerShape(16.dp))
            .border(1.dp, CosmicBorderGold, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(
            text = chartTitle,
            color = SacredGold,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Build a 4x4 grid; center 2x2 cells stay empty.
        val grid: Array<Array<Int?>> = Array(4) { arrayOfNulls<Int>(4) }
        signCells.forEach { (sign, pos) -> grid[pos.first][pos.second] = sign }

        for (row in 0 until 4) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 4) {
                    val sign = grid[row][col]
                    if (sign == null) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        SouthIndianCell(
                            signNumber = sign,
                            planets = planetsBySign[sign] ?: emptyList(),
                            isAscendant = sign == ascendantSign,
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SouthIndianCell(
    signNumber: Int,
    planets: List<PlanetInfo>,
    isAscendant: Boolean,
    modifier: Modifier = Modifier
) {
    val signNames = mapOf(
        1 to "Mesha", 2 to "Vrishabha", 3 to "Mithuna", 4 to "Karka",
        5 to "Simha", 6 to "Kanya", 7 to "Tula", 8 to "Vrishchika",
        9 to "Dhanu", 10 to "Makara", 11 to "Kumbha", 12 to "Meena"
    )
    Column(
        modifier = modifier
            .border(0.75.dp, CosmicBorderGold)
            .background(if (isAscendant) SacredGold.copy(alpha = 0.12f) else Color.Transparent)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$signNumber",
                color = SacredGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            if (isAscendant) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "Asc",
                    color = RadiantAmber,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Text(
            text = signNames[signNumber] ?: "",
            color = StarlightWhite.copy(alpha = 0.55f),
            fontSize = 8.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        planets.take(4).forEach { planet ->
            Text(
                text = planetAbbreviation(planet.name) + if (planet.isRetrograde) " (R)" else "",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
        if (planets.size > 4) {
            Text("+${planets.size - 4} more", color = TextSecondary, fontSize = 8.sp)
        }
    }
}

private fun planetAbbreviation(name: String): String {
    val lower = name.lowercase()
    return when {
        lower.startsWith("sun") || lower == "surya" -> "Su"
        lower.startsWith("moon") || lower == "chandra" -> "Mo"
        lower.startsWith("mars") || lower == "mangal" -> "Ma"
        lower.startsWith("mercury") || lower == "budh" -> "Me"
        lower.startsWith("jupiter") || lower == "guru" -> "Ju"
        lower.startsWith("venus") || lower == "shukra" -> "Ve"
        lower.startsWith("saturn") || lower == "shani" -> "Sa"
        lower.startsWith("rahu") -> "Ra"
        lower.startsWith("ketu") -> "Ke"
        else -> name.take(2).replaceFirstChar { it.uppercase() }
    }
}
