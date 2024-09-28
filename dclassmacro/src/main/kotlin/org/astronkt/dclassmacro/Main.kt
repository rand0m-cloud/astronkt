package org.astronkt.dclassmacro

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import java.io.File

fun main(args: Array<String>) {
    val inputFile = File(args.firstOrNull() ?: "../game.dc")
    val outputDir = File(args.getOrNull(2) ?: "../clientapp/src/main/kotlin/GameSpec").apply {
        if (!exists()) {
            mkdirs()
        } else {
            deleteRecursively()
            mkdirs()
        }
    }
    val fileSrc = inputFile.readText()

    // debugTokens(fileSrc)
    val file = DClassFileParser.parseToEnd(fileSrc)

    // file.printDebug()
    generateDClassHelper(file, outputDir)
}

fun DClassFile.printDebug() {
    for (decl in decls) {
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
}

fun debugTokens(input: String) {
    println(DClassFileParser.tokens)
    DClassFileParser.tokenizer.tokenize(input)
        .forEach { println("${it.tokenIndex}, \"${it.text}\" $it") }
}
