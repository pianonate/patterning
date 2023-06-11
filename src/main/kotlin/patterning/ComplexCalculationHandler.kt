package patterning

import java.util.concurrent.locks.ReentrantLock
import java.util.function.BiFunction

class ComplexCalculationHandler<P>(private val method: BiFunction<P, Void?, Void>) {
    var isCalculationInProgress = false
        private set
    private var parameter: P? = null

    fun startCalculation(parameter: P) {
        lock()
        try {
            if (isCalculationInProgress) {
                return
            }
            isCalculationInProgress = true
            this.parameter = parameter
            Thread(ComplexCalculationTask()).start()
        } finally {
            unlock()
        }
    }

    private inner class ComplexCalculationTask : Runnable {
        override fun run() {
            parameter?.let {
                method.apply(it, null)
            }
            lock()
            try {
                isCalculationInProgress = false
            } finally {
                unlock()
            }
        }
    }

    companion object {
        private val lock = ReentrantLock()

        @JvmStatic
        fun lock() {
            lock.lock()
        }

        @JvmStatic
        fun unlock() {
            lock.unlock()
        }
    }
}