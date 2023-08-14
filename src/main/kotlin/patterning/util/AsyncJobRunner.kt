package patterning.util

import java.util.LinkedList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AsyncJobRunner(
    private val rateWindowSeconds: Int = RATE_PER_SECOND_WINDOW,
    val method: suspend () -> Unit,
) {
    
    
    private val mutex = Mutex()
    private val handlerScope = CoroutineScope(Job())
    private var job: Job = Job().apply { cancel() }
    private var timestamps = LinkedList<Long>()
    private var timestampsSnapshot = LinkedList<Long>()
    private val rateWindow = rateWindowSeconds * 1000 // milliseconds
    
    val isActive: Boolean
        get() = job.isActive
    
    fun startJob() {
        if (job.isActive) {
            return
        }
        job = handlerScope.launch {
            runJob(job)
        }
    }
    
    private suspend fun runJob(job: Job) {
        if (job.isActive) {
            method()
            mutex.withLock {
                timestamps.add(System.currentTimeMillis())
                timestampsSnapshot = LinkedList(timestamps)
            }
        }
    }
    
    
    fun cancelAndWait() {
        runBlocking {
            mutex.withLock {
                if (job.isActive) {
                    job.cancelAndJoin()
                }
                timestamps.clear()
                timestampsSnapshot.clear()
            }
        }
    }
    
    fun getRate(): Float {
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - rateWindow
        while (timestampsSnapshot.isNotEmpty() && timestampsSnapshot.peek() < cutoffTime) {
            timestampsSnapshot.poll()
        }
        return timestampsSnapshot.size.toFloat() / rateWindowSeconds
    }
    
    companion object {
        private const val RATE_PER_SECOND_WINDOW = 1 // how big is your window to calculate the rate?
    }
}