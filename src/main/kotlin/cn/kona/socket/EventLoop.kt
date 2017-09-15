package cn.kona.socket

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async

interface EventLoop : Runnable {
    fun loop() {
        while (!Thread.interrupted()) {
            async(CommonPool) {
                run()
            }
        }
    }
}