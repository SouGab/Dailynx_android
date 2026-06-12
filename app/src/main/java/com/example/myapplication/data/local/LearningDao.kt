package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.model.LearningEntity

@Dao
interface LearningDao {
    @Query("SELECT * FROM daily_learning WHERE date = :date LIMIT 1")
    suspend fun getLearningByDate(date: String): LearningEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearning(learning: LearningEntity)

    @Query("SELECT * FROM daily_learning WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getLearningsInRange(startDate: String, endDate: String): List<LearningEntity>

    @Query("SELECT * FROM daily_learning WHERE date BETWEEN :startDate AND :endDate AND isLiked = 1")
    suspend fun getLearningsLikedInRange(startDate: String, endDate: String): List<LearningEntity>

    @Query("UPDATE daily_learning SET isLiked = :isLiked WHERE date = :date")
    suspend fun updateLikedStatus(date: String, isLiked: Boolean)
}