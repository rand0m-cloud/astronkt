@file:Suppress("unused")

package org.astronkt.dclassmacro

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import java.io.File

fun main(args: Array<String>) {
    assert(args.size >= 2) { "expected arguments to be <input_file>+ <output_dir>" }

    val inputFiles = (0..<args.size - 1).map {
        val filename = args[it]
        File(filename)
    }
    val outputDir = File(args.last()).apply {
        if (!exists()) {
            mkdirs()
        } else {
            deleteRecursively()
            mkdirs()
        }
    }
    val fileSrc = inputFiles.joinToString("\n") { it.readText() }

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
