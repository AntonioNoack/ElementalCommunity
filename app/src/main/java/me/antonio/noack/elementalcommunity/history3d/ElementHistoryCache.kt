package me.antonio.noack.elementalcommunity.history3d

import me.antonio.noack.elementalcommunity.Element
import me.antonio.noack.elementalcommunity.api.ServerService.Companion.defaultOnError
import me.antonio.noack.elementalcommunity.api.WebServices
import me.antonio.noack.elementalcommunity.utils.Compact.compacted
import java.util.Random

object ElementHistoryCache {

    private val testElements = """
        Earth;5;0;99;0;0;99;0;0;25921098
        Air;16;0;0;99;0;0;99;0;1
        Water;20;0;99;99;0;99;99;0;0
        Fire;1;0;0;0;0;0;0;0;0
        Sand;6;20;99;0;20;0;0;20;1074
        Energy;7;5;99;99;25;0;0;25;0
        Cloud;30;20;0;99;45;99;99;45;118
        Quicksand;6;5;99;99;50;49;0;30;0
        Desert;6;5;0;0;55;49;0;35;0
        DrySoil;4;5;99;0;60;49;0;40;0
        Heat;2;5;0;0;65;49;49;40;1
        Wind;16;5;0;99;70;49;49;45;4
        Storm;29;5;49;49;50;24;74;5;0
        Death;32;5;0;0;80;24;0;25;1
        Life;10;5;99;0;85;49;49;60;0
        Roasted;2;5;0;0;90;74;24;5;2
        Coal;32;5;0;0;95;37;12;5;1
        Plants;10;5;99;0;100;74;24;15;0
        Sugar;30;5;49;49;80;86;12;5;1
        Soil;5;5;74;49;60;74;0;50;4
        Thunderstorm;29;5;99;99;115;36;61;40;2
        Sandstorm;6;11;49;0;106;36;61;51;58
        Trees;11;20;99;0;146;86;12;46;950
        Waves;20;5;99;99;151;24;74;81;0
        Saturation;1;5;74;24;71;37;12;66;0
        BBQ;1;5;37;12;71;37;12;71;0
        War;2;20;36;61;106;12;0;101;1631
        Nature;10;5;86;12;86;86;12;86;0
        Branches;5;5;36;61;116;92;6;45;7
        Forest;11;5;92;6;50;92;6;50;20
        Sapling;10;15;99;0;211;64;33;20;75
        Campfire;4;5;0;0;216;64;33;25;0
        Forest Fire;2;5;0;0;221;92;6;25;0
        Coral Reef;31;5;99;99;226;92;6;30;21
        Glass;12;5;49;0;211;24;24;166;0
        Animals;10;5;49;49;211;86;12;50;0
        Cacti;11;5;24;0;186;86;12;141;0
        Stone;29;5;99;0;246;12;0;166;22
        Poison Gas;10;5;0;99;251;12;0;171;0
        Oven;29;5;0;0;256;18;6;161;0
        Fish;20;5;49;49;236;95;52;35;0
        Scrubs;5;5;24;0;211;64;33;75;0
        Scrubland;4;5;24;0;216;92;6;75;0
        Caramel;3;5;24;24;211;67;30;171;0
        Error;2;5;37;12;191;95;52;55;0
        Rain;20;5;99;99;286;24;74;216;1
        AC Current;7;5;49;49;266;61;86;140;1
        Swamp;11;5;74;49;246;92;6;100;1
        Grassland;10;5;49;0;281;83;27;5;1
        Lava;2;5;24;24;241;55;0;60;1
        Birds;1;5;0;99;311;74;24;226;12
        Magma;2;5;49;49;291;39;12;10;0
        Cobblestone;29;5;12;0;241;55;0;75;0
        Living Wood;4;5;74;24;241;64;33;135;0
        Flies;13;5;74;24;246;83;27;35;0
        Magma Monster;2;5;74;24;251;39;12;30;0
        Sinkhole;4;5;99;0;341;0;99;341;27
        Thundercloud;29;5;49;49;321;49;99;301;0
        Plasma;2;5;49;49;326;24;24;286;0
        Tsunami;20;5;36;61;281;61;86;205;0
        Lightning;7;5;49;49;336;36;61;286;10
        Sandtornado;17;5;36;61;291;42;30;240;1
        Magmastorm;2;5;36;61;296;44;30;55;3
        Vulcano;29;5;24;0;321;39;12;70;5
        Future;13;5;86;12;281;36;12;150;0
        Candyland;25;5;67;30;281;92;6;190;0
        Cotton Candy;24;5;0;99;391;67;30;286;5
        Metropole;12;5;92;6;200;36;12;165;0
        Gravel;28;20;12;0;336;33;0;95;166
        Cement;29;5;49;0;401;22;0;5;1
        Concrete;28;5;99;99;426;35;0;5;0
        Love Beads;22;5;67;30;326;22;0;15;3
        Broken Glass;12;5;36;12;205;55;0;190;1
        Blood;2;5;74;24;356;45;6;5;0
        Murder;2;5;12;0;366;59;15;5;2
        Vampire;2;5;74;24;366;59;15;10;1
        Ash;29;5;0;0;456;92;6;310;9
        Breeding;1;5;67;30;225;67;30;225;0
        Humans;4;5;67;30;230;67;49;40;1
        Wood;5;5;92;6;325;67;39;5;1
        Thor;7;5;42;55;115;67;39;10;0
        Food;4;5;67;30;245;67;39;15;1
        Nomads;6;5;24;0;431;67;39;20;1
        Sweets;25;5;67;30;386;67;39;25;1
        Decorations;26;5;39;12;190;67;39;30;0
        Craft;14;5;67;39;35;79;22;30;1
        Pets;4;5;67;30;270;73;30;5;0
        Snakes;6;5;49;0;491;70;30;5;1
        Sea Snakes;13;5;99;99;516;59;15;5;1
        Bat;32;5;67;30;285;66;19;70;2
        Parrot;31;5;0;99;526;70;30;20;1
        Meat;1;5;37;12;441;67;30;295;2
        Building;29;5;36;12;305;67;49;110;2
        Magic;25;5;99;0;541;59;15;100;2
        Magician;22;5;67;39;80;79;7;5;1
        Sun;7;5;61;86;265;79;7;10;1
        deleted;32;5;18;51;0;18;51;0;492280
        Bird;16;5;0;99;561;67;30;325;-492277
        Pigeon;6;5;73;23;20;33;64;5;0
        Rainbow;31;5;36;12;340;70;46;20;3
        Love;1;5;67;39;110;70;30;70;3
        Genetics;10;5;67;30;120;73;30;80;1
        Deforestation;5;5;92;6;390;67;39;120;2
        Photosynthesis;6;5;49;49;566;70;46;40;1
        Anti;26;5;92;6;400;79;22;10;8
        Hate;2;5;68;34;25;85;14;5;0
        Black Hole;32;5;70;46;55;85;14;10;2
        Science;13;5;73;30;110;79;7;70;2
        Biology;11;5;67;30;380;76;18;5;0
        Astronomy;7;5;70;46;70;76;18;10;1
        Physics;7;5;49;49;601;76;18;15;1
        Engineering;17;5;73;30;130;62;33;5;0
        Planning;16;5;74;24;551;67;31;5;1
        Geography;5;20;99;0;656;76;18;45;660
        Tornado;28;20;0;99;676;49;99;631;2911
        Hurricane;28;5;49;99;636;12;0;601;0
        Corpse;32;5;12;0;606;67;30;450;0
        Woodpecker;10;20;79;22;235;33;64;145;651
        Fire Gravel;29;20;0;0;726;22;0;310;935
        deleted;32;5;16;68;0;16;68;0;487095
        Mud;5;5;99;0;736;99;99;736;-487086
        Hill;5;5;99;0;741;99;0;741;3
        Mountain;5;5;99;0;5;99;0;5;0
        Watercraft;19;20;99;99;766;73;30;265;4615
        Water Engineering;20;5;99;99;771;67;31;140;0
        Boat;5;5;79;22;305;86;64;10;0
        Submarine;20;5;99;99;781;86;64;15;1
        Hovercraft;16;5;0;99;786;86;64;20;1
        Aircraft;15;5;0;99;791;73;30;290;0
        Jet;19;5;36;61;721;36;64;5;1
        Airplane;27;5;24;74;731;36;64;10;0
        Helicopter;10;5;0;99;806;36;64;15;0
        Jetski;15;5;99;99;811;36;62;15;2
        Stream;20;5;61;86;665;48;73;460;5
        Flood;20;5;99;0;821;48;73;465;1
        Moraines;7;5;72;50;565;59;15;315;1
        Cloudy;30;20;49;99;801;61;86;560;22207
        very hot;2;5;79;90;0;79;90;0;0
        downpour;20;5;61;86;570;61;86;570;0
        hut;5;5;79;22;390;51;30;325;0
        heavy plane;19;5;36;62;70;36;62;70;0
        Strong wind;15;20;0;99;886;0;99;886;42947
        Sea;20;5;99;99;891;99;99;891;0
        Beach ;6;5;99;99;896;24;0;841;0
        Firenado;2;5;0;0;901;24;99;225;0
        Volcano;2;5;0;0;906;99;0;160;0
        Sahara desert;3;5;24;0;856;24;0;856;0
        Sand dune;6;5;24;0;861;99;0;175;0
        Window ;12;5;36;12;690;36;12;690;0
        stone building;29;5;55;0;680;51;30;390;0
        Mt.  Everest ;30;5;99;0;185;99;0;185;0
        Fog;16;20;99;0;951;49;99;906;70286
        Dirty quicksand;5;5;99;0;956;74;49;906;0
        Dryer soil;4;5;99;0;961;74;0;901;0
        Hot climate;5;5;99;0;966;24;24;901;0
        Big hill;5;5;99;0;971;99;0;230;0
        Green Sahara;5;5;99;0;976;24;0;65;0
        Quickersand;3;5;99;99;981;74;49;931;0
        Waterfall;20;5;99;99;986;99;0;240;0
        Caspian Sea;20;5;99;99;991;99;99;100;0
    """.trimIndent().parseElements3D()

    fun String.parseElements3D(): List<Element3D> {
        val raw = lines()
        val list = ArrayList<Element3D>(raw.size)
        val loadedZScale = 4
        var prevZ = 0
        var prevTimestamp = 0
        val random = Random(12354)
        val rnd = 5
        for (news in raw) {
            val data = news.split(';')
            if (data.size >= 9) {
                val name = data[0]
                val groupId = data[1].toIntOrNull() ?: continue

                val deltaZ = data[2].toIntOrNull() ?: continue
                val currZ = prevZ + deltaZ
                prevZ = currZ

                var parentAX = data[3].toIntOrNull() ?: continue
                var parentAY = data[4].toIntOrNull() ?: continue
                val parentAZ = data[5].toIntOrNull() ?: continue
                var parentBX = data[6].toIntOrNull() ?: continue
                var parentBY = data[7].toIntOrNull() ?: continue
                val parentBZ = data[8].toIntOrNull() ?: continue
                val deltaT = data[9].toIntOrNull() ?: continue
                prevTimestamp += deltaT

                val chainRecipe = parentAX == parentBX &&
                        parentAY == parentBY &&
                        parentAZ == parentBZ

                parentAX += random.nextInt(-rnd, rnd)
                parentAY += random.nextInt(-rnd, rnd)

                if (chainRecipe) {
                    parentBX = parentAX
                    parentBY = parentAY
                } else {
                    parentAX += random.nextInt(-rnd, rnd)
                    parentAY += random.nextInt(-rnd, rnd)
                }

                list.add(
                    Element3D(
                        name, groupId, currZ * loadedZScale,
                        parentAX, parentAY, (currZ - parentAZ) * loadedZScale,
                        parentBX, parentBY, (currZ - parentBZ) * loadedZScale,
                        prevTimestamp
                    )
                )
            }
        }
        return list
    }

    private val chunkSizeBits = 9
    private val chunkSize = 1 shl chunkSizeBits
    private val chunks = ArrayList<List<Element3D>>()
    private var isQuerying = false
    private var waitUntilNanos = Long.MIN_VALUE
    private val voidElement = Element3D(
        "", 0, 0,
        0, 0, 0,
        0, 0, 0, 0
    )

    fun getElement(index: Int): Element3D? {
        if (index < 0) return null

        val chunkIndex = index shr chunkSizeBits
        if (chunkIndex in chunks.indices) {
            val chunk = chunks[chunkIndex]
            return chunk.getOrNull(index - chunkIndex * chunkSize)
        }

        if (isQuerying) {
            return testElements.getOrNull(index)
        }

        if (System.nanoTime() < waitUntilNanos) {
            // wait a little
            return testElements.getOrNull(index)
        }

        isQuerying = true

        val nextChunkIndex = chunks.size
        WebServices.askHistory(chunkSize, nextChunkIndex, { elements ->
            // println("got good response: ${elements.size}x")
            if (elements.isNotEmpty()) {
                synchronized(chunks) {
                    if (chunks.size == nextChunkIndex) {
                        val listWithVoids = if (elements.size == chunkSize) elements else {
                            elements + List(chunkSize - elements.size) { voidElement }
                        }
                        chunks.add(listWithVoids)
                    }//  else println("duplicate/skipped???")
                }
            } else {
                waitUntilNanos = System.nanoTime() + (60 * 1e9).toLong() // no more elements
            }
            isQuerying = false
        }, { error ->
            // println("got bad response: $error")
            waitUntilNanos = System.nanoTime() + (30 * 1e9).toLong()
            isQuerying = false
            defaultOnError(error)
        })

        return testElements.getOrNull(index)
    }

    fun find(element: Element): Pair<Element3D, Int>? {
        val searched = element.compacted
        // in theory, we could skip ~0.99 * element.uuid...
        for (chunkIndex in chunks.indices) {
            val chunk = chunks[chunkIndex]
            for (elementIndex in chunk.indices) {
                val element2 = chunk[elementIndex]
                if (compacted(element2.name) == searched) {
                    return element2 to (chunkIndex * chunkSize + elementIndex)
                }
            }
        }
        return null
    }

}