package com.omastro.app.ui.share

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.omastro.app.data.local.LangPrefs
import com.omastro.app.data.model.FaceReading
import com.omastro.app.data.model.KundaliResponse
import com.omastro.app.data.model.PalmistryReading
import com.omastro.app.data.model.VastuAnalysis
import com.omastro.app.ui.i18n.W
import com.omastro.app.ui.theme.SacredGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ------------------------------------------------------------------
// Shareable reading cards (2026-10-10).
//
// Renders a premium portrait (1080x1920) image card from a REAL server
// reading — Vastu, Palmistry, Face Reading, Kundali summary — and shares
// it through the Android system share sheet (WhatsApp etc.).
//
// HONESTY: every builder below returns null when there is nothing
// meaningful to share (refusal responses, empty readings). The button
// hides itself on null. Every highlight is copied from the actual
// result object — nothing is fabricated.
// Bilingual: all card labels come from W (follow the Settings language).
// No emojis anywhere on the card.
// ------------------------------------------------------------------

/** One labeled highlight row on the card. */
data class CardHighlight(val label: String, val value: String)

/**
 * Complete card payload. `scoreBig`/`scoreUnit`/`scoreCaption` drive the
 * optional hero block (Vastu score dial); everything else renders as
 * label → value rows.
 */
data class ReadingCardContent(
    val title: String,
    val scoreBig: String? = null,
    val scoreUnit: String? = null,
    val scoreCaption: String? = null,
    val highlights: List<CardHighlight>,
    val date: String
)

private fun String.ellipsize(max: Int): String {
    val t = replace('\n', ' ').replace("\\s+".toRegex(), " ").trim()
    return if (t.length <= max) t else t.take(max - 1).trimEnd() + "…"
}

private fun cardDate(hi: Boolean): String {
    val locale = if (hi) Locale("hi") else Locale.ENGLISH
    return SimpleDateFormat("d MMMM yyyy", locale).format(Date())
}

private fun severityName(severity: String, hi: Boolean): String =
    when (severity.lowercase()) {
        "high" -> if (hi) "उच्च" else "High"
        "medium" -> if (hi) "मध्यम" else "Medium"
        else -> if (hi) "निम्न" else "Low"
    }

private fun verdictName(verdict: String, hi: Boolean): String =
    when (verdict.lowercase()) {
        "ideal" -> if (hi) "आदर्श" else "Ideal"
        "acceptable" -> if (hi) "स्वीकार्य" else "Acceptable"
        "needs_care" -> if (hi) "सावधानी आवश्यक" else "Needs care"
        "dosha" -> if (hi) "दोष" else "Dosha"
        "auspicious" -> if (hi) "शुभ" else "Auspicious"
        "inauspicious" -> if (hi) "अशुभ" else "Inauspicious"
        else -> if (hi) "अज्ञात" else "Unknown"
    }

// ------------------------------------------------------------------
// Content builders — real data only, null when nothing to share.
// ------------------------------------------------------------------

/** Palmistry card: hand, top lines, top observations, guidance. Null on refusal. */
fun PalmistryReading.shareCardContent(): ReadingCardContent? {
    if (isRefusal) return null
    val hi = LangPrefs.language.value == "hi"
    val items = mutableListOf<CardHighlight>()
    val handLine = buildString {
        if (hand.isNotBlank()) append(hand.replaceFirstChar { it.uppercase() })
        if (handType.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(handType)
        }
    }
    if (handLine.isNotBlank()) items.add(CardHighlight(W.t("share_hand"), handLine))
    lines.take(2).forEach { l ->
        val v = l.quality.ifBlank { l.note }.ellipsize(140)
        if (l.name.isNotBlank() && v.isNotBlank())
            items.add(CardHighlight(l.name.ellipsize(60), v))
    }
    keyObservations.take(2).forEachIndexed { i, obs ->
        val label = if (i == 0) W.t("share_observation") else "${W.t("share_observation")} ${i + 1}"
        items.add(CardHighlight(label, obs.ellipsize(150)))
    }
    guidance.firstOrNull()?.let { g ->
        items.add(CardHighlight(W.t("share_guidance"), g.ellipsize(150)))
    }
    if (items.isEmpty()) return null
    return ReadingCardContent(
        title = W.t("share_type_palm"),
        highlights = items.take(5),
        date = cardDate(hi)
    )
}

/** Vastu card: score hero + top doshas + top verdicts. Honest on null score. */
fun VastuAnalysis.shareCardContent(): ReadingCardContent? {
    val hi = LangPrefs.language.value == "hi"
    val items = mutableListOf<CardHighlight>()
    val scoreBig: String?
    val scoreUnit: String?
    val scoreCaption: String?
    if (score != null) {
        scoreBig = score.toString()
        scoreUnit = "/100"
        scoreCaption = grade.ifBlank { null }
    } else {
        scoreBig = null; scoreUnit = null; scoreCaption = null
        items.add(CardHighlight(W.t("share_score"), W.t("share_insufficient")))
    }
    if (doshas.isNotEmpty()) {
        val v = doshas.take(3).joinToString(" · ") {
            "${it.title.ellipsize(44)} (${severityName(it.severity, hi)})"
        }.ellipsize(190)
        items.add(CardHighlight(W.t("share_doshas"), v))
    } else if (score != null || checks.isNotEmpty()) {
        items.add(CardHighlight(W.t("share_doshas"), W.t("share_no_doshas")))
    }
    checks.filter { it.title.isNotBlank() && it.verdict.isNotBlank() }
        .take(3)
        .forEach { c ->
            val v = buildString {
                if (c.value.isNotBlank()) append(c.value.ellipsize(70)).append(" — ")
                append(verdictName(c.verdict, hi))
            }
            items.add(CardHighlight(c.title.ellipsize(60), v.ellipsize(150)))
        }
    if (items.isEmpty()) return null
    return ReadingCardContent(
        title = W.t("share_type_vastu"),
        scoreBig = scoreBig,
        scoreUnit = scoreUnit,
        scoreCaption = scoreCaption,
        highlights = items.take(5),
        date = cardDate(hi)
    )
}

/** Face reading card: personality, top features, career, relationships. Null on refusal. */
fun FaceReading.shareCardContent(): ReadingCardContent? {
    if (!isFace) return null
    val hi = LangPrefs.language.value == "hi"
    val items = mutableListOf<CardHighlight>()
    if (overallPersonality.isNotBlank())
        items.add(CardHighlight(W.t("share_personality"), overallPersonality.ellipsize(160)))
    features.take(2).forEach { f ->
        val v = f.observation.ifBlank { f.interpretation }.ellipsize(150)
        if (v.isNotBlank())
            items.add(CardHighlight(W.faceFeatureName(f.key).ellipsize(60), v))
    }
    if (career.isNotBlank())
        items.add(CardHighlight(W.t("share_career"), career.ellipsize(150)))
    if (relationships.isNotBlank())
        items.add(CardHighlight(W.t("share_relationships"), relationships.ellipsize(150)))
    if (items.isEmpty() && reading.isNotBlank())
        items.add(CardHighlight(W.t("share_observation"), reading.ellipsize(200)))
    if (items.isEmpty()) return null
    return ReadingCardContent(
        title = W.t("share_type_face"),
        highlights = items.take(5),
        date = cardDate(hi)
    )
}

/** Kundali summary card: lagna, moon sign, nakshatra, dasha, yogas — all server data. */
fun KundaliResponse.shareCardContent(subjectName: String): ReadingCardContent? {
    val hi = LangPrefs.language.value == "hi"
    val items = mutableListOf<CardHighlight>()
    val lagnaText = buildString {
        append(lagna.rashi.ifBlank { meta.lagnaEn })
        if (lagna.rashiHi.isNotBlank()) append(" (").append(lagna.rashiHi).append(")")
        if (lagna.lord.isNotBlank()) append(" · ").append(lagna.lord)
        if (lagna.deg > 0 || lagna.min > 0) append(" · ").append(lagna.deg).append("°").append(lagna.min).append("'")
    }.trim()
    if (lagnaText.isNotBlank()) items.add(CardHighlight(W.t("share_lagna"), lagnaText.ellipsize(160)))
    val moon = meta.moonSignEn.ifBlank { meta.moonSign }
    val moonText = buildString {
        append(moon)
        if (meta.moonSignHi.isNotBlank() && meta.moonSignHi != moon)
            append(" (").append(meta.moonSignHi).append(")")
    }.trim()
    if (moonText.isNotBlank()) items.add(CardHighlight(W.t("share_moon_sign"), moonText.ellipsize(120)))
    val naks = buildString {
        append(meta.nakshatra.ifBlank { meta.nakshatraName })
        if (meta.nakshatraPada > 0) append(" · ").append(if (hi) "चरण" else "Pada").append(" ").append(meta.nakshatraPada)
    }.trim()
    if (naks.isNotBlank()) items.add(CardHighlight(W.t("share_nakshatra"), naks.ellipsize(120)))
    if (meta.curDasha.isNotBlank())
        items.add(CardHighlight(W.t("share_dasha"), meta.curDasha.ellipsize(140)))
    if (meta.yogasSummary.isNotBlank())
        items.add(CardHighlight(W.t("share_yogas"), meta.yogasSummary.ellipsize(160)))
    if (items.isEmpty()) return null
    val title = if (subjectName.isNotBlank())
        "${W.t("share_type_kundali")} — ${subjectName.ellipsize(40)}"
    else W.t("share_type_kundali")
    return ReadingCardContent(
        title = title,
        highlights = items.take(5),
        date = cardDate(hi)
    )
}

// ------------------------------------------------------------------
// Canvas renderer — 1080x1920 portrait, gold/dark premium theme.
// System font fallback chain renders Devanagari correctly (no custom
// font needed; Android shapes ॐ/देवनागरी via fallback fonts).
// ------------------------------------------------------------------

private const val CARD_W = 1080
private const val CARD_H = 1920
private val GOLD: Int = 0xFFFFB300.toInt() // SacredGold
private val DEEP_TOP: Int = 0xFF0D1424.toInt()
private val DEEP_BOTTOM: Int = 0xFF1D2947.toInt()

private fun paint(
    color: Int = 0xFFFFFFFF.toInt(),
    size: Float = 40f,
    bold: Boolean = false,
    serif: Boolean = false,
    align: Paint.Align = Paint.Align.LEFT
): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = color
    textSize = size
    typeface = when {
        serif && bold -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
        serif -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        bold -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        else -> Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    textAlign = align
}

private fun wrapText(p: Paint, text: String, maxWidth: Float): List<String> {
    val out = mutableListOf<String>()
    for (para in text.split("\n")) {
        var line = StringBuilder()
        for (word in para.split(" ")) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (p.measureText(test) <= maxWidth) {
                line = StringBuilder(test)
            } else {
                if (line.isNotEmpty()) out.add(line.toString())
                var w = word
                line = StringBuilder()
                while (p.measureText(w) > maxWidth && w.length > 1) {
                    var i = w.length - 1
                    while (i > 1 && p.measureText(w.substring(0, i)) > maxWidth) i--
                    out.add(w.substring(0, i))
                    w = w.substring(i)
                }
                line = StringBuilder(w)
            }
        }
        if (line.isNotEmpty()) out.add(line.toString())
    }
    return out
}

/**
 * Card layout spec (1080 x 1920):
 *  - gradient bg + vignette + star field, double gold frame
 *  - ॐ + "OmAstro" brand header, gold divider with diamond
 *  - reading type (active language) + date
 *  - optional Vastu score hero (big number + grade)
 *  - 3-5 label/value highlight rows with hairline separators
 *  - footer: "Generated by OmAstro" / Hindi equivalent
 */
fun renderReadingCard(content: ReadingCardContent, hi: Boolean): Bitmap {
    val bmp = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val wf = CARD_W.toFloat()
    val hf = CARD_H.toFloat()

    // Background: deep-navy vertical gradient + vignette.
    c.drawRect(
        0f, 0f, wf, hf,
        Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, hf, DEEP_TOP, DEEP_BOTTOM, Shader.TileMode.CLAMP)
        }
    )
    c.drawRect(
        0f, 0f, wf, hf,
        Paint().apply {
            shader = RadialGradient(
                wf / 2f, hf * 0.40f, 950f,
                0x00000000, 0x66000000.toInt(), Shader.TileMode.CLAMP
            )
        }
    )
    // Star field (fixed seed — identical card on every render).
    val rnd = java.util.Random(42)
    val starP = Paint(Paint.ANTI_ALIAS_FLAG)
    repeat(90) {
        val x = rnd.nextFloat() * wf
        val y = rnd.nextFloat() * hf
        val r = 1f + rnd.nextFloat() * 2.4f
        if (rnd.nextBoolean()) starP.setARGB(38, 255, 179, 0)
        else starP.setARGB(26, 255, 255, 255)
        c.drawCircle(x, y, r, starP)
    }

    // Double gold frame + diamond on the top edge.
    val frame = paint(GOLD, 1f).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
    c.drawRect(44f, 44f, wf - 44f, hf - 44f, frame)
    frame.strokeWidth = 1.5f
    frame.alpha = 110
    c.drawRect(64f, 64f, wf - 64f, hf - 64f, frame)
    val dia = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GOLD }
    c.save()
    c.rotate(45f, wf / 2f, 44f)
    c.drawRect(wf / 2f - 11f, 44f - 11f, wf / 2f + 11f, 44f + 11f, dia)
    c.restore()

    var y = 300f
    // Brand: ॐ + OmAstro.
    c.drawText("ॐ", wf / 2f, y, paint(GOLD, 118f, bold = true, serif = true, align = Paint.Align.CENTER))
    y += 108f
    c.drawText("OmAstro", wf / 2f, y, paint(GOLD, 72f, bold = true, serif = true, align = Paint.Align.CENTER))
    y += 56f

    // Gold divider with center diamond.
    val div = paint(GOLD, 1f).apply { style = Paint.Style.STROKE; strokeWidth = 2f; alpha = 170 }
    c.drawLine(180f, y, wf - 180f, y, div)
    c.save()
    c.rotate(45f, wf / 2f, y)
    c.drawRect(wf / 2f - 9f, y - 9f, wf / 2f + 9f, y + 9f, dia)
    c.restore()
    y += 92f

    // Reading type + date.
    val titleP = paint(0xFFFFFFFF.toInt(), 54f, bold = true, align = Paint.Align.CENTER)
    val titleLines = wrapText(titleP, content.title, 880f).take(2)
    titleLines.forEach { line -> c.drawText(line, wf / 2f, y, titleP); y += 70f }
    y += 6f
    c.drawText(
        content.date, wf / 2f, y,
        paint(GOLD, 34f, align = Paint.Align.CENTER).apply { alpha = 200 }
    )
    y += 84f

    // Optional score hero.
    if (content.scoreBig != null) {
        val numP = paint(GOLD, 190f, bold = true, align = Paint.Align.CENTER)
        val unitP = paint(0xFFFFFFFF.toInt(), 60f, bold = true, align = Paint.Align.LEFT)
        val numW = numP.measureText(content.scoreBig)
        val unitW = if (content.scoreUnit != null) unitP.measureText(content.scoreUnit) else 0f
        val startX = wf / 2f - (numW + unitW) / 2f
        c.drawText(content.scoreBig, startX + numW / 2f, y + 130f, numP)
        if (content.scoreUnit != null)
            c.drawText(content.scoreUnit, startX + numW + 14f, y + 130f, unitP)
        y += 190f
        content.scoreCaption?.let { cap ->
            c.drawText(
                cap.ellipsize(60), wf / 2f, y,
                paint(0xFFFFFFFF.toInt(), 40f, bold = true, align = Paint.Align.CENTER)
            )
            y += 70f
        }
        y += 30f
    }

    // Highlight rows: fit as many as fit above the footer (min 1 kept).
    val labelP = paint(GOLD, 30f, bold = true).apply { letterSpacing = 0.14f }
    val valueP = paint(0xFFFFFFFF.toInt(), 38f)
    val hairP = paint(GOLD, 1f).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f; alpha = 70 }
    val x0 = 116f
    val valueW = wf - x0 * 2f
    val footerTop = 1690f

    fun itemHeight(h: CardHighlight): Float {
        val lines = wrapText(valueP, h.value, valueW).size.coerceAtMost(4)
        return 46f + lines * 54f + 44f
    }
    val items = content.highlights.toMutableList()
    while (items.size > 1 && y + items.sumOf { itemHeight(it).toDouble() } > footerTop) {
        items.removeLast()
    }
    items.forEachIndexed { i, h ->
        if (i > 0) {
            c.drawLine(x0, y - 22f, wf - x0, y - 22f, hairP)
        }
        c.drawText(h.label.uppercase(), x0, y + 20f, labelP)
        y += 66f
        wrapText(valueP, h.value, valueW).take(4).forEach { line ->
            c.drawText(line, x0, y, valueP)
            y += 54f
        }
        y += 44f
    }

    // Footer.
    val footDiv = paint(GOLD, 1f).apply { style = Paint.Style.STROKE; strokeWidth = 2f; alpha = 170 }
    c.drawLine(180f, footerTop + 30f, wf - 180f, footerTop + 30f, footDiv)
    c.drawText(
        W.t("share_generated_by"), wf / 2f, footerTop + 96f,
        paint(GOLD, 36f, bold = true, align = Paint.Align.CENTER)
    )
    return bmp
}

/**
 * Saves [bitmap] to the app cache and opens the Android system share
 * sheet. PNG in cache dir — no storage permission needed.
 */
fun shareCardBitmap(context: Context, bitmap: Bitmap, fileName: String) {
    val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
    // Keep the cache tidy: retain only the 4 newest cards.
    dir.listFiles()
        ?.sortedByDescending { it.lastModified() }
        ?.drop(4)
        ?.forEach { runCatching { it.delete() } }
    val file = File(dir, fileName)
    FileOutputStream(file).use { out ->
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
            throw IllegalStateException("bitmap compress failed")
    }
    val uri = FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", file
    )
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, W.t("share"))
    if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}

/**
 * Full-width gold-outlined Share button. Renders the bitmap off the UI
 * thread, then opens the share sheet. Renders nothing when [content] is
 * null (nothing meaningful to share — honesty rule).
 */
@Composable
fun ReadingCardShareButton(
    content: ReadingCardContent?,
    modifier: Modifier = Modifier
) {
    if (content == null) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var generating by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = {
            if (generating) return@OutlinedButton
            generating = true
            scope.launch {
                try {
                    val hi = LangPrefs.language.value == "hi"
                    val bitmap = withContext(Dispatchers.Default) {
                        renderReadingCard(content, hi)
                    }
                    shareCardBitmap(
                        context, bitmap,
                        "omastro-card-${System.currentTimeMillis()}.png"
                    )
                } catch (_: Exception) {
                    Toast.makeText(context, W.t("share_failed"), Toast.LENGTH_SHORT).show()
                } finally {
                    generating = false
                }
            }
        },
        enabled = !generating,
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, SacredGold),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SacredGold)
    ) {
        if (generating) {
            CircularProgressIndicator(
                color = SacredGold,
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(W.t("share_preparing"))
        } else {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(W.t("share"))
        }
    }
}
