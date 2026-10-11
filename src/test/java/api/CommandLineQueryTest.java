package api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static api.Payloads.uniqueTitle;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandLineQueryTest extends BaseApiTest {

    @Test
    void curlGetFiltersTodosByTitle() throws Exception {
        String title = uniqueTitle("curl");
        String otherTitle = uniqueTitle("other");
        api.createTodo(title);
        api.createTodo(otherTitle);

        String output = curl("-G", api.url("/todos"), "--data-urlencode", "title=" + title);
        assertTrue(output.startsWith("HTTP/1.1 200"));
        assertTrue(output.contains(title));
        assertFalse(output.contains(otherTitle));
    }

    @Test
    void curlHeadReturnsHeadersOnly() throws Exception {
        String output = curl("-I", api.url("/todos"));
        assertTrue(output.startsWith("HTTP/1.1 200"));
        assertFalse(output.contains("\"todos\""));
    }

    @Test
    void curlGetOfUnknownTodoReturns404() throws Exception {
        String output = curl(api.url("/todos/-1"));
        assertTrue(output.startsWith("HTTP/1.1 404"));
        assertTrue(output.contains("Could not find an instance with todos/-1"));
    }

    private String curl(String... arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("curl", "-s", "-i"));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        process.waitFor();
        return output;
    }
}
