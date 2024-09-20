package me.antonio.noack.elementalcommunity.api

import me.antonio.noack.elementalcommunity.AllManager
import me.antonio.noack.elementalcommunity.Element
import me.antonio.noack.elementalcommunity.ElementStats
import me.antonio.noack.elementalcommunity.api.web.Candidate
import me.antonio.noack.elementalcommunity.api.web.News

interface ServerService {

    companion object {
        val defaultOnError: (Exception) -> Unit = {
            AllManager.toast(
                "${it::class.simpleName}: ${it.message}",
                true
            )
        }
    }

    fun askRecipe(
        a: Element, b: Element, all: AllManager,
        onSuccess: (Element?) -> Unit, onError: (Exception) -> Unit = defaultOnError
    )

    fun askNews(
        count: Int,
        onSuccess: (ArrayList<News>) -> Unit, onError: (Exception) -> Unit = defaultOnError
    )

    fun askPage(
        pageIndex: Int, search: String,
        onSuccess: (ArrayList<Element>, Int) -> Unit,
        onError: (Exception) -> Unit = defaultOnError
    )

    fun askStats(
        elementId: Int,
        onSuccess: (ElementStats) -> Unit,
        onError: (Exception) -> Unit = defaultOnError
    )

    fun getCandidates(
        a: Element, b: Element,
        onSuccess: (ArrayList<Candidate>) -> Unit, onError: (Exception) -> Unit = defaultOnError
    )

    fun suggestRecipe(
        all: AllManager, a: Element, b: Element, resultName: String, resultGroup: Int,
        onSuccess: (text: String) -> Unit, onError: (Exception) -> Unit = defaultOnError
    )

    fun likeRecipe(
        all: AllManager, uuid: Long,
        onSuccess: () -> Unit, onError: (Exception) -> Unit = defaultOnError
    )

    fun dislikeRecipe(
        all: AllManager, uuid: Long,
        onSuccess: () -> Unit, onError: (Exception) -> Unit = defaultOnError
    )

    fun askRecipes(
        name: String,
        onSuccess: (raw: String) -> Unit, onError: (Exception) -> Unit = defaultOnError
    )

    fun getRandomRecipe(
        onSuccess: (raw: String) -> Unit,
        onError: (Exception) -> Unit = defaultOnError
    )

    fun updateGroupSizesAndNames()

    /**
     * calls onSuccess(null, 0) if the password is wrong
     * calls onSuccess(null, -1) if the server does not exist
     * calls onSuccess(serverName, serverInstanceID) if everything worked fine
     * */
    fun requestServerInstance(
        all: AllManager, name: String, password: Long,
        onSuccess: (serverName: String?, serverInstanceID: Int) -> Unit,
        onError: (Exception) -> Unit = defaultOnError
    )

    /**
     * calls onSuccess(null, any) if sth went wrong
     * calls onSuccess(serverName, serverInstanceID) if everything worked fine
     * */
    fun createServerInstance(
        all: AllManager, name: String, password: Long,
        onSuccess: (serverName: String?, serverInstanceID: Int) -> Unit,
        onError: (Exception) -> Unit = defaultOnError
    )

    fun askAllRecipesOfGroup(
        group: Int,
        onSuccess: (raw: String) -> Unit, onError: (Exception) -> Unit = defaultOnError
    )

}