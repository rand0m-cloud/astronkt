@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.astronkt.dclassmacro

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.lexer.token
import com.github.h0tk3y.betterParse.parser.Parser

val reservedWords =
    setOf(
        // Keywords
        "required",
        "broadcast",
        "ram",
        "airecv",
        "ownrecv",
        "ownsend",
        "db",
        "clrecv",
        "clsend",
        "dclass",
        "typedef",
        "struct",
        "keyword",
        // Types
        "int8",
        "uint8",
        "int16",
        "uint16",
        "int32",
        "uint32",
        "int64",
        "uint64",
        "float64",
        "blob",
        "string",
        "char",
    )

// tysm! https://github.com/h0tk3y/better-parse
@Suppress("MemberVisibilityCanBePrivate")
object DClassFileParser : Grammar<DClassFile>() {
    // Tokens sorted by precedence
    @Suppress("unused")
    val ws by regexToken("\\s+", ignore = true)

    @Suppress("unused")
    val comment by regexToken("//[^\\n]*\\n", ignore = true)

    @Suppress("unused")
    val ignoreFrom by regexToken("from [^\\n]*import[^\\n]*\\n", ignore = true)

    val ident by token { charSequence, i ->
        val end = regexToken("[A-Za-z]([A-Za-z]|[0-9]|_)*", ignore = true).match(charSequence, i)
        val str = charSequence.substring(i, i + end)
        if (reservedWords.contains(str)
        ) {
            0
        } else {
            end
        }
    }

    val dclassKeyword by literalToken("dclass")
    val structKeyword by literalToken("struct")
    val typeDefKeyword by literalToken("typedef")

    val ram by literalToken("ram")
    val required by literalToken("required")
    val db by literalToken("db")

    val airecv by literalToken("airecv")
    val ownrecv by literalToken("ownrecv")
    val clrecv by literalToken("clrecv")
    val broadcast by literalToken("broadcast")
    val ownsend by literalToken("ownsend")
    val clsend by literalToken("clsend")

    val uint8 by literalToken("uint8")
    val int8 by literalToken("int8")
    val uint16 by literalToken("uint16")
    val int16 by literalToken("int16")
    val uint32 by literalToken("uint32")
    val int32 by literalToken("int32")
    val uint64 by literalToken("uint64")
    val int64 by literalToken("int64")
    val string by literalToken("string")
    val blob by literalToken("blob")
    val char by literalToken("char")
    val float64 by literalToken("float64")

    val openParen by literalToken("(")
    val closeParen by literalToken(")")
    val openBrace by literalToken("{")
    val closeBrace by literalToken("}")
    val openBracket by literalToken("[")
    val closeBracket by literalToken("]")
    val semicolon by literalToken(";")
    val colon by literalToken(":")
    val comma by literalToken(",")
    val equals by literalToken("=")

    val minus by literalToken("-")
    val plus by literalToken("+")
    val star by literalToken("*")
    val divide by literalToken("/")
    val modolus by literalToken("%")

    val rawFloatLiteral by regexToken("""\d+\.\d*|\.\d+""")
    val rawCharLiteral by regexToken("""" '([^'\n\\]|\\[nrtx][0-9A-Fa-f]*|\\.)' """.trim())
    val rawStringLiteral by regexToken(""" "([^"\n\\]|\\[nrtx][0-9A-Fa-f]*|\\.)*" """.trim())

    val hexIntLit by regexToken("0[xX][0-9A-Fa-f]+")
    val octIntLit by regexToken("0[1-7]+")
    val decIntLit by regexToken("([1-9]\\d*)|0")
    val binIntLit by regexToken("0[bB][01]+")

    // Token parsers
    val intType: Parser<DClassFile.DClassRawFieldType.IntType> by (
        (uint64 map { DClassFile.DClassRawFieldType.UInt64 })
            or (int64 map { DClassFile.DClassRawFieldType.Int64 })
            or (uint32 map { DClassFile.DClassRawFieldType.UInt32 })
            or (int32 map { DClassFile.DClassRawFieldType.Int32 })
            or (uint16 map { DClassFile.DClassRawFieldType.UInt16 })
            or (int16 map { DClassFile.DClassRawFieldType.Int16 })
            or (uint8 map { DClassFile.DClassRawFieldType.UInt8 })
            or (int8 map { DClassFile.DClassRawFieldType.Int8 })
    ) map { it }
    val charType by char map { DClassFile.DClassRawFieldType.Char }
    val sizedType: Parser<DClassFile.DClassRawFieldType.SizedType> by (
        string map {
            DClassFile.DClassRawFieldType.String
        }
    ) or (blob map { DClassFile.DClassRawFieldType.Blob })
    val floatType: Parser<DClassFile.DClassRawFieldType.FloatType> by float64 map { DClassFile.DClassRawFieldType.Float64 }

    val operator =
        (minus map { DClassFile.Operator.Minus }) or
            (plus map { DClassFile.Operator.Plus }) or
            (star map { DClassFile.Operator.Multiply }) or
            (divide map { DClassFile.Operator.Divide }) or
            (modolus map { DClassFile.Operator.Modulo })
    val dclassFieldModifierParser by (required map { DClassFile.DClassFieldModifier.Required }) or
        (broadcast map { DClassFile.DClassFieldModifier.Broadcast }) or
        (clsend map { DClassFile.DClassFieldModifier.ClSend }) or
        (clrecv map { DClassFile.DClassFieldModifier.ClRecv }) or
        (ram map { DClassFile.DClassFieldModifier.Ram }) or
        (db map { DClassFile.DClassFieldModifier.Db }) or
        (airecv map { DClassFile.DClassFieldModifier.AiRecv }) or
        (ownsend map { DClassFile.DClassFieldModifier.OwnSend }) or
        (ownrecv map { DClassFile.DClassFieldModifier.OwnRecv })

    // Literals
    val decLiteral by (optional(minus) * decIntLit) map { (minus, lit) ->
        DClassFile.Literal.IntLiteral.DecLiteral(
            lit.text.toLong(10).let {
                if (minus == null) it else it * -1
            },
        )
    }
    val octLiteral by (optional(minus) * octIntLit) map { (minus, lit) ->
        DClassFile.Literal.IntLiteral.OctLiteral(
            lit.text.toLong(8).let {
                if (minus == null) it else it * -1
            },
        )
    }
    val hexLiteral by (optional(minus) * hexIntLit) map { (minus, lit) ->
        DClassFile.Literal.IntLiteral.HexLiteral(
            lit.text.toLong(16).let {
                if (minus == null) it else it * -1
            },
        )
    }
    val binLiteral by (optional(minus) * binIntLit) map { (minus, lit) ->
        DClassFile.Literal.IntLiteral.BinLiteral(
            lit.text.toLong(2).let {
                if (minus == null) it else it * -1
            },
        )
    }
    val intLiteral by decLiteral or octLiteral or hexLiteral or binLiteral

    val floatLiteral by rawFloatLiteral map {
        val period = it.text.indexOf('.')
        DClassFile.Literal.FloatLiteral(
            if (it.text.getOrNull(period - 1)?.isDigit() == true) {
                it.text.toDouble()
            } else {
                val hasMinus = it.text.indexOf('-') >= 0
                "${if (hasMinus) "-" else ""}0${it.text.substring(period)}".toDouble()
            },
        )
    }

    val numLiteral: Parser<DClassFile.Literal.NumLiteral> by intLiteral or floatLiteral

    val charLiteral by rawCharLiteral map { char ->
        when (val text = char.text) {
            "\\n" -> '\n'
            "\\r" -> '\r'
            "\\t" -> '\t'
            else ->
                if (text.startsWith("\\x")) {
                    val hexValue = text.substring(2).toInt(16)
                    hexValue.toChar()
                } else {
                    text.last()
                }
        }.let {
            DClassFile.Literal.CharLiteral(it.code.toByte())
        }
    }

    val stringLiteral by rawStringLiteral map { string ->
        string.text
            .substring(1, string.text.length - 1)
            .replace(Regex("\\\\[nrtx][0-9A-Fa-f]*")) { match ->
                when (val text = match.value) {
                    "\\n" -> "\n"
                    "\\r" -> "\r"
                    "\\t" -> "\t"
                    else ->
                        if (text.startsWith("\\x")) {
                            val hexValue = text.substring(2).toInt(16)
                            hexValue.toChar().toString()
                        } else {
                            text.last().toString()
                        }
                }
            }.let {
                DClassFile.Literal.StringLiteral(it)
            }
    }

    val arrayShorthandLiteral: Parser<DClassFile.Literal.ArrayLiteral.ArrayShorthandLiteral> =
        (intLiteral * -star * intLiteral) map { (value, repeat) ->
            DClassFile.Literal.ArrayLiteral.ArrayShorthandLiteral(
                value,
                repeat,
            )
        }
    val arrayValueLiteral: Parser<DClassFile.Literal.ArrayValueLiteral> = arrayShorthandLiteral or intLiteral
    val arrayLiteral: Parser<DClassFile.Literal.ArrayLiteral> =
        (-openBracket * separatedTerms(arrayValueLiteral, comma, acceptZero = true) * -closeBracket) map { literals ->
            DClassFile.Literal.ArrayLiteral(literals)
        }

    val sizedLiteral: Parser<DClassFile.Literal.SizedLiteral> = arrayLiteral or stringLiteral

    // Field components
    val intRange by (-openParen * intLiteral * -minus * intLiteral * -closeParen) map { (open, close) ->
        DClassFile.DClassParameter.IntParameter.IntRange(
            open,
            close,
        )
    }

    val intTransform: Parser<DClassFile.DClassParameter.IntParameter.IntTransform> =
        (operator * intLiteral * optional(parser(this::intTransform))) map { (op, lit, next) ->
            DClassFile.DClassParameter.IntParameter.IntTransform(
                op,
                lit,
                next,
            )
        }

    val intConstant: Parser<DClassFile.DClassParameter.IntParameter.IntConstant> =
        (
            intLiteral map {
                DClassFile.DClassParameter.IntParameter.IntConstant(
                    it,
                    null,
                )
            }
        ) or
            (
                (-openParen * intLiteral * intTransform * -closeParen) map { (lit, transform) ->
                    DClassFile.DClassParameter.IntParameter.IntConstant(
                        lit,
                        transform,
                    )
                }
            )

    val floatRange by (-openParen * floatLiteral * -minus * floatLiteral * -closeParen) map { (open, close) ->
        DClassFile.DClassParameter.FloatParameter.FloatRange(
            open,
            close,
        )
    }
    val floatTransform: Parser<DClassFile.DClassParameter.FloatParameter.FloatTransform> by (
        (operator * numLiteral * optional(parser(this::floatTransform))) map { (op, lit, next) ->
            DClassFile.DClassParameter.FloatParameter.FloatTransform(
                op,
                lit,
                next,
            )
        }
    ) or ((-openParen * parser(this::floatTransform) * -closeParen))
    val floatConstant by (
        numLiteral map {
            DClassFile.DClassParameter.FloatParameter.FloatConstant(
                it,
                null,
            )
        }
    ) or (
        (-openBrace * numLiteral * floatTransform * -closeBrace) map { (lit, transform) ->
            DClassFile.DClassParameter.FloatParameter.FloatConstant(lit, transform)
        }
    )

    val arrayRange by (-openBracket * optional(intLiteral * optional(-minus * intLiteral)) * -closeBracket) map {
        when {
            it == null -> DClassFile.DClassParameter.ArrayParameter.ArrayRange.Empty
            it.t2 != null -> DClassFile.DClassParameter.ArrayParameter.ArrayRange.Range(it.t1, it.t2!!)
            else -> DClassFile.DClassParameter.ArrayParameter.ArrayRange.Size(it.t1)
        }
    }

    val sizeConstraint by (-openParen * intLiteral * optional(-minus * intLiteral) * -closeParen) map { (open, close) ->
        DClassFile.DClassFieldType.Sized.SizeConstraint(
            open,
            close,
        )
    }

    // Field Types
    val dclassIntType by (intType * optional(intTransform) * optional(intRange)) map { (type, transform, range) ->
        DClassFile.DClassFieldType.Int(
            type,
            range,
            transform,
        )
    }

    val dclassFloatType by (floatType * optional(floatRange) * optional(floatTransform)) map { (type, range, transform) ->
        DClassFile.DClassFieldType.Float(type, range, transform)
    }

    val dclassCharType by charType map { DClassFile.DClassFieldType.Char }

    val dclassSizedType by (sizedType * optional(sizeConstraint)) map { (type, size) -> DClassFile.DClassFieldType.Sized(type, size) }

    val dclassUserType by ident map {
        DClassFile.DClassFieldType.User(
            DClassFile.DClassRawFieldType.UserType(it.text),
        )
    }

    val dclassArrayType by (
        (dclassIntType or dclassFloatType or dclassCharType or dclassSizedType or dclassUserType) *
            oneOrMore(arrayRange)
    ) map { (type, ranges) ->
        ranges.fold(type) { acc, x ->
            DClassFile.DClassFieldType.Array(acc, x)
        } as DClassFile.DClassFieldType.Array
    }

    val dclassType by (dclassArrayType or dclassIntType or dclassFloatType or dclassCharType or dclassSizedType or dclassUserType)

    val dclassIntParameterParser by (
        dclassIntType * optional(ident) *
            optional(
                -equals * intConstant,
            )
    ) map { (intType, name, constant) ->
        DClassFile.DClassParameter.IntParameter(intType, name?.text, constant)
    }

    val dclassCharParameterParser by (dclassCharType * optional(ident) * optional(-equals * charLiteral)) map { (type, name, lit) ->
        DClassFile.DClassParameter.CharParameter(type, name?.text, lit)
    }

    val dclassFloatParameterParser by (
        dclassFloatType *
            optional(ident) *
            optional(-equals * floatConstant)
    ) map { (floatType, ident, constant) ->
        DClassFile.DClassParameter.FloatParameter(floatType, ident?.text, constant)
    }

    val dclassSizedParameterParser by (
        dclassSizedType * optional(ident) * optional(-equals * sizedLiteral)
    ) map { (type, name, default) ->
        DClassFile.DClassParameter.SizedParameter(
            type,
            name?.text,
            default,
        )
    }

    val dclassArrayParameterParser by
        dclassArrayType * optional(ident) * optional(-equals * arrayLiteral) map { (type, name, literal) ->
            DClassFile.DClassParameter.ArrayParameter(type, name?.text, literal)
        }

    val dclassUserTypeParameterParser by (dclassUserType * optional(ident) * optional(-equals * intLiteral)) map { (type, name, lit) ->
        DClassFile.DClassParameter.UserTypeParameter(type, name?.text, lit)
    }
    val dclassParameterParser: Parser<DClassFile.DClassParameter> by
        dclassArrayParameterParser or
            dclassSizedParameterParser or
            dclassUserTypeParameterParser or
            dclassCharParameterParser or
            dclassFloatParameterParser or
            dclassIntParameterParser

    val dclassParameterFieldParser by (
        dclassParameterParser *
            zeroOrMore(
                dclassFieldModifierParser,
            ) * -semicolon
    ) map { (parameter, modifiers) ->
        DClassFile.DClassField.ParameterField(parameter, modifiers)
    }

    val dclassAtomicFieldParser by (
        ident * -openParen *
            separatedTerms(
                dclassParameterParser,
                comma,
                acceptZero = true,
            ) * -closeParen * zeroOrMore(dclassFieldModifierParser) * -semicolon
    ) map { (ident, parameters, modifiers) ->
        DClassFile.DClassField.AtomicField(ident.text, parameters, modifiers)
    }

    val dclassMolecularFieldParser by (
        ident * -colon *
            separatedTerms(
                ident,
                comma,
                acceptZero = false,
            ) * -semicolon
    ) map { (name, fields) ->
        DClassFile.DClassField.MolecularField(name.text, fields.map { it.text })
    }

    val dclassFieldParser: Parser<DClassFile.DClassField> by dclassParameterFieldParser or
        dclassAtomicFieldParser or
        dclassMolecularFieldParser

    val dclassParser by (
        -dclassKeyword *
            ident *
            optional(-colon * separatedTerms(ident, comma)) *
            -openBrace *
            zeroOrMore(dclassFieldParser) *
            -closeBrace *
            -optional(semicolon)
    ) map
        { (name, parents, fields) ->
            DClassFile.TypeDecl.DClass(name.text, parents?.map { it.text } ?: listOf(), fields)
        }

    val structParser by (
        -structKeyword *
            ident *
            -openBrace *
            oneOrMore(dclassParameterParser * -semicolon) *
            -closeBrace *
            -optional(semicolon)
    ) map { (name, parameters) ->
        DClassFile.TypeDecl.Struct(name.text, parameters)
    }

    val typeDefParser by (
        -typeDefKeyword *
            dclassType *
            (
                (ident * optional(arrayRange) map { (ident, range) -> ident to range }) or
                    (optional(arrayRange) * ident map { (range, ident) -> ident to range })
            ) *
            -optional(semicolon)
    ) map { (type, tuple) ->
        val (name, range) = tuple
        DClassFile.TypeDecl.TypeDef(
            if (range == null) type else DClassFile.DClassFieldType.Array(type, range),
            name.text,
        )
    }

    val typeDeclParser: Parser<DClassFile.TypeDecl> = dclassParser or structParser or typeDefParser

    override val rootParser: Parser<DClassFile> = oneOrMore(typeDeclParser) map { DClassFile(it) }
}
