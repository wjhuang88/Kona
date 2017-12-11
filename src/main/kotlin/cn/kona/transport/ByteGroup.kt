package cn.kona.transport

import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList

/**
 * Instance of this class could combine a set of [ByteArray]s. You can push [Byte] to
 * it without worry about the container size(will automatically increase)
 *
 * @property pool [ByteArray] container
 * @property index the index now of [ByteArray] pool
 * @property count byte count
 *
 * @author HuangWj
 */
class ByteGroup {

    companion object {
        const val BUCKET_SIZE = 1024
    }

    private var index = 0
    private var count = 0

    private val pool: MutableList<ByteArray> = ArrayList()

    fun put(byte: Byte) {
        if (pool.size < index + 1) {
            pool.add(index, ByteArray(BUCKET_SIZE))
        }
        val thisBuffer = pool[index]
        val offset = count % BUCKET_SIZE
        thisBuffer[offset] = byte
        count++
        if (offset + 1 >= BUCKET_SIZE) {
            index++
        }
    }

    fun available(): Int {
        return count
    }

    operator fun get(i: Int): Byte {
        return when {
            pool.isEmpty()
                -> throw NoSuchElementException("byte group pool is empty")
            i < BUCKET_SIZE
                -> pool[0][i]
            else
                -> pool[i / BUCKET_SIZE][i % BUCKET_SIZE]
        }
    }

    operator fun iterator(): Iterator<Byte> {
        return object : Iterator<Byte> {
            private var offset = 0

            override fun hasNext(): Boolean {
                return offset < count
            }

            override fun next(): Byte {
                if (!hasNext())
                    throw NoSuchElementException("Cannot find next byte in this group")
                return get(offset++)
            }
        }
    }

    fun clear() {
        // TODO: Maybe there's a memory leak if [pool] increased?
        index = 0
        count = 0
    }

    fun flush(): ByteBuffer {
        when {
            pool.isEmpty() -> {
                clear()
                return ByteBuffer.allocate(0)
            }
            0 == index -> {
                val thisArr = pool[0]
                val buffer = ByteBuffer.wrap(Arrays.copyOf(thisArr, count), 0, count)
                clear()
                return buffer
            }
            else -> {
                val firstArr = Arrays.copyOf(pool[0], BUCKET_SIZE * (index + 1))
                for (i in 1..index) {
                    val thisBytes = pool[i]
                    System.arraycopy(thisBytes, 0, firstArr, BUCKET_SIZE * i, thisBytes.size)
                }
                val buffer = ByteBuffer.wrap(firstArr, 0, count)
                clear()
                return buffer
            }
        }
    }
}