package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.DatabaseConfiguration
import androidx.room.RoomDatabase
import com.example.myapplication.data.model.WorkoutEntity
import com.example.myapplication.data.model.EquipmentEntity
import com.example.myapplication.data.model.LearningEntity
import com.example.myapplication.data.model.PoemEntity

@Database(entities = [WorkoutEntity::class, EquipmentEntity::class, LearningEntity::class, PoemEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun learningDao(): LearningDao
    abstract fun poemDao(): PoemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}