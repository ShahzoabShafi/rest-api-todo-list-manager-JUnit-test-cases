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

    // Bug 11: Only a title resets the description and removes the project's relationships to its todos

    @Test
    @Tag("expected-behavior")
    void bug11_expected_putWithOnlyATitleKeepsTheOtherFields() {
        //create a new project
        Response projectCreated = api.postJson("/projects", "{\"title\":\"New Project\",\"description\":\"keep me\",\"active\":true}");
        expectStatus(201, "POST /projects", projectCreated);
        String id = projectCreated.jsonPath().getString("id");
        // rename the project using the id
        Response renameResponse = api.putJson("/projects/" + id, "{\"title\":\"New Project renamed\"}");
        expectStatus(200, "PUT /projects/" + id + " Project renamed", renameResponse);
        // get data for the id
        Response getProjects = api.get("/projects/" + id);
        // check if the project was renamed and the description is still the same
        assertEquals("New Project renamed", getProjects.jsonPath().getString("projects[0].title"));
        assertEquals("keep me", getProjects.jsonPath().getString("projects[0].description"));
    }

    @Test
    @Tag("actual-behavior")
    void bug11_actual_putWithOnlyATitleResetsTheOtherFields() {
        //create a new project
        Response projectCreated = api.postJson("/projects", "{\"title\":\"New Project\",\"description\":\"keep me\",\"active\":true}");
        expectStatus(201, "POST /projects", projectCreated);
        String id = projectCreated.jsonPath().getString("id");
        // rename the project using the id
        Response renameResponse = api.putJson("/projects/" + id, "{\"title\":\"New Project renamed\"}");
        expectStatus(200, "PUT /projects/" + id + " Project renamed", renameResponse);
        // get data for the id
        Response getProjects = api.get("/projects/" + id);
        // check if the project was renamed and the description is empty
        assertEquals("false",getProjects.jsonPath().getString("projects[0].active"));
        assertEquals("", getProjects.jsonPath().getString("projects[0].description"));
    }

    // bug 12: project with a missing or empty title

    @Test
    @Tag("expected-behavior")
    void bug12_expected_missingOrEmptyTitleIsRejected() {
        int before = api.projectIds().size();
        expectStatus(400, "POST /projects ({})", api.postJson("/projects", "{}"));
        expectStatus(400, "POST /projects (empty title)", api.postJson("/projects", "{\"title\":\"\"}"));
        assertEquals(before, api.projectIds().size());
    }

    @Test
    @Tag("actual-behavior")
    void bug12_actual_missingOrEmptyTitleCreatesAProject() {
        int before = api.projectIds().size();
        Response noTitle = api.postJson("/projects", "{}");
        Response emptyTitle = api.postJson("/projects", "{\"title\":\"\"}");
        expectStatus(201, "POST /projects ({})", noTitle);
        expectStatus(201, "POST /projects (empty title)", emptyTitle);
        assertEquals("", noTitle.jsonPath().getString("title"));
        assertEquals("", emptyTitle.jsonPath().getString("title"));
        assertEquals(before + 2, api.projectIds().size());
    }

    // bug 13: categories of an unknown project

    @Test
    @Tag("expected-behavior")
    void bug13_expected_categoriesOfUnknownProjectReturns404() {
        expectStatus(404, "GET /projects/999999/categories", api.get("/projects/999999/categories"));
    }

    @Test
    @Tag("actual-behavior")
    void bug13_actual_categoriesOfUnknownProjectReturns200WithNoCategories() {
        Response response = api.get("/projects/999999/categories");
        expectStatus(200, "GET /projects/999999/categories", response);
        assertTrue(response.jsonPath().getList("categories").isEmpty());
    }

    // bug 14: project <-> category link is one-sided

    @Test
    @Tag("expected-behavior")
    void bug14_expected_linkedProjectIsListedUnderTheCategory() {
        String projectId = api.createProject(uniqueTitle("one-sided"));
        String categoryId = api.createCategory(uniqueTitle("one-sided"));
        expectStatus(201, "POST /projects/" + projectId + "/categories",
                api.postJson("/projects/" + projectId + "/categories", "{\"id\":\"" + categoryId + "\"}"));
        List<String> projects = api.get("/categories/" + categoryId + "/projects").jsonPath().getList("projects.id");
        assertTrue(projects.contains(projectId));
    }

    @Test
    @Tag("actual-behavior")
    void bug14_actual_linkedProjectIsMissingUnderTheCategory() {
        String projectId = api.createProject(uniqueTitle("one-sided"));
        String categoryId = api.createCategory(uniqueTitle("one-sided"));
        expectStatus(201, "POST /projects/" + projectId + "/categories",
                api.postJson("/projects/" + projectId + "/categories", "{\"id\":\"" + categoryId + "\"}"));
        // the project side shows the link...
        assertEquals(List.of(categoryId),
                api.get("/projects/" + projectId + "/categories").jsonPath().getList("categories.id"));
        // ...but the category side does not
        assertTrue(api.get("/categories/" + categoryId + "/projects").jsonPath().getList("projects").isEmpty());
    }

    // bug 15: X-HTTP-Method-Override bypasses 405 on project endpoints

    @Test
    @Tag("expected-behavior")
    void bug15_expected_methodOverrideIsIgnoredOnProjects() {
        String projectId = api.createProject(uniqueTitle("override"));
        String todoId = api.createTodo(uniqueTitle("override"));
        int projectsBefore = api.projectIds().size();

        expectStatus(405, "PUT /projects/" + projectId + "/tasks (override POST)",
                withPostOverride("{\"id\":\"" + todoId + "\"}").put(api.url("/projects/" + projectId + "/tasks")));
        assertTrue(api.todoIdsOfProject(projectId).isEmpty());

        expectStatus(405, "DELETE /projects (override POST)",
                withPostOverride("{\"title\":\"override\"}").delete(api.url("/projects")));
        assertEquals(projectsBefore, api.projectIds().size());
    }

    @Test
    @Tag("actual-behavior")
    void bug15_actual_methodOverrideTurnsPutAndDeleteIntoPost() {
        String projectId = api.createProject(uniqueTitle("override"));
        String todoId = api.createTodo(uniqueTitle("override"));
        int projectsBefore = api.projectIds().size();

        expectStatus(201, "PUT /projects/" + projectId + "/tasks (override POST)",
                withPostOverride("{\"id\":\"" + todoId + "\"}").put(api.url("/projects/" + projectId + "/tasks")));
        assertEquals(List.of(todoId), api.todoIdsOfProject(projectId));
        assertEquals(List.of(projectId), api.projectIdsOfTodo(todoId));

        expectStatus(201, "DELETE /projects (override POST)",
                withPostOverride("{\"title\":\"override\"}").delete(api.url("/projects")));
        assertEquals(projectsBefore + 1, api.projectIds().size());
    }

    // bug 16: array title on projects

    @Test
    @Tag("expected-behavior")
    void bug16_expected_arrayTitleOnProjectIsRejected() {
        int before = api.projectIds().size();
        expectStatus(400, "POST /projects (title is an array)",
                api.postJson("/projects", "{\"title\":[\"a\",\"b\"]}"));
        assertEquals(before, api.projectIds().size());
    }

    @Test
    @Tag("actual-behavior")
    void bug16_actual_arrayTitleOnProjectKeepsOnlyTheLastElement() {
        Response response = api.postJson("/projects", "{\"title\":[\"a\",\"b\"]}");
        expectStatus(201, "POST /projects (title is an array)", response);
        assertEquals("b", response.jsonPath().getString("title"));
    }

    // bug 17: unsupported methods on /projects/:id/tasks/:id

    @Test
    @Tag("expected-behavior")
    void bug17_expected_getAndPostOnTaskLinkReturn405() {
        String projectId = api.createProject(uniqueTitle("task-link"));
        String todoId = api.createTodo(uniqueTitle("task-link"));
        api.linkTodoToProject(todoId, projectId);
        String path = "/projects/" + projectId + "/tasks/" + todoId;
        expectStatus(405, "GET " + path, api.get(path));
        expectStatus(405, "POST " + path, api.postJson(path, "{}"));
    }

    @Test
    @Tag("actual-behavior")
    void bug17_actual_getAndPostOnTaskLinkReturn404() {
        String projectId = api.createProject(uniqueTitle("task-link"));
        String todoId = api.createTodo(uniqueTitle("task-link"));
        api.linkTodoToProject(todoId, projectId);
        String path = "/projects/" + projectId + "/tasks/" + todoId;
        expectStatus(404, "GET " + path, api.get(path));
        expectStatus(404, "POST " + path, api.postJson(path, "{}"));
        // the link really exists, so 404 is misleading
        assertEquals(List.of(todoId), api.todoIdsOfProject(projectId));
    }

    // bug 18: a rejected POST still consumes a project id

    @Test
    @Tag("expected-behavior")
    void bug18_expected_rejectedPostDoesNotConsumeAnId() {
        int first = Integer.parseInt(api.createProject(uniqueTitle("id-gap")));
        expectStatus(400, "POST /projects (active not boolean)",
                api.postJson("/projects", "{\"title\":\"bad\",\"active\":\"yes\"}"));
        int second = Integer.parseInt(api.createProject(uniqueTitle("id-gap")));
        assertEquals(first + 1, second);
    }

    @Test
    @Tag("actual-behavior")
    void bug18_actual_rejectedPostSkipsAnId() {
        int first = Integer.parseInt(api.createProject(uniqueTitle("id-gap")));
        expectStatus(400, "POST /projects (active not boolean)",
                api.postJson("/projects", "{\"title\":\"bad\",\"active\":\"yes\"}"));
        int second = Integer.parseInt(api.createProject(uniqueTitle("id-gap")));
        assertEquals(first + 2, second);
    }

    // bug 19: parse errors are sent with Content-Type text/html

    @Test
    @Tag("expected-behavior")
    void bug19_expected_parseErrorsAreSentAsJson() {
        Response json = api.postJson("/projects", "{\"title\":\"broken\"");
        Response xml = api.postXml("/projects", "<project><title>broken");
        expectStatus(400, "POST /projects (malformed JSON)", json);
        expectStatus(400, "POST /projects (malformed XML)", xml);
        assertTrue(json.getContentType().contains("json"));
        assertTrue(xml.getContentType().contains("json"));
    }

    @Test
    @Tag("actual-behavior")
    void bug19_actual_parseErrorsAreSentAsHtmlWithAJsonBody() {
        Response json = api.postJson("/projects", "{\"title\":\"broken\"");
        Response xml = api.postXml("/projects", "<project><title>broken");
        expectStatus(400, "POST /projects (malformed JSON)", json);
        expectStatus(400, "POST /projects (malformed XML)", xml);
        assertTrue(json.getContentType().contains("text/html"));
        assertTrue(xml.getContentType().contains("text/html"));
        // the body is still JSON
        assertTrue(json.asString().startsWith("{\"errorMessages\""));
    }

    //helper
    private io.restassured.specification.RequestSpecification withPostOverride(String body) {
        return api.request()
                .header("X-HTTP-Method-Override", "POST")
                .contentType(ContentType.JSON)
                .body(body);
    }
}
