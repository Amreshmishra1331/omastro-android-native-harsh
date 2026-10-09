package com.omastro.app.ui.i18n

import com.omastro.app.data.local.LangPrefs

/**
 * Bilingual (EN / हिन्दी) strings for the world-class feature set:
 * voice typing, voice mode, photo AI attach, Palmistry, Vastu.
 *
 * Centralized: every new screen reads from here and auto-switches with the
 * global [LangPrefs.language] set in Settings. No per-screen toggles.
 *
 * Keyed pairs — the English source is the canonical string, Hindi is the
 * translation. Call [t] from a @Composable after collecting
 * `LangPrefs.language` so recomposition follows language changes:
 *
 *     val lang by LangPrefs.language.collectAsState()
 *     Text(W.t("photo_title"))
 */
object W {
    private val HI = mapOf(
        // ---- Permission dialogs (VoiceSpeech.rememberPermissionRequest) ----
        "perm_allow" to "अनुमति दें",
        "perm_not_now" to "अभी नहीं",
        "perm_ok" to "ठीक है",
        "perm_settings_note" to "आप इसे कभी भी सिस्टम सेटिंग्स में बदल सकते हैं।",
        "perm_denied_msg" to "अनुमति नहीं दी गई, इसलिए यह सुविधा उपलब्ध नहीं है। इसे उपयोग करने के लिए सिस्टम सेटिंग्स में सक्षम करें।",

        // ---- Voice typing errors ----
        "voice_unavailable" to "इस डिवाइस पर वॉयस टाइपिंग उपलब्ध नहीं है।",
        "voice_no_match" to "सुनाई नहीं दिया। कृपया पुनः प्रयास करें।",
        "voice_need_mic_perm" to "वॉयस टाइपिंग के लिए माइक्रोफ़ोन अनुमति आवश्यक है।",
        "voice_need_net" to "वॉयस टाइपिंग के लिए इंटरनेट कनेक्शन आवश्यक है।",
        "voice_failed" to "वॉयस टाइपिंग विफल रही। कृपया पुनः प्रयास करें।",
        "voice_could_not_start" to "वॉयस टाइपिंग शुरू नहीं हो सकी।",

        // ---- AI chat: voice + photo UI ----
        "back" to "वापस",
        "chat_mic_title" to "माइक्रोफ़ोन एक्सेस",
        "chat_mic_rationale" to "वॉयस टाइपिंग आपकी आवाज़ को चैट के लिए टेक्स्ट में बदलती है। माइक्रोफ़ोन केवल तभी उपयोग होता है जब आप माइक बटन दबाते हैं।",
        "chat_voice_on_snack" to "वॉयस मोड चालू। AI उत्तर ज़ोर से सुनाए जाएंगे।",
        "chat_voice_on_cd" to "वॉयस मोड चालू",
        "chat_voice_off_cd" to "वॉयस मोड बंद",
        "chat_voice_stop_cd" to "बोलना बंद करें",
        "chat_stop_listening" to "सुनना बंद करें",
        "chat_voice_typing" to "वॉयस टाइपिंग",
        "chat_analyze_photo_cd" to "फोटो का विश्लेषण करें",
        "chat_input_hint" to "अपना प्रश्न पूछें...",
        "chat_photo_attached" to "फोटो विश्लेषण जुड़ गया। इसके बारे में कुछ भी पूछें।",
        "chat_close" to "बंद करें",

        // ---- Photo attach dialog ----
        "photo_title" to "फोटो का विश्लेषण करें",
        "photo_sub" to "फोटो लें या चुनें — हथेली, घर का नक्शा, चार्ट — और AI से इसके बारे में पूछें।",
        "photo_take" to "फोटो लें",
        "photo_take_sub" to "कैमरा उपयोग करें",
        "photo_gallery" to "गैलरी से चुनें",
        "photo_gallery_sub" to "सहेजी गई फोटो चुनें",
        "photo_file" to "फाइल चुनें",
        "photo_file_sub" to "कोई भी इमेज फाइल",
        "photo_preview_cd" to "फोटो पूर्वावलोकन",
        "photo_ready" to "फोटो तैयार",
        "photo_question_label" to "इस फोटो के बारे में आपका प्रश्न",
        "photo_question_hint" to "जैसे: मेरी हथेली करियर के बारे में क्या कहती है?",
        "photo_retake" to "पुनः लें",
        "photo_analyze_btn" to "विश्लेषण करें",
        "photo_analyzing" to "आपकी फोटो का विश्लेषण हो रहा है...",
        "photo_cam_title" to "कैमरा एक्सेस",
        "photo_cam_rationale" to "फोटो लेने के लिए कैमरा एक्सेस आवश्यक है — जैसे विश्लेषण के लिए हथेली या घर के नक्शे की फोटो।",
        "photo_lib_title" to "फोटो एक्सेस",
        "photo_lib_rationale" to "गैलरी से फोटो चुनने के लिए फोटो लाइब्रेरी एक्सेस आवश्यक है।",
        "photo_err_save" to "फोटो सहेजी नहीं जा सकी। कृपया पुनः प्रयास करें।",
        "photo_err_read" to "चुनी गई फोटो पढ़ी नहीं जा सकी।",
        "photo_err_read_file" to "चुनी गई फाइल पढ़ी नहीं जा सकी।",
        "photo_err_failed" to "फोटो विश्लेषण विफल रहा।",

        // ---- Photo analysis result dialog ----
        "result_title" to "फोटो विश्लेषण",
        "result_empty" to "कोई विश्लेषण प्राप्त नहीं हुआ।",
        "result_attach_hint" to "निष्कर्षों को इस चैट से जोड़ें ताकि आप फोटो के बारे में अनुवर्ती प्रश्न पूछ सकें।",
        "result_attach" to "चैट से जोड़ें",
        "result_ask" to "AI से पूछें",

        // ---- Palmistry ----
        "palm_title" to "हस्तरेखा",
        "palm_sub" to "आपकी फोटो से AI हस्तरेखा विश्लेषण",
        "palm_capture_title" to "अपनी हथेली की फोटो लें",
        "palm_tip_1" to "हथेली सीधी रखें, उंगलियां मिलाकर",
        "palm_tip_2" to "तेज़, एकसमान रोशनी रखें — छाया से बचें",
        "palm_tip_3" to "फ्रेम में पूरी हथेली आए",
        "palm_tip_4" to "फोटो स्पष्ट हो, धुंधली नहीं",
        "palm_which_hand" to "कौन सा हाथ?",
        "palm_right" to "दायाँ हाथ",
        "palm_left" to "बायाँ हाथ",
        "palm_preview_cd" to "हथेली पूर्वावलोकन",
        "palm_ready" to "विश्लेषण के लिए फोटो तैयार",
        "palm_camera" to "कैमरा",
        "palm_gallery" to "गैलरी",
        "palm_analyze" to "मेरी हथेली का विश्लेषण करें",
        "palm_loading" to "आपकी हथेली पढ़ी जा रही है...",
        "palm_loading_sub" to "AI हर रेखा और पर्वत का अध्ययन कर रहा है।",
        "palm_your_palm" to "आपकी हथेली",
        "palm_hand_word_right" to "दायाँ हाथ",
        "palm_hand_word_left" to "बायाँ हाथ",
        "palm_hand_word" to "हाथ",
        "palm_detailed" to "विस्तृत विश्लेषण",
        "palm_lines" to "रेखाएं",
        "palm_mounts" to "पर्वत",
        "palm_obs" to "मुख्य अवलोकन",
        "palm_guidance" to "मार्गदर्शन",
        "palm_unclear" to "फोटो में अस्पष्ट",
        "palm_new" to "नई फोटो",
        "palm_ask" to "इस रीडिंग के बारे में AI से पूछें",
        "palm_err_save" to "फोटो सहेजी नहीं जा सकी।",
        "palm_err_read" to "चुनी गई फोटो पढ़ी नहीं जा सकी।",
        "palm_err_failed" to "हथेली विश्लेषण विफल रहा। कृपया पुनः प्रयास करें।",
        "palm_cam_rationale" to "हथेली की फोटो लेने के लिए कैमरा एक्सेस आवश्यक है।",
        "palm_lib_rationale" to "गैलरी से हथेली की फोटो चुनने के लिए फोटो लाइब्रेरी एक्सेस आवश्यक है।",
        "palm_cam_title" to "कैमरा एक्सेस",
        "palm_lib_title" to "फोटो एक्सेस",

        // ---- Face reading ----
        "face_title" to "चेहरा विश्लेषण",
        "face_sub" to "आपकी फोटो से सामुद्रिक शास्त्र चेहरा विश्लेषण",
        "face_capture_title" to "अपने चेहरे की फोटो लें",
        "face_tip_1" to "कैमरे की ओर सीधे देखें, सिर सीधा रखें",
        "face_tip_2" to "चेहरे पर तेज़, एकसमान रोशनी रखें — छाया से बचें",
        "face_tip_3" to "तटस्थ भाव रखें, आंखें खुली",
        "face_tip_4" to "फ्रेम में पूरा चेहरा आए, चश्मा या टोपी न पहनें",
        "face_preview_cd" to "चेहरा पूर्वावलोकन",
        "face_ready" to "विश्लेषण के लिए फोटो तैयार",
        "face_analyze" to "मेरे चेहरे का विश्लेषण करें",
        "face_loading" to "आपका चेहरा पढ़ा जा रहा है...",
        "face_loading_sub" to "AI आपके चेहरे की हर विशेषता का अध्ययन कर रहा है।",
        "face_your_face" to "आपका चेहरा",
        "face_features" to "चेहरे की विशेषताएं",
        "face_observation" to "अवलोकन",
        "face_interpretation" to "व्याख्या",
        "face_personality" to "समग्र व्यक्तित्व",
        "face_career" to "करियर",
        "face_relationships" to "संबंध",
        "face_health" to "स्वास्थ्य संकेत",
        "face_guidance" to "मार्गदर्शन",
        "face_err_failed" to "चेहरा विश्लेषण विफल रहा। कृपया पुनः प्रयास करें।",
        "face_err_unavailable" to "चेहरा विश्लेषण अभी उपलब्ध नहीं है। कृपया बाद में पुनः प्रयास करें।",
        "face_cam_rationale" to "चेहरे की फोटो लेने के लिए कैमरा एक्सेस आवश्यक है।",
        "face_lib_rationale" to "गैलरी से चेहरे की फोटो चुनने के लिए फोटो लाइब्रेरी एक्सेस आवश्यक है।",
        "face_not_face_title" to "कोई स्पष्ट चेहरा नहीं मिला",
        "face_not_face_hint" to "कृपया चेहरे की स्पष्ट, सामने से ली गई फोटो अपलोड करें।",
        "face_ctx_label" to "चेहरा विश्लेषण",

        // ---- Vastu: tabs & titles ----
        "vastu_title" to "वास्तु शास्त्र",
        "vastu_sub" to "शास्त्रीय गृह-ऊर्जा विश्लेषण",
        "vastu_tab_manual" to "मैन्युअल प्रविष्टि",
        "vastu_tab_photo" to "घर के नक्शे की फोटो",
        "vastu_card_facing" to "दिशा एवं प्रवेश",
        "vastu_facing_label" to "मुख्य प्रवेश दिशा",
        "vastu_pada_label" to "प्रवेश पद (1–32, वैकल्पिक)",
        "vastu_pada_hint" to "1–32 दर्ज करें। हर दिशा में 4 पद होते हैं, जिनके अलग-अलग देवता हैं।",
        "vastu_card_plot" to "प्लॉट",
        "vastu_plot_shape" to "प्लॉट का आकार",
        "vastu_length" to "लंबाई (फीट)",
        "vastu_breadth" to "चौड़ाई (फीट)",
        "vastu_slope" to "ढलान की ओर (वैकल्पिक)",
        "vastu_road" to "सड़क की दिशा (वैकल्पिक)",
        "vastu_card_water" to "जल एवं बुनियादी ढांचा",
        "vastu_water" to "जल स्रोत / बोरवेल",
        "vastu_tank" to "ओवरहेड टंकी",
        "vastu_septic" to "सेप्टिक टंकी",
        "vastu_staircase" to "सीढ़ियां",
        "vastu_card_rooms" to "कमरे",
        "vastu_add_room" to "कमरा जोड़ें",
        "vastu_remove_room_cd" to "कमरा हटाएं",
        "vastu_analyze" to "वास्तु विश्लेषण करें",
        "vastu_not_set" to "निर्धारित नहीं",
        "vastu_analyzing" to "वास्तु का विश्लेषण हो रहा है...",
        "vastu_err_failed" to "वास्तु विश्लेषण विफल रहा।",
        "vastu_photo_err_failed" to "फोटो विश्लेषण विफल रहा।",
        "vastu_photo_title" to "अपने घर का नक्शा अपलोड करें",
        "vastu_photo_sub" to "AI फ्लोर प्लान पढ़ता है — कमरे, प्रवेश, दिशाएं — फिर शास्त्रीय वास्तु इंजन इसे स्कोर करता है। यदि संभव हो तो प्लान पर उत्तर दिशा अंकित करें।",
        "vastu_photo_ready" to "घर का नक्शा तैयार",
        "vastu_photo_map_cd" to "घर के नक्शे का पूर्वावलोकन",
        "vastu_notes_label" to "नोट्स (वैकल्पिक) — जैसे प्रवेश पूर्व की ओर है",
        "vastu_analyze_map" to "घर के नक्शे का विश्लेषण करें",
        "vastu_cam_rationale" to "घर के नक्शे की फोटो लेने के लिए कैमरा एक्सेस आवश्यक है।",
        "vastu_lib_rationale" to "घर के नक्शे की फोटो चुनने के लिए फोटो लाइब्रेरी एक्सेस आवश्यक है।",
        "vastu_cam_title" to "कैमरा एक्सेस",
        "vastu_lib_title" to "फोटो एक्सेस",
        "vastu_photo_err_save" to "फोटो सहेजी नहीं जा सकी।",
        "vastu_photo_err_read" to "चुनी गई फोटो पढ़ी नहीं जा सकी।",
        "vastu_doshas_found" to "मिले दोष",
        "vastu_doshas" to "दोष",
        "vastu_no_doshas" to "कोई दोष नहीं मिला। यह लेआउट शास्त्रीय वास्तु सिद्धांतों का अच्छी तरह पालन करता है।",
        "vastu_verdicts" to "स्थान निर्णय",
        "vastu_new" to "नया विश्लेषण",
        "vastu_ask_ai" to "इस विश्लेषण के बारे में AI से पूछें",
        "vastu_ctx_label" to "वास्तु विश्लेषण",
        "vastu_compass_title" to "कम्पास मोड",
        "vastu_compass_open" to "कम्पास से दिशा जानें",
        "vastu_compass_hint" to "फ़ोन का ऊपरी हिस्सा मुख्य प्रवेश द्वार की ओर रखें — कम्पास बताएगा कि वह किस दिशा में है।",
        "vastu_compass_calibrate" to "कम्पास की सटीकता कम है — फ़ोन को हवा में 8 के आकार में घुमाकर कैलिब्रेट करें।",
        "vastu_compass_use" to "इस दिशा का उपयोग करें",
        "vastu_compass_cam_title" to "कैमरा एक्सेस",
        "vastu_compass_cam_rationale" to "प्रवेश द्वार की दिशा देखते समय लाइव कैमरा प्रीव्यू के लिए कैमरा एक्सेस आवश्यक है।",
        "vastu_compass_unavailable_title" to "कम्पास उपलब्ध नहीं",
        "vastu_compass_unavailable_msg" to "इस डिवाइस में कम्पास सेंसर नहीं है, इसलिए लाइव दिशा नहीं दिखाई जा सकती। कृपया दिशा मैन्युअल रूप से चुनें।",
        "vastu_compass_style_classic" to "क्लासिक",
        "vastu_compass_style_minimal" to "मिनिमल",
        "vastu_compass_style_mandala" to "वास्तु मंडल",
        "vastu_compass_style_digital" to "डिजिटल",
        "vastu_compass_capture_cd" to "दिशा फोटो लें",
        "vastu_compass_photo_title" to "दिशा फोटो",
        "vastu_compass_saved" to "फोटो गैलरी में सहेजी गई।",
        "vastu_compass_save_failed" to "गैलरी में नहीं सहेजी जा सकी, लेकिन फोटो तैयार है।",
        "vastu_compass_capture_failed" to "फोटो नहीं ली जा सकी। कृपया पुनः प्रयास करें।",
        "vastu_compass_use_photo" to "नक्शा विश्लेषण में उपयोग करें",
        "vastu_sev_high" to "गंभीर",
        "vastu_sev_medium" to "मध्यम",
        "vastu_sev_low" to "हल्का",
        "vastu_count_high" to "गंभीर",
        "vastu_count_medium" to "मध्यम",
        "vastu_count_low" to "हल्का",
        "vastu_rule" to "नियम",
        "vastu_remedies" to "उपाय (तोड़फोड़ के बिना):",
        "vastu_dosha_fallback" to "दोष",
        "vastu_grade_85" to "उत्कृष्ट — वास्तु के अनुरूप",
        "vastu_grade_70" to "अच्छा — अधिकतर अनुरूप",
        "vastu_grade_50" to "औसत — कुछ सुधार सलाहित",
        "vastu_grade_low" to "सुधार आवश्यक",
        "vastu_insufficient_data" to "अपर्याप्त जानकारी — स्कोर नहीं निकाला जा सका",

        // ---- Kundali "Ask AI about this" (all 23 tabs) ----
        "kundali_ask_ai_title" to "इस भाग का व्यक्तिगत विश्लेषण चाहिए?",
        "kundali_ask_ai_sub" to "इस भाग का डेटा Om Astro AI से जोड़ें — आपका अगला चैट संदेश इसका पूरा संदर्भ साथ ले जाएगा।",
        "kundali_ask_ai_btn" to "इसके बारे में AI से पूछें",

        // ---- Dashboard: AI-Powered Readings section ----
        "dash_new_badge" to "नया",
        "dash_ai_readings_title" to "AI-संचालित विश्लेषण",
        "dash_ai_readings_sub" to "Om Astro AI की फोटो और गृह ऊर्जा रीडिंग",
        "dash_vastu_sub" to "गृह ऊर्जा विश्लेषण, दोष और उपाय",
        "dash_palm_sub" to "AI आपकी हथेली की रेखाएं, पर्वत और चिह्न पढ़ता है",
        "dash_face_sub" to "सामुद्रिक शास्त्र चेहरा विश्लेषण",

        // ---- Honesty audit: no fabricated scores/readings ----
        "honesty_not_returned" to "सर्वर से प्राप्त नहीं हुआ",
        "honesty_not_available" to "उपलब्ध नहीं",
        "honesty_saved_bad_data" to "इस सहेजी गई कुंडली में जन्म विवरण अधूरा है — इसे खोला नहीं जा सकता।",
        "honesty_gochar_coords" to "सटीक निर्देशांक के लिए जन्म शहर खोज सुझावों में से चुनें।",
        "honesty_vimshopaka_na" to "स्कोर उपलब्ध नहीं — सर्वर से प्राप्त नहीं हुआ।",
        "honesty_num_tip_generic" to "वर्तनी बदलने से पहले ऊपर दिया गया पूरा सर्वर विश्लेषण अवश्य पढ़ें।"
    )

    private val EN = mapOf(
        "perm_allow" to "Allow",
        "perm_not_now" to "Not now",
        "perm_ok" to "OK",
        "perm_settings_note" to "You can change this anytime in the system Settings.",
        "perm_denied_msg" to "Permission was denied, so this feature is unavailable. Enable it in the system Settings to use it.",
        "voice_unavailable" to "Voice typing is not available on this device.",
        "voice_no_match" to "Did not catch that. Please try again.",
        "voice_need_mic_perm" to "Microphone permission is required for voice typing.",
        "voice_need_net" to "Voice typing needs an internet connection.",
        "voice_failed" to "Voice typing failed. Please try again.",
        "voice_could_not_start" to "Voice typing could not start.",
        "back" to "Back",
        "chat_mic_title" to "Microphone access",
        "chat_mic_rationale" to "Voice typing converts your speech into text for the chat. The microphone is only used while you tap the mic button.",
        "chat_voice_on_snack" to "Voice mode on. AI replies will be read aloud.",
        "chat_voice_on_cd" to "Voice mode on",
        "chat_voice_off_cd" to "Voice mode off",
        "chat_voice_stop_cd" to "Stop speaking",
        "chat_stop_listening" to "Stop listening",
        "chat_voice_typing" to "Voice typing",
        "chat_analyze_photo_cd" to "Analyze a photo",
        "chat_input_hint" to "Ask your question...",
        "chat_photo_attached" to "Photo analysis attached. Ask anything about it.",
        "chat_close" to "Close",
        "photo_title" to "Analyze a Photo",
        "photo_sub" to "Take or choose a photo — palm, house map, chart — and ask AI about it.",
        "photo_take" to "Take Photo",
        "photo_take_sub" to "Use the camera",
        "photo_gallery" to "Choose from Gallery",
        "photo_gallery_sub" to "Pick a saved photo",
        "photo_file" to "Choose File",
        "photo_file_sub" to "Any image file",
        "photo_preview_cd" to "Photo preview",
        "photo_ready" to "Photo ready",
        "photo_question_label" to "Your question about this photo",
        "photo_question_hint" to "e.g. What does my palm say about career?",
        "photo_retake" to "Retake",
        "photo_analyze_btn" to "Analyze",
        "photo_analyzing" to "Analyzing your photo...",
        "photo_cam_title" to "Camera access",
        "photo_cam_rationale" to "Taking a photo needs camera access — e.g. a palm photo or a house-map photo for analysis.",
        "photo_lib_title" to "Photo access",
        "photo_lib_rationale" to "Choosing a photo from your gallery needs photo-library access.",
        "photo_err_save" to "Could not save the photo. Please try again.",
        "photo_err_read" to "Could not read the selected photo.",
        "photo_err_read_file" to "Could not read the selected file.",
        "photo_err_failed" to "Photo analysis failed.",
        "result_title" to "Photo Analysis",
        "result_empty" to "No analysis returned.",
        "result_attach_hint" to "Attach the findings to this chat so you can ask follow-up questions about the photo.",
        "result_attach" to "Attach to Chat",
        "result_ask" to "Ask AI",
        "palm_title" to "Palmistry",
        "palm_sub" to "AI palm reading from your photo",
        "palm_capture_title" to "Photograph your palm",
        "palm_tip_1" to "Hold your palm flat with fingers together",
        "palm_tip_2" to "Use bright, even light — avoid shadows",
        "palm_tip_3" to "Fill the frame with your palm",
        "palm_tip_4" to "Keep the photo sharp, not blurry",
        "palm_which_hand" to "Which hand?",
        "palm_right" to "Right Hand",
        "palm_left" to "Left Hand",
        "palm_preview_cd" to "Palm preview",
        "palm_ready" to "Photo ready for analysis",
        "palm_camera" to "Camera",
        "palm_gallery" to "Gallery",
        "palm_analyze" to "Analyze My Palm",
        "palm_loading" to "Reading your palm...",
        "palm_loading_sub" to "The AI is studying every line and mount.",
        "palm_your_palm" to "Your Palm",
        "palm_hand_word_right" to "Right hand",
        "palm_hand_word_left" to "Left hand",
        "palm_hand_word" to " hand",
        "palm_detailed" to "Detailed Reading",
        "palm_lines" to "Lines",
        "palm_mounts" to "Mounts",
        "palm_obs" to "Key Observations",
        "palm_guidance" to "Guidance",
        "palm_unclear" to "Unclear in Photo",
        "palm_new" to "New Photo",
        "palm_ask" to "Ask AI About This Reading",
        "palm_err_save" to "Could not save the photo.",
        "palm_err_read" to "Could not read the selected photo.",
        "palm_err_failed" to "Palm analysis failed. Please try again.",
        "palm_cam_rationale" to "Taking a palm photo needs camera access.",
        "palm_lib_rationale" to "Choosing a palm photo from your gallery needs photo-library access.",
        "palm_cam_title" to "Camera access",
        "palm_lib_title" to "Photo access",
        "face_title" to "Face Reading",
        "face_sub" to "Samudrika Shastra face reading from your photo",
        "face_capture_title" to "Photograph your face",
        "face_tip_1" to "Face the camera directly, head straight",
        "face_tip_2" to "Use bright, even light on your face — avoid shadows",
        "face_tip_3" to "Keep a neutral expression, eyes open",
        "face_tip_4" to "Fill the frame with your face, no sunglasses or hats",
        "face_preview_cd" to "Face preview",
        "face_ready" to "Photo ready for analysis",
        "face_analyze" to "Analyze My Face",
        "face_loading" to "Reading your face...",
        "face_loading_sub" to "The AI is studying every feature of your face.",
        "face_your_face" to "Your Face",
        "face_features" to "Facial Features",
        "face_observation" to "Observation",
        "face_interpretation" to "Interpretation",
        "face_personality" to "Overall Personality",
        "face_career" to "Career",
        "face_relationships" to "Relationships",
        "face_health" to "Health Indications",
        "face_guidance" to "Guidance",
        "face_err_failed" to "Face analysis failed. Please try again.",
        "face_err_unavailable" to "Face reading is not available yet. Please try again later.",
        "face_cam_rationale" to "Taking a face photo needs camera access.",
        "face_lib_rationale" to "Choosing a face photo from your gallery needs photo-library access.",
        "face_not_face_title" to "No clear face detected",
        "face_not_face_hint" to "Please upload a clear, front-facing photo of a face.",
        "face_ctx_label" to "Face reading",
        "vastu_title" to "Vastu Shastra",
        "vastu_sub" to "Classical home-energy analysis",
        "vastu_tab_manual" to "Manual Entry",
        "vastu_tab_photo" to "House-Map Photo",
        "vastu_card_facing" to "Facing & Entrance",
        "vastu_facing_label" to "Main facing direction",
        "vastu_pada_label" to "Entrance pada (1–32, optional)",
        "vastu_pada_hint" to "Enter 1–32. Each direction has 4 padas with different devatas.",
        "vastu_card_plot" to "Plot",
        "vastu_plot_shape" to "Plot shape",
        "vastu_length" to "Length (ft)",
        "vastu_breadth" to "Breadth (ft)",
        "vastu_slope" to "Slope towards (optional)",
        "vastu_road" to "Road direction (optional)",
        "vastu_card_water" to "Water & Infrastructure",
        "vastu_water" to "Water source / borewell",
        "vastu_tank" to "Overhead tank",
        "vastu_septic" to "Septic tank",
        "vastu_staircase" to "Staircase",
        "vastu_card_rooms" to "Rooms",
        "vastu_add_room" to "Add Room",
        "vastu_remove_room_cd" to "Remove room",
        "vastu_analyze" to "Analyze Vastu",
        "vastu_not_set" to "Not set",
        "vastu_analyzing" to "Analyzing Vastu...",
        "vastu_err_failed" to "Vastu analysis failed.",
        "vastu_photo_err_failed" to "Photo analysis failed.",
        "vastu_photo_title" to "Upload your house map",
        "vastu_photo_sub" to "AI reads the floor plan — rooms, entrance, directions — then the classical Vastu engine scores it. Mark North on the plan if you can.",
        "vastu_photo_ready" to "House map ready",
        "vastu_photo_map_cd" to "House map preview",
        "vastu_notes_label" to "Notes (optional) — e.g. entrance faces east",
        "vastu_analyze_map" to "Analyze House Map",
        "vastu_cam_rationale" to "Photographing your house map needs camera access.",
        "vastu_lib_rationale" to "Choosing a house-map photo needs photo-library access.",
        "vastu_cam_title" to "Camera access",
        "vastu_lib_title" to "Photo access",
        "vastu_photo_err_save" to "Could not save the photo.",
        "vastu_photo_err_read" to "Could not read the selected photo.",
        "vastu_doshas_found" to "Doshas Found",
        "vastu_doshas" to "Doshas",
        "vastu_no_doshas" to "No doshas found. This layout follows classical Vastu principles well.",
        "vastu_verdicts" to "Placement Verdicts",
        "vastu_new" to "New Analysis",
        "vastu_ask_ai" to "Ask AI About This Reading",
        "vastu_ctx_label" to "Vastu analysis",
        "vastu_compass_title" to "Compass Mode",
        "vastu_compass_open" to "Find direction with compass",
        "vastu_compass_hint" to "Point the top of your phone at your main entrance — the compass shows which direction it faces.",
        "vastu_compass_calibrate" to "Compass accuracy is low — calibrate by moving your phone in a figure-8 motion.",
        "vastu_compass_use" to "Use this direction",
        "vastu_compass_cam_title" to "Camera Access",
        "vastu_compass_cam_rationale" to "Camera access is needed for the live preview while you check your entrance direction.",
        "vastu_compass_unavailable_title" to "Compass not available",
        "vastu_compass_unavailable_msg" to "This device has no compass sensor, so live direction cannot be shown. Please choose the direction manually.",
        "vastu_compass_style_classic" to "Classic",
        "vastu_compass_style_minimal" to "Minimal",
        "vastu_compass_style_mandala" to "Vastu Mandala",
        "vastu_compass_style_digital" to "Digital",
        "vastu_compass_capture_cd" to "Capture direction photo",
        "vastu_compass_photo_title" to "Direction photo",
        "vastu_compass_saved" to "Photo saved to gallery.",
        "vastu_compass_save_failed" to "Could not save to gallery, but the photo is ready.",
        "vastu_compass_capture_failed" to "Could not capture photo. Please try again.",
        "vastu_compass_use_photo" to "Use for house-map analysis",
        "vastu_sev_high" to "High",
        "vastu_sev_medium" to "Medium",
        "vastu_sev_low" to "Low",
        "vastu_count_high" to "High",
        "vastu_count_medium" to "Medium",
        "vastu_count_low" to "Low",
        "vastu_rule" to "Rule",
        "vastu_remedies" to "Remedies (no demolition):",
        "vastu_dosha_fallback" to "Dosha",
        "vastu_grade_85" to "Excellent — strongly Vastu-aligned",
        "vastu_grade_70" to "Good — mostly aligned",
        "vastu_grade_50" to "Average — some corrections advised",
        "vastu_grade_low" to "Needs correction",
        "vastu_insufficient_data" to "Insufficient data — no score computed",

        // ---- Kundali "Ask AI about this" (all 23 tabs) ----
        "kundali_ask_ai_title" to "Want a personalised reading of this section?",
        "kundali_ask_ai_sub" to "Attach this section's data to Om Astro AI — your next chat message will carry its full context.",
        "kundali_ask_ai_btn" to "Ask AI about this",

        // ---- Dashboard: AI-Powered Readings section ----
        "dash_new_badge" to "New",
        "dash_ai_readings_title" to "AI-Powered Readings",
        "dash_ai_readings_sub" to "Photo and home-energy readings by Om Astro AI",
        "dash_vastu_sub" to "Home-energy analysis, doshas and remedies",
        "dash_palm_sub" to "AI reads your palm lines, mounts and signs",
        "dash_face_sub" to "Samudrika Shastra face reading",

        // ---- Honesty audit: no fabricated scores/readings ----
        "honesty_not_returned" to "Not returned by server",
        "honesty_not_available" to "Not available",
        "honesty_saved_bad_data" to "This saved chart has incomplete birth data — it cannot be opened.",
        "honesty_gochar_coords" to "Please pick your birth city from the search suggestions so coordinates are accurate.",
        "honesty_vimshopaka_na" to "Score unavailable — not returned by server.",
        "honesty_num_tip_generic" to "Read the full server analysis above before changing any spellings."
    )

    /** Active-language string for [key]. Falls back to English. */
    fun t(key: String): String {
        val map = if (LangPrefs.language.value == "hi") HI else EN
        return map[key] ?: EN[key] ?: key
    }

    // ---- Bilingual picker display names (Vastu) ----

    private val DIR_HI = mapOf(
        "N" to "उत्तर", "NE" to "उत्तर-पूर्व", "E" to "पूर्व", "SE" to "दक्षिण-पूर्व",
        "S" to "दक्षिण", "SW" to "दक्षिण-पश्चिम", "W" to "पश्चिम", "NW" to "उत्तर-पश्चिम",
        "C" to "केंद्र (ब्रह्मस्थान)"
    )
    private val DIR_EN = mapOf(
        "N" to "North", "NE" to "North-East", "E" to "East", "SE" to "South-East",
        "S" to "South", "SW" to "South-West", "W" to "West", "NW" to "North-West",
        "C" to "Centre (Brahmasthan)"
    )

    private val ROOM_HI = mapOf(
        "pooja" to "पूजा कक्ष", "kitchen" to "रसोईघर",
        "master_bedroom" to "मास्टर बेडरूम", "bedroom" to "शयनकक्ष",
        "children_room" to "बच्चों का कमरा", "guest_room" to "अतिथि कक्ष",
        "living_room" to "लिविंग रूम", "drawing_room" to "ड्राइंग रूम",
        "dining" to "भोजन कक्ष", "study" to "अध्ययन कक्ष",
        "home_office" to "होम ऑफिस", "toilet" to "शौचालय",
        "bathroom" to "स्नानघर", "store" to "स्टोर रूम",
        "garage" to "गैराज", "balcony" to "बालकनी",
        "staircase" to "सीढ़ियां", "water_source" to "जल स्रोत",
        "overhead_tank" to "ओवरहेड टंकी", "septic_tank" to "सेप्टिक टंकी"
    )
    private val ROOM_EN = mapOf(
        "pooja" to "Pooja Room", "kitchen" to "Kitchen",
        "master_bedroom" to "Master Bedroom", "bedroom" to "Bedroom",
        "children_room" to "Children's Room", "guest_room" to "Guest Room",
        "living_room" to "Living Room", "drawing_room" to "Drawing Room",
        "dining" to "Dining Room", "study" to "Study Room",
        "home_office" to "Home Office", "toilet" to "Toilet",
        "bathroom" to "Bathroom", "store" to "Store Room",
        "garage" to "Garage", "balcony" to "Balcony",
        "staircase" to "Staircase", "water_source" to "Water Source",
        "overhead_tank" to "Overhead Tank", "septic_tank" to "Septic Tank"
    )

    /** Direction display name in the active language. */
    fun dirName(code: String): String {
        val map = if (LangPrefs.language.value == "hi") DIR_HI else DIR_EN
        return map[code] ?: code
    }

    private val DEITY_HI = mapOf(
        "N" to "कुबेर", "NE" to "ईशान", "E" to "इंद्र", "SE" to "अग्नि",
        "S" to "यम", "SW" to "निरृति", "W" to "वरुण", "NW" to "वायु"
    )
    private val DEITY_EN = mapOf(
        "N" to "Kubera", "NE" to "Ishana", "E" to "Indra", "SE" to "Agni",
        "S" to "Yama", "SW" to "Nirriti", "W" to "Varuna", "NW" to "Vayu"
    )

    /** Dikpala (guardian deity) of a direction, in the active language. */
    fun vastuDeityName(code: String): String {
        val map = if (LangPrefs.language.value == "hi") DEITY_HI else DEITY_EN
        return map[code] ?: code
    }

    /** Room-type display name in the active language. */
    fun roomName(value: String): String {
        val map = if (LangPrefs.language.value == "hi") ROOM_HI else ROOM_EN
        return map[value] ?: value
    }

    /** Plot-shape display name in the active language. */
    fun plotShapeName(value: String): String {
        if (LangPrefs.language.value != "hi") return value.replaceFirstChar { it.uppercase() }
        return when (value) {
            "square" -> "वर्गाकार"
            "rectangle" -> "आयताकार"
            "irregular" -> "अनियमित"
            "L-shaped" -> "L-आकार"
            "triangular" -> "त्रिकोणीय"
            else -> value
        }
    }

    /** Facial-feature display name in the active language (face reading). */
    fun faceFeatureName(key: String): String {
        val hi = LangPrefs.language.value == "hi"
        return when (key) {
            "face_shape" -> if (hi) "चेहरे का आकार" else "Face Shape"
            "forehead" -> if (hi) "माथा" else "Forehead"
            "eyebrows" -> if (hi) "भौहें" else "Eyebrows"
            "eyes" -> if (hi) "आंखें" else "Eyes"
            "nose" -> if (hi) "नाक" else "Nose"
            "lips" -> if (hi) "होंठ" else "Lips"
            "chin" -> if (hi) "ठोड़ी" else "Chin"
            "ears" -> if (hi) "कान" else "Ears"
            else -> key.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
    }
}
