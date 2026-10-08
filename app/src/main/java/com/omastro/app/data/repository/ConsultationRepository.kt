package com.omastro.app.data.repository

import com.omastro.app.data.model.Astrologer
import com.omastro.app.data.model.ChatMessage
import com.omastro.app.data.model.SendMessageRequest
import com.omastro.app.data.remote.ApiClient
import com.omastro.app.data.remote.OmAstroApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConsultationRepository(
    private val api: OmAstroApiService = ApiClient.apiService
) {
    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages.asStateFlow()

    suspend fun getAstrologers(category: String? = null): Result<List<Astrologer>> {
        return try {
            val res = api.getAstrologers(category)
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                Result.success(res.body()!!)
            } else {
                Result.success(getDefaultAstrologers())
            }
        } catch (e: Exception) {
            Result.success(getDefaultAstrologers())
        }
    }

    suspend fun loadChatHistory(astrologerId: String): List<ChatMessage> {
        val existing = _messages.value[astrologerId]
        if (!existing.isNullOrEmpty()) return existing

        val initial = listOf(
            ChatMessage(
                id = "msg_1",
                senderId = astrologerId,
                isUser = false,
                message = "Namaste! I am Pandit Radhey Shyam. Please share your birth details and what question brings you here today.",
                timestamp = System.currentTimeMillis() - 60000
            )
        )
        val updated = _messages.value.toMutableMap()
        updated[astrologerId] = initial
        _messages.value = updated
        return initial
    }

    suspend fun sendChatMessage(astrologerId: String, text: String): ChatMessage {
        val userMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            senderId = "user",
            isUser = true,
            message = text,
            timestamp = System.currentTimeMillis()
        )
        val currentList = _messages.value[astrologerId]?.toMutableList() ?: mutableListOf()
        currentList.add(userMsg)

        val updated = _messages.value.toMutableMap()
        updated[astrologerId] = currentList
        _messages.value = updated

        // Simulate intelligent Vedic Astrologer response
        val replyText = generateVedicAstrologerReply(text)
        val replyMsg = ChatMessage(
            id = "msg_reply_${System.currentTimeMillis()}",
            senderId = astrologerId,
            isUser = false,
            message = replyText,
            timestamp = System.currentTimeMillis() + 1000
        )
        currentList.add(replyMsg)
        updated[astrologerId] = currentList
        _messages.value = updated

        return userMsg
    }

    private fun generateVedicAstrologerReply(query: String): String {
        val lower = query.lowercase()
        return when {
            lower.contains("career") || lower.contains("job") ->
                "Analyzing your 10th House (Karma Bhava)... Saturn's transit over your natal Sun is concluding, opening high-growth doors and promotions in the coming 3 months. Wear a brass or copper ring on Thursday."
            lower.contains("marriage") || lower.contains("love") || lower.contains("relationship") ->
                "Looking into your 7th House (Kalatra Bhava) and Venus (Shukra)... Jupiter is aspecting your lagna lord, blessing your matrimonial timeline. Auspicious developments will unfold around Shukla Paksha."
            lower.contains("money") || lower.contains("wealth") || lower.contains("finance") ->
                "Looking at your 2nd House (Dhana Bhava) and 11th House (Labha Bhava)... An auspicious Dhana Yoga is forming. Avoid speculative investments on Saturdays and chant Sri Suktam."
            else ->
                "Hari Om! Your planetary dasha indicates transformative positive vibrations. Focus your mind on Lord Shiva's Maha Mrityunjaya mantra for immense peace and success."
        }
    }

    private fun getDefaultAstrologers(): List<Astrologer> {
        return listOf(
            Astrologer(
                id = "astro_1",
                name = "Acharya Raman Shastri",
                title = "Vedic Jyotish & Prashna Specialist",
                speciality = listOf("Vedic", "Kundali", "Career"),
                languages = listOf("Hindi", "English", "Sanskrit"),
                experienceYears = 18,
                pricePerMin = 25,
                rating = 4.9,
                totalConsultations = 14200,
                isOnline = true,
                avatarUrl = ""
            ),
            Astrologer(
                id = "astro_2",
                name = "Dr. Gayatri Devi",
                title = "Gold Medalist Numerologist & Tarot",
                speciality = listOf("Tarot", "Numerology", "Love"),
                languages = listOf("English", "Hindi"),
                experienceYears = 12,
                pricePerMin = 30,
                rating = 4.85,
                totalConsultations = 9800,
                isOnline = true,
                avatarUrl = ""
            ),
            Astrologer(
                id = "astro_3",
                name = "Pt. Deendayal Joshi",
                title = "KP Astrology & Vastu Master",
                speciality = listOf("KP System", "Vastu", "Marriage"),
                languages = listOf("Hindi", "Gujarati"),
                experienceYears = 22,
                pricePerMin = 35,
                rating = 4.95,
                totalConsultations = 21500,
                isOnline = true,
                avatarUrl = ""
            ),
            Astrologer(
                id = "astro_4",
                name = "Swami Anand Giri",
                title = "Nadi Astrology & Gemology",
                speciality = listOf("Nadi", "Gemstones", "Remedies"),
                languages = listOf("Hindi", "Tamil", "English"),
                experienceYears = 16,
                pricePerMin = 20,
                rating = 4.75,
                totalConsultations = 7600,
                isOnline = false,
                avatarUrl = ""
            )
        )
    }
}
