package com.example.historyalert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.*

class NotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val entryType = if ((1..10).random() == 1) "Birthday" else "Event"
        val dbHelper = DatabaseHelper(applicationContext)
        val currentDate = SimpleDateFormat("MMMM dd", Locale.ENGLISH).format(Date())
        val factEntry = dbHelper.getRandomEntry(currentDate, entryType)

        val yearsAgoText = factEntry?.let {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            it.year.toIntOrNull()?.let { eventYear ->
                val yearsAgo = currentYear - eventYear
                "\n\n${yearsAgo} years ago"
            } ?: ""
        } ?: ""
        val factText = factEntry?.let {
            val birthdayInfo: String = if (entryType == "Birthday") {"Birthday of "} else {""}
            val result = "${it.date}, ${it.year} - $birthdayInfo${it.fact}"
            Log.d("NotificationWorker", "HistoryAlert:\n$result")
            result
        }
        val factTextExtended = factText?.let { "$factText$yearsAgoText" }
        val linkText = factEntry?.let {
            val result = it.links.joinToString("\n\n")
            Log.d("NotificationWorker", "Links:\n$result")
            result
        }

        // Save the fact and links to SharedPreferences
        val sharedPreferences = applicationContext.getSharedPreferences("HistoryFacts", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("latestFact", factTextExtended)
            putString("latestLinks", linkText)
            apply()
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel("historyFacts", "History Facts", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // Create an intent that opens the MainActivity when clicked
        val intent = Intent(applicationContext, MainActivity::class.java)

        // Create a task stack builder to ensure proper back stack behavior when navigating to MainActivity
        val stackBuilder = TaskStackBuilder.create(applicationContext)
        stackBuilder.addNextIntentWithParentStack(intent)
        val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(applicationContext, "historyFacts")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setContentTitle("Today in History")
            .setContentText(factText)
            .setAutoCancel(true)
            .setContentIntent(resultPendingIntent)
            .build()

        notificationManager.notify(1, notification)
        return Result.success()
    }
}
