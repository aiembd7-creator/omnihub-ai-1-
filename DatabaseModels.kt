package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "telegram_channels")
data class TelegramChannel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val channelName: String,
    val botToken: String,
    val channelId: String,
    val subscriberCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "post_history")
data class PostHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val channelId: String,
    val channelName: String,
    val postText: String,
    val imageUri: String? = null,
    val status: String,
    val reason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "ইউজার নাম",
    val bio: String = "আমি একজন এআই কনটেন্ট ক্রিয়েটর এবং সোশ্যাল মিডিয়া ইনফ্লুয়েন্সার।",
    val avatarUri: String? = null,
    val telegramGenerations: Int = 0,
    val imageGenerations: Int = 0,
    val translations: Int = 0,
    val socialPosts: Int = 0,
    val songGenerations: Int = 0
)

@Dao
interface AppDao {
    // Channels
    @Query("SELECT * FROM telegram_channels ORDER BY createdAt DESC")
    fun getAllChannels(): Flow<List<TelegramChannel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: TelegramChannel)

    @Delete
    suspend fun deleteChannel(channel: TelegramChannel)

    @Query("SELECT COUNT(*) FROM telegram_channels")
    fun getChannelCount(): Flow<Int>

    // Post History
    @Query("SELECT * FROM post_history ORDER BY timestamp DESC")
    fun getAllPostHistory(): Flow<List<PostHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPostHistory(history: PostHistory)

    @Query("DELETE FROM post_history")
    suspend fun clearPostHistory()

    // Profile
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProfile(profile: UserProfile)
}
