package api;

import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Dynamic analysis of the /todos endpoints: for a growing number of todos already stored,
 * measures the time of POST (add), POST /:id (update) and DELETE /:id (delete) together with
 * CPU use and free memory. BaseApiTest saves and restores the system state around each test.
 */
@Tag("performance")
class TodoPerformanceTest extends BaseApiTest {

    private static final String ENTITY = "todo";
    private static final int[] OBJECT_COUNTS = {
            1, 5, 10, 50, 75, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000,
            2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000};
    private static final int OPERATIONS_PER_MEASUREMENT = 100;
    private static final int WARM_UP_REQUESTS = 300;
    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;

    private final PerformanceRecorder recorder = new PerformanceRecorder();
    private final List<String> storedTodoIds = new ArrayList<>();
    private long experimentStartNanos;
    private long lastSetupMillis;

    @Test
    void addTodoTimeAsNumberOfTodosGrows() {
        runExperiment("add", targetCount -> {
            populateTo(targetCount);
            return measure("add", targetCount, STATUS_CREATED,
                    iteration -> api.postJson("/todos", RandomTodoData.todoPayload()),
                    response -> storedTodoIds.add(response.jsonPath().getString("id")));
        });
        report("todo_add.csv");
    }

    @Test
    void updateTodoTimeAsNumberOfTodosGrows() {
        runExperiment("update", targetCount -> {
            populateTo(targetCount);
            List<String> targets = new ArrayList<>(storedTodoIds);
            return measure("update", targetCount, STATUS_OK,
                    iteration -> api.postJson("/todos/" + targets.get(iteration % targets.size()),
                            RandomTodoData.todoPayload()),
                    response -> { });
        });
        report("todo_update.csv");
    }

    @Test
    void deleteTodoTimeAsNumberOfTodosGrows() {
        runExperiment("delete", targetCount -> {
            populateTo(targetCount + OPERATIONS_PER_MEASUREMENT);
            List<String> victims = new ArrayList<>(storedTodoIds.subList(0, OPERATIONS_PER_MEASUREMENT));
            Measurement measurement = measure("delete", targetCount, STATUS_OK,
                    iteration -> api.delete("/todos/" + victims.get(iteration)),
                    response -> { });
            storedTodoIds.removeAll(victims);
            return measurement;
        });
        report("todo_delete.csv");
    }

    private void runExperiment(String operation, IntFunction<Measurement> measureAt) {
        System.out.println("    experiment: " + operation);
        warmUp();
        experimentStartNanos = System.nanoTime();
        for (int targetCount : OBJECT_COUNTS) {
            recorder.record(measureAt.apply(targetCount));
        }
    }

    private void report(String fileName) {
        Path file = recorder.writeCsv(fileName);
        System.out.println("    results written to " + file.toAbsolutePath());
    }

    private void warmUp() {
        for (int i = 0; i < WARM_UP_REQUESTS; i++) {
            String id = api.createTodo(RandomTodoData.title());
            api.delete("/todos/" + id);
        }
    }

    private void populateTo(int targetCount) {
        long start = System.nanoTime();
        adjustPopulation(targetCount);
        lastSetupMillis = nanosToMillis(System.nanoTime() - start);
    }

    private void adjustPopulation(int targetCount) {
        while (storedTodoIds.size() < targetCount) {
            storedTodoIds.add(api.createTodo(RandomTodoData.title(), RandomTodoData.doneStatus(),
                    RandomTodoData.description()));
        }
        while (storedTodoIds.size() > targetCount) {
            String id = storedTodoIds.remove(storedTodoIds.size() - 1);
            api.delete("/todos/" + id);
        }
    }

    private Measurement measure(String operation, int objectCount, int expectedStatus,
                                IntFunction<Response> request, Consumer<Response> afterEachRequest) {
        List<Double> durations = new ArrayList<>();
        ResourceSampler sampler = new ResourceSampler();
        sampler.start();
        ResourceUsage usage;
        try {
            for (int i = 0; i < OPERATIONS_PER_MEASUREMENT; i++) {
                long start = System.nanoTime();
                Response response = request.apply(i);
                durations.add((System.nanoTime() - start) / 1_000_000.0);
                assertEquals(expectedStatus, response.getStatusCode(), operation + " status code");
                afterEachRequest.accept(response);
            }
        } finally {
            usage = sampler.stop();
        }
        return new Measurement(ENTITY, operation, objectCount, lastSetupMillis,
                nanosToMillis(System.nanoTime() - experimentStartNanos), durations, usage);
    }

    private long nanosToMillis(long nanos) {
        return nanos / 1_000_000L;
    }
}
