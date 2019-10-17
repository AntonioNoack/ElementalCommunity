package me.antonio.noack.elementalcommunity.api

import com.android.billingclient.api.*
import me.antonio.noack.elementalcommunity.AllManager

object BillingServices {

    const val OK = BillingClient.BillingResponseCode.OK
    lateinit var client: BillingClient
    var hasClient = false
    var isWorking = false

    fun init(all: AllManager){

        if(!hasClient){

            client = BillingClient.newBuilder(all).setListener { billingResult: BillingResult?, purchases: MutableList<Purchase>? ->
                // PurchasesUpdatedListener
                if(billingResult != null && purchases != null){
                    for(purchase in purchases){

                    }
                }
            }.build()

            hasClient = true

        }

        client.startConnection(object: BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == OK) {
                    // The BillingClient is ready. You can query purchases here.
                    val skuList = ArrayList<String>()
                    skuList.add("premium_upgrade")
                    skuList.add("gas")
                    val params = SkuDetailsParams.newBuilder()
                    params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
                    client.querySkuDetailsAsync(params.build()){ billingResult, skuDetailsList ->
                        // Process the result.
                    }
                    isWorking = true
                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                isWorking = false
            }
        })

    }

    fun buildProductList(all: AllManager){

        synchronized(this){

            if(!isWorking) init(all)




        }

    }

}