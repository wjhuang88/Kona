package cn.kona

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer

@Suppress("unused")
inline fun <reified T> T.getLogger(): Logger = LoggerFactory.getLogger(T::class.java)

fun ByteBuffer.clone(): ByteBuffer = ByteBuffer.allocate(this.remaining()).put(this)

inline fun <T> MutableIterable<T>.forEachAndRemove(action: (T) -> Unit) = this.iterator().let {
    while (it.hasNext()) {
        val item = it.next()
        it.remove()
        action(item)
    }
}

var systemCharset = Charsets.UTF_8

class MultiValuesMap<K, V> : LinkedHashMap<K, MutableList<V>> {

    constructor() : super()
    constructor(initialCapacity: Int, loadFactor: Float) : super(initialCapacity, loadFactor)
    constructor(initialCapacity: Int) : super(initialCapacity)
    constructor(m: MutableMap<out K, out MutableList<V>>?) : super(m)
    constructor(initialCapacity: Int, loadFactor: Float, accessOrder: Boolean) : super(initialCapacity, loadFactor, accessOrder)

    fun putValue(key: K, value: V) {
        if (containsKey(key)) {
            get(key)?.add(value)
        } else {
            put(key, mutableListOf(value))
        }
    }

    fun getFirst(key: K): V? {
        val values = get(key)
        return values?.get(0)
    }
}
