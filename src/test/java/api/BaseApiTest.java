package api;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(ResultReporter.class)
public abstract class BaseApiTest {

    protected final TodoApi api = new TodoApi();
    private SystemSnapshot initialState;

    @BeforeEach
    void prepareSystem(TestInfo testInfo) {
        printTestName(testInfo);
        assertServiceIsRunning();
        initialState = SystemSnapshot.capture(api);
    }

    @AfterEach
    void restoreSystem() {
        if (initialState != null) {
            initialState.restore();
            initialState = null;
        }
    }

    protected void expectStatus(int expected, String request, Response response) {
        int received = response.getStatusCode();
        String flag = expected == received ? "" : "   <-- MISMATCH";
        System.out.printf("    %-50s expected %d, received %d%s%n", request, expected, received, flag);
        assertEquals(expected, received, request + " status code");
    }

    protected void expectClientError(String request, Response response) {
        int received = response.getStatusCode();
        boolean isClientError = received >= 400 && received < 500;
        String flag = isClientError ? "" : "   <-- MISMATCH";
        System.out.printf("    %-50s expected 4xx, received %d%s%n", request, received, flag);
        assertTrue(isClientError, request + " should return a 4xx status but returned " + received);
    }

    private void printTestName(TestInfo testInfo) {
        String className = testInfo.getTestClass().map(Class::getSimpleName).orElse("?");
        String methodName = testInfo.getTestMethod().map(method -> method.getName()).orElse("?");
        System.out.println();
        System.out.println(className + "." + methodName);
    }

    private void assertServiceIsRunning() {
        try {
            int status = api.get("/todos").getStatusCode();
            assertEquals(200, status, "Todo manager answered with an unexpected status");
        } catch (Exception exception) {
            fail("Todo manager is not running at " + TodoApi.BASE_URL);
        }
    }
}
