package me.antonio.noack.webdroid

import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import kotlin.concurrent.thread

object Captcha {

    private const val CAPTCHA_API_URL = "https://api.phychi.com/rotarylockCaptcha/"
    private const val STORAGE_KEY = "captchaToken"

    // for server to differentiate ReCaptcha and RotaryLockCaptcha
    private const val KEY_PREFIX = "rlc:"

    private fun JSONArray.toDoubleList(): List<Double> {
        return (0 until length()).map { index ->
            getDouble(index)
        }
    }

    private fun JSONArray.toIntList(): List<Int> {
        return (0 until length()).map { index ->
            getInt(index)
        }
    }

    private fun extractChallenge(data: String): Challenge {
        val json = JSONObject(data)
        val angles = json.getJSONArray("angles")
            .toDoubleList()
        val slots = json.getJSONArray("slots")
            .toIntList()
        val num = json.getInt("num")
        val da = json.getDouble("da")
        val uuid = json.getString("uuid")
        if (angles.isEmpty() || slots.isEmpty() || num <= 1 || !(da > 0f) || uuid.isEmpty()) {
            throw IOException("Invalid challenge")
        }
        return Challenge(ArrayList(angles), slots, num, da, uuid)
    }

    private fun extractToken(data: String): String {
        val json = JSONObject(data)
        if (!json.getBoolean("success")) throw IOException("Try again")
        return json.getString("token")
    }

    // todo disable OK button when loading

    fun get(all: AllManager, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        val dialog = AlertDialog.Builder(all)
            .setView(R.layout.captcha)
            .show()

        val captchaView = dialog.findViewById<CaptchaView>(R.id.captchaView)!!
        val okButton = dialog.findViewById<Button>(R.id.ok)!!
        okButton.setOnClickListener {
            okButton.isEnabled = false
            thread(name = "Pretend busy") {
                // always wait a little to pretend we're working
                Thread.sleep(500)
                validateSolution(all, captchaView, dialog, onSuccess, onError, okButton)
            }
        }
        okButton.isEnabled = false

        requestChallenge(all, captchaView, dialog, onSuccess, onError, okButton)
    }

    private fun requestChallenge(
        all: AllManager, captchaView: CaptchaView, dialog: AlertDialog,
        onSuccess: (String) -> Unit, onError: (Exception) -> Unit,
        okButton: Button
    ) {
        // request challenge from api.phychi.com
        val lastToken = all.pref.getString(STORAGE_KEY, "")
        HTTP.request("$CAPTCHA_API_URL?n=${URLEncoder.encode(lastToken, "UTF-8")}", { data ->
            try {
                val token = extractToken(data)
                handleSuccess(all, dialog, token, onSuccess)
            } catch (_: Exception) {
                try {
                    val challenge = extractChallenge(data)
                    // apply challenge to dialog
                    all.runOnUiThread { // prevent ConcurrencyException by running on UI thread
                        captchaView.markReady(challenge)
                        okButton.isEnabled = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    dialog.cancel()
                    onError(e)
                }
            }
        }, { e ->
            e.printStackTrace()
            dialog.cancel()
            onError(e)
        })
    }

    private fun validateSolution(
        all: AllManager, captchaView: CaptchaView, dialog: AlertDialog,
        onSuccess: (String) -> Unit, onError: (Exception) -> Unit, okButton: Button
    ) {
        if (!captchaView.isSolved) {
            return requestChallenge(all, captchaView, dialog, onSuccess, onError, okButton)
        }

        val challenge = captchaView.challenge
        val json = JSONObject()
        json.put("uuid", challenge.uuid)
        val sequence = JSONArray()
        for (press in captchaView.sequence) {
            val jsonI = JSONObject()
            jsonI.put("sign", press.sign)
            jsonI.put("dt", press.dt)
            sequence.put(jsonI)
        }
        json.put("sequence", sequence)
        HTTP.request("$CAPTCHA_API_URL?c=${URLEncoder.encode(json.toString(), "UTF-8")}", { data ->
            try {
                handleSuccess(all, dialog, extractToken(data), onSuccess)
            } catch (e: Exception) {
                requestChallenge(all, captchaView, dialog, onSuccess, onError, okButton)
            }
        }, { e ->
            e.printStackTrace()
            requestChallenge(all, captchaView, dialog, onSuccess, onError, okButton)
        })
    }

    private fun handleSuccess(
        all: AllManager, dialog: AlertDialog, result: String,
        onSuccess: (String) -> Unit
    ) {
        all.runOnUiThread {
            dialog.cancel()
            all.pref.edit { putString(STORAGE_KEY, result) }
            onSuccess("$KEY_PREFIX$result")
        }
    }
}