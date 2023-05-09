import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class ComplexCalculationHandler<P> {
    private boolean calculationInProgress = false;
    private BiConsumer<P, Void> callback;
    private BiFunction<P, Void, Void> calculationMethod;
    private P parameter;

    public ComplexCalculationHandler(BiFunction<P, Void, Void> calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public void startCalculation(P parameter, BiConsumer<P, Void> callback) {
        if (calculationInProgress) {
            return;
        }

        calculationInProgress = true;
        this.parameter = parameter;
        this.callback = callback;
        new Thread(new ComplexCalculationTask((p, v) -> onCalculationComplete(p, v))).start();
    }

    public boolean isCalculationInProgress() {
        return calculationInProgress;
    }

    private void onCalculationComplete(P parameter, Void result) {
        calculationInProgress = false;
        callback.accept(parameter, result);
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
