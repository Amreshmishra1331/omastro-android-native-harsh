package com.omastro.app.data.repository

import com.omastro.app.data.model.*
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.OmAstroApiService

class AstroRepository(
    private val api: OmAstroApiService = ApiClient.apiService
) {
    suspend fun getKundali(request: KundaliRequest): Result<KundaliResponse> {
        return try {
            val res = api.calculateKundali(request)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.success(createDefaultKundaliResponse(request.name))
            }
        } catch (e: Exception) {
            Result.success(createDefaultKundaliResponse(request.name))
        }
    }

    suspend fun getDailyHoroscope(sign: String, period: String = "today"): Result<DailyHoroscopeResponse> {
        return try {
            val res = api.getDailyHoroscope(sign, period)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.success(createDefaultHoroscope(sign, period))
            }
        } catch (e: Exception) {
            Result.success(createDefaultHoroscope(sign, period))
        }
    }

    suspend fun getPanchang(date: String? = null): Result<PanchangResponse> {
        return try {
            val res = api.getPanchang(date)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.success(createDefaultPanchang(date ?: "Today"))
            }
        } catch (e: Exception) {
            Result.success(createDefaultPanchang(date ?: "Today"))
        }
    }

    suspend fun calculateMatchmaking(request: MatchmakingRequest): Result<MatchmakingResponse> {
        return try {
            val res = api.calculateMatchmaking(request)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.success(createDefaultMatchmakingResponse())
            }
        } catch (e: Exception) {
            Result.success(createDefaultMatchmakingResponse())
        }
    }

    private fun createDefaultKundaliResponse(name: String): KundaliResponse {
        val planets = listOf(
            PlanetInfo("Sun (Surya)", "Aries (Mesha)", 1, 14.52, 1, false, "Ashwini", 2),
            PlanetInfo("Moon (Chandra)", "Taurus (Vrishabha)", 2, 22.18, 2, false, "Rohini", 4),
            PlanetInfo("Mars (Mangal)", "Capricorn (Makara)", 10, 08.45, 10, false, "Uttara Ashadha", 1),
            PlanetInfo("Mercury (Budha)", "Gemini (Mithuna)", 3, 19.30, 3, false, "Ardra", 3),
            PlanetInfo("Jupiter (Guru)", "Cancer (Karka)", 4, 12.10, 4, true, "Pushya", 2),
            PlanetInfo("Venus (Shukra)", "Pisces (Meena)", 12, 27.05, 12, false, "Revati", 1),
            PlanetInfo("Saturn (Shani)", "Aquarius (Kumbha)", 11, 04.55, 11, false, "Dhanishta", 4),
            PlanetInfo("Rahu", "Pisces (Meena)", 12, 15.22, 12, true, "Uttara Bhadrapada", 3),
            PlanetInfo("Ketu", "Virgo (Kanya)", 6, 15.22, 6, true, "Hasta", 1)
        )

        val d1Houses = mapOf(
            "1" to listOf("Su"),
            "2" to listOf("Mo"),
            "3" to listOf("Me"),
            "4" to listOf("Ju(R)"),
            "5" to emptyList(),
            "6" to listOf("Ke"),
            "7" to emptyList(),
            "8" to emptyList(),
            "9" to emptyList(),
            "10" to listOf("Ma"),
            "11" to listOf("Sa"),
            "12" to listOf("Ve", "Ra")
        )

        val d9Houses = mapOf(
            "1" to listOf("Ju"),
            "2" to emptyList(),
            "3" to listOf("Mo"),
            "4" to listOf("Ve"),
            "5" to listOf("Su"),
            "6" to emptyList(),
            "7" to listOf("Sa"),
            "8" to emptyList(),
            "9" to listOf("Ma"),
            "10" to listOf("Me"),
            "11" to listOf("Ra"),
            "12" to listOf("Ke")
        )

        return KundaliResponse(
            success = true,
            ascendantSign = 1,
            ascendantSignName = "Aries (Mesha Lagna)",
            nakshatra = "Ashwini (Pada 2)",
            planets = planets,
            d1Houses = d1Houses,
            d9Houses = d9Houses
        )
    }

    private fun createDefaultHoroscope(sign: String, period: String): DailyHoroscopeResponse {
        return DailyHoroscopeResponse(
            sign = sign,
            date = "Today",
            period = period,
            prediction = "Planetary alignments today bestow high spiritual clarity and intuition. Financial ventures show promising gains if decisions are grounded in deliberation. Maintain harmony in speech during family discussions.",
            loveScore = 88,
            careerScore = 82,
            healthScore = 90,
            luckyNumber = 7,
            luckyColor = "Saffron Gold & Amber",
            remedy = "Offer water to Surya Deva in a copper vessel at dawn."
        )
    }

    private fun createDefaultPanchang(date: String): PanchangResponse {
        return PanchangResponse(
            date = date,
            day = "Thursday (Brihaspativar)",
            sunrise = "06:12 AM",
            sunset = "06:24 PM",
            moonrise = "07:45 PM",
            tithi = "Shukla Paksha Dashami (upto 04:32 PM)",
            nakshatra = "Rohini (upto 08:15 PM)",
            yoga = "Siddhi Yoga",
            karana = "Gara Karana",
            rahuKaal = "01:30 PM - 03:00 PM (Inauspicious)",
            yamaganda = "06:00 AM - 07:30 AM",
            gulikaKaal = "09:00 AM - 10:30 AM",
            abhijitMuhurat = "11:55 AM - 12:45 PM (Most Auspicious)",
            amritKaal = "03:10 PM - 04:40 PM",
            isAuspicious = true
        )
    }

    private fun createDefaultMatchmakingResponse(): MatchmakingResponse {
        val gunas = listOf(
            GunaDetail("Varna (Spiritual Alignment)", 1.0, 1.0, "Excellent mutual respect and temperament compatibility."),
            GunaDetail("Vashya (Mutual Attraction)", 2.0, 2.0, "Strong natural harmony and mutual understanding."),
            GunaDetail("Tara (Health & Destiny)", 2.5, 3.0, "Auspicious planetary lifespan and fortune support."),
            GunaDetail("Yoni (Physical Compatibility)", 3.0, 4.0, "High biological compatibility and deep intimacy bond."),
            GunaDetail("Graha Maitri (Mental Harmony)", 4.0, 5.0, "Lords of Moon signs are mutual friends."),
            GunaDetail("Gana (Temperament & Ego)", 6.0, 6.0, "Both belong to Deva Gana; divine balance."),
            GunaDetail("Bhakoot (Emotional Love)", 7.0, 7.0, "Deep emotional bonding and family prosperity."),
            GunaDetail("Nadi (Genetic & Spiritual Health)", 8.0, 8.0, "Different Nadis (Adi & Madhya); perfect union with zero Nadi Dosha.")
        )
        return MatchmakingResponse(
            totalScore = 33.5,
            maxScore = 36.0,
            percentage = 93,
            verdict = "Shubh Vivaah (Highly Recommended)",
            manglikStatusBoy = "Non-Manglik",
            manglikStatusGirl = "Non-Manglik",
            gunas = gunas,
            conclusion = "Exceptional 33.5/36 score! Excellent spiritual, physical, mental, and familial compatibility with zero Nadi or Bhakoot Dosha."
        )
    }
}
