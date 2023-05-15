import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
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
    private BiConsumer<P, Void> callback;
    private final BiFunction<P, Void, Void> calculationMethod;
    private P parameter;


    public ComplexCalculationHandler(BiFunction<P, Void, Void> calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public void startCalculation(P parameter, BiConsumer<P, Void> callback) {
        lock.lock();
        try {
            if (calculationInProgress) {
                return;
            }
            calculationInProgress = true;
            this.parameter = parameter;
            this.callback = callback;
            new Thread(new ComplexCalculationTask((p, v) -> onCalculationComplete(p, v))).start();
        } finally {
            lock.unlock();
        }
    }

    public boolean isCalculationInProgress() {
        return calculationInProgress;
    }

    private void onCalculationComplete(P parameter, Void result) {
        lock.lock();
        try {
            calculationInProgress = false;
            callback.accept(parameter, result);
        } finally {
            lock.unlock();
        }
    }

    private class ComplexCalculationTask implements Runnable {
        private final BiConsumer<P, Void> callback;

        ComplexCalculationTask(BiConsumer<P, Void> callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            Void result = calculationMethod.apply(parameter, null);
            callback.accept(parameter, result);
        }
    }
}
