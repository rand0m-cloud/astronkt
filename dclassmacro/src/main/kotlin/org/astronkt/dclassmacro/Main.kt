package org.astronkt.dclassmacro

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import java.io.File

fun main(args: Array<String>) {
    val file = DClassFileParser.parseToEnd(File(args.firstOrNull() ?: "../game.dc").readText())
    println(generateDClassHelper(file))
}
