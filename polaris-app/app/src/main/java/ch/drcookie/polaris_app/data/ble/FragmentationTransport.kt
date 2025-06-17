package ch.drcookie.polaris_app.data.ble

import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

@OptIn(ExperimentalUnsignedTypes::class)
class FragmentationTransport {

    companion object{
        const val TAG = "FragTransport"
        enum class ReassemblyState { IDLE, REASSEMBLING }
    }


    // --- State for Reassembly (Incoming Data) ---
    private var reassemblyState = ReassemblyState.IDLE
    private var reassemblyBuffer = mutableListOf<UByte>()
    private var currentInTransactionId: UByte = 0u

    // Use a Flow to emit fully reassembled messages. Replay=0 and extraBuffer=1 makes it behave like a channel.
    private val _reassembledMessages = MutableSharedFlow<UByteArray>(0, 1, BufferOverflow.DROP_OLDEST)
    val reassembledMessages = _reassembledMessages.asSharedFlow()

    // --- State for Fragmentation (Outgoing Data) ---
    private var maxChunkPayloadSize: Int = 20 // Default: MTU(23) - GATT(3) - ALFP(1)
    private val outgoingTransactionId = AtomicInteger(0)

    fun onMtuChanged(newMtu: Int) {
        maxChunkPayloadSize = newMtu - 3 - Alfp.HEADER_SIZE
        Log.i(TAG, "MTU updated to $newMtu, max chunk payload is now $maxChunkPayloadSize bytes.")
    }

    suspend fun process(chunkData: ByteArray) {
        val uChunk = chunkData.toUByteArray()
        if (uChunk.size < Alfp.HEADER_SIZE) {
            Log.w(TAG, "Chunk too small (${uChunk.size} bytes), ignoring.")
            return
        }

        val header = uChunk[0]
        val payload = uChunk.sliceArray(Alfp.HEADER_SIZE until uChunk.size)
        val packetType = header and Alfp.MASK_TYPE
        val transactionId = header and Alfp.MASK_TRANSACTION_ID

        when (packetType) {
            Alfp.FLAG_UNFRAGMENTED -> {
                Log.d(TAG, "Received unfragmented message (${payload.size} bytes).")
                _reassembledMessages.emit(payload)
            }
            Alfp.FLAG_START -> {
                if (reassemblyState == ReassemblyState.REASSEMBLING) {
                    Log.w(TAG, "Received START while already reassembling. Starting over.")
                }
                resetReassembly()
                reassemblyState = ReassemblyState.REASSEMBLING
                currentInTransactionId = transactionId
                reassemblyBuffer.addAll(payload)
            }
            Alfp.FLAG_MIDDLE -> {
                if (reassemblyState != ReassemblyState.REASSEMBLING || transactionId != currentInTransactionId) {
                    Log.e(TAG, "Received out-of-sequence MIDDLE packet. Ignoring.")
                    resetReassembly()
                    return
                }
                reassemblyBuffer.addAll(payload)
            }
            Alfp.FLAG_END -> {
                if (reassemblyState != ReassemblyState.REASSEMBLING || transactionId != currentInTransactionId) {
                    Log.e(TAG, "Received out-of-sequence END packet. Ignoring.")
                    resetReassembly()
                    return
                }
                reassemblyBuffer.addAll(payload)
                Log.i(TAG, "Reassembly complete. Total size: ${reassemblyBuffer.size} bytes.")
                _reassembledMessages.emit(reassemblyBuffer.toUByteArray())
                resetReassembly()
            }
            else -> Log.e(TAG, "Unknown packet type in ALFP header. Ignoring.")
        }
    }

    fun fragment(fullMessageData: ByteArray): Sequence<ByteArray> {
        val uMessage = fullMessageData.toUByteArray()
        val transactionId = (outgoingTransactionId.getAndIncrement() and Alfp.MASK_TRANSACTION_ID.toInt()).toUByte()

        if (uMessage.size <= maxChunkPayloadSize) {
            Log.d(TAG, "Sending unfragmented message of ${uMessage.size} bytes.")
            return sequenceOf(
                ubyteArrayOf(Alfp.FLAG_UNFRAGMENTED or transactionId)
                    .plus(uMessage)
                    .asByteArray()
            )
        }

        Log.i(TAG, "Fragmenting message of ${uMessage.size} bytes into chunks of max $maxChunkPayloadSize.")
        return sequence {
            var bytesSent = 0
            var isFirst = true
            while (bytesSent < uMessage.size) {
                val chunkSize = min(maxChunkPayloadSize, uMessage.size - bytesSent)
                val payloadChunk = uMessage.sliceArray(bytesSent until bytesSent + chunkSize)

                val packetType = when {
                    isFirst -> Alfp.FLAG_START
                    (bytesSent + chunkSize >= uMessage.size) -> Alfp.FLAG_END
                    else -> Alfp.FLAG_MIDDLE
                }
                isFirst = false

                val header = packetType or transactionId
                val packet = ubyteArrayOf(header).plus(payloadChunk).asByteArray()
                Log.d(TAG, "Yielding chunk type ${packetType.toString(16)}, size ${packet.size}")
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