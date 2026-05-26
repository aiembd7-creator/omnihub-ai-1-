package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.PostHistory
import com.example.data.database.TelegramChannel
import com.example.data.database.UserProfile
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(database.dao(), application)

    // Current navigation tab: Home, Telegram, ImageToPrompt, PromptToImage, Translator, Social, Song, Profile
    private val _currentTab = MutableStateFlow("Home")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Observable states from Local Database
    val channels: StateFlow<List<TelegramChannel>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val histories: StateFlow<List<PostHistory>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .map { it ?: UserProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val totalChannelsCount: StateFlow<Int> = repository.channelCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // --- TELEGRAM SCREEN STATES ---
    private val _telegramLoading = MutableStateFlow(false)
    val telegramLoading: StateFlow<Boolean> = _telegramLoading.asStateFlow()

    private val _telegramMessage = MutableStateFlow<String?>(null)
    val telegramMessage: StateFlow<String?> = _telegramMessage.asStateFlow()

    // --- IMAGE TO PROMPT STATES ---
    private val _selectedBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedBitmap: StateFlow<Bitmap?> = _selectedBitmap.asStateFlow()

    private val _imageToPromptResult = MutableStateFlow("")
    val imageToPromptResult: StateFlow<String> = _imageToPromptResult.asStateFlow()

    private val _imageToPromptLoading = MutableStateFlow(false)
    val imageToPromptLoading: StateFlow<Boolean> = _imageToPromptLoading.asStateFlow()

    // --- PROMPT TO IMAGE STATES ---
    private val _originalImagePrompt = MutableStateFlow("")
    val originalImagePrompt: StateFlow<String> = _originalImagePrompt.asStateFlow()

    private val _generatedImageUrl = MutableStateFlow("")
    val generatedImageUrl: StateFlow<String> = _generatedImageUrl.asStateFlow()

    private val _promptToImageLoading = MutableStateFlow(false)
    val promptToImageLoading: StateFlow<Boolean> = _promptToImageLoading.asStateFlow()

    // --- TRANSLATOR STATES ---
    private val _translatorInput = MutableStateFlow("")
    val translatorInput: StateFlow<String> = _translatorInput.asStateFlow()

    private val _translatorOutput = MutableStateFlow("")
    val translatorOutput: StateFlow<String> = _translatorOutput.asStateFlow()

    private val _translatorLoading = MutableStateFlow(false)
    val translatorLoading: StateFlow<Boolean> = _translatorLoading.asStateFlow()

    // --- SOCIAL METADATA STATES ---
    private val _socialTitle = MutableStateFlow("")
    val socialTitle: StateFlow<String> = _socialTitle.asStateFlow()

    private val _socialDescription = MutableStateFlow("")
    val socialDescription: StateFlow<String> = _socialDescription.asStateFlow()

    private val _socialHashtags = MutableStateFlow("")
    val socialHashtags: StateFlow<String> = _socialHashtags.asStateFlow()

    private val _socialLoading = MutableStateFlow(false)
    val socialLoading: StateFlow<Boolean> = _socialLoading.asStateFlow()

    // --- SONG & LYRICS STATES ---
    private val _songResultLyrics = MutableStateFlow("")
    val songResultLyrics: StateFlow<String> = _songResultLyrics.asStateFlow()

    private val _songLoading = MutableStateFlow(false)
    val songLoading: StateFlow<Boolean> = _songLoading.asStateFlow()

    private val _isPlayingSong = MutableStateFlow(false)
    val isPlayingSong: StateFlow<Boolean> = _isPlayingSong.asStateFlow()

    private val _visualizerBars = MutableStateFlow<List<Float>>(List(16) { 0.1f })
    val visualizerBars: StateFlow<List<Float>> = _visualizerBars.asStateFlow()

    private var songPlayJob: Job? = null

    init {
        // Enforce presence of at least one user profile row upon launch.
        viewModelScope.launch {
            repository.userProfile.collect { profile ->
                if (profile == null) {
                    repository.updateProfile(UserProfile())
                }
            }
        }
    }

    // --- TAB FLOW ACTIONS ---
    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    // --- TELEGRAM MANAGEMENT ACTIONS ---

    fun verifyAndAddChannel(botToken: String, channelIdInput: String, onResponse: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _telegramLoading.value = true
            _telegramMessage.value = "চ্যানেলটি এবং বট যাচাই করা হচ্ছে..."
            try {
                // Formatting channel identifier
                val channelId = if (channelIdInput.trim().startsWith("@") || channelIdInput.startsWith("-")) {
                    channelIdInput.trim()
                } else {
                    "@${channelIdInput.trim()}"
                }

                val (title, subCount) = repository.verifyAndFetchChat(botToken, channelId)
                val newChannel = TelegramChannel(
                    channelName = title,
                    botToken = botToken,
                    channelId = channelId,
                    subscriberCount = subCount
                )
                repository.saveChannel(newChannel)
                _telegramLoading.value = false
                _telegramMessage.value = "চ্যানেল '$title' সঠিকভাবে যোগ করা হয়েছে!"
                onResponse(true, "চ্যানেল '$title' সঠিকভাবে যোগ করা হয়েছে!")
            } catch (e: Exception) {
                _telegramLoading.value = false
                _telegramMessage.value = "ভুল: ${e.localizedMessage}"
                onResponse(false, e.localizedMessage ?: "টোকেন বা চ্যানেল ভুল হয়েছে।")
            }
        }
    }

    fun deleteTelegramChannel(channel: TelegramChannel) {
        viewModelScope.launch {
            repository.deleteChannel(channel)
            _telegramMessage.value = "চ্যানেল '${channel.channelName}' ডিলিট করা হয়েছে।"
        }
    }

    fun sendBroadcastPost(text: String, imageUrl: String? = null, onResponse: (String) -> Unit) {
        viewModelScope.launch {
            if (channels.value.isEmpty()) {
                onResponse("দয়া করে প্রথমে চ্যানেল যোগ করুন!")
                return@launch
            }
            _telegramLoading.value = true
            _telegramMessage.value = "সবগুলো চ্যানেলে পোস্ট ব্রডকাস্ট করা হচ্ছে..."

            var successCount = 0
            var failCount = 0
            var finalMsg = ""

            channels.value.forEach { channel ->
                val (success, msg) = repository.sendTelegramPost(
                    botToken = channel.botToken,
                    channelId = channel.channelId,
                    channelName = channel.channelName,
                    text = text,
                    imageUrl = imageUrl
                )
                if (success) successCount++ else {
                    failCount++
                    finalMsg += "\n[${channel.channelName}]: $msg"
                }
            }

            _telegramLoading.value = false
            val summary = "ব্রডকাস্ট সম্পন্ন! সফল: $successCount, ব্যর্থ: $failCount.$finalMsg"
            _telegramMessage.value = summary
            onResponse(summary)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearPostHistory()
            _telegramMessage.value = "পোস্ট ইতিহাস মুছে ফেলা হয়েছে।"
        }
    }

    fun dismissTelegramMessage() {
        _telegramMessage.value = null
    }

    fun checkGeminiKeyValid(): Boolean {
        return repository.getApiKey().isNotEmpty()
    }

    // --- FEATURE: IMAGE TO PROMPT ---

    fun selectBitmap(bitmap: Bitmap?) {
        _selectedBitmap.value = bitmap
        _imageToPromptResult.value = ""
    }

    fun generatePromptFromImage(customInstruction: String = "") {
        val bitmap = _selectedBitmap.value
        if (bitmap == null) {
            _imageToPromptResult.value = "ত্রুটি: প্রথমে একটি ছবি নির্বাচন বা ক্যামেরায় তুলুন।"
            return
        }

        viewModelScope.launch {
            _imageToPromptLoading.value = true
            val prompt = if (customInstruction.isEmpty()) {
                "Analyze this image carefully. Write a beautiful, descriptive, poetic prompt that captures the ambiance, style, layout, elements, lighting, and exact aesthetic of this image. This generated prompt will be used for text-to-image generator. Send only the refined prompt in English, no other textual chatter."
            } else {
                customInstruction
            }

            val result = repository.generateImageToPrompt(bitmap, prompt)
            _imageToPromptResult.value = result
            _imageToPromptLoading.value = false
            repository.incrementStat("image")
        }
    }

    // --- FEATURE: PROMPT TO IMAGE ---

    fun setOriginalImagePrompt(prompt: String) {
        _originalImagePrompt.value = prompt
    }

    fun generateImageFromPrompt(refinedPromptInput: String) {
        if (refinedPromptInput.isEmpty()) {
            _generatedImageUrl.value = "ত্রুটি: অনুগ্রহ করে কোনো প্রম্পট লিখুন।"
            return
        }

        viewModelScope.launch {
            _promptToImageLoading.value = true
            _generatedImageUrl.value = ""

            // Refine prompt using Gemini first to make the generated visuals look cinematic and highly detailed!
            val enhancerInstruction = "Re-write and enhance the following simple image description prompt into a stunningly beautiful, highly detailed, photorealistic prompt with epic cinematic lighting, Unreal Engine 5 details, depth of field, 8k resolution, and high artistic value. Send ONLY the enhanced prompt string in English, no introductory text: \"$refinedPromptInput\""
            
            val enhancedPrompt = if (checkGeminiKeyValid()) {
                repository.generateText(enhancerInstruction)
            } else {
                refinedPromptInput
            }

            Log.d("DashboardViewModel", "Enhanced prompt for Image: $enhancedPrompt")

            // Using Pollinations AI simple REST URL model (it's fast, free, beautiful, and supports arbitrary content!)
            // Encoding the enhanced prompt to safe URL parameters
            val encodedPrompt = java.net.URLEncoder.encode(enhancedPrompt, "UTF-8")
            val selectedUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=1024&height=1024&nologo=true&seed=${kotlin.random.Random.nextInt(100000)}"

            delay(2000) // Aesthetic delay simulating AI model pass
            _generatedImageUrl.value = selectedUrl
            _promptToImageLoading.value = false
            repository.incrementStat("image")
        }
    }

    // --- FEATURE: ANY TO ANY TRANSLATOR ---

    fun setTranslatorInput(text: String) {
        _translatorInput.value = text
    }

    fun translateText(sourceLang: String, targetLang: String) {
        val input = _translatorInput.value
        if (input.trim().isEmpty()) {
            _translatorOutput.value = "দয়া করে সেন্টেন্স বা বাক্য ইনপুট দিন।"
            return
        }

        viewModelScope.launch {
            _translatorLoading.value = true
            val prompt = "Translate the following text from '$sourceLang' language to '$targetLang' language. Deliver only the translation outcome without any external conversational text or notes. Here is the text:\n\n$input"
            val result = repository.generateText(prompt)
            _translatorOutput.value = result
            _translatorLoading.value = false
            repository.incrementStat("translation")
        }
    }

    // --- FEATURE: PLATFORM POST PROMPT METADATA GENERATOR ---

    fun generateSocialMediaPost(platform: String, mode: String, keywords: String, tone: String) {
        if (keywords.trim().isEmpty()) {
            _socialTitle.value = "ত্রুটি: অনুগ্রহ করে বিষয়বস্তু বা কী-ওয়ার্ড উল্লেখ করুন।"
            _socialDescription.value = ""
            _socialHashtags.value = ""
            return
        }

        viewModelScope.launch {
            _socialLoading.value = true
            val prompt = """
                Generate social media metadata optimized for the '$platform' platform about the topic '$keywords'. 
                The target objective is '$mode' (e.g. Title, Description, Hashtags, or All-in-One). 
                The tone of the content must be '$tone' and predominantly tailored in beautiful sounding Bengali (with English tags if helpful). 
                Generate a structured JSON response representation or structured plain text containing three distinct blocks clearly labelled as:
                ---TITLE---
                (A list of 3-5 high click-through catchy titles)
                
                ---DESCRIPTION---
                (A highly optimized, engaging description including hooks for the platform)
                
                ---HASHTAGS---
                (10-15 trending and niche hashtags)
            """.trimIndent()

            val rawResult = repository.generateText(prompt)
            parseSocialResult(rawResult)
            _socialLoading.value = false
            repository.incrementStat("social")
        }
    }

    private fun parseSocialResult(raw: String) {
        try {
            // Primitive but highly robust text blocks extractor
            val titleMarker = "---TITLE---"
            val descMarker = "---DESCRIPTION---"
            val tagsMarker = "---HASHTAGS---"

            val titleIdx = raw.indexOf(titleMarker)
            val descIdx = raw.indexOf(descMarker)
            val tagsIdx = raw.indexOf(tagsMarker)

            if (titleIdx != -1 && descIdx != -1 && tagsIdx != -1) {
                val ordered = listOf(titleIdx, descIdx, tagsIdx).sorted()
                
                fun getBlockContent(markerIdx: Int, blockText: String): String {
                    val start = markerIdx + blockText.length
                    val nextMarkerIdx = ordered.firstOrNull { it > markerIdx } ?: raw.length
                    return raw.substring(start, nextMarkerIdx).trim()
                }

                _socialTitle.value = getBlockContent(titleIdx, titleMarker)
                _socialDescription.value = getBlockContent(descIdx, descMarker)
                _socialHashtags.value = getBlockContent(tagsIdx, tagsMarker)
            } else {
                // Formatting fallback
                _socialTitle.value = "Catchy Title Suggestions:"
                _socialDescription.value = raw
                _socialHashtags.value = "#viral #aihub #bangladesh"
            }
        } catch (e: Exception) {
            _socialTitle.value = "ক্রিয়েশন সম্পন্ন"
            _socialDescription.value = raw
            _socialHashtags.value = ""
        }
    }

    // --- FEATURE: SONG BUILDER ---

    fun generateSongLyrics(theme: String, genre: String, mood: String) {
        if (theme.trim().isEmpty()) {
            _songResultLyrics.value = "দয়া করে গানের বিষয়বস্তু বা থিম উল্লেখ করুন।"
            return
        }

        viewModelScope.launch {
            _songLoading.value = true
            _songResultLyrics.value = ""
            val prompt = """
                Generate a complete structural song lyric set in Bengali (with English musical chord markers placed above lines, formatted clearly like lyric sheet) about the theme '$theme'. 
                The genre of the song should be '$genre' and the emotional vibe/mood is '$mood'.
                Make it incredibly poetic, expressive, and properly structured into:
                - Title
                - Verse 1
                - Refrain/Chorus (স্থায়ী)
                - Verse 2
                - Bridge (অন্তরা)
                - Chorus (স্থায়ী)
                - Outro
                Include chords in brackets like [C], [Am], [G], [F] right above the lyric phrases. Use natural sounding emotional Bengali words.
            """.trimIndent()

            val result = repository.generateText(prompt)
            _songResultLyrics.value = result
            _songLoading.value = false
            repository.incrementStat("song")
        }
    }

    fun togglePlaySynthesizer() {
        if (_isPlayingSong.value) {
            // Stop playing synth
            _isPlayingSong.value = false
            songPlayJob?.cancel()
            _visualizerBars.value = List(16) { 0.1f }
        } else {
            // Start playing synth
            if (_songResultLyrics.value.isEmpty()) {
                _songResultLyrics.value = "একটি ডেমো লিরিক্স তৈরি হচ্ছে যা সিন্থ প্লেয়ার শুনতে সুবিধা দেবে..."
                generateSongLyrics("মেঘলা দিন ও নদী", "ফোক (Folk)", "রোমান্টিক")
            }
            _isPlayingSong.value = true
            startAmbientAudioSynth()
        }
    }

    private fun startAmbientAudioSynth() {
        songPlayJob?.cancel()
        songPlayJob = viewModelScope.launch {
            // Notes for ambient synth chords.
            // Sound codes mapped in standard DTMF tone generators (C, E, G, B chords)
            val synthTones = intArrayOf(
                ToneGenerator.TONE_PROP_PROMPT,
                ToneGenerator.TONE_SUP_PIP,
                ToneGenerator.TONE_PROP_BEEP,
                ToneGenerator.TONE_PROP_BEEP2
            )

            try {
                // Initialize tone generator on low volume (STREAM_MUSIC, vol level 30)
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 35)
                var tick = 0

                while (_isPlayingSong.value) {
                    val selectedTone = synthTones[tick % synthTones.size]
                    
                    // Simple retro note beep play
                    toneGen.startTone(selectedTone, 140)

                    // Refresh visualizer bar values
                    _visualizerBars.value = List(16) { 
                        val base = if (tick % 2 == 0) 0.8f else 0.4f
                        (base + kotlin.random.Random.nextFloat() * 0.2f).coerceIn(0.1f, 1.0f)
                    }

                    delay(350)
                    tick++
                }
                toneGen.release()
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Synth audio fail", e)
                // Fallback to purely visual animated waves
                var tick = 0
                while (_isPlayingSong.value) {
                    _visualizerBars.value = List(16) { 
                        kotlin.random.Random.nextFloat() * 0.9f + 0.1f
                    }
                    delay(350)
                    tick++
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        songPlayJob?.cancel()
    }

    // --- FEATURE: USER SETTINGS PROFILE MANAGER ---

    fun updateProfileInfo(name: String, bio: String) {
        viewModelScope.launch {
            val original = userProfile.value
            val updated = original.copy(name = name, bio = bio)
            repository.updateProfile(updated)
            _telegramMessage.value = "প্রোফাইল সঠিকভাবে আপডেট করা হয়েছে!"
        }
    }

    fun updateProfileAvatar(avatarUriString: String?) {
        viewModelScope.launch {
            val original = userProfile.value
            val updated = original.copy(avatarUri = avatarUriString)
            repository.updateProfile(updated)
            _telegramMessage.value = "প্রোফাইল ছবি হালনাগাদ করা হয়েছে!"
        }
    }
}
