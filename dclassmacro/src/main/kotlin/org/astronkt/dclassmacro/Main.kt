package org.astronkt.dclassmacro

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import java.io.File

fun main(args: Array<String>) {
    val inputFile = File(args.firstOrNull() ?: "../game.dc")
    //println(DClassFileParser.tokens)
    //DClassFileParser.tokenizer.tokenize(inputFile.readText())
    //    .forEach { println("${it.tokenIndex}, \"${it.text}\" $it") }
    val file = DClassFileParser.parseToEnd(inputFile.readText())

    for (decl in file.decls) {
        if (decl is DClassFile.TypeDecl.DClass) {
            println("${decl.name}: ${decl.parents} {")
            for (field in decl.fields) {
                println("\t$field")
            }
            println("}")
        } else {
            println(decl)
        }
    }
    //println(generateDClassHelper(file))
}
