package me.antonio.noack.webdroid

import com.google.android.gms.safetynet.SafetyNet
import me.antonio.noack.elementalcommunity.AllManager
import java.lang.Exception
import kotlin.concurrent.thread

object Captcha {

    fun get(all: AllManager, onSuccess: (String) -> Unit, onError: (Exception) -> Unit){

        SafetyNet.getClient(all)
            .verifyWithRecaptcha("6LevynQUAAAAAHAWQSRqe27TgwsrLJci_GK_tApw")
            .addOnSuccessListener { response ->
                val responseToken = response.tokenResult
                if(responseToken?.isNotEmpty() == true){
                    thread {
                        onSuccess(responseToken)
                    }
                }
            }
            .addOnFailureListener {
                onError(RuntimeException("Captcha-API: ${it.message}"))
            }

    }

}