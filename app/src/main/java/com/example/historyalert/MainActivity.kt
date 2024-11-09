package com.example.historyalert

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.FileOutputStream
import java.io.IOException
import android.Manifest
import android.content.SharedPreferences
import android.widget.TextView
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkInfo

class MainActivity : AppCompatActivity() {
    private lateinit var factTextView: TextView
    private lateinit var linksTextView: TextView
    private lateinit var sharedPreferences: SharedPreferences

    // Define the listener to update TextViews when SharedPreferences change
    private val sharedPreferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "latestFact" || key == "latestLinks") {
            // Update TextViews with the latest fact and links from SharedPreferences
            factTextView.text = sharedPreferences.getString("latestFact", getString(R.string.no_data_error))
            linksTextView.text = sharedPreferences.getString("latestLinks", "")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load database
        copyDatabaseFromAssets(this)

        // Request permission if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE)
            } else {
                checkAndScheduleNotificationWorker()
            }
        } else {
            // If the device is below Android 13, permission is not required.
            checkAndScheduleNotificationWorker()
        }

        // Get SharedPreferences
        val sharedPreferences = getSharedPreferences("HistoryFacts", Context.MODE_PRIVATE)
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)

        // Get UI access
        val factTextView: TextView = findViewById(R.id.factTextView)
        val linksTextView: TextView = findViewById(R.id.linksTextView)

        factTextView.text = sharedPreferences.getString("latestFact", getString(R.string.no_data_error))
        linksTextView.text = sharedPreferences.getString("latestLinks", "")

        // Reference to the button
        val notificationButton: Button = findViewById(R.id.btn_show_notification)

        // Set click listener to trigger an instant notification
        notificationButton.setOnClickListener {
            scheduleNotificationWorker()
            // Retrieve and display latest fact and links
            factTextView.text = sharedPreferences.getString("latestFact", getString(R.string.no_data_error))
            linksTextView.text = sharedPreferences.getString("latestLinks", "")
        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, schedule the worker
            scheduleNotificationWorker()
        } else {
            Log.e("MainActivity", "Notification permission denied.")
        }
    }

    // Function to set up periodic notification
    private fun scheduleNotificationWorker() {
        // Set constraints if necessary (e.g., only when the device is charging or connected to Wi-Fi)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        // Create a periodic work request with a minimum of 15 minutes interval
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)  // Apply constraints if needed
            .build()

        // Enqueue the periodic work with a unique name
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PeriodicNotification",  // Unique work name
            ExistingPeriodicWorkPolicy.REPLACE,  // Replace existing work if needed
            workRequest
        )
    }

    // Function to check if the worker is running and schedule it if not
    private fun checkAndScheduleNotificationWorker() {
        val workManager = WorkManager.getInstance(this)
        workManager.getWorkInfosForUniqueWorkLiveData("PeriodicNotification")
            .observe(this) { workInfoList ->
                val isRunning =
                    workInfoList.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }

                // Start the worker only if it is not already running
                if (!isRunning) {
                    scheduleNotificationWorker()
                }
            }
    }

    // Function to trigger an instant notification
    private fun showNotificationNow() {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>().build()
        WorkManager.getInstance(this).enqueue(oneTimeWorkRequest)
    }


    private fun copyDatabaseFromAssets(context: Context) {
        val dbPath = context.getDatabasePath("history.db")
        if (!dbPath.exists()) {
            dbPath.parentFile?.mkdirs()

            try {
                context.assets.open("history.db").use { inputStream ->
                    FileOutputStream(dbPath).use { outputStream ->
                        inputStream.copyTo(outputStream)
                        Log.d("Database", "Database copied successfully.")
                    }
                }
            } catch (e: IOException) {
                Log.e("Database", "Failed to copy database", e)
            }
        } else {
            Log.d("Database", "Database already exists.")
        }
    }

    companion object {
        private const val REQUEST_CODE = 1001
    }

}
