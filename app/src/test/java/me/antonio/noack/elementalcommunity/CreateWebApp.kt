package me.antonio.noack.elementalcommunity

import org.junit.Test
import java.io.File
import java.util.Locale

class CreateWebApp {

    // Linux system
    // val androidAppPath = File("/home/antonio/AndroidStudioProjects/ElementalCommunity/")
    // val webAppPath = File("/home/antonio/IdeaProjects/ElementalCommunityWeb/")

    val user = File(System.getProperty("user.home")!!)

    // Windows system
    val androidAppPath = File(user, "Documents\\IdeaProjects\\ElementalCommunity2")
    val webAppPath = File(user, "Documents\\IdeaProjects\\ElementalCommunityWeb")

    @Test
    fun main() {

        val self = File(androidAppPath, "app/src/main")
        val selfCode = File(self, "java")
        val target = File(webAppPath, "src/main/kotlin")

        // copyCodeFiles(selfCode, target)

        val res = File(self, "res")
        val values = File(res, "values")
        val layout = File(res, "layout")

        val r = File(target, "R.kt").writer()

        var tabs = 0

        fun send() {
            r.write("\n")
        }

        fun writeTabs() {
            for (i in 0 until tabs) {
                r.write("\t")
            }
        }

        fun send(string: String, dir: Int = 0) {
            writeTabs()
            r.write(string)
            when (dir) {
                1 -> {
                    tabs++
                }
                -1 -> {
                    tabs--
                }
            }
            r.write("\n")
        }

        fun open(name: String) {
            send("object $name {", +1)
            send()
        }

        fun close() {
            send()
            tabs--
            send("}")
        }

        val colors = ArrayList<Pair<String, Int>>()
        val strings = ArrayList<Pair<String, String>>()
        val styles = ArrayList<Pair<String, Style>>()

        fun beautifyValue(value: String): String {
            return when {
                value.startsWith("@color/") -> {
                    "color.${value.substring(7)}"
                    // colorString(colors.first { it.first == value.substring(7) }.second)
                }
                value.startsWith("@string/") -> {
                    "string.${value.substring(8)}"
                    // "\"${strings.first { it.first == value.substring(8) }.second}\""
                }
                value.startsWith("@drawable/") -> {
                    "drawable.${value.substring(10)}"
                }
                value.startsWith("#") -> {
                    colorString(parseColor(value))
                }
                value.startsWith("@+id/") -> {
                    "\"${value.substring(5)}\""
                }
                value.startsWith("@style") -> {
                    "style.${value.substring(7)}"
                }
                else -> "\"$value\""
            }
        }

        for (file in values.listFiles()!!) {
            val fileName = file.name
            if (fileName.endsWith(".xml") && !fileName.startsWith('.')) {
                val resources =
                    (XMLReader().parse(file.inputStream()) as XMLNode).children ?: continue
                for (resource in resources) {
                    resource as XMLNode
                    when (resource.type) {
                        "color" -> {
                            val name = resource["name"]!!
                            colors.add(name to parseColor(resource.getContentString()))
                        }
                        "string" -> {
                            val name = resource["name"]!!
                            strings.add(name to parseString(resource.getContentString()))
                        }
                        "style" -> {
                            val name = resource["name"]!!
                            val style = Style()
                            style["parent"] = resource["parent"]!!
                            styles.add(name to style)
                            for (child in resource.children) {
                                child as XMLNode
                                assert(child.type == "item")
                                style[child["name"]!!] = child.getContentString().trim()
                            }
                        }
                    }
                }
            }
        }

        for (imported in ("kotlinx.browser.document," +
                "org.w3c.files.File," +
                "android.view.*," +
                "me.antonio.noack.elementalcommunity.AllManager," +
                "android.widget.*").split(',').sorted()) {
            send("import $imported")
        }

        send()
        open("R")
        send("val all = AllManager()")
        send()
        open("color")
        for ((name, color) in colors) {
            send("const val $name = ${colorString(color)}")
        }
        close()
        send()
        open("string")
        for ((name, string) in strings) {
            send(
                "const val $name = \"${escape(string)}\""
            )
        }
        close()
        send()
        open("drawable")
        for (file in File(res, "drawable").listFiles() ?: emptyArray()) {
            if (!file.isDirectory && !file.name.startsWith(".")) {
                when (file.extension.lowercase()) {
                    "xml" -> {
                        val srcText = file.readText()
                        if (srcText.contains("<vector")) {
                            send(
                                "const val ${file.nameWithoutExtension} = \"${
                                    escape(makeSVG(srcText.substring(srcText.indexOf("<vector"))))
                                }\""
                            )
                        }
                    }
                    "png", "jpg", "gif" -> {
                        send("const val ${file.nameWithoutExtension} = \"drawable/${file.name}\"")
                    }
                    else -> {
                        println("got unexpected drawable ${file.name}")
                    }
                }
            }
        }
        close()
        send()

        val ids = HashSet<String>()
        fun printLayout(node: XMLNode) {
            if (node.type == "include") {
                send("R.layout." + node["layout"]!!.substring("@layout/".length))
            } else {

                // the id
                if (node["android:id"] != null) {
                    ids.add(node["android:id"]!!)
                }

                // attributes
                tabs++
                send("${node.type}(all, null)")
                tabs++
                for ((key, value) in node.attributes.toSortedMap()) {
                    send(
                        ".attr(\"${
                            key.replace(
                                "android:",
                                ""
                            )
                        }\", ${beautifyValue(value)})"
                    )
                }
                tabs--

                // children
                tabs++
                for (child in node.children) {
                    child as XMLNode
                    if (child.type == "include") {
                        send(".addView(" + child["layout"]!!.substring("@layout/".length) + ")")
                    } else {
                        send(".addView(")
                        printLayout(child)
                        send(")")
                    }
                }
                tabs -= 2
            }
        }

        open("layout")
        val layoutNames = HashSet<String>()
        for (file in layout.listFiles() ?: emptyArray()) {
            if (!file.isDirectory && file.extension == "xml") {
                val layoutName = file.nameWithoutExtension
                layoutNames.add(layoutName)
                send("val $layoutName: View by lazy {")
                val node = XMLReader().parse(file.inputStream()) as XMLNode
                node.attributes.remove("xmlns:android")
                node.attributes.remove("xmlns:tools")
                node.attributes.remove("tools:context")
                node.attributes["layout_id"] = layoutName
                printLayout(node)
                send("}")
                send()
            }
        }
        send("val allLayouts: List<View> by lazy { listOf(")
        for (subs in layoutNames.sorted().chunked(6)) {
            send("\t${subs.joinToString()},")
        }
        send(") }")
        close()

        send()

        open("id")
        for (rawId in ids.sortedBy { it.lowercase(Locale.getDefault()) }) {
            val id = rawId.substring("@+id/".length)
            send("const val $id = \"$id\"")
        }
        close()

        send()
        open("raw")
        for (file in File(res, "raw").listFiles()!!) {
            if (!file.isDirectory && !file.name.startsWith(".")) {
                val withoutEnding = file.nameWithoutExtension
                send("const val $withoutEnding = \"raw/${file.name}\"")
            }
        }
        close()
        send()
        send("val allStyles = ")
        val stylesNode = XMLNode("LinearLayout")
        for ((name, style) in styles) {
            val styleNode = XMLNode("View")
            styleNode.attributes["id"] = name
            for ((key, value) in style) {
                styleNode.attributes[key] = value.toString()
            }
            stylesNode.add(styleNode)
        }
        printLayout(stylesNode)
        send()
        open("style")
        for ((name, _) in styles) {
            send("const val $name = \"${escape(name)}\"")
        }
        close()
        close()
        send("// ${Math.random()}")

        r.close()

        println("done")


    }

    fun colorString(color: Int) = "0x${(color.shr(16).and(0xffff).toString(16))}${
        color.and(0xffff).toString(16)
    }${if (color < 0) ".toInt()" else ""}"

    fun parseString(str: String) = str.replace("\n", "")

    fun parseColor(str: String): Int {
        val text = str.trim()
        if (text.startsWith("#")) {
            return when (text.length) {
                4 -> {
                    val num = text.substring(1).toInt(16)
                    return 0xff000000.toInt() or (0x110000 * num.shr(8)) or (0x1100 * num.shr(4)
                        .and(15)) or (0x11 * num.and(15))
                }
                5 -> {
                    val num = text.substring(1).toInt(16)
                    return (0x11000000 * num.shr(12)) or (0x110000 * num.shr(8)) or (0x1100 * num.shr(
                        4
                    ).and(15)) or (0x11 * num.and(15))
                }
                7 -> 0xff000000.toInt() or text.substring(1).toInt(16)
                9 -> text.substring(1).toLong(16).toInt()
                else -> throw RuntimeException("Unknown color $text")
            }
        } else throw RuntimeException("Unknown color $text")
    }

    fun makeSVG(source: String): String {

        var svg = source
            .replace("vector", "svg")
            .replace("android:", "")
            .replace("dp", "px")
            .replace("pathData=", "d=")
            .replace("xmlns:android=\"http://schemas.android.com/apk/res/android\"", "")
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace('\t', ' ')
            .replace("    ", " ")
            .replace("   ", " ")
            .replace("  ", " ")
            .replace("  ", " ")

        svg = svgReplace("fillColor", "fill", svg)
        svg = svgReplace("strokeColor", "stroke", svg)
            .replace("strokeWidth", "stroke-width")

        println("svg $svg")

        return svg

    }

    fun svgReplace(src: String, dst: String, svg: String): String {
        val svgParts = svg.split("$src=")
        val result = StringBuilder()
        result.append(svgParts[0])

        for (i in 1 until svgParts.size) {
            val part = svgParts[i]
            // println(part)
            assert(part[0] == '"')
            val nextIndex = part.indexOf('"', 1)
            val colorString = part.substring(1, nextIndex)
            val color = parseColor(colorString)
            result.append(dst)
            result.append('=')
            result.append('"')
            result.append(rgbaString(color))
            result.append(part.substring(nextIndex))
            result.append('"')
        }
        return result.toString()
    }

    fun rgbaString(color: Int): String {
        return "rgba(${color.shr(16).and(255)}, ${
            color.shr(8).and(255)
        }, ${color.and(255)}, ${color.shr(24).and(255) / 255f})"
    }

    fun filterSourceLine(line: String, file: String): Boolean {
        when (line) {
            "import kotlin.concurrent.thread",
            "import me.antonio.noack.elementalcommunity.R",
            "import java.lang.Math.abs" -> return false
        }
        if (line.startsWith("import kotlinx.android")) {
            println("Warning: Synthetic in $file")
            return false
        }
        return true
    }

    fun copyCodeFiles(src: File, dst: File) {
        if (src.isDirectory) {
            for (file in src.listFiles() ?: emptyArray()) {
                if (!file.name.startsWith(".")) {
                    copyCodeFiles(file, File(dst, file.name))
                }
            }
        } else if (when (src.name) {
                "Maths.kt", "Sound.kt", "HTTP.kt", "Captcha.kt", "IDTypes.kt", "BetterPreferences.kt", "FileChooser.kt", "FileSaver.kt" -> false
                else -> true
            }
        ) {
            dst.parentFile?.mkdirs()
            var text = src.readText()
                .replace("\r\n", "\n")
                .replace(".javaClass.", "::class.")
                .split('\n').filter { filterSourceLine(it, src.name) }.joinToString("\n")
                .replace("System.", "java.util.System.")
                .replace("LinearLayout.LayoutParams", "View.LayoutParams")
                .replace(
                    "import android.content.Intent.",
                    "import android.content.Intent.Companion."
                )
                .replace(".code", ".toInt()")
                .replace(".lowercase(Locale.getDefault())", ".toLowerCase()")
            if (text.contains("thread {") || text.contains("thread(true") || text.contains("thread{")) {
                text = text.replaceFirst("import", "import java.util.*\nimport")
            }
            if (!dst.exists() || dst.readText() != text) {
                dst.writeText(text)
            }
        }
    }

}