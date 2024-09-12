package Utils.org.astronkt

import java.nio.ByteBuffer

fun ByteBuffer.getRemaining(): ByteArray = ByteArray(remaining()).also { get(it) }