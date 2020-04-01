package me.antonio.noack.elementalcommunity.api

import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.Element
import me.antonio.noack.elementalcommunity.GroupsEtc
import me.antonio.noack.elementalcommunity.api.web.Candidate
import me.antonio.noack.elementalcommunity.api.web.News
import me.antonio.noack.elementalcommunity.cache.CombinationCache
import me.antonio.noack.webdroid.Captcha
import me.antonio.noack.webdroid.HTTP
import java.lang.Exception
import java.net.URLEncoder

open class WebService(private val serverURL: String): ServerService {

    val ms = 1000000L
    val deadlyReactionTime = 200 * ms
    // a cache is not only built when the connection is slow, but also when it's fast -> for using it in bad times
    val goodReactionTime = 60 * ms
    val reactionResetCounter = 30

    fun hasGoodConnection() = lastReactionTime < goodReactionTime
    fun hasSlowConnection() = lastReactionTime > deadlyReactionTime

    /**
     * the id of the server portion,
     * for groups of people, which want their own server
     * */
    var serverInstance = 0
    var serverName = "Default"

    /**
     * for security against crashes of old versions
     * webVersionName is the GET parameter
     * */
    private val webVersion = 1
    private val webVersionName = "v"

    fun isExternalSource() = serverName.startsWith("http://") || serverName.startsWith("https://")
    fun getURL() = if(isExternalSource()) serverName else serverURL

    fun tryCaptcha(all: AllManager, args: String, onSuccess: (String) -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    }){

        val url = "$serverURL?$args" +
                "&$webVersionName=$webVersion" +
                "&sid=$serverInstance"

        HTTP.request(url, { text ->
            if(text.isBlank() || text.startsWith("#reauth", true) || text.startsWith("#auth", true)){

                Captcha.get(all, { token ->
                    HTTP.request("$url&t=${URLEncoder.encode(token, "UTF-8")}", onSuccess, onError)
                }, onError)

            } else onSuccess(text)
        }, onError)

    }

    fun tryCaptchaLarge(all: AllManager, largeArgs: String, args: String, onSuccess: (String) -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    }){

        // nah, we always ask directly, so we don't waste upload speed?, or do we? idk...

        val url = "$serverURL?$args" +
                "&$webVersionName=$webVersion" +
                "&sid=$serverInstance"

        if(largeArgs.length < 100000){

            HTTP.requestLarge(url, largeArgs, { text ->
                if(text.isBlank() || text.startsWith("#reauth", true) || text.startsWith("#auth", true)){

                    Captcha.get(all, { token ->
                        HTTP.requestLarge("$url&t=${URLEncoder.encode(token, "UTF-8")}", largeArgs, onSuccess, onError, true)
                    }, onError)

                } else onSuccess(text)
            }, onError, true)

        } else {

            Captcha.get(all, { token ->
                HTTP.requestLarge("$url&t=${URLEncoder.encode(token, "UTF-8")}", largeArgs, onSuccess, onError, true)
            }, onError)

        }

    }

    var lastReactionTime = 0L
    var lastReactionCtr = 0

    override fun askRecipe(a: Element, b: Element, all: AllManager, onSuccess: (Element?) -> Unit, onError: (Exception) -> Unit){

        println("asking for $a + $b = ?")

        val url = "${getURL()}?a=${a.uuid}&b=${b.uuid}" +
                "&$webVersionName=$webVersion" +
                "&sid=$serverInstance"

        if(hasSlowConnection() && lastReactionCtr > 0){
            lastReactionCtr--
            CombinationCache.askRegularly(all, a, b, onSuccess)
            println("[CCache] used cached answer")
            return
        }

        val reactionStart = System.nanoTime()

        HTTP.request(url, { text ->

            // register ourselves for cache updates instead of real time ones, if the connection is slow
            val reactionEnd = System.nanoTime()
            lastReactionTime = reactionEnd - reactionStart
            lastReactionCtr = reactionResetCounter

            println("[CCache] connection speed: ${lastReactionTime/ms}ms")

            if(hasGoodConnection()){
                CombinationCache.updateInWealth(a, b)
            }

            val data = text.split(';')
            if(data.size > 1){
                val id = data[0].toInt()
                val group = data[1].toInt()
                val name = data[2]
                val craftingCounter = -1
                val element = Element.get(
                    name,
                    id,
                    group,
                    craftingCounter
                )
                AllManager.addRecipe(a, b, element, all)
                onSuccess(element)
            } else {
                println("found no result, data = $data by $text")
                onSuccess(null)
            }

        }, {
            // a network exception -> offline?
            // send a warning? once only?
            lastReactionTime = 30000L * ms
            lastReactionCtr = reactionResetCounter
            CombinationCache.askInEmergency(all, a, b, onSuccess)
            // onError(it)
            it.printStackTrace()
        })

    }

    override fun askNews(count: Int, onSuccess: (ArrayList<News>) -> Unit, onError: (Exception) -> Unit){

        HTTP.request("${getURL()}?n=${count * 3}" +
                "&$webVersionName=$webVersion" +
                "&sid=$serverInstance",

            { text ->
            val raw = text.split(";;")
            val list = ArrayList<News>()
            for(news in raw){
                val data = news.split(';')
                if(data.size > 9){
                    val dt = data[0].toIntOrNull() ?: continue
                    val aId = data[1].toIntOrNull() ?: continue
                    val aName = data[2]
                    val aGroup = data[3].toIntOrNull() ?: continue
                    val a = Element.get(aName, aId, aGroup, -1)
                    val bId = data[4].toIntOrNull() ?: continue
                    val bName = data[5]
                    val bGroup = data[6].toIntOrNull() ?: continue
                    val b = Element.get(bName, bId, bGroup, -1)
                    val result = data[7]
                    val resultGroup = data[8].toIntOrNull() ?: continue
                    val weight = data[9].toIntOrNull() ?: continue
                    list.add(
                        News(
                            dt,
                            a,
                            b,
                            result,
                            resultGroup,
                            weight
                        )
                    )
                }
            }
            onSuccess(list)
        }, onError)

    }


    override fun getCandidates(a: Element, b: Element, onSuccess: (ArrayList<Candidate>) -> Unit, onError: (Exception) -> Unit){

        HTTP.request("${getURL()}?o=1&a=${a.uuid}&b=${b.uuid}" +
                "&$webVersionName=$webVersion" +
                "&sid=$serverInstance",

            { text ->
            val data = text.split(';')
            var i = 0
            val candidates = ArrayList<Candidate>()
            while(i+2 < data.size){
                val uuid = data[i++].toLong()
                val name = data[i++]
                val group = data[i++].toInt()
                candidates.add(Candidate(uuid, name, group))
            }
            onSuccess(candidates)
        }, onError)

    }

    override fun suggestRecipe(all: AllManager, a: Element, b: Element, resultName: String, resultGroup: Int, onSuccess: (text: String) -> Unit, onError: (Exception) -> Unit){

        val edit = all.pref.edit()
        CombinationCache.invalidate(edit)
        edit.apply()

        if(isExternalSource()){
            HTTP.request("a=${a.uuid}&b=${b.uuid}" +
                    "&r=${URLEncoder.encode(resultName, "UTF-8")}" +
                    "&g=$resultGroup" +
                    "&u=${AllManager.customUUID}" +
                    "&$webVersionName=$webVersion" +
                    "&sid=$serverInstance", onSuccess, onError)
        } else {
            // a,b,r,g,u,t
            tryCaptcha(all, "a=${a.uuid}&b=${b.uuid}" +
                    "&r=${URLEncoder.encode(resultName, "UTF-8")}" +
                    "&g=$resultGroup" +
                    "&u=${AllManager.customUUID}" +
                    "&$webVersionName=$webVersion" +
                    "&sid=$serverInstance", onSuccess, onError)
        }


    }

    override fun likeRecipe(all: AllManager, uuid: Long, onSuccess: () -> Unit, onError: (Exception) -> Unit){

        val edit = all.pref.edit()
        CombinationCache.invalidate(edit)
        edit.apply()

        if(isExternalSource()){
            HTTP.request("${getURL()}?s=$uuid&r=1&u=${AllManager.customUUID}", { onSuccess() }, onError)
        } else {
            tryCaptcha(all, "s=$uuid&r=1&u=${AllManager.customUUID}", { onSuccess() }, onError)
        }

    }

    override fun dislikeRecipe(all: AllManager, uuid: Long, onSuccess: () -> Unit, onError: (Exception) -> Unit){

        val edit = all.pref.edit()
        CombinationCache.invalidate(edit)
        edit.apply()

        if(isExternalSource()){
            HTTP.request("${getURL()}?s=$uuid&r=-1&u=${AllManager.customUUID}", { onSuccess() }, onError)
        } else {
            tryCaptcha(all, "s=$uuid&r=-1&u=${AllManager.customUUID}", { onSuccess() }, onError)
        }
    }

    override fun askRecipes(name: String, onSuccess: (raw: String) -> Unit, onError: (Exception) -> Unit){

        HTTP.request("${getURL()}?qr=$name" +
                "&$webVersionName=$webVersion" +
                "&sid=$serverInstance", onSuccess, onError)

    }

    override fun askAllRecipesOfGroup(group: Int, onSuccess: (raw: String) -> Unit, onError: (Exception) -> Unit){

        // query group recipes
        HTTP.request("${getURL()}?qgr=$group" +
                "&$webVersionName=$webVersion" +
                "&sid=$serverInstance", onSuccess, onError)

    }

    override fun updateGroupSizesAndNames(){

        HTTP.request("${getURL()}?l3" +
                "&$webVersionName=$webVersion" +
                "&sid=$serverInstance", {

            val data = it.split('\n')
            val groupSizes = IntArray(GroupsEtc.GroupSizes.size)
            for(entry in data){
                val parts = entry.split(':')
                if(parts.size >= 4){
                    val uuid = parts[0].toIntOrNull() ?: continue
                    val group = parts[1].toIntOrNull() ?: continue
                    if(group in 0 until groupSizes.size){
                        groupSizes[group]++
                    }
                    val name = parts[2]
                    val craftingCount = parts[3].toIntOrNull() ?: continue
                    Element.get(name, uuid, group, craftingCount)
                }
            }

            for((id, size) in groupSizes.withIndex()){
                GroupsEtc.GroupSizes[id] = size
            }

            AllManager.invalidate()

        }, {})

    }

    override fun requestServerInstance(all: AllManager, name: String, password: Long,
                                       onSuccess: (serverName: String?, serverInstanceID: Int) -> Unit,
                                       onError: (Exception) -> Unit) {
        tryCaptcha(all, "gsid=${URLEncoder.encode(name, "UTF-8")}" +
                "&u=${AllManager.customUUID}" +
                "&pwd=$password", {
            val ix = it.split(';')
            if(ix.size > 1){
                val instanceId = ix[0].toIntOrNull() ?: return@tryCaptcha
                val instanceName = ix[1].trim()
                onSuccess(if(instanceName.isEmpty()) null else instanceName, instanceId)
            }
        }, onError)
    }

    override fun createServerInstance(all: AllManager, name: String, password: Long,
                                       onSuccess: (serverName: String?, serverInstanceID: Int) -> Unit,
                                       onError: (Exception) -> Unit) {
        tryCaptcha(all, "csid=${URLEncoder.encode(name, "UTF-8")}" +
                "&u=${AllManager.customUUID}" +
                "&pwd=$password", {
            val ix = it.split(';')
            if(ix.size > 1){
                val instanceId = ix[0].toIntOrNull() ?: return@tryCaptcha
                val instanceName = ix[1].trim()
                onSuccess(if(instanceName.isEmpty()) null else instanceName, instanceId)
            }
        }, onError)
    }
}