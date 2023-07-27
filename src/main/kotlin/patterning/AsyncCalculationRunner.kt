package patterning

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class AsyncCalculationRunner(
    val rateWindowSeconds: Int,
    val method: suspend () -> Unit
) {
    private val mutex = Mutex()
    private var atomicIsRunning = AtomicBoolean(false)
    private val handlerScope = CoroutineScope(Dispatchers.Default)
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

    fun getRate(): Float {
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - rateWindow
        while (timestampsSnapshot.isNotEmpty() && timestampsSnapshot.peek() < cutoffTime) {
            timestampsSnapshot.poll()
        }
        return timestampsSnapshot.size.toFloat() / rateWindowSeconds
    }
}