package api;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static api.Payloads.uniqueTitle;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Every bug has two tests. The "expected" one asserts what the documentation says and fails
 * while the bug exists. The "actual" one asserts what the server really does and passes.
 */
class KnownBugsTest extends BaseApiTest {

    // bug 1: array title

    @Test
    @Tag("expected-behavior")
    void bug1_expected_arrayTitleIsRejected() {
        Set<Todo> before = api.fetchAllTodos();
        Response response = api.postJson("/todos", "{\"title\":[\"a\",\"c\"]}");
        expectStatus(400, "POST /todos (title is an array)", response);
        assertEquals(before, api.fetchAllTodos());
    }

    @Test
    @Tag("actual-behavior")
    void bug1_actual_arrayTitleKeepsOnlyTheLastElement() {
        Response response = api.postJson("/todos", "{\"title\":[\"a\",\"c\"]}");
        expectStatus(201, "POST /todos (title is an array)", response);
        assertEquals("c", response.jsonPath().getString("title"));
    }

    // bug 2: tasksof of an unknown todo

    @Test
    @Tag("expected-behavior")
    void bug2_expected_tasksOfUnknownTodoReturns404() {
        expectStatus(404, "GET /todos/999999/tasksof", api.get("/todos/999999/tasksof"));
    }

    @Test
    @Tag("actual-behavior")
    void bug2_actual_tasksOfUnknownTodoReturns200WithProjectsOfOtherTodos() {
        String todoId = api.createTodo(uniqueTitle("other-todo"));
        String projectId = api.createProject("office");
        api.linkTodoToProject(todoId, projectId);
        Response response = api.get("/todos/999999/tasksof");
        expectStatus(200, "GET /todos/999999/tasksof", response);
        assertFalse(response.jsonPath().getList("projects").isEmpty());
    }

    // bug 3: categories of an unknown todo

    @Test
    @Tag("expected-behavior")
    void bug3_expected_categoriesOfUnknownTodoReturns404() {
        expectStatus(404, "GET /todos/999999/categories", api.get("/todos/999999/categories"));
    }

    @Test
    @Tag("actual-behavior")
    void bug3_actual_categoriesOfUnknownTodoReturns200() {
        expectStatus(200, "GET /todos/999999/categories", api.get("/todos/999999/categories"));
    }

    // bug 4: tasksof without a project id

    @Test
    @Tag("expected-behavior")
    void bug4_expected_tasksOfWithoutIdIsRejected() {
        String todoId = api.createTodo(uniqueTitle("no-project-id"));
        api.createProject("second project");
        expectClientError("POST /todos/" + todoId + "/tasksof ({})",
                api.postJson("/todos/" + todoId + "/tasksof", "{}"));
        assertTrue(api.projectIdsOfTodo(todoId).isEmpty());
    }

    @Test
    @Tag("actual-behavior")
    void bug4_actual_tasksOfWithoutIdCreatesAnEmptyProjectAndLinksIt() {
        String todoId = api.createTodo(uniqueTitle("no-project-id"));
        Set<String> projectsBefore = new HashSet<>(api.projectIds());
        expectStatus(201, "POST /todos/" + todoId + "/tasksof ({})",
                api.postJson("/todos/" + todoId + "/tasksof", "{}"));
        Set<String> createdProjects = new HashSet<>(api.projectIds());
        createdProjects.removeAll(projectsBefore);
        assertEquals(1, createdProjects.size(), "exactly one new project should have been created");
        String newProjectId = createdProjects.iterator().next();
        assertEquals("", api.get("/projects/" + newProjectId).jsonPath().getString("projects[0].title"));
        assertEquals(List.of(newProjectId), api.projectIdsOfTodo(todoId));
    }

    // bug 5: GET and POST on /todos/:id/tasksof/:id

    @Test
    @Tag("expected-behavior")
    void bug5_expected_getAndPostOnTasksOfByIdReturn405() {
        String path = linkedTasksOfPath();
        expectStatus(405, "GET " + path, api.get(path));
        expectStatus(405, "POST " + path, api.postJson(path, "{}"));
    }

    @Test
    @Tag("actual-behavior")
    void bug5_actual_getAndPostOnTasksOfByIdReturn404() {
        String path = linkedTasksOfPath();
        expectStatus(404, "GET " + path, api.get(path));
        expectStatus(404, "POST " + path, api.postJson(path, "{}"));
    }

    // bug 6: X-HTTP-Method-Override turns PUT into POST

    @Test
    @Tag("expected-behavior")
    void bug6_expected_methodOverrideHeaderIsIgnored() {
        String todoId = api.createTodo(uniqueTitle("override"));
        String categoryId = api.createCategory("override");
        expectStatus(405, "PUT /todos/" + todoId + "/categories (override POST)",
                putWithPostOverride(todoId, categoryId));
        assertTrue(api.categoryIdsOfTodo(todoId).isEmpty());
    }

    @Test
    @Tag("actual-behavior")
    void bug6_actual_methodOverrideHeaderTurnsPutIntoPost() {
        String todoId = api.createTodo(uniqueTitle("override"));
        String categoryId = api.createCategory("override");
        expectStatus(201, "PUT /todos/" + todoId + "/categories (override POST)",
                putWithPostOverride(todoId, categoryId));
        assertEquals(List.of(categoryId), api.categoryIdsOfTodo(todoId));
    }

    // bug 7: PUT with only a title

    @Test
    @Tag("expected-behavior")
    void bug7_expected_putWithOnlyATitleKeepsTheOtherFields() {
        String id = api.createTodo("put keeps fields", true, "keep me");
        expectStatus(200, "PUT /todos/" + id, api.putJson("/todos/" + id, Payloads.todo("renamed")));
        assertEquals(new Todo(id, "renamed", "true", "keep me"), api.fetchTodo(id));
    }

    @Test
    @Tag("actual-behavior")
    void bug7_actual_putWithOnlyATitleResetsTheOtherFields() {
        String id = api.createTodo("put resets fields", true, "keep me");
        expectStatus(200, "PUT /todos/" + id, api.putJson("/todos/" + id, Payloads.todo("renamed")));
        assertEquals(new Todo(id, "renamed", "false", ""), api.fetchTodo(id));
    }

    // bug 8: PUT drops the relationships

    @Test
    @Tag("expected-behavior")
    void bug8_expected_putKeepsTheRelationships() {
        String id = api.createTodo("put keeps links");
        String projectId = api.createProject("linked before put");
        String categoryId = api.createCategory("linked before put");
        api.linkTodoToProject(id, projectId);
        api.linkTodoToCategory(id, categoryId);
        expectStatus(200, "PUT /todos/" + id, api.putJson("/todos/" + id, Payloads.todo("renamed")));
        assertEquals(List.of(projectId), api.projectIdsOfTodo(id));
        assertEquals(List.of(categoryId), api.categoryIdsOfTodo(id));
    }

    @Test
    @Tag("actual-behavior")
    void bug8_actual_putRemovesTheRelationships() {
        String id = api.createTodo("put drops links");
        String projectId = api.createProject("linked before put");
        String categoryId = api.createCategory("linked before put");
        api.linkTodoToProject(id, projectId);
        api.linkTodoToCategory(id, categoryId);
        expectStatus(200, "PUT /todos/" + id, api.putJson("/todos/" + id, Payloads.todo("renamed")));
        assertTrue(api.projectIdsOfTodo(id).isEmpty());
        assertTrue(api.categoryIdsOfTodo(id).isEmpty());
    }

    // bug 9: tasks of an unknown project

    @Test
    @Tag("expected-behavior")
    void bug9_expected_projectsTasksOfNonExistentProjectReturns404() {
        expectStatus(404, "GET /projects/999999/tasks (non-existent project)", api.get("/projects/999999/tasks"));
    }

    @Test
    @Tag("actual-behavior")
    void bug9_actual_projectsTasksOfNonExistentProjectReturns200WithTodosOfOtherProjects() {
        String todoId = api.createTodo(uniqueTitle("other-project-todo"));
        String projectId = api.createProject("office");
        api.linkTodoToProject(todoId, projectId);
        Response response = api.get("/projects/999999/tasks");
        expectStatus(200, "GET /projects/999999/tasks (non-existent project)", response);
        assertFalse(response.jsonPath().getList("todos").isEmpty());
    }

    // bug 10: POST with an empty tasksof array

    @Test
    @Tag("expected-behavior")
    void bug10_expected_postClearsRelationshipsWithEmptyArrays() {
        String todoId = api.createTodo(uniqueTitle("post-clear-links"));
        String projectId = api.createProject("office project");
        api.linkTodoToProject(todoId, projectId);
        Response response = api.postJson("/todos/" + todoId, postBodyWithEmptyTasksOf(todoId));
        expectStatus(200, "POST /todos/" + todoId + " (clear tasksof via POST)", response);
        assertTrue(api.projectIdsOfTodo(todoId).isEmpty());
    }

    @Test
    @Tag("actual-behavior")
    void bug10_actual_postWithEmptyArraysKeepsTheRelationships() {
        String todoId = api.createTodo(uniqueTitle("post-keeps-links"));
        String projectId = api.createProject("office project");
        api.linkTodoToProject(todoId, projectId);
        Response response = api.postJson("/todos/" + todoId, postBodyWithEmptyTasksOf(todoId));
        expectStatus(200, "POST /todos/" + todoId + " (empty tasksof array)", response);
        assertEquals(List.of(projectId), api.projectIdsOfTodo(todoId));
    }

    private String postBodyWithEmptyTasksOf(String todoId) {
        return "{\"title\":\"" + api.fetchTodo(todoId).title() + "\",\"tasksof\":[]}";
    }

    private String linkedTasksOfPath() {
        String todoId = api.createTodo(uniqueTitle("tasksof-by-id"));
        String projectId = api.createProject("office");
        api.linkTodoToProject(todoId, projectId);
        return "/todos/" + todoId + "/tasksof/" + projectId;
    }

    private Response putWithPostOverride(String todoId, String categoryId) {
        return api.request()
                .header("X-HTTP-Method-Override", "POST")
                .contentType(ContentType.JSON)
                .body(Payloads.idOnly(categoryId))
                .put(api.url("/todos/" + todoId + "/categories"));
    }
}
