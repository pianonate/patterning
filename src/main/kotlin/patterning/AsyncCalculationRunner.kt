package patterning

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class AsyncCalculationRunner<P>(
    val rateWindowSeconds: Int,
    val method: suspend (P) -> Unit
) {
    private val mutex = Mutex()

    private var atomicIsRunning = AtomicBoolean(false)
    private var parameter: P? = null
    private val handlerScope = CoroutineScope(Dispatchers.Default)
    private var calculationJob: Job? = null
    private var timestamps = LinkedList<Long>()
    private var timestampsSnapshot = LinkedList<Long>()
    private val rateSeconds = rateWindowSeconds
    private val rateWindow = rateWindowSeconds * 1000 // milliseconds

    val isRunning: Boolean
        get() = atomicIsRunning.get()

    fun startCalculation(parameter: P) {
        synchronized(this) {
            if (atomicIsRunning.get()) {
                return
            }
            this.parameter = parameter
            atomicIsRunning.set(true)
            calculationJob = handlerScope.launch {
                runCalculation()
            }
        }
    }

    private suspend fun runCalculation() {
        parameter?.let {
            method(it)
            synchronized(this) {
                timestamps.add(System.currentTimeMillis())
                timestampsSnapshot = LinkedList(timestamps)
            }
            atomicIsRunning.set(false)
        }
    }

    fun cancelAndWait() {
        runBlocking {
            mutex.withLock {
                calculationJob?.cancelAndJoin()
                calculationJob = null
                timestamps.clear()
                timestampsSnapshot.clear()
            }
        }
    }

    fun getRate(): Float {
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - rateWindow
        // use the copy made in runCalculation - if runCalculation happens to
        // create a new invocationTimeStampsSnapshot, that's okay as you'll be looking
        // at the last known good one
        while (timestampsSnapshot.isNotEmpty() && timestampsSnapshot.peek() < cutoffTime) {
            timestampsSnapshot.poll()
        }
        return timestampsSnapshot.size.toFloat() / rateSeconds
    }
}