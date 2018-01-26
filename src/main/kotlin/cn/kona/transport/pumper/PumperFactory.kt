package cn.kona.transport.pumper

interface PumperFactory<out T> where T : BytePumper {
    fun create(): T
}