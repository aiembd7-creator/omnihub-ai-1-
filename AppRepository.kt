package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.data.api.Content
import com.example.data.api.GeminiRequest
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.database.AppDao
import com.example.data.database.PostHistory
import com.example.data.database.TelegramChannel
import com.example.data.database.UserProfile
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class AppRepository(
    private val dao: AppDao,
    private val context: Context
) {
    // Database access
    val allChannels: Flow<List<TelegramChannel>> = dao.getAllChannels()
    val allHistory: Flow<List<PostHistory>> = dao.getAllPostHistory()
    val userProfile: Flow<UserProfile?> = dao.getUserProfile()
    val channelCount: Flow<Int> = dao.getChannelCount()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Gemini API Key lookup
    fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key == "MY_GEMINI_API_KEY" || key.isEmpty()) {
            ""
        } else {
            key
        }
    }

    // --- GEMINI FUNCTIONS ---

    suspend fun generateText(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isEmpty()) {
            return@withContext "ত্রুটি: Gemini API Key সেট করা নেই! অনুগ্রহ করে AI Studio Secrets প্যানেলে GEMINI_API_KEY ডিক্লেয়ার করুন।"
        }

        val parts = listOf(Part(text = prompt))
        val request = GeminiRequest(
            contents = listOf(Content(parts = parts))
        )

        try {
            val response = RetrofitClient.service.generateContent(key, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "কোনো রেসপন্স পাওয়া যায়নি। অনুগ্রহ করে আবার চেষ্টা করুন।"
        } catch (e: Exception) {
            Log.e("AppRepository", "Gemini error", e)
            "ত্রুটি: ${e.localizedMessage ?: "সার্ভারে সংযোগ করা যাচ্ছে না"}"
        }
    }

    suspend fun generateImageToPrompt(bitmap: Bitmap, customPrompt: String): String = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isEmpty()) {
            return@withContext "ত্রুটি: Gemini API Key সেট করা নেই! অনুগ্রহ করে AI Studio Secrets প্যানেলে GEMINI_API_KEY ডিক্লেয়ার করুন।"
        }

        val base64Image = bitmapToBase64(bitmap)
        val parts = listOf(
            Part(text = if (customPrompt.isEmpty()) "Describe this image in extreme detail and construct an advanced artistic prompt that can recreate this visual in high fidelity. Keep the prompt in English." else customPrompt),
            Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
        )

        val request = GeminiRequest(
            contents = listOf(Content(parts = parts))
        )

        try {
            val response = RetrofitClient.service.generateContent(key, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "কোনো রেসপন্স পাওয়া যায়নি।"
        } catch (e: Exception) {
            Log.e("AppRepository", "Gemini vision error", e)
            "ত্রুটি: ${e.localizedMessage ?: "ছবি বিশ্লেষণ করতে সমস্যা হয়েছে"}"
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    // --- TELEGRAM SERVICES ---

    // Automatically inspects channel with Telegram Bot API to fetch its information (Title, types, sub count)
    suspend fun verifyAndFetchChat(botToken: String, channelIdentifier: String): Pair<String, Int> = withContext(Dispatchers.IO) {
        val url = "https://api.telegram.org/bot$botToken/getChat?chat_id=$channelIdentifier"
        val request = Request.Builder().url(url).build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: throw IOException("খালি রেসপন্স")
                val json = JSONObject(bodyString)
                if (json.optBoolean("ok")) {
                    val result = json.getJSONObject("result")
                    val title = result.optString("title", result.optString("username", "ডান চ্যানেল"))
                    
                    // Also attempt to get subscriber count if type is channel
                    val subCount = getSubscriberCount(botToken, channelIdentifier)
                    Pair(title, subCount)
                } else {
                    val description = json.optString("description", "চ্যানেলটি খুঁজে পাওয়া যায়নি বা আপনার বট এই চ্যানেলের অ্যাডমিন নয়।")
                    throw Exception(description)
                }
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Telegram connection error", e)
            throw Exception("বট বা চ্যানেল ডাটা সঠিক নয়: ${e.localizedMessage}")
        }
    }

    private fun getSubscriberCount(botToken: String, channelIdentifier: String): Int {
        val url = "https://api.telegram.org/bot$botToken/getChatMemberCount?chat_id=$channelIdentifier"
        val request = Request.Builder().url(url).build()
        return try {
            okHttpClient.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string() ?: "")
                if (json.optBoolean("ok")) {
                    json.optInt("result", 0)
                } else 0
            }
        } catch (e: Exception) {
            0
        }
    }

    suspend fun sendTelegramPost(
        botToken: String,
        channelId: String,
        channelName: String,
        text: String,
        imageUrl: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val responseString: String
            val isSuccess: Boolean

            if (!imageUrl.isNullOrEmpty()) {
                // If we have an imageUrl, we send photo
                val url = "https://api.telegram.org/bot$botToken/sendPhoto"
                val formBody = JSONObject().apply {
                    put("chat_id", channelId)
                    put("photo", imageUrl)
                    put("caption", text)
                    put("parse_mode", "HTML")
                }
                val body = formBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    responseString = response.body?.string() ?: ""
                    isSuccess = response.isSuccessful && JSONObject(responseString).optBoolean("ok")
                }
            } else {
                // Otherwise normal sendMessage
                val url = "https://api.telegram.org/bot$botToken/sendMessage"
                val formBody = JSONObject().apply {
                    put("chat_id", channelId)
                    put("text", text)
                    put("parse_mode", "HTML")
                }
                val body = formBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    responseString = response.body?.string() ?: ""
                    isSuccess = response.isSuccessful && JSONObject(responseString).optBoolean("ok")
                }
            }

            if (isSuccess) {
                // Record history
                val history = PostHistory(
                    channelId = channelId,
                    channelName = channelName,
                    postText = text,
                    imageUri = imageUrl,
                    status = "সফল",
                    timestamp = System.currentTimeMillis()
                )
                dao.insertPostHistory(history)
                
                // Increment counter for Profile statistics
                val profile = dao.getUserProfileDirect() ?: UserProfile()
                dao.updateProfile(profile.copy(telegramGenerations = profile.telegramGenerations + 1))

                Pair(true, "চ্যানেলে সফলভাবে পোস্ট পাঠানো হয়েছে!")
            } else {
                val json = JSONObject(responseString)
                val errMsg = json.optString("description", "সার্ভার রেসপন্স ভুল")
                val history = PostHistory(
                    channelId = channelId,
                    channelName = channelName,
                    postText = text,
                    imageUri = imageUrl,
                    status = "ব্যর্থ",
                    reason = errMsg,
                    timestamp = System.currentTimeMillis()
                )
                dao.insertPostHistory(history)
                Pair(false, "তথ্য পোস্ট হতে ব্যর্থ হয়েছে: $errMsg")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Telegram post failed", e)
            val history = PostHistory(
                channelId = channelId,
                channelName = channelName,
                postText = text,
                imageUri = imageUrl,
                status = "ব্যর্থ",
                reason = e.localizedMessage,
                timestamp = System.currentTimeMillis()
            )
            dao.insertPostHistory(history)
            Pair(false, "নেটওয়ার্ক সংক্রান্ত ভুল: ${e.localizedMessage}")
        }
    }

    // --- ROOM DATABASE OPERATIONS ---

    suspend fun saveChannel(channel: TelegramChannel) {
        dao.insertChannel(channel)
    }

    suspend fun deleteChannel(channel: TelegramChannel) {
        dao.deleteChannel(channel)
    }

    suspend fun clearPostHistory() {
        dao.clearPostHistory()
    }

    suspend fun updateProfile(profile: UserProfile) {
        dao.updateProfile(profile)
    }

    suspend fun incrementStat(type: String) {
        val currentProfile = dao.getUserProfileDirect() ?: UserProfile()
        val updatedProfile = when (type) {
            "image" -> currentProfile.copy(imageGenerations = currentProfile.imageGenerations + 1)
            "translation" -> currentProfile.copy(translations = currentProfile.translations + 1)
            "social" -> currentProfile.copy(socialPosts = currentProfile.socialPosts + 1)
            "song" -> currentProfile.copy(songGenerations = currentProfile.songGenerations + 1)
            else -> currentProfile
        }
        dao.updateProfile(updatedProfile)
    }
}
