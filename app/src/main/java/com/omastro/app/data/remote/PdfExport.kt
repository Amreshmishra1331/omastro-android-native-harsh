package com.omastro.app.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Kundali PDF export — POST /wp-json/omastro/v1/pdf/export.
 *
 * Live-verified 2026-10-10 on production omastroai
 * (kundali-v7_7-fixed/includes/rest-api.php:198, `kg_rest_pdf_export`,
 * permission callback $pub = public, no auth required).
 *
 * Params mirror the website's PDF generator (birth data + language; Hindi
 * report when language=hi). Envelope:
 * {"success":true,"data":{"html":"...","filename":"...pdf"}}.
 *
 * The returned html is the server's OFFICIAL mPDF-ready Kundali report
 * (recomputed from the same birth data by KG_Calculation_Engine, then
 * kg_build_pdf_html). The device only renders that HTML into PDF bytes
 * ([com.omastro.app.ui.share.renderHtmlToPdfFile]) — report content is
 * 100% server-computed, never invented client-side.
 *
 * (The raw PDF binary only exists on the website's nonce-gated
 * admin-ajax.php?action=kg_export_pdf_file flow, which the app cannot
 * authenticate — hence the REST HTML + on-device render approach.)
 */
data class PdfExportRequest(
    @SerializedName("name") val name: String,
    @SerializedName("gender") val gender: String = "male",
    @SerializedName("dob") val dob: String, // YYYY-MM-DD
    @SerializedName("tob_hr") val tobHr: Int,
    @SerializedName("tob_mn") val tobMn: Int,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("tz") val tz: Double = 5.5,
    @SerializedName("place") val place: String = "",
    @SerializedName("language") val language: String = "en", // en | hi
    @SerializedName("ayanamsa") val ayanamsa: String = "lahiri"
)

data class PdfExportPayload(
    @SerializedName("html") val html: String = "",
    @SerializedName("filename") val filename: String = "Kundali-Report.pdf"
)

data class PdfExportEnvelope(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: PdfExportPayload? = null,
    @SerializedName("message") val message: String? = null
)
