package patterning.util

import java.util.LinkedList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AsyncJobRunner(
    val rateWindowSeconds: Int = RATE_PER_SECOND_WINDOW,
    val method: suspend () -> Unit,
    threadName: String
) {
    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, threadName) }
    private val singleThreadContext = executor.asCoroutineDispatcher()

    private val mutex = Mutex()
    private var atomicIsRunning = AtomicBoolean(false)
    private val handlerScope = CoroutineScope(singleThreadContext)
    private var calculationJob: Job? = null
    private var timestamps = LinkedList<Long>()
    private var timestampsSnapshot = LinkedList<Long>()
    private val rateWindow = rateWindowSeconds * 1000 // milliseconds

    val isRunning: Boolean
        get() = atomicIsRunning.get()

    fun startCalculation() {
        synchronized(this) {
            if (atomicIsRunning.get()) {
                return
            }
            atomicIsRunning.set(true)
            calculationJob = handlerScope.launch {
                runCalculation()
            }
        }
    }

    private suspend fun runCalculation() {
        method()
        synchronized(this) {
            timestamps.add(System.currentTimeMillis())
            timestampsSnapshot = LinkedList(timestamps)
        }
        atomicIsRunning.set(false)
    }

    fun cancelAndWait() {
        runBlocking {
            mutex.withLock {
                calculationJob?.cancelAndJoin()
                atomicIsRunning.set(false)
                calculationJob = null
                timestamps.clear()
                timestampsSnapshot.clear()
            }
        }
    }

    fun shutdown() {
        executor.shutdown()
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