package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.model.WorkoutEntity

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM daily_workouts WHERE date = :date LIMIT 1")
    suspend fun getWorkoutByDate(date: String): WorkoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Query("UPDATE daily_workouts SET isCompleted = :isCompleted WHERE date = :date")
    suspend fun updateCompletionStatus(date: String, isCompleted: Boolean)

    @Query("SELECT date FROM daily_workouts WHERE date >= :startDate AND date <= :endDate AND isCompleted = 1")
    suspend fun getCompletedDatesInRange(startDate: String, endDate: String): List<String>

    @Query("SELECT * FROM daily_workouts WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getWorkoutsInRange(startDate: String, endDate: String): List<WorkoutEntity>
}