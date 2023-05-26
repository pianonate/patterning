package patterning;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

public class ComplexCalculationHandler<P> {

    // ensure that only one calculation can be in progress at a time
    private static final ReentrantLock lock = new ReentrantLock();

    public static void lock() {
        lock.lock();
    }

    public static void unlock() {
        lock.unlock();
    }

    private boolean calculationInProgress = false;
    private final BiFunction<P, Void, Void> calculationMethod;
    private P parameter;

    public ComplexCalculationHandler(BiFunction<P, Void, Void> calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public void startCalculation(P parameter) {
        lock.lock();
        try {
            if (calculationInProgress) {
                return;
            }
            calculationInProgress = true;
            this.parameter = parameter;
            new Thread(new ComplexCalculationTask()).start();
        } finally {
            lock.unlock();
        }
    }

    public boolean isCalculationInProgress() {
        return calculationInProgress;
    }

    private class ComplexCalculationTask implements Runnable {
        @Override
        public void run() {
            Void result = calculationMethod.apply(parameter, null);
            lock.lock();
            try {
                calculationInProgress = false;
            } finally {
                lock.unlock();
            }
        }
    }
}
