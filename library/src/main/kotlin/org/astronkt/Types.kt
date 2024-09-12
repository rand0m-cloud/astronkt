package org.astronkt

data class ZoneId(val id: UInt)

fun UInt.toZoneId() = ZoneId(this)
fun ZoneId.toFieldValue() = id.toFieldValue()

class ChannelId(val id: ULong) {
    companion object {
        val ZERO = ChannelId(0UL)
        val CONTROL = ChannelId(1UL)
    }

    override fun toString(): String = "$id"
    override fun equals(other: Any?): Boolean = id == (other as ChannelId?)?.id
}

data class InterestId(val id: UShort)

fun UInt.toInterestId() = toUShort().toInterestId()
fun UShort.toInterestId() = InterestId(this)

fun UInt.toChannelId() = toULong().toChannelId()
fun Long.toChannelId() = toULong().toChannelId()
fun ULong.toChannelId() = ChannelId(this)

fun ChannelId.toFieldValue() = id.toFieldValue()
