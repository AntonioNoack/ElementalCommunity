package me.antonio.noack.webdroid

import me.antonio.noack.elementalcommunity.AllManager
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object HTTP {

    var hasWarned = false

    fun request(url: String, onSuccess: (String) -> Unit, onError: (IOException) -> Unit) =
        request(url, "GET", onSuccess, onError)

    // fun request(url: String, onSuccess: (String) -> Unit, onError: (IOException) -> Unit) =
    //    request(url, "GET", onSuccess, onError)

    // fun request(url: String, type: String, onSuccess: (String) -> Unit, onError: (IOException) -> Unit) =
    //    request(url, type, onSuccess, onError)

    // Cleartext HTTP traffic to api.phychi.com not permitted
    // lol, first try https, then http
    fun requestInternal(url: String, type: String, largeArgs: String?, onSuccess: (String) -> Unit, onError: (IOException) -> Unit){
        if(url.startsWith("https://")) return requestInternal(url.substring(8), type, largeArgs, onSuccess, onError)
        if(url.startsWith("http://" )) return requestInternal(url.substring(7), type, largeArgs, onSuccess, onError)
        thread {
            try {
                val con = URL("https://$url").openConnection() as HttpURLConnection
                // idc, working app > privacy
                // con as? HttpsURLConnection ?: throw IOException("Connection wasn't https!")
                con.requestMethod = type
                con.useCaches = false
                if(largeArgs != null){
                    con.doOutput = true
                    val out = con.outputStream.buffered()
                    out.write(largeArgs.toByteArray())
                    out.flush()
                }
                onSuccess(String(con.inputStream.buffered().readBytes()))
            } catch (e: IOException){
                synchronized(this){
                    if(!hasWarned){
                        hasWarned = true
                        AllManager.toast("HTTPS is somehow not available :/.\n" +
                                "The app will probably work fine anyways.", true)
                    }
                }
                try {
                    val con = URL("http://$url").openConnection() as HttpURLConnection
                    con.requestMethod = type
                    con.useCaches = false
                    if(largeArgs != null){
                        con.doOutput = true
                        val out = con.outputStream.buffered()
                        out.write(largeArgs.toByteArray())
                        out.flush()
                    }
                    onSuccess(String(con.inputStream.buffered().readBytes()))
                } catch (e: IOException){
                    onError(e)
                }
            }
        }
    }

    fun request(url: String, type: String, onSuccess: (String) -> Unit, onError: (IOException) -> Unit) {
        requestInternal(url, type, null, onSuccess, onError)
    }

    fun requestLarge(url: String, largeArgs: String, onSuccess: (String) -> Unit, onError: (IOException) -> Unit, https: Boolean) {
        requestInternal(url, "POST", null, onSuccess, onError)
    }

}