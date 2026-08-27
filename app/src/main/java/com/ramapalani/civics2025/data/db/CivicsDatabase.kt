package com.ramapalani.civics2025.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SessionEntity::class, AttemptEntity::class, QuestionStatEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CivicsDatabase : RoomDatabase() {
    abstract fun dao(): CivicsDao

    companion object {
        fun create(context: Context): CivicsDatabase {
            return Room.databaseBuilder(context, CivicsDatabase::class.java, "civics.db").build()
        }
    }
}
