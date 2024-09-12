package org.astronkt.dclassmacro

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser

//tysm! https://github.com/h0tk3y/better-parse
object DClassFileParser : Grammar<DClassFile>() {
    val dclassKeyword by literalToken("dclass")
    val required by literalToken("required")
    val broadcast by literalToken("broadcast")
    val clsend by literalToken("clsend")

    val uint32 by literalToken("uint32")
    val string by literalToken("string")

    val ident by regexToken("[A-z]([A-z]|[0-9])*")

    val openParen by literalToken("(")
    val closeParen by literalToken(")")
    val openBrace by literalToken("{")
    val closeBrace by literalToken("}")
    val semicolon by literalToken(";")
    val comma by literalToken(",")
    val ws by regexToken("\\s+", ignore = true)

    val dclassFieldType by (uint32 map { DClassFile.DClassFieldType.UInt32 }) or
            (string map { DClassFile.DClassFieldType.String })
    val dclassFieldModifierParser by (required use { DClassFile.DClassFieldModifier.Required }) or
            (broadcast use { DClassFile.DClassFieldModifier.Broadcast }) or
            (clsend use { DClassFile.DClassFieldModifier.ClSend })


    // Fields
    val dclassSimpleFieldParser by (dclassFieldType * ident * zeroOrMore(dclassFieldModifierParser) * semicolon) map { (ty, ident, modifiers, _) ->
        DClassFile.DClassField.Simple(ident.text, ty, modifiers)
    }
    val dclassMethodFieldParser by (ident * openParen * separatedTerms(
        dclassFieldType * ident,
        comma
    ) * closeParen * zeroOrMore(dclassFieldModifierParser) * semicolon) map { (ident, _, ty, _, modifiers, _) ->
        DClassFile.DClassField.Method(ident.text, ty.map { it.t2.text to it.t1 }, modifiers)
    }
    val dclassFieldParser: Parser<DClassFile.DClassField> by dclassSimpleFieldParser or dclassMethodFieldParser

    // DClass block
    val dclassParser by (dclassKeyword * ident * openBrace * zeroOrMore(dclassFieldParser) * closeBrace) map { (_, ident, _, fields, _) ->
        DClassFile.TypeDecl.DClass(ident.text, fields)
    }

    val typeDeclParser: Parser<DClassFile.TypeDecl> = dclassParser
    override val rootParser: Parser<DClassFile> = oneOrMore(typeDeclParser) map { DClassFile(it) }
}

