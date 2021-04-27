package com.skgezhil.chatzz

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.alarm.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, RecognitionListener {

    @RequiresApi(Build.VERSION_CODES.O)
    var timing = LocalDateTime.now()
    private val client = OkHttpClient()
    private val permission = 100
    private lateinit var returnedText: TextView
    private lateinit var toggleButton: ToggleButton
    private lateinit var progressBar: ProgressBar
    private lateinit var speech: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var notificationManager: NotificationManager
    lateinit var builder: Notification.Builder
    private var logTag = "VoiceRecognitionActivity"
    private val channelId = "12345"
    private val description = "Notification"
    companion object {
        val PERMISSIONS_REQUEST_READ_CONTACTS = 100
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        title = "Assistant"
        returnedText = findViewById(R.id.qs_text)
        progressBar = findViewById(R.id.progressBar)
        toggleButton = findViewById(R.id.toggleButton)
//        var time_picker: TimePicker = findViewById(R.id.time_picker)
        time_picker.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        speech = SpeechRecognizer.createSpeechRecognizer(this)
        Log.i(logTag, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(this))
        speech.setRecognitionListener(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "US-en")
        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                qs_text.text = ""
                ans_text.text = ""
                progressBar.visibility = View.VISIBLE
                progressBar.isIndeterminate = true
                toggleButton.setText("")
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    permission
                )
            } else {
                progressBar.isIndeterminate = false
                toggleButton.setText("")
                progressBar.visibility = View.VISIBLE
                speech.stopListening()
            }
        }

        go_btn.setOnClickListener {
            qs_text.text = ""
            ans_text.text = ""
            answers(qs_edittext.text.toString())
            qs_text.text = qs_edittext.text
            notification("SKGEzhil", "Test Notification")

        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            permission -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager
                    .PERMISSION_GRANTED
            ) {
                speech.startListening(recognizerIntent)
            } else {
                Toast.makeText(
                    this@MainActivity, "Permission Denied!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        speech.destroy()
        Log.i(logTag, "destroy")
    }

    override fun onReadyForSpeech(params: Bundle?) {
    }

    override fun onRmsChanged(rmsdB: Float) {
        progressBar.progress = rmsdB.toInt()
    }

    override fun onBufferReceived(buffer: ByteArray?) {
    }

    override fun onPartialResults(partialResults: Bundle?) {
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
    }

    override fun onBeginningOfSpeech() {
        Log.i(logTag, "onBeginningOfSpeech")
        progressBar.isIndeterminate = false
        progressBar.max = 10
    }

    override fun onEndOfSpeech() {
        progressBar.isIndeterminate = true
        toggleButton.isChecked = false
    }

    override fun onError(error: Int) {
        val errorMessage: String = getErrorText(error)
        Log.d(logTag, "FAILED $errorMessage")
        returnedText.text = errorMessage
        toggleButton.isChecked = false
    }

    private fun getErrorText(error: Int): String {
        var message = ""
        message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Didn't understand, please try again."
        }
        return message
    }

    override fun onResults(results: Bundle?) {
        Log.i(logTag, "onResults")
        val matches = results!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        var text = ""
        if (matches != null) {
            for (result in matches) text = """
          $result
          """.trimIndent()
        }
        returnedText.text = text
        qs_text.text = text
        qs_text.visibility = View.VISIBLE
        answers(qs_text.text as String)
    }

    override fun onInit(status: Int) {

    }

    fun speak(text: String) {
        TextToSpeech(this, this)!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    override fun onDestroy() {
        // Shutdown TTS
        TextToSpeech(this, this)!!.stop()
        TextToSpeech(this, this)!!.shutdown()
        super.onDestroy()
    }

    private fun api_call(url: String): String {
        var answer: String = ""
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                println(response.body()?.string())
                answer = response.body().toString()
            }
        })

        return answer
    }

    private fun answers(question: String) {
        if ("hallo" in question) {
            ans_text.visibility = View.VISIBLE
            ans_text.text = "Hello, how are u!"
        } else if ("time" in question) {
            ans_text.visibility = View.VISIBLE
            var time_now: String =
                timing.toString()[11] + "" + timing.toString()[12] + timing.toString()[13] + timing.toString()[14] + timing.toString()[15]
            var hour_min: List<String> = time_now.split(":")
            var hours = hour_min[0].toInt()
            var minutes = hour_min[1].toInt()
            var ampm = "am"
            if (hours > 12) {
                hours = (hours - 12)
                ampm = "pm"
            }
            time_now = "${hours}:${minutes}" + ampm
            ans_text.text = "The time is now $time_now"
        } else if ("weather" in question) {
            weatherTask().execute()
        } else if ("news" in question) {
            News().execute()
        } else if ("google" in question) {
            google_search("google").execute()
        } else if ("alarm" in question) {
            alarm_command(question)
        }
        else if ("whatsapp" in question){
            whatsapp_specific(question, "917904513092")
        }


    }

    @SuppressLint("StaticFieldLeak")
    inner class weatherTask() : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            /* Showing the ProgressBar, Making the main design GONE */
        }

        override fun doInBackground(vararg params: String?): String? {
            var response: String?
            var weather_api = "8ef61edcf1c576d65d836254e11ea420"
            try {
                response =
                    URL("https://api.openweathermap.org/data/2.5/weather?q=chennai&units=metric&appid=$weather_api").readText(
                        Charsets.UTF_8
                    )
            } catch (e: Exception) {
                response = null
                println("response is null")
            }
            return response
        }


        @SuppressLint("SetTextI18n")
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            println("result : $result")
            var json_obj = JSONObject(result)
            var main = json_obj.getJSONObject("main")
            var temp = main.getString("temp")
            var humidity = main.getString("humidity")
            var min_temp = main.getString("temp_min")
            var max_temp = main.getString("temp_max")
            var feels_like = main.getString("feels_like")
            println("temperature = ${temp.toString()}")
            findViewById<TextView>(R.id.ans_text).text =
                "Temperature : $temp°C \n " +
                        "Humidity : $humidity% \n " +
                        "Minimum : $min_temp°C \n " +
                        "Maximum : $max_temp°C \n " +
                        "Feels Like $feels_like°C"
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class News() : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            /* Showing the ProgressBar, Making the main design GONE */
        }

        override fun doInBackground(vararg params: String?): String? {
            var response: String?
            var news_api = "7dfb535cf0c44d3c99e73ec00a2d8152"
            try {
                response =
                    URL("https://newsapi.org/v2/top-headlines?sources=google-news-in&apiKey=7dfb535cf0c44d3c99e73ec00a2d8152").readText(
                        Charsets.UTF_8
                    )
            } catch (e: Exception) {
                println("error here : $e")
                response = null
                println("response is null")
            }
            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            var json_obj = JSONObject(result)
            var articles: JSONObject = json_obj.getJSONObject("articles")
            for (i in arrayListOf(articles)) {
                println(i)
            }

        }

    }

    private fun cricket_score() {
        thread {
            // network call, so run it in the background
            val doc =
                Jsoup.connect("https://www.cricbuzz.com/cricket-match/live-scores")
                    .get()
            var div = doc.getElementsByTag("div")
            var cricket = doc.getElementById("page-wrapper")
            val imageElements = doc.getElementsByClass("img-responsive")
            val textElements = doc.getElementsByTag("h1")

            val imageUrl = imageElements[0].absUrl("src")

            // can't access UI elements from the background thread
            this.runOnUiThread {
                var text = textElements[0].text()

            }
        }
    }


    @SuppressLint("StaticFieldLeak")
    inner class google_search(query: String) : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            /* Showing the ProgressBar, Making the main design GONE */
        }

        var question = query
        override fun doInBackground(vararg params: String?): String? {
            var custom_search_api = "AIzaSyB2CHrIXCerEertGu1bLUxmZgVBq-gqCH4"
            var custom_search_id = "015295736575808137604:x3cf3qjl5fs"
            println("question is : $question")
            var url =
                "https://www.googleapis.com/customsearch/v1?key=$custom_search_api&cx=$custom_search_id&q=$question&start=1"
            var response: String?
            try {
                response =
                    URL(url).readText(
                        Charsets.UTF_8
                    )
            } catch (e: Exception) {
                println("error here : $e")
                response = null
                println("response is null")
            }
            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            var json_obj = JSONObject(result)
            var search_items: JSONArray = json_obj.getJSONArray("items")
            for (i in arrayListOf(search_items)) {
                println(i)
                var item = i.getJSONObject(0)
                var snippet = item.getString("snippet")
                println(snippet)
                break
            }

        }

    }

    fun alarm_command(command: String) {
        if ("at" in command) {
            var command_splitter: List<String> = command.split(" at ")
            var time = command_splitter[1]
            var ampm: String = ""
            time = time.replace(" ", "")
            if ("am" in time) {
                time = time.replace("am", "")
                ampm = "am"
            } else if ("pm" in time) {
                time = time.replace("pm", "")
                ampm = "pm"
            }
            var real_time = time.split(":")
            var hours = real_time[0].toInt()
            var minutes = real_time[1].toInt()
            if (hours != 12) {
                if (ampm == "pm") {
                    hours = hours + 12
                }
            }

            val alarm_intent = Intent(AlarmClock.ACTION_SET_ALARM)
            alarm_intent.putExtra(AlarmClock.EXTRA_MESSAGE, "New Alarm")
            alarm_intent.putExtra(AlarmClock.EXTRA_HOUR, hours)
            alarm_intent.putExtra(AlarmClock.EXTRA_MINUTES, minutes)
            startActivity(alarm_intent)

        }
        else{
            time_picker.visibility = View.VISIBLE
            alarm_set.setOnClickListener {
                var hours = timePicker.currentHour
                var minutes = timePicker.currentMinute
                ans_text.text = "$hours:$minutes"
                val alarm_intent = Intent(AlarmClock.ACTION_SET_ALARM)
                alarm_intent.putExtra(AlarmClock.EXTRA_MESSAGE, "New Alarm")
                alarm_intent.putExtra(AlarmClock.EXTRA_HOUR, hours)
                alarm_intent.putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                startActivity(alarm_intent)
            }
        }
    }

    fun alarm_ringer(hours: Int, minutes: Int) {
        var time: Long
        var pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        val calendar: Calendar = Calendar.getInstance()
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, minutes);
        time = (calendar.timeInMillis - (calendar.timeInMillis % 60000));
        if (System.currentTimeMillis() > time) {
            alarmManager.setRepeating(
                AlarmManager.RTC,
                time,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } else {
            alarmManager.cancel(pendingIntent);
            Toast.makeText(this, "ALARM OFF", Toast.LENGTH_SHORT).show();
        }
    }

    fun whatsapp(message: String) {
        // Creating intent with action send
        val intent = Intent(Intent.ACTION_SEND)

        // Setting Intent type
        intent.type = "text/plain"

        // Setting whatsapp package name
        intent.setPackage("com.whatsapp")

        // Give your message here
        intent.putExtra(Intent.EXTRA_TEXT, message)

        // Checking whether whatsapp is installed or not
        if (intent.resolveActivity(packageManager) == null) {
            Toast.makeText(
                this,
                "Please install whatsapp first.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Starting Whatsapp
        startActivity(intent)
    }

    fun whatsapp_specific(message: String, phone: String){
        val packageManager: PackageManager = packageManager
        val i = Intent(Intent.ACTION_VIEW)

        try {
            val url = "https://api.whatsapp.com/send?phone=$phone&text=" + URLEncoder.encode(
                message,
                "UTF-8"
            )
            i.setPackage("com.whatsapp")
            i.data = Uri.parse(url)
            if (i.resolveActivity(packageManager) != null) {
                startActivity(i)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun notification(title: String, message: String) {
        var notificationChannel: NotificationChannel
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(channelId, description, NotificationManager .IMPORTANCE_HIGH)
            notificationChannel.lightColor = Color.BLUE
            notificationManager.createNotificationChannel(notificationChannel)
            builder = Notification.Builder(this, channelId).setContentTitle(title).setContentText(message).setSmallIcon(R.drawable.ic_launcher_foreground).setLargeIcon(
                BitmapFactory.decodeResource(this.resources, R.drawable
                .ic_launcher_background)).setContentIntent(pendingIntent)
        }
        notificationManager.notify(12345, builder.build())
    }

    fun reminder_command(command: String){

        if("in" in command){
            var cal = Calendar.getInstance()
            var command_splitter = command.split(" in ")
            var time = command_splitter[1]
            time = time.replace(" ", "")
            if ("minutes" in time){
                time = time.replace("minutes", "")
                var minutes = time.toInt()
//                var min = currentDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            }
        }

    }

    fun calendar_create_event(event: String, hours: Int, minutes: Int){
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes)
        val intent = Intent(Intent.ACTION_EDIT)
        intent.type = "vnd.android.cursor.item/event"
        intent.putExtra("beginTime", cal.timeInMillis)
        intent.putExtra("allDay", true)
        intent.putExtra("rrule", "FREQ=YEARLY")
        intent.putExtra("endTime", cal.timeInMillis + 60 * 60 * 1000)
        intent.putExtra("title", event)
        startActivity(intent)

    }

}

