package me.antonio.noack.webdroid

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread

object HTTP {

    fun request(url: String, onSuccess: (String) -> Unit, onError: (IOException) -> Unit, https: Boolean) =
        request(url, "GET", onSuccess, onError, https)

    fun request(url: String, onSuccess: (String) -> Unit, onError: (IOException) -> Unit) =
        request(url, "GET", onSuccess, onError, true)

    fun request(url: String, type: String, onSuccess: (String) -> Unit, onError: (IOException) -> Unit) =
        request(url, type, onSuccess, onError, true)

    fun request(url: String, type: String, onSuccess: (String) -> Unit, onError: (IOException) -> Unit, https: Boolean) {
        thread {
            try {
                val con = URL(url).openConnection() as HttpURLConnection
                if(https){
                    con as HttpsURLConnection
                }
                con.requestMethod = type
                onSuccess(String(con.inputStream.buffered().readBytes()))
            } catch (e: IOException){
                onError(e)
            }
        }
    }

}