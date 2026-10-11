package api;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static api.Payloads.uniqueTitle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UndocumentedApiTest extends BaseApiTest {

    @Test
    void optionsOnTodosListsAllowedMethods() {
        Response response = api.options("/todos");
        expectStatus(200, "OPTIONS /todos", response);
        String allowed = response.getHeader("Allow");
        assertTrue(allowed.contains("GET") && allowed.contains("POST") && allowed.contains("HEAD"));
        assertFalse(allowed.contains("DELETE"));
    }

    @Test
    void optionsOnTodoByIdListsAllowedMethods() {
        String id = api.createTodo(uniqueTitle("options"));
        Response response = api.options("/todos/" + id);
        expectStatus(200, "OPTIONS /todos/" + id, response);
        String allowed = response.getHeader("Allow");
        assertTrue(allowed.contains("PUT") && allowed.contains("POST") && allowed.contains("DELETE"));
    }

    @Test
    void optionsOnRelationshipsListsAllowedMethods() {
        String id = api.createTodo(uniqueTitle("options-relations"));
        for (String relation : new String[] {"tasksof", "categories"}) {
            Response response = api.options("/todos/" + id + "/" + relation);
            expectStatus(200, "OPTIONS /todos/" + id + "/" + relation, response);
            assertTrue(response.getHeader("Allow").contains("POST"));
        }
    }

    @Test
    void patchIsNotAllowedAndChangesNothing() {
        String id = api.createTodo("patch target", false, "untouched");
        Todo before = api.fetchTodo(id);
        expectStatus(405, "PATCH /todos", api.patch("/todos"));
        expectStatus(405, "PATCH /todos/" + id, api.patch("/todos/" + id));
        expectStatus(405, "PATCH /todos/999999", api.patch("/todos/999999"));
        assertEquals(before, api.fetchTodo(id));
    }

    @Test
    void unknownQueryParameterIsIgnored() {
        int total = api.fetchAllTodos().size();
        Response response = api.getFiltered("/todos", "randomParameter", "test");
        expectStatus(200, "GET /todos?randomParameter=test", response);
        assertEquals(total, response.jsonPath().getList("todos").size());
    }

    @Test
    void swaggerFileDescribesTheTodoEndpoints() {
        Response response = api.get("/docs/swagger");
        expectStatus(200, "GET /docs/swagger", response);
        assertTrue(response.asString().contains("\"openapi\""));
        assertTrue(response.asString().contains("\"/todos\""));
    }

    @Test
    void docsPageIsServedAsHtml() {
        Response response = api.get("/docs");
        expectStatus(200, "GET /docs", response);
        assertTrue(response.getContentType().contains("html"));
    }
}
