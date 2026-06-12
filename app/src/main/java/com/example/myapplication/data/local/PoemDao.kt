package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.model.PoemEntity

@Dao
interface PoemDao {
    @Query("SELECT * FROM daily_poems WHERE date = :date LIMIT 1")
    suspend fun getPoemByDate(date: String): PoemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoem(poem: PoemEntity)

    @Query("DELETE FROM daily_poems WHERE date = :date")
    suspend fun deletePoemByDate(date: String)
}