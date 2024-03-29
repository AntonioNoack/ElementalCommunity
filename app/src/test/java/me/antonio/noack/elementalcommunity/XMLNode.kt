package me.antonio.noack.elementalcommunity

class XMLNode(val type: String) {

    val properties = HashMap<String, String>()
    val attributes = properties
    val children = ArrayList<Any>()

    operator fun get(key: String): String? = properties[key]
    operator fun set(key: String, value: String?) {
        if (value == null) properties.remove(key)
        else properties[key] = value
    }

    fun add(node: XMLNode) {
        children.add(node)
    }

    fun getContentString() = children.joinToString("")

    operator fun contains(key: String) = key in properties

    fun toString(depth: Int): String {
        val tabs = " ".repeat(depth * 2)
        return if (children.isEmpty()) {
            "$tabs<$type ${properties.entries.joinToString(" ") { "${it.key}=\"${it.value}\"" }}/>" +
                    if (depth == 0) "" else "\n"
        } else {
            "$tabs<$type ${properties.entries.joinToString(" ") { "${it.key}=\"${it.value}\"" }}>\n" +
                    children.joinToString("") {
                        (it as? XMLNode)?.toString(depth + 1) ?: it.toString()
                    } +
                    if (depth == 0) "$tabs</$type>" else "$tabs</$type>\n"
        }
    }

    override fun toString() = toString(0)

    fun deepClone(): XMLNode {
        val clone = XMLNode(type)
        clone.properties.putAll(properties)
        clone.children.addAll(children.map {
            if (it is XMLNode) it.deepClone()
            else it
        })
        return clone
    }

    fun shallowClone(): XMLNode {
        val clone = XMLNode(type)
        clone.properties.putAll(properties)
        clone.children.addAll(children)
        return clone
    }

}