package com.skgezhil.chatzz

import okhttp3.*
import org.json.JSONObject
import java.io.IOException

fun weather_report() {
    val client = OkHttpClient()
    var weather_api = "8ef61edcf1c576d65d836254e11ea420"
    var base_url = "https://api.openweathermap.org/data/2.5/weather?"
    var complete_url = base_url + "appid=" + weather_api + "&q=" + "chennai"
    val request = Request.Builder()
        .url(complete_url)
        .build()
    var report = "nothing"
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {}
        override fun onResponse(call: Call, response: Response) {
            var resultt: String = response.body()!!.string()
            var json_obj = JSONObject(resultt)
            var main = json_obj.getJSONObject("main")
            var temp = main.getString("temp")
            report = temp.toString()
            println("temperature = ${temp.toString()}")
        }
    })

}