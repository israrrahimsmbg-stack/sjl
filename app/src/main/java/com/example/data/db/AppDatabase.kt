package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Ball
import com.example.data.model.Innings
import com.example.data.model.Match
import com.example.data.model.Player
import com.example.data.model.Team

@Database(
    entities = [Team::class, Player::class, Match::class, Innings::class, Ball::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun teamDao(): TeamDao
    abstract fun playerDao(): PlayerDao
    abstract fun matchDao(): MatchDao
    abstract fun inningsDao(): InningsDao
    abstract fun ballDao(): BallDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sjl_cricket_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
