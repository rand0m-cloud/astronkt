@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.astronkt.server.app

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.astronkt.*
import org.astronkt.internal.AstronInternalRepositoryConfig

@Suppress("ClassName")
data class bool(val value: UByte) {
    companion object : ToFieldValue<bool> {
        override val type: FieldValue.Type = FieldValue.Type.UInt8

        override fun fromFieldValue(value: FieldValue): bool {
            val inner = value.toUInt8()!!
            return bool(inner)
        }

        override fun bool.toFieldValue(): FieldValue = value.toFieldValue()
    }
}

data class DoId(val value: UInt) {
    companion object : ToFieldValue<DoId> {
        override val type: FieldValue.Type = FieldValue.Type.UInt32

        override fun fromFieldValue(value: FieldValue): DoId {
            val inner = value.toUInt32()!!
            return DoId(inner)
        }

        override fun DoId.toFieldValue(): FieldValue = value.toFieldValue()
    }

}

data class AvatarPendingDel(val Avatar: UInt, val date: UInt) {
    companion object : ToFieldValue<AvatarPendingDel> {
        override fun fromFieldValue(value: FieldValue): AvatarPendingDel {
            val tuple = value.toTuple()!!
            val Avatar = tuple[0].toUInt32()!!
            val date = tuple[1].toUInt32()!!
            return AvatarPendingDel(Avatar, date)
        }

        override val type: FieldValue.Type = FieldValue.Type.Tuple(FieldValue.Type.UInt32, FieldValue.Type.UInt32)

        override fun AvatarPendingDel.toFieldValue(): FieldValue =
            FieldValue.TupleValue(Avatar.toFieldValue(), date.toFieldValue())
    }


}

open class TestClass(doId: DOId) : DistributedObjectBase(doId, 3U.toDClassId()) {
    var x: List<AvatarPendingDel>
        get() =
            getField(1U.toFieldId())?.toList()!!.map { AvatarPendingDel.fromFieldValue(it) }
        set(value) =
            setField(
                1U.toFieldId(),
                with(AvatarPendingDel) { value.toFieldValue() }
            )
    override val objectFields: Map<FieldId, DistributedField>
        get() = TODO("Not yet implemented")
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
