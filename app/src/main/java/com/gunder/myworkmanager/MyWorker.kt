package com.gunder.myworkmanager


import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.SyncHttpClient
import cz.msebera.android.httpclient.Header
import org.json.JSONObject
import java.lang.Exception
import java.text.DecimalFormat

class MyWorker(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {
    companion object {
        private val TAG = MyWorker::class.java.simpleName
        const val APP_ID = "4929fb383798ff55af57ce569fb41e3f"
        const val EXTRA_CITY = "city"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "channel_01"
        const val CHANNEL_NAME = "gunder channel"
    }

    private var resultStatus: Result? = null
    override fun doWork(): Result {
        val dataCity = inputData.getString(EXTRA_CITY)
        return getCurrnentWheater(dataCity)
    }

    private fun getCurrnentWheater(city: String?): ListenableWorker.Result {
        Log.d(TAG, "getCurrentWeather: Mulai...")
        Looper.prepare()
        val client = SyncHttpClient()
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$APP_ID"
        Log.d(TAG, "getCurrentWeather: $url")
        client.post(url, object : AsyncHttpResponseHandler() {
            override fun onSuccess(
                statusCode: Int,
                headers: Array<out Header>?,
                responseBody: ByteArray?
            ) {
                val result = String(responseBody!!)
                Log.d(TAG, result)
                try {
                    val responseObject = JSONObject(result)
                    val currentWeather: String =
                        responseObject.getJSONArray("weather").getJSONObject(0).getString("main")
                    val desctiption: String =
                        responseObject.getJSONArray("weather").getJSONObject(0)
                            .getString("description")
                    val tempInKelvin = responseObject.getJSONObject("main").getDouble("temp")
                    val tempCelcius = tempInKelvin - 273
                    val temperature: String = DecimalFormat("##.##").format(tempCelcius)
                    val title = "Current Weather in $city"
                    val message = "$currentWeather, $desctiption with $temperature celcius"
                    showNotification(title, message)
                    Log.d(TAG, "onSuccess : Selesai...")
                    resultStatus = Result.success()
                } catch (e: Exception) {
                    showNotification("Get current weather not success", e.message!!)
                    Log.d(TAG, "onSuccess: Gagal...")
                    resultStatus = Result.failure()
                }
            }


            override fun onFailure(
                statusCode: Int,
                headers: Array<out Header>?,
                responseBody: ByteArray?,
                error: Throwable?
            ) {
                Log.d(TAG, "onFailure: Gagal...")
//                ketika proses gagal, maka jobFinished diset dengan parameter true. Yang artinya job perlu di reschedule
                showNotification("get current weather failed", error?.message!!)
                resultStatus = Result.failure()
            }

        })
        return resultStatus as Result
    }

    private fun showNotification(title: String, description: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification: NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notification.setChannelId(CHANNEL_ID)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(NOTIFICATION_ID, notification.build())

    }


}