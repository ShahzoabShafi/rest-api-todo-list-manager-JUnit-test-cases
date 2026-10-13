package api;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    //Undocumented project behaviour

    @Test
    void optionsOnProjectEndpointsListAllowedMethods() {
        String projectId = api.createProject(uniqueTitle("options-proj"));
        String categoryId = api.createCategory(uniqueTitle("options-cat"));
        api.postJson("/projects/" + projectId + "/categories", "{\"id\":\"" + categoryId + "\"}");

        Response list = api.options("/projects");
        expectStatus(200, "OPTIONS /projects", list);
        String allowed = list.getHeader("Allow");
        assertTrue(allowed.contains("GET") && allowed.contains("POST") && allowed.contains("HEAD"));
        assertFalse(allowed.contains("PUT") || allowed.contains("DELETE"));

        Response one = api.options("/projects/" + projectId);
        expectStatus(200, "OPTIONS /projects/:id", one);
        assertTrue(one.getHeader("Allow").contains("PUT") && one.getHeader("Allow").contains("DELETE"));
        assertFalse(one.getHeader("Allow").contains("PATCH"));

        for (String relation : new String[] {"tasks", "categories"}) {
            Response response = api.options("/projects/" + projectId + "/" + relation);
            expectStatus(200, "OPTIONS /projects/:id/" + relation, response);
            assertTrue(response.getHeader("Allow").contains("POST"));
            assertFalse(response.getHeader("Allow").contains("PUT"));
        }

        Response link = api.options("/projects/" + projectId + "/categories/" + categoryId);
        expectStatus(200, "OPTIONS /projects/:id/categories/:id", link);
        assertTrue(link.getHeader("Allow").contains("DELETE"));
        assertFalse(link.getHeader("Allow").contains("GET"));
    }

    @Test
    void postTodoBodyToProjectTasksCreatesLinkedTodo() {
        String projectId = api.createProject(uniqueTitle("creates-todo"));
        String title = uniqueTitle("made-via-project");
        int todosBefore = api.todoIds().size();

        Response response = api.postJson("/projects/" + projectId + "/tasks",
                "{\"title\":\"" + title + "\",\"description\":\"made via project\"}");

        expectStatus(201, "POST todo body to /projects/:id/tasks", response);
        String todoId = response.jsonPath().getString("id");
        assertEquals(title, response.jsonPath().getString("title"));
        assertEquals(todosBefore + 1, api.todoIds().size());
        assertEquals(List.of(todoId), api.todoIdsOfProject(projectId));
        assertEquals(List.of(projectId), api.projectIdsOfTodo(todoId));
    }

    @Test
    void unknownQueryParameterOnProjectsIsIgnored() {
        api.createProject(uniqueTitle("ignored-param"));
        int total = api.projectIds().size();

        Response response = api.getFiltered("/projects", "randomParam", "x");

        expectStatus(200, "GET /projects?randomParam=x", response);
        assertEquals(total, response.jsonPath().getList("projects").size());
    }

}
