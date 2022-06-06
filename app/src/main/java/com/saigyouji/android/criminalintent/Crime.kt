package com.saigyouji.android.criminalintent

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.*

@Entity
data class Crime(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    var title: String = "",
    var date : Date = Date(),
    var isSolved : Boolean = false,
    var requiresPolicy: Boolean = false,
    var suspect: String = ""
){
    val photoFileName
    get() = "IMG_$id.jpg"
}
val MIGRATION_1_2 = object : Migration(1, 2){
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE Crime ADD COLUMN requiresPolicy INTEGER NOT NULL DEFAULT 0"
        )
    }
}