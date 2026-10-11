package api;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.concurrent.atomic.AtomicBoolean;

public class ResultReporter implements TestWatcher, BeforeAllCallback {

    private static final String SEED_KEY = "junit.jupiter.execution.order.random.seed";
    private static final AtomicBoolean seedPrinted = new AtomicBoolean(false);

    @Override
    public void beforeAll(ExtensionContext context) {
        if (seedPrinted.compareAndSet(false, true)) {
            String seed = context.getConfigurationParameter(SEED_KEY).orElse("not set");
            System.out.println("Random order seed: " + seed);
        }
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        System.out.println("    result: PASS");
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        System.out.println("    result: FAIL - " + firstLine(cause.getMessage()));
    }

    private String firstLine(String message) {
        if (message == null) {
            return "no message";
        }
        return message.lines().findFirst().orElse(message);
    }
}
