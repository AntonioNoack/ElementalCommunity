package me.antonio.noack.elementalcommunity

import org.junit.Test
import java.io.File
import java.io.FileWriter
import kotlin.math.min

class ImportElemental3 {

    val unknown = -1

    val colorMap = mapOf(
        1 to 2, // dark red, hell, explosion
        2 to 1, // red, too
        3 to 1, // fire, normal red
        4 to 5, // brown, hill
        5 to 3, // yellowish paper
        6 to 7, // electricity
        7 to 8, // hazel, polution, Durchfall
        8 to 10, // light green, "memes" (seen in the video from Carykh) + "acid sludge"
        9 to 11, // dark green, "green"
        10 to 15, // glass, very soft blue
        11 to 16, // soft blue, used for "water"
        12 to 20, // dark blue, used for "ocean"
        13 to 21, // "color" / "hue", soft purple, would be between 21 and 22, however seems to be closer to 21
        14 to 23, // "purple", concepts
        15 to 30, // white
        16 to 28, // gray, robots and such
        17 to 29, // "stone", dark gray
        18 to 32, // black
        19 to 25, // "aids"
        20 to 0, // rose
        21 to 31, // rainbow
        0 to 0
    )

    class Elemental3Element(node: JsonNode, val uuid: Int){
        val name: String
        init {
            var pre = (node.getString("name") ?: "")
                .replace(" +", "ßßß")
                .replace("+", " ")
                .replace('\u00b4', '\'')
                .replace("ßßß", " +")
                .replace("AT&T", "ATnT")
                .replace('\u00d7', 'x')
                .replace("\u03c0", " pi")
                .replace("\u2122", " TM")
                .replace("\u00a2", " cent")
                .replace('\u00ad', '-')
                .replace("+", " plus ")
                .replace("&", " and ")
                // .replace("...", "")
                .replace("...", "↓")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace("  ", " ")
                .replace("  ", " ")
                .replace("  ", " ")
                .replace("?", "")
                .replace("!", "")

            /*if(pre.startsWith("the ", true) && !pre.startsWith("the force ", true) && uuid > 1535 && when(uuid){
                    1602, 1709, 2099, 2288, 2348, 2384, 2415, 2745, 2764, 2803, 3084, 3121, 3362, 3548, 3757, 3908, 4001, 4064, 4069, 4079, 4241, 4335, 4342, 4495, 4645, 4692, 4706, 4824, 4953, 5065, 5206, 5317, 5343, 5414,
                    5550, 5691, 5802, 5905, 6288, 6398, 6407, 6553, 6596, 6618, 6798, 6916, 6953, 7048, 7052, 7155, 7165, 7197 -> true
                    1691, 1698, 1841, 1799, 2312, 1941, 2498, 2753, 2754 -> false
                    else -> {
                        println("the? $pre, $uuid")
                        false
                    }
                }){
                pre = pre.substring(4)
            }*/

            pre = replaceDoubleToxics(pre.trim())
            pre = replaceSingularToxics(pre)
            if(pre.endsWith("^2")) pre = pre.substring(0, pre.length-2).trim() + " squared"
            if(pre.startsWith("\"") && pre.endsWith("\"") && '"' !in pre.substring(1, pre.lastIndex)){
                pre = "*${pre.substring(1, pre.lastIndex)}*"
            }
            if(pre.count { it == '/' } == 1){
                val index = pre.indexOf('/')
                if(pre.substring(0, index).toIntOrNull() != null && pre.substring(index+1).toIntOrNull() != null){
                    pre = pre.substring(0, index) + pre.substring(index+1)
                }
            }
            name = pre.trim()
                .replace("↓", "...")
                .replace("......", "...")
                .replace(".....", "...")
                .replace("....", "...")
                .replace(" ...", "...")
                .replace("......", "...")
                .replace(".....", "...")
                .replace("....", "...")
                .replace("  ", " ")
                .replace("  ", " ")
                .replace("  ", " ")
                .replace("o_o", "o o")
                .replace("o_O", "o O")
                .replace("O_O", "O O")
                .replace("O_o", "O o")
                .trim()
            if(name.isEmpty()){
                println("#empty from ${node.get("name")}")
            }
        }

        val color = node.getInt("color")
        val parents = node.get("parents") as? JsonArray
        val children = HashSet<Elemental3Element>()
        lateinit var parentA: Elemental3Element
        lateinit var parentB: Elemental3Element
        var isIllegal = false
        override fun toString(): String = "$name ($uuid)"

        fun replaceSingularToxics(txt: String): String {
            for(i in 0 until txt.length-3){
                val char = txt[i]
                if(i == txt.length-4 && txt.endsWith("...")) return txt
                if(txt[i+1] == char && txt[i+2] == char && txt[i+3] == char && (char !in '0' .. '9' && char !in "IX")){
                    var j = i+3
                    while(txt.getOrNull(j) == char){
                        j++
                    }
                    val newString = txt.substring(0, i+3) + txt.substring(j)
                    // println("$txt -> $newString")
                    return replaceSingularToxics(newString)
                }
            }
            return txt
        }

        fun replaceDoubleToxics(txt: String): String {
            for(i in 0 until txt.length-8){
                val c0 = txt[i]
                val c1 = txt[i+1]
                if(c0 != c1 && txt[i+2] == c0 && txt[i+3] == c1 && txt[i+4] == c0 && txt[i+5] == c1 && txt[i+6] == c0 && txt[i+7] == c1
                    && (c0 !in '0' .. '9' && c0 !in "IX")
                    && (c1 !in '0' .. '9' && c1 !in "IX")){
                    var j = i+8
                    while(txt.getOrNull(j) == c0 && txt.getOrNull(j+1) == c1){
                        j += 2
                    }
                    val newString = txt.substring(0, i+6) + txt.substring(j)
                    // println("$txt -> $newString")
                    return replaceDoubleToxics(newString)
                }
            }
            return txt
        }

    }

    @Test
    fun checkElemental3Recipes(){

        val byColor = Array(30){ ArrayList<Elemental3Element>() }

        val json = JsonReader(File("/home/antonio/AndroidStudioProjects/ElementalCommunity/Elemental3Recipes.json").readText()).readObject()
        val elements = HashMap<Int, Elemental3Element>()
        for(uuid in 1 .. json.attributes.keys.map { it.toIntOrNull() ?: -1 }.max()!!){
            val node = json.map[uuid.toString()] as? JsonNode
            if(node != null){
                elements[uuid] = Elemental3Element(node, uuid)
            }
        }

        for((_, element) in elements){
            val parents = element.parents
            if(parents != null){
                element.parentA = elements[parents[0] as Int]!!
                element.parentB = elements[parents[1] as Int]!!
                element.parentA.children.add(element)
                element.parentB.children.add(element)
            }
        }

        val isLegalCharacter = { char: Char ->
            char in 'A' .. 'Z' || char in 'a' .. 'z' || char in '0' .. '9' || char in " \'.,-*"
        }

        val isIllegalCharacter = { char: Char ->
            !isLegalCharacter(char)
        }

        var ctr = 0
        var sum = 0
        for(element in elements.values){
            element.apply {
                val illegal = name.count(isIllegalCharacter)
                val isIllegal = illegal > 0 || name.isBlank()
                if(isIllegal){
                    // println("$illegal: $name")
                    ctr++
                }//  else println("non toxic? $name")
                this.isIllegal = isIllegal
            }
            sum++
        }

        do {
            var changed = false
            for(element in elements.values){
                if(element.isIllegal){
                    for(child in element.children){
                        if(!child.isIllegal){
                            child.isIllegal = true
                            println("$child became illegal by $element")
                            changed = true
                        }
                    }
                }
            }
        } while (changed)

        // done :)
        for(element in elements.values){
            if(!element.isIllegal){
                byColor[element.color].add(element)
            }
        }

        for((index, color) in byColor.withIndex()){
            if(color.isNotEmpty()){
                println("$index (${color.size}): ${color.subList(0, min(20, color.size)).joinToString()}")
            }
        }

        val ctr2 = elements.values.count { it.isIllegal }

        println("$ctr (source) -> $ctr + ${ctr2-ctr} (because of that) of $sum elements contain illegal characters")

        val validElements = elements.values.filter { !it.isIllegal && it.parents != null }

        val writer = FileWriter(File("/home/antonio/Desktop/internet/api/elemental/elemental3import.json"))
        writer.write("[")

        fun writeElement(element: Elemental3Element){
            writer.write("[\"${escape(element.name)}\", ${colorMap[element.color]}, \"${escape(element.parentA.name)}\", \"${escape(element.parentB.name)}\"]")
        }

        val first = validElements.first()
        for(element in validElements){
            if(element != first){
                writer.write(",\n")
            }
            writeElement(element)
        }

        writer.write("]")
        writer.close()

    }

}