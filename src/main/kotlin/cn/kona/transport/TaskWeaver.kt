package cn.kona.transport

interface TaskWeaver {
    fun addTask(task: Runnable)
    fun hasTask()
    fun removeTask(task: Runnable)
    fun takeTask(): List<Runnable>
    fun addTimerTask(period: Long, task: Runnable, repeat: Boolean)
}