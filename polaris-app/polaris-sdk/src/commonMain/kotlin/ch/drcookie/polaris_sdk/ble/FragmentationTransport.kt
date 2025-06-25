package ch.drcookie.polaris_sdk.ble

import ch.drcookie.polaris_sdk.ble.util.FragmentationHeader
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.atomicfu.atomic
import kotlin.math.min

private val Log = KotlinLogging.logger {}

@OptIn(ExperimentalUnsignedTypes::class)
internal class FragmentationTransport {

    internal companion object {
        enum class ReassemblyState { IDLE, REASSEMBLING }
    }

    // --- State for Reassembly (Incoming Data) ---
    private var reassemblyState = ReassemblyState.IDLE
    private var reassemblyBuffer = mutableListOf<UByte>()
    private var currentInTransactionId: UByte = 0u

    // Use a Flow to emit fully reassembled messages. Replay=0 and extraBuffer=1 makes it behave like a channel.
    private val _reassembledMessages = MutableSharedFlow<UByteArray>(0, 1, BufferOverflow.DROP_OLDEST)
    internal val reassembledMessages = _reassembledMessages.asSharedFlow()

    // --- State for Fragmentation (Outgoing Data) ---
    private var maxChunkPayloadSize: Int = 20 // Default: MTU(23) - GATT(3) - frag header(1)
    private val outgoingTransactionId = atomic(0)

    internal fun onMtuChanged(newMtu: Int) {
        maxChunkPayloadSize = newMtu - 3 - FragmentationHeader.HEADER_SIZE
        Log.info { "MTU updated to $newMtu, max chunk payload is now $maxChunkPayloadSize bytes." }
    }

    internal suspend fun process(chunkData: ByteArray) {
        val uChunk = chunkData.toUByteArray()
        if (uChunk.size < FragmentationHeader.HEADER_SIZE) {
            Log.warn { "Chunk too small (${uChunk.size} bytes), ignoring." }
            return
        }

        val header = uChunk[0]
        val payload = uChunk.sliceArray(FragmentationHeader.HEADER_SIZE until uChunk.size)
        val packetType = header and FragmentationHeader.MASK_TYPE
        val transactionId = header and FragmentationHeader.MASK_TRANSACTION_ID

        when (packetType) {
            FragmentationHeader.FLAG_UNFRAGMENTED -> {
                Log.debug { "Received unfragmented message (${payload.size} bytes)." }
                _reassembledMessages.emit(payload)
            }

            FragmentationHeader.FLAG_START -> {
                if (reassemblyState == ReassemblyState.REASSEMBLING) {
                    Log.warn { "Received START while already reassembling. Starting over." }
                }
                resetReassembly()
                reassemblyState = ReassemblyState.REASSEMBLING
                currentInTransactionId = transactionId
                reassemblyBuffer.addAll(payload)
            }

            FragmentationHeader.FLAG_MIDDLE -> {
                if (reassemblyState != ReassemblyState.REASSEMBLING || transactionId != currentInTransactionId) {
                    Log.error { "Received out-of-sequence MIDDLE packet. Ignoring." }
                    resetReassembly()
                    return
                }
                reassemblyBuffer.addAll(payload)
            }

            FragmentationHeader.FLAG_END -> {
                if (reassemblyState != ReassemblyState.REASSEMBLING || transactionId != currentInTransactionId) {
                    Log.error { "Received out-of-sequence END packet. Ignoring." }
                    resetReassembly()
                    return
                }
                reassemblyBuffer.addAll(payload)
                Log.info { "Reassembly complete. Total size: ${reassemblyBuffer.size} bytes." }
                _reassembledMessages.emit(reassemblyBuffer.toUByteArray())
                resetReassembly()
            }

            else -> Log.error { "Unknown packet type in fragmentation header. Ignoring." }
        }
    }

    internal fun fragment(fullMessageData: ByteArray): Sequence<ByteArray> {
        val uMessage = fullMessageData.toUByteArray()
        val transactionId =
            (outgoingTransactionId.getAndIncrement() and FragmentationHeader.MASK_TRANSACTION_ID.toInt()).toUByte()

        if (uMessage.size <= maxChunkPayloadSize) {
            Log.debug { "Sending unfragmented message of ${uMessage.size} bytes." }
            return sequenceOf(
                ubyteArrayOf(FragmentationHeader.FLAG_UNFRAGMENTED or transactionId)
                    .plus(uMessage)
                    .asByteArray()
            )
        }

        Log.info { "Fragmenting message of ${uMessage.size} bytes into chunks of max $maxChunkPayloadSize." }
        return sequence {
            var bytesSent = 0
            var isFirst = true
            while (bytesSent < uMessage.size) {
                val chunkSize = min(maxChunkPayloadSize, uMessage.size - bytesSent)
                val payloadChunk = uMessage.sliceArray(bytesSent until bytesSent + chunkSize)

                val packetType = when {
                    isFirst -> FragmentationHeader.FLAG_START
                    (bytesSent + chunkSize >= uMessage.size) -> FragmentationHeader.FLAG_END
                    else -> FragmentationHeader.FLAG_MIDDLE
                }
                isFirst = false

                val header = packetType or transactionId
                val packet = ubyteArrayOf(header).plus(payloadChunk).asByteArray()
                Log.debug { "Yielding chunk type ${packetType.toString(16)}, size ${packet.size}" }
                yield(packet)
                bytesSent += chunkSize
            }
        }
    }

    private fun resetReassembly() {
        reassemblyBuffer.clear()
        reassemblyState = ReassemblyState.IDLE
    }
}