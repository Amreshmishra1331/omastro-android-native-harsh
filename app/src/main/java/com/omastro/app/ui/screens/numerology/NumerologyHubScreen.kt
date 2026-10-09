package com.omastro.app.ui.screens.numerology

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omastro.app.ui.components.CosmicCard
import com.omastro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumerologyScreen(
    onBack: () -> Unit,
    onToolSelected: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Numerology", color = StarlightWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SacredGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDeepBlue)
            )
        },
        containerColor = CosmicDeepBlue
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Column {
                    Text(
                        text = "Numerology",
                        color = StarlightWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Discover what your numbers reveal about your life path, personality and destiny.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
            items(NumerologyTool.values()) { tool ->
                NumerologyToolCard(tool = tool, onClick = { onToolSelected(tool.id) })
            }
        }
    }
}

@Composable
private fun NumerologyToolCard(tool: NumerologyTool, onClick: () -> Unit) {
    CosmicCard(
        borderGold = false,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tool.glyph,
                    color = SacredGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tool.title,
                color = StarlightWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tool.subtitle,
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }
}
