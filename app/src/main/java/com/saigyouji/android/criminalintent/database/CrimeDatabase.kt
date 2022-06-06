package com.saigyouji.android.criminalintent.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.saigyouji.android.criminalintent.Crime


@Database(entities = [Crime::class], version = 3)
@TypeConverters(CrimeTypeConverter::class)
abstract class CrimeDatabase: RoomDatabase(){
    abstract fun crimeDao(): CrimeDao
}
val migration_2_3 = object: Migration(2, 3){
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE crime ADD COLUMN suspect TEXT NOT NULL DEFAULT ''"
        )
    }
}