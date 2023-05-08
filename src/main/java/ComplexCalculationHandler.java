import java.util.function.Consumer;
import java.util.function.Supplier;

public class ComplexCalculationHandler<R> {
    private boolean calculationInProgress = false;
    private Consumer<R> callback;
    private Supplier<R> calculationMethod;

    public ComplexCalculationHandler(Supplier<R> calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public void startCalculation(Consumer<R> callback) {
        if (calculationInProgress) {
            return;
        }

        calculationInProgress = true;
        this.callback = callback;
        new Thread(new ComplexCalculationTask(this::onCalculationComplete)).start();
    }

    public boolean isCalculationInProgress() {
        return calculationInProgress;
    }

    private void onCalculationComplete(R result) {
        calculationInProgress = false;
        callback.accept(result);
    }

    private class ComplexCalculationTask implements Runnable {
        private final Consumer<R> callback;

        ComplexCalculationTask(Consumer<R> callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            R result = calculationMethod.get();
            callback.accept(result);
        }
    }
}