package cn.kona.transport.impl

import cn.kona.transport.TaskWeaver
import java.util.concurrent.LinkedBlockingQueue

class QueueTaskWeaver : TaskWeaver {

    companion object {
        const val MAX_PENDING_TASK = 16
    }

    private val taskQueue = LinkedBlockingQueue<Runnable>(MAX_PENDING_TASK)

    override fun addTask(task: Runnable) {
       taskQueue.offer(task)
    }

    override fun hasTask() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeTask(task: Runnable) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun takeTask(): List<Runnable> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addTimerTask(period: Long, task: Runnable, repeat: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}