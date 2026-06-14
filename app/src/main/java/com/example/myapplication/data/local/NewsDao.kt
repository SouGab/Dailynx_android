package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.model.NewsEntity

@Dao
interface NewsDao {
    @Query("SELECT * FROM daily_news WHERE date = :date")
    suspend fun getNewsByDate(date: String): List<NewsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(news: List<NewsEntity>)

    @Query("DELETE FROM daily_news WHERE date = :date")
    suspend fun deleteNewsByDate(date: String)
}
