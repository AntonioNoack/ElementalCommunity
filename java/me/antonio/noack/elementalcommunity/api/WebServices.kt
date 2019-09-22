package me.antonio.noack.elementalcommunity.api

import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.AllManager.Companion.addRecipe
import me.antonio.noack.elementalcommunity.AllManager.Companion.customUUID
import me.antonio.noack.elementalcommunity.AllManager.Companion.elementById
import me.antonio.noack.elementalcommunity.AllManager.Companion.invalidate
import me.antonio.noack.elementalcommunity.Element
import me.antonio.noack.elementalcommunity.GroupsEtc.GroupSizes
import me.antonio.noack.webdroid.Captcha
import me.antonio.noack.webdroid.HTTP
import java.lang.Exception
import java.net.URLEncoder

// done require less Captchas...

object WebServices {

    private const val ServerURL = "https://api.phychi.com/elemental/"

    fun tryCaptcha(all: AllManager, args: String, onSuccess: (String) -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    }){

        HTTP.request("$ServerURL?$args", { text ->
            if(text.isBlank() || text.startsWith("#reauth", true) || text.startsWith("#auth", true)){

                Captcha.get(all, { token ->
                    HTTP.request("$ServerURL?$args&t=${URLEncoder.encode(token, "UTF-8")}", onSuccess, onError)
                }, onError)

            } else onSuccess(text)
        }, onError)

    }



    fun askRecipe(a: Element, b: Element, onSuccess: (Element?) -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    }){

        HTTP.request("$ServerURL?a=${a.uuid}&b=${b.uuid}", { text ->
            val data = text.split(';')
            if(data.size > 1){
                val id = data[0].toInt()
                val group = data[1].toInt()
                val name = data[2]
                val element = elementById[id] ?: Element.get(
                    name,
                    id,
                    group
                )
                addRecipe(a, b, element)
                onSuccess(element)
            } else {
                onSuccess(null)
            }
        }, onError)

    }

    class News(val dt: Int, val a: Element, val b: Element, val result: String, val resultGroup: Int, val w: Int){
        override fun toString(): String {
            return "$a + $b -> $result($resultGroup) at $dt, $w"
        }
    }

    fun askNews(count: Int, onSuccess: (ArrayList<News>) -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    }){

        HTTP.request("$ServerURL?n=${count * 3}", { text ->
            val raw = text.split(";;")
            val list = ArrayList<News>()
            for(news in raw){
                val data = news.split(';')
                if(data.size > 9){
                    val dt = data[0].toIntOrNull() ?: continue
                    val aId = data[1].toIntOrNull() ?: continue
                    val aName = data[2]
                    val aGroup = data[3].toIntOrNull() ?: continue
                    val a = Element.get(aName, aId, aGroup)
                    val bId = data[4].toIntOrNull() ?: continue
                    val bName = data[5]
                    val bGroup = data[6].toIntOrNull() ?: continue
                    val b = Element.get(bName, bId, bGroup)
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

    class Candidate(val uuid: Long, val name: String, val group: Int)

    fun getCandidates(a: Element, b: Element, onSuccess: (ArrayList<Candidate>) -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    }){

        HTTP.request("$ServerURL?o=1&a=${a.uuid}&b=${b.uuid}", { text ->
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

    fun suggestRecipe(all: AllManager, a: Element, b: Element, resultName: String, resultGroup: Int, onSuccess: (text: String) -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    }){

        // a,b,r,g,u,t
        tryCaptcha(all, "a=${a.uuid}&b=${b.uuid}&r=${URLEncoder.encode(resultName, "UTF-8")}&g=$resultGroup&u=$customUUID", onSuccess, onError)

        /*Captcha.get(all, { token ->

            // a,b,r,g,u,t

            try {

                val url = URL("$ServerURL?a=${a.uuid}&b=${b.uuid}&r=${URLEncoder.encode(resultName, "UTF-8")}&g=$resultGroup&u=$customUUID&t=${URLEncoder.encode(token, "UTF-8")}")
                val con = url.openConnection()
                val bytes = con.getInputStream().readBytes()
                onSuccess(bytes)

            } catch (e: Exception){
                onError(e)
            }

        }, onError)*/

    }

    fun likeRecipe(all: AllManager, uuid: Long, onSuccess: () -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    }){

        tryCaptcha(all, "s=$uuid&r=1&u=$customUUID", { onSuccess() }, onError)

        /*Captcha.get(all, { token ->

            try {

                val url = URL("$ServerURL?s=$uuid&r=1&u=$customUUID&t=${URLEncoder.encode(token, "UTF-8")}")
                val con = url.openConnection()
                con.getInputStream().readBytes()
                onSuccess()

            } catch (e: Exception){
                onError(e)
            }

        }, onError)*/

    }

    fun dislikeRecipe(all: AllManager, uuid: Long, onSuccess: () -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    }){

        tryCaptcha(all, "s=$uuid&r=-1&u=$customUUID", { onSuccess() }, onError)

        /*Captcha.get(all, { token ->

            try {

                val url = URL("$ServerURL?s=$uuid&r=-1&u=$customUUID&t=${URLEncoder.encode(token, "UTF-8")}")
                val con = url.openConnection()
                con.getInputStream().readBytes()
                onSuccess()

            } catch (e: Exception){
                onError(e)
            }

        }, onError)*/

    }

    fun updateGroupSizes(){

        HTTP.request("$ServerURL?c", { text ->
            val data = text.split(';')
            for(entry in data){
                val index = entry.indexOf(':')
                if(index > 0){
                    val id = entry.substring(0, index).toIntOrNull() ?: continue
                    val size = entry.substring(index+1).toIntOrNull() ?: continue
                    GroupSizes[id] = size
                    invalidate()
                }
            }
        }, {})

    }

    fun updateGroupSizesAndNames(){

        HTTP.request("$ServerURL?l", {

            val data = it.split('\n')
            val groupSizes = IntArray(GroupSizes.size)
            for(entry in data){
                val index1 = entry.indexOf(':')
                val index2 = entry.indexOf(':', index1+1)
                if(index1 > 0 && index2 > 0){
                    val uuid = entry.substring(0, index1).toIntOrNull() ?: continue
                    val group = entry.substring(index1+1, index2).toIntOrNull() ?: continue
                    groupSizes[group]++
                    val name = entry.substring(index2+1)
                    Element.get(name, uuid, group)
                }
            }

            for((id, size) in groupSizes.withIndex()){
                GroupSizes[id] = size
            }

            invalidate()

        }, {})

    }

}