package patterning

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicBoolean

class ComplexCalculationHandler<P>(private val method: suspend (P) -> Unit) {
    private var calculationInProgress = AtomicBoolean(false)
    private var parameter: P? = null
    private val handlerScope = CoroutineScope(Dispatchers.Default)
    private var calculationJob: Job? = null

    val isCalculationInProgress: Boolean
        get() = calculationInProgress.get()

    fun startCalculation(parameter: P) {
        synchronized(this) {
            if (calculationInProgress.get()) {
                return
            }
            this.parameter = parameter
            calculationInProgress.set(true)
            calculationJob = handlerScope.launch {
                runCalculation()
            }
        }
    }

    private suspend fun runCalculation() {
        parameter?.let {
            method(it)
            calculationInProgress.set(false)
        }
    }

    // maybe you want to cancel on instantiating new life...
/*    fun cancel() {
        calculationJob?.cancel()
        calculationInProgress.set(false)
    }*/

    companion object {
        private val mutex = Mutex()

        suspend fun lock() {
            mutex.lock()
        }

        fun unlock() {
            mutex.unlock()
        }
    }
}