package com.example.historyalert

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlin.random.Random

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "history.db"
        private const val DATABASE_VERSION = 1
    }

    private val databasePath: String = context.getDatabasePath(DATABASE_NAME).path

    override fun onCreate(db: SQLiteDatabase) {
        // No need to create tables as we're using a pre-built database
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades here if necessary
    }

    fun getRandomEntry(date: String, entryType: String): FactEntry? {
        // Open the database directly from the database path
        Log.d("DatabaseHelper", "Opening database at path: $databasePath")
        val db = SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READONLY)
        db.use {
            Log.d("DatabaseHelper", date)
            val cursor: Cursor = db.rawQuery("SELECT year, description, links FROM events WHERE date LIKE ? AND type LIKE ?", arrayOf(date, entryType))

            val entryCount = cursor.count
            Log.d("DatabaseHelper", "Number of rows returned: $entryCount")
            return if (entryCount > 0) {
                val randomPosition = Random.nextInt(entryCount)
                cursor.moveToPosition(randomPosition)
                val year: String = cursor.getString(0)
                val description: String = cursor.getString(1)
                val links: String = cursor.getString(2)
                val linksArray: List<String> = links.split(" ").map { it.trim() }
                Log.d("DatabaseHelper", "Return: $year - $description")
                FactEntry(date = date, year = year, type = entryType, fact = description, links = linksArray)
            } else {
                Log.d("DatabaseHelper", "No data found ($date, $entryType).")
                null
            }.also { cursor.close() }
        }
    }
}
