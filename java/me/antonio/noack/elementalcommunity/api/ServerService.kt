package me.antonio.noack.elementalcommunity.api

import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.Element
import me.antonio.noack.elementalcommunity.api.web.Candidate
import me.antonio.noack.elementalcommunity.api.web.News
import java.lang.Exception

interface ServerService {

    fun askRecipe(a: Element, b: Element, all: AllManager, onSuccess: (Element?) -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    })

    fun askNews(count: Int, onSuccess: (ArrayList<News>) -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    })

    fun getCandidates(a: Element, b: Element, onSuccess: (ArrayList<Candidate>) -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    })

    fun suggestRecipe(all: AllManager, a: Element, b: Element, resultName: String, resultGroup: Int, onSuccess: (text: String) -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    })

    fun likeRecipe(all: AllManager, uuid: Long, onSuccess: () -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    })

    fun dislikeRecipe(all: AllManager, uuid: Long, onSuccess: () -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    })

    fun askRecipes(name: String, onSuccess: (raw: String) -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    })

    fun updateGroupSizesAndNames()

    /**
     * calls onSuccess(null, 0) if the password is wrong
     * calls onSuccess(null, -1) if the server does not exist
     * calls onSuccess(serverName, serverInstanceID) if everything worked fine
     * */
    fun requestServerInstance(all: AllManager, name: String, password: Long, onSuccess: (serverName: String?, serverInstanceID: Int) -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    })

    /**
     * calls onSuccess(null, any) if sth went wrong
     * calls onSuccess(serverName, serverInstanceID) if everything worked fine
     * */
    fun createServerInstance(all: AllManager, name: String, password: Long, onSuccess: (serverName: String?, serverInstanceID: Int) -> Unit, onError: (Exception) -> Unit = {
        AllManager.toast(
            "${it.javaClass.simpleName}: ${it.message}",
            true
        )
    })

}