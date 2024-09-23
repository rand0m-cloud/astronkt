@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.astronkt.server.app

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.astronkt.*
import org.astronkt.internal.AstronInternalRepositoryConfig

open class TestClass(doId: DOId) : DistributedObject(doId, 3U.toDClassId()) {
    var x: Double
        get() =
            getField(1U.toFieldId())?.toUInt32()!!.transform {
                divide(1000f)
                modulo(360f)
            }
        set(value) =
            setField(
                1U.toFieldId(),
                value.unTransform(FieldValue.Type.UInt32) {
                    divide(1000f)
                    modulo(360f)
                },
            )
    override val objectFields: Map<FieldId, DistributedField>
        get() = TODO("Not yet implemented")
}

interface TransformScope {
    fun divide(divisor: Float)

    fun modulo(modulus: Float)
}

fun Double.unTransform(
    type: FieldValue.Type,
    block: TransformScope.() -> Unit,
): FieldValue {
    var value = this
    object : TransformScope {
        override fun divide(divisor: Float) {
            value *= divisor
        }

        override fun modulo(modulus: Float) {
            value %= modulus
        }
    }.block()

    return when (type) {
        FieldValue.Type.Float64 -> value.toFieldValue()
        FieldValue.Type.Int8 -> value.toInt().toByte().toFieldValue()
        FieldValue.Type.Int16 -> value.toInt().toShort().toFieldValue()
        FieldValue.Type.Int32 -> value.toInt().toFieldValue()
        FieldValue.Type.Int64 -> value.toLong().toFieldValue()
        FieldValue.Type.UInt8 -> value.toUInt().toUByte().toFieldValue()
        FieldValue.Type.UInt16 -> value.toUInt().toUShort().toFieldValue()
        FieldValue.Type.UInt32 -> value.toUInt().toFieldValue()
        FieldValue.Type.UInt64 -> value.toULong().toFieldValue()
        else -> throw Throwable("cannot untransform to $type")
    }
}

fun UByte.transform(block: TransformScope.() -> Unit) = toULong().transform(block)

fun UShort.transform(block: TransformScope.() -> Unit) = toULong().transform(block)

fun UInt.transform(block: TransformScope.() -> Unit) = toULong().transform(block)

fun ULong.transform(block: TransformScope.() -> Unit): Double {
    var value = toDouble()
    object : TransformScope {
        override fun divide(divisor: Float) {
            value /= divisor
        }

        override fun modulo(modulus: Float) {
            value %= modulus
        }
    }.block()
    return value
}

fun Byte.transform(block: TransformScope.() -> Unit) = toLong().transform(block)

fun Short.transform(block: TransformScope.() -> Unit) = toLong().transform(block)

fun Int.transform(block: TransformScope.() -> Unit) = toLong().transform(block)

fun Long.transform(block: TransformScope.() -> Unit): Double {
    var value = toDouble()
    object : TransformScope {
        override fun divide(divisor: Float) {
            value /= divisor
        }

        override fun modulo(modulus: Float) {
            value %= modulus
        }
    }.block()

    return value
}

suspend fun main() {
    val classSpecRepository =
        ClassSpecRepository.build {
            dclass {
                field(FieldValue.Type.UInt32) {
                    required()
                    broadcast()
                }
                field(FieldValue.Type.UInt32) {
                    required()
                    broadcast()
                }
                field(FieldValue.Type.UInt32) {
                    required()
                    broadcast()
                }
                field(FieldValue.Type.UInt32) {
                    required()
                    broadcast()
                }
            }

            dclass {
                field(FieldValue.Type.Tuple(FieldValue.Type.String, FieldValue.Type.String)) {
                    clsend()
                }
                field(FieldValue.Type.String)
            }

            dclass {}
        }

    setupAstronInternalRepository(
        classSpecRepository,
        AstronInternalRepositoryConfig(
            "127.0.0.1:7199",
            10000U.toChannelId(),
            4002U.toChannelId(),
        ),
    )

    MainScope().launch {
        internalRepository.astronInternalNetwork.networkMessages.collect {
            println(it)
        }
    }

    // internalRepository.registerClass(Block.dClassId, BlockAI::class, BlockClient::class)
    // internalRepository.registerClass(
    //    LoginManager.dClassId,
    //    LoginManagerUD::class,
    //    LoginManagerClient::class,
    // )

    internalRepository.launch()

    internalRepository.createDO(
        1000U.toDOId(),
        0U,
        1000U.toZoneId(),
        2U.toDClassId(),
        listOf(0U.toFieldValue(), 0U.toFieldValue(), 0U.toFieldValue()),
    )

    internalRepository.createDO(
        5000U.toDOId(),
        1000U,
        1000U.toZoneId(),
        1U.toDClassId(),
        listOf(0U.toFieldValue(), 0U.toFieldValue(), 0U.toFieldValue(), 0U.toFieldValue()),
    )

    println("astron launched")
}
