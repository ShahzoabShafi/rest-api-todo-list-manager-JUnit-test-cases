package api;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static api.Payloads.uniqueTitle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TodosTest extends BaseApiTest {

    // /todos

    @Test
    void getAllTodos() {
        Response response = api.get("/todos");
        expectStatus(200, "GET /todos", response);
        List<String> titles = response.jsonPath().getList("todos.title");
        assertTrue(titles.containsAll(List.of("scan paperwork", "file paperwork")));
    }

    @Test
    void getAllTodosAsXml() {
        String id = api.createTodo(uniqueTitle("xml-list"));
        Response response = api.getAs("/todos", ContentType.XML);
        expectStatus(200, "GET /todos (Accept xml)", response);
        assertTrue(response.getContentType().contains("xml"));
        assertTrue(response.xmlPath().getList("todos.todo.id").contains(id));
    }

    @Test
    void headTodos() {
        Response response = api.head("/todos");
        expectStatus(200, "HEAD /todos", response);
        assertTrue(response.asString().isEmpty());
    }

    @Test
    void filterByDoneStatus() {
        String doneId = api.createTodo(uniqueTitle("done"), true, "");
        String openId = api.createTodo(uniqueTitle("open"), false, "");

        Response done = api.getFiltered("/todos", "doneStatus", "true");
        expectStatus(200, "GET /todos?doneStatus=true", done);
        assertTrue(done.jsonPath().getList("todos.id").contains(doneId));
        assertFalse(done.jsonPath().getList("todos.id").contains(openId));

        Response open = api.getFiltered("/todos", "doneStatus", "false");
        expectStatus(200, "GET /todos?doneStatus=false", open);
        assertTrue(open.jsonPath().getList("todos.id").contains(openId));
        assertFalse(open.jsonPath().getList("todos.id").contains(doneId));
    }

    @Test
    void filterByTitle() {
        String title = uniqueTitle("filter");
        String id = api.createTodo(title);
        api.createTodo(uniqueTitle("other"));
        Response response = api.getFiltered("/todos", "title", title);
        expectStatus(200, "GET /todos?title=" + title, response);
        assertEquals(List.of(id), response.jsonPath().getList("todos.id"));
    }

    @Test
    void filterByAllFields() {
        String title = uniqueTitle("filter-all");
        String id = api.createTodo(title, true, "quarterly report");
        api.createTodo(title, false, "quarterly report");
        Response response = api.getFiltered("/todos",
                "title", title, "doneStatus", "true", "description", "quarterly report");
        expectStatus(200, "GET /todos?title&doneStatus&description", response);
        assertEquals(List.of(id), response.jsonPath().getList("todos.id"));
    }

    @Test
    void filterWithEmptyOrUnmatchedTitleReturnsNoTodos() {
        Response empty = api.getFiltered("/todos", "title", "");
        expectStatus(200, "GET /todos?title=", empty);
        assertTrue(empty.jsonPath().getList("todos").isEmpty());

        Response unmatched = api.getFiltered("/todos", "title", "nobody has this title");
        expectStatus(200, "GET /todos?title=<no match>", unmatched);
        assertTrue(unmatched.jsonPath().getList("todos").isEmpty());
    }

    @Test
    void postCreatesTodoAndAddsNothingElse() {
        Set<Todo> expected = new HashSet<>(api.fetchAllTodos());
        Response response = api.postJson("/todos", Payloads.todo("Wash Dishes", false, "Home chore"));
        expectStatus(201, "POST /todos", response);
        expected.add(new Todo(response.jsonPath().getString("id"), "Wash Dishes", "false", "Home chore"));
        assertEquals(expected, api.fetchAllTodos());
    }

    @Test
    void postTwiceCreatesTwoTodos() {
        String body = Payloads.todo(uniqueTitle("twice"));
        Response first = api.postJson("/todos", body);
        Response second = api.postJson("/todos", body);
        expectStatus(201, "POST /todos (first)", first);
        expectStatus(201, "POST /todos (second)", second);
        assertNotEquals(first.jsonPath().getString("id"), second.jsonPath().getString("id"));
    }

    @Test
    void postWithXmlBody() {
        String title = uniqueTitle("xml");
        Response response = api.postXml("/todos", Payloads.todoXml(title, true));
        expectStatus(201, "POST /todos (xml)", response);
        String id = response.jsonPath().getString("id");
        assertEquals(new Todo(id, title, "true", ""), api.fetchTodo(id));
    }

    @Test
    void postWithXmlBodyAndXmlResponse() {
        String title = uniqueTitle("xml-both");
        Response response = api.request()
                .contentType(ContentType.XML)
                .accept(ContentType.XML)
                .body(Payloads.todoXml(title, false))
                .post(api.url("/todos"));
        expectStatus(201, "POST /todos (xml in, xml out)", response);
        assertTrue(response.getContentType().contains("xml"));
        assertEquals(title, response.xmlPath().getString("todo.title"));
    }

    @Test
    void postWithInvalidBodyIsRejectedAndCreatesNothing() {
        String[] invalidBodies = {
                "{\"doneStatus\":false,\"description\":\"no title\"}",
                Payloads.todo(""),
                "{\"id\":\"999\",\"title\":\"has an id\"}"
        };
        Set<Todo> before = api.fetchAllTodos();
        for (String body : invalidBodies) {
            expectStatus(400, "POST /todos " + body, api.postJson("/todos", body));
        }
        assertEquals(before, api.fetchAllTodos());
    }

    @Test
    void postWithMalformedJsonIsRejected() {
        Set<Todo> before = api.fetchAllTodos();
        Response response = api.postJson("/todos", "{\"title\":\"Broken Todo\"");
        expectStatus(400, "POST /todos (malformed json)", response);
        assertEquals(before, api.fetchAllTodos());
    }

    @Test
    void postWithMalformedXmlIsRejected() {
        Set<Todo> before = api.fetchAllTodos();
        Response response = api.postXml("/todos", "<todo><title>Broken Todo");
        expectStatus(400, "POST /todos (malformed xml)", response);
        assertEquals(before, api.fetchAllTodos());
    }

    @Test
    void putAndDeleteOnTodosReturn405() {
        Set<Todo> before = api.fetchAllTodos();
        expectStatus(405, "PUT /todos", api.put("/todos"));
        expectStatus(405, "DELETE /todos", api.delete("/todos"));
        assertEquals(before, api.fetchAllTodos());
    }

    // /todos/:id

    @Test
    void getTodoById() {
        String title = uniqueTitle("by-id");
        String id = api.createTodo(title, true, "details");
        Response response = api.get("/todos/" + id);
        expectStatus(200, "GET /todos/" + id, response);
        assertEquals(new Todo(id, title, "true", "details"), api.fetchTodo(id));
    }

    @Test
    void getTodoByIdAsXml() {
        String title = uniqueTitle("get-xml");
        String id = api.createTodo(title);
        Response response = api.getAs("/todos/" + id, ContentType.XML);
        expectStatus(200, "GET /todos/" + id + " (Accept xml)", response);
        assertTrue(response.getContentType().contains("xml"));
        assertEquals(title, response.xmlPath().getString("todos.todo.title"));
    }

    @Test
    void headTodoById() {
        String id = api.createTodo(uniqueTitle("head"));
        Response response = api.head("/todos/" + id);
        expectStatus(200, "HEAD /todos/" + id, response);
        assertTrue(response.asString().isEmpty());
    }

    @Test
    void getAndHeadUnknownTodoReturn404() {
        Response get = api.get("/todos/-1");
        expectStatus(404, "GET /todos/-1", get);
        assertTrue(get.asString().contains("Could not find an instance with todos/-1"));
        expectStatus(404, "HEAD /todos/-1", api.head("/todos/-1"));
    }

    @Test
    void postAmendsOnlyTheGivenField() {
        String id = api.createTodo("before", true, "keep me");
        Set<Todo> others = todosExcept(id);
        Response response = api.postJson("/todos/" + id, Payloads.todo("after"));
        expectStatus(200, "POST /todos/" + id, response);
        assertEquals(new Todo(id, "after", "true", "keep me"), api.fetchTodo(id));
        assertEquals(others, todosExcept(id));
    }

    @Test
    void postAmendWithXmlBodyAndXmlResponse() {
        String id = api.createTodo("before xml post");
        String title = uniqueTitle("xml-post");
        Response response = api.request()
                .contentType(ContentType.XML)
                .accept(ContentType.XML)
                .body(Payloads.todoXml(title, true))
                .post(api.url("/todos/" + id));
        expectStatus(200, "POST /todos/" + id + " (xml in, xml out)", response);
        assertEquals(title, response.xmlPath().getString("todo.title"));
        assertEquals(new Todo(id, title, "true", ""), api.fetchTodo(id));
    }

    @Test
    void putAmendsTheTodo() {
        String id = api.createTodo("before", false, "old");
        Set<Todo> others = todosExcept(id);
        Response response = api.putJson("/todos/" + id, Payloads.todo("after", true, "new"));
        expectStatus(200, "PUT /todos/" + id, response);
        assertEquals(new Todo(id, "after", "true", "new"), api.fetchTodo(id));
        assertEquals(others, todosExcept(id));
    }

    @Test
    void putWithXmlBody() {
        String id = api.createTodo("before xml put");
        String title = uniqueTitle("xml-put");
        Response response = api.putXml("/todos/" + id, Payloads.todoXml(title, true));
        expectStatus(200, "PUT /todos/" + id + " (xml)", response);
        assertEquals(new Todo(id, title, "true", ""), api.fetchTodo(id));
    }

    @Test
    void putWithJsonBodyAndXmlResponse() {
        String id = api.createTodo("before xml response");
        Response response = api.request()
                .contentType(ContentType.JSON)
                .accept(ContentType.XML)
                .body(Payloads.todo("XML Response Update"))
                .put(api.url("/todos/" + id));
        expectStatus(200, "PUT /todos/" + id + " (json in, xml out)", response);
        assertEquals("XML Response Update", response.xmlPath().getString("todo.title"));
        assertEquals("XML Response Update", api.fetchTodo(id).title());
    }

    @Test
    void putAndPostWithEmptyTitleLeaveTheTodoUnchanged() {
        String id = api.createTodo("stays the same", false, "untouched");
        Todo before = api.fetchTodo(id);
        expectStatus(400, "PUT /todos/" + id + " (empty title)", api.putJson("/todos/" + id, Payloads.todo("")));
        expectStatus(400, "POST /todos/" + id + " (empty title)", api.postJson("/todos/" + id, Payloads.todo("")));
        assertEquals(before, api.fetchTodo(id));
    }

    @Test
    void putWithMalformedJsonLeavesTheTodoUnchanged() {
        String id = api.createTodo("stays the same", false, "untouched");
        Todo before = api.fetchTodo(id);
        Response response = api.putJson("/todos/" + id, "{\"title\":\"Broken\"");
        expectStatus(400, "PUT /todos/" + id + " (malformed json)", response);
        assertEquals(before, api.fetchTodo(id));
    }

    @Test
    void putWithMalformedXmlLeavesTheTodoUnchanged() {
        String id = api.createTodo("stays the same", false, "untouched");
        Todo before = api.fetchTodo(id);
        Response response = api.putXml("/todos/" + id, "<todo><title>Broken");
        expectStatus(400, "PUT /todos/" + id + " (malformed xml)", response);
        assertEquals(before, api.fetchTodo(id));
    }

    @Test
    void putAndPostOnUnknownTodoReturn404() {
        Set<Todo> before = api.fetchAllTodos();
        expectStatus(404, "PUT /todos/999999", api.putJson("/todos/999999", Payloads.todo("eat breakfast")));
        expectStatus(404, "POST /todos/999999", api.postJson("/todos/999999", Payloads.todo("write in journal")));
        assertEquals(before, api.fetchAllTodos());
    }

    @Test
    void deleteRemovesOnlyThatTodo() {
        String id = api.createTodo(uniqueTitle("delete"));
        Set<Todo> others = todosExcept(id);
        expectStatus(200, "DELETE /todos/" + id, api.delete("/todos/" + id));
        expectStatus(404, "GET /todos/" + id, api.get("/todos/" + id));
        assertEquals(others, api.fetchAllTodos());
    }

    @Test
    void deleteTodoRemovesItFromItsProject() {
        String todoId = api.createTodo(uniqueTitle("delete-linked"));
        String projectId = api.createProject("office");
        api.linkTodoToProject(todoId, projectId);
        expectStatus(200, "DELETE /todos/" + todoId, api.delete("/todos/" + todoId));
        assertFalse(api.todoIdsOfProject(projectId).contains(todoId));
    }

    @Test
    void deleteUnknownOrAlreadyDeletedTodoReturns404() {
        String id = api.createTodo(uniqueTitle("delete-twice"));
        api.delete("/todos/" + id);
        expectStatus(404, "DELETE /todos/" + id + " (again)", api.delete("/todos/" + id));
        expectStatus(404, "DELETE /todos/999999", api.delete("/todos/999999"));
    }

    // /todos/:id/categories

    @Test
    void getCategoriesOfTodo() {
        String todoId = api.createTodo(uniqueTitle("categories"));
        String categoryId = api.createCategory("home");
        api.linkTodoToCategory(todoId, categoryId);
        Response response = api.get("/todos/" + todoId + "/categories");
        expectStatus(200, "GET /todos/" + todoId + "/categories", response);
        assertEquals(List.of(categoryId), response.jsonPath().getList("categories.id"));
    }

    @Test
    void getCategoriesOfTodoWithoutAnyIsEmpty() {
        String todoId = api.createTodo(uniqueTitle("no-categories"));
        Response response = api.get("/todos/" + todoId + "/categories");
        expectStatus(200, "GET /todos/" + todoId + "/categories", response);
        assertTrue(response.jsonPath().getList("categories").isEmpty());
    }

    @Test
    void getCategoriesAsXml() {
        String todoId = api.createTodo(uniqueTitle("categories-xml"));
        String categoryId = api.createCategory("home");
        api.linkTodoToCategory(todoId, categoryId);
        Response response = api.getAs("/todos/" + todoId + "/categories", ContentType.XML);
        expectStatus(200, "GET /todos/" + todoId + "/categories (Accept xml)", response);
        assertTrue(response.getContentType().contains("xml"));
        assertEquals(List.of(categoryId), response.xmlPath().getList("categories.category.id"));
    }

    @Test
    void headCategories() {
        String todoId = api.createTodo(uniqueTitle("head-categories"));
        Response response = api.head("/todos/" + todoId + "/categories");
        expectStatus(200, "HEAD /todos/" + todoId + "/categories", response);
        assertTrue(response.asString().isEmpty());
    }

    @Test
    void postLinksTodoToCategoryOnlyOnce() {
        String todoId = api.createTodo(uniqueTitle("link"));
        String otherTodoId = api.createTodo(uniqueTitle("not-linked"));
        String categoryId = api.createCategory("work");
        String path = "/todos/" + todoId + "/categories";

        expectStatus(201, "POST " + path, api.postJson(path, Payloads.idOnly(categoryId)));
        api.postJson(path, Payloads.idOnly(categoryId));
        assertEquals(List.of(categoryId), api.categoryIdsOfTodo(todoId));
        assertTrue(api.categoryIdsOfTodo(otherTodoId).isEmpty());
    }

    @Test
    void postWithUnknownCategoryOrNoIdLinksNothing() {
        String todoId = api.createTodo(uniqueTitle("bad-link"));
        String path = "/todos/" + todoId + "/categories";
        Set<String> categoriesBefore = new HashSet<>(api.categoryIds());
        expectClientError("POST " + path + " (unknown category)", api.postJson(path, Payloads.idOnly("999999")));
        expectStatus(400, "POST " + path + " (no id)", api.postJson(path, "{}"));
        assertTrue(api.categoryIdsOfTodo(todoId).isEmpty());
        assertEquals(categoriesBefore, new HashSet<>(api.categoryIds()));
    }

    @Test
    void postCategoryWithMalformedJsonLinksNothing() {
        String todoId = api.createTodo(uniqueTitle("malformed"));
        String path = "/todos/" + todoId + "/categories";
        expectStatus(400, "POST " + path + " (malformed json)", api.postJson(path, "{\"id\":"));
        assertTrue(api.categoryIdsOfTodo(todoId).isEmpty());
    }

    @Test
    void postCategoryToUnknownTodoIsRejected() {
        String categoryId = api.createCategory("work");
        expectClientError("POST /todos/999999/categories",
                api.postJson("/todos/999999/categories", Payloads.idOnly(categoryId)));
        assertTrue(api.todoIdsOfCategory(categoryId).isEmpty());
    }

    @Test
    void putOnCategoriesReturns405() {
        String todoId = api.createTodo(uniqueTitle("put-categories"));
        expectStatus(405, "PUT /todos/" + todoId + "/categories", api.put("/todos/" + todoId + "/categories"));
    }

    @Test
    void deleteLinkKeepsTodoCategoryAndOtherLinks() {
        String todoId = api.createTodo(uniqueTitle("unlink"));
        String removed = api.createCategory("removed");
        String kept = api.createCategory("kept");
        api.linkTodoToCategory(todoId, removed);
        api.linkTodoToCategory(todoId, kept);
        String path = "/todos/" + todoId + "/categories/" + removed;

        expectStatus(200, "DELETE " + path, api.delete(path));
        assertEquals(List.of(kept), api.categoryIdsOfTodo(todoId));
        expectStatus(200, "GET /todos/" + todoId, api.get("/todos/" + todoId));
        expectStatus(200, "GET /categories/" + removed, api.get("/categories/" + removed));
    }

    @Test
    void deleteLinkAgainOrForUnrelatedCategoryReturns404() {
        String todoId = api.createTodo(uniqueTitle("unlink-twice"));
        String linked = api.createCategory("linked");
        String unrelated = api.createCategory("unrelated");
        api.linkTodoToCategory(todoId, linked);
        String linkedPath = "/todos/" + todoId + "/categories/" + linked;

        api.delete(linkedPath);
        expectStatus(404, "DELETE " + linkedPath + " (again)", api.delete(linkedPath));
        String unrelatedPath = "/todos/" + todoId + "/categories/" + unrelated;
        expectStatus(404, "DELETE " + unrelatedPath, api.delete(unrelatedPath));
    }

    // /todos/:id/tasksof

    @Test
    void getTasksOfTodo() {
        String todoId = api.createTodo(uniqueTitle("tasksof"));
        String projectId = api.createProject("office");
        api.linkTodoToProject(todoId, projectId);
        Response response = api.get("/todos/" + todoId + "/tasksof");
        expectStatus(200, "GET /todos/" + todoId + "/tasksof", response);
        assertEquals(List.of(projectId), response.jsonPath().getList("projects.id"));
    }

    @Test
    void getTasksOfTodoWithoutProjectIsEmpty() {
        String todoId = api.createTodo(uniqueTitle("no-project"));
        Response response = api.get("/todos/" + todoId + "/tasksof");
        expectStatus(200, "GET /todos/" + todoId + "/tasksof", response);
        assertTrue(response.jsonPath().getList("projects").isEmpty());
    }

    @Test
    void getTasksOfAsXml() {
        String todoId = api.createTodo(uniqueTitle("tasksof-xml"));
        String projectId = api.createProject("office");
        api.linkTodoToProject(todoId, projectId);
        Response response = api.getAs("/todos/" + todoId + "/tasksof", ContentType.XML);
        expectStatus(200, "GET /todos/" + todoId + "/tasksof (Accept xml)", response);
        assertTrue(response.getContentType().contains("xml"));
        assertEquals(List.of(projectId), response.xmlPath().getList("projects.project.id"));
    }

    @Test
    void headTasksOf() {
        String todoId = api.createTodo(uniqueTitle("head-tasksof"));
        Response response = api.head("/todos/" + todoId + "/tasksof");
        expectStatus(200, "HEAD /todos/" + todoId + "/tasksof", response);
        assertTrue(response.asString().isEmpty());
    }

    @Test
    void postLinksTodoToProjectInBothDirectionsOnlyOnce() {
        String todoId = api.createTodo(uniqueTitle("link"));
        String otherTodoId = api.createTodo(uniqueTitle("not-linked"));
        String projectId = api.createProject("office");
        String path = "/todos/" + todoId + "/tasksof";

        expectStatus(201, "POST " + path, api.postJson(path, Payloads.idOnly(projectId)));
        api.postJson(path, Payloads.idOnly(projectId));
        assertEquals(List.of(projectId), api.projectIdsOfTodo(todoId));
        assertEquals(List.of(todoId), api.todoIdsOfProject(projectId));
        assertTrue(api.projectIdsOfTodo(otherTodoId).isEmpty());
    }

    @Test
    void postWithUnknownProjectLinksNothing() {
        String todoId = api.createTodo(uniqueTitle("bad-link"));
        String path = "/todos/" + todoId + "/tasksof";
        expectClientError("POST " + path + " (unknown project)", api.postJson(path, Payloads.idOnly("999999")));
        assertTrue(api.projectIdsOfTodo(todoId).isEmpty());
    }

    @Test
    void postProjectWithMalformedJsonLinksNothing() {
        String todoId = api.createTodo(uniqueTitle("malformed"));
        String path = "/todos/" + todoId + "/tasksof";
        expectStatus(400, "POST " + path + " (malformed json)", api.postJson(path, "{\"id\":"));
        assertTrue(api.projectIdsOfTodo(todoId).isEmpty());
    }

    @Test
    void postProjectToUnknownTodoIsRejected() {
        String projectId = api.createProject("office");
        expectClientError("POST /todos/999999/tasksof",
                api.postJson("/todos/999999/tasksof", Payloads.idOnly(projectId)));
        assertTrue(api.todoIdsOfProject(projectId).isEmpty());
    }

    @Test
    void putOnTasksOfReturns405() {
        String todoId = api.createTodo(uniqueTitle("put-tasksof"));
        expectStatus(405, "PUT /todos/" + todoId + "/tasksof", api.put("/todos/" + todoId + "/tasksof"));
    }

    @Test
    void deleteLinkKeepsTodoProjectAndOtherLinks() {
        String todoId = api.createTodo(uniqueTitle("unlink"));
        String removed = api.createProject("removed");
        String kept = api.createProject("kept");
        api.linkTodoToProject(todoId, removed);
        api.linkTodoToProject(todoId, kept);
        String path = "/todos/" + todoId + "/tasksof/" + removed;

        expectStatus(200, "DELETE " + path, api.delete(path));
        assertEquals(List.of(kept), api.projectIdsOfTodo(todoId));
        assertFalse(api.todoIdsOfProject(removed).contains(todoId));
        expectStatus(200, "GET /todos/" + todoId, api.get("/todos/" + todoId));
        expectStatus(200, "GET /projects/" + removed, api.get("/projects/" + removed));
    }

    @Test
    void deleteLinkAgainOrForUnrelatedProjectReturns404() {
        String todoId = api.createTodo(uniqueTitle("unlink-twice"));
        String linked = api.createProject("linked");
        String unrelated = api.createProject("unrelated");
        api.linkTodoToProject(todoId, linked);
        String linkedPath = "/todos/" + todoId + "/tasksof/" + linked;

        api.delete(linkedPath);
        expectStatus(404, "DELETE " + linkedPath + " (again)", api.delete(linkedPath));
        String unrelatedPath = "/todos/" + todoId + "/tasksof/" + unrelated;
        expectStatus(404, "DELETE " + unrelatedPath, api.delete(unrelatedPath));
    }

    @Test
    void putAndPatchOnTasksOfByIdReturn405() {
        String todoId = api.createTodo(uniqueTitle("methods"));
        String projectId = api.createProject("office");
        api.linkTodoToProject(todoId, projectId);
        String path = "/todos/" + todoId + "/tasksof/" + projectId;
        expectStatus(405, "PUT " + path, api.put(path));
        expectStatus(405, "PATCH " + path, api.patch(path));
        assertEquals(List.of(projectId), api.projectIdsOfTodo(todoId));
    }

    @Test
    void deleteWithInjectionInTodoIdReturns400() {
        String todoId = api.createTodo(uniqueTitle("injection"));
        String projectId = api.createProject("office");
        api.linkTodoToProject(todoId, projectId);
        Response response = api.request()
                .urlEncodingEnabled(false)
                .delete(api.url("/todos/1%3Bls/tasksof/" + projectId));
        expectStatus(400, "DELETE /todos/1%3Bls/tasksof/" + projectId, response);
        assertEquals(List.of(projectId), api.projectIdsOfTodo(todoId));
    }

    // undocumented endpoints

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

    // command line queries

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

    // bugs: each one has an expected-behavior test that fails

    // bug 1: array title

    @Test
    @Tag("expected-behavior")
    void bug1_expected_arrayTitleIsRejected() {
        Set<Todo> before = api.fetchAllTodos();
        Response response = api.postJson("/todos", "{\"title\":[\"a\",\"c\"]}");
        expectStatus(400, "POST /todos (title is an array)", response);
        assertEquals(before, api.fetchAllTodos());
    }

    // bug 2: tasksof of an unknown todo

    @Test
    @Tag("expected-behavior")
    void bug2_expected_tasksOfUnknownTodoReturns404() {
        expectStatus(404, "GET /todos/999999/tasksof", api.get("/todos/999999/tasksof"));
    }

    // bug 3: categories of an unknown todo

    @Test
    @Tag("expected-behavior")
    void bug3_expected_categoriesOfUnknownTodoReturns404() {
        expectStatus(404, "GET /todos/999999/categories", api.get("/todos/999999/categories"));
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

    // bug 5: GET and POST on /todos/:id/tasksof/:id

    @Test
    @Tag("expected-behavior")
    void bug5_expected_getAndPostOnTasksOfByIdReturn405() {
        String path = linkedTasksOfPath();
        expectStatus(405, "GET " + path, api.get(path));
        expectStatus(405, "POST " + path, api.postJson(path, "{}"));
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

    // bug 7: PUT with only a title

    @Test
    @Tag("expected-behavior")
    void bug7_expected_putWithOnlyATitleKeepsTheOtherFields() {
        String id = api.createTodo("put keeps fields", true, "keep me");
        expectStatus(200, "PUT /todos/" + id, api.putJson("/todos/" + id, Payloads.todo("renamed")));
        assertEquals(new Todo(id, "renamed", "true", "keep me"), api.fetchTodo(id));
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


    // Bug: GET /projects/:id/tasks with a non-existent project ID returns tasks of another project instead of 404
    @Test
    @Tag("expected-behavior")
    void bug9_expected_projectsTasksOfNonExistentProjectReturns404() {
        Response response = api.get("/projects/999999/tasks");
        expectStatus(404, "GET /projects/999999/tasks (non-existent project)", response);
    }

    // Bug: POST /todos/:id with empty relationship arrays does not clear them (unlike PUT)
    @Test
    @Tag("expected-behavior")
    void bug10_expected_postClearsRelationshipsWithEmptyArrays() {
        String todoId = api.createTodo(uniqueTitle("post-clear-links"));
        String projectId = api.createProject("office project");
        api.linkTodoToProject(todoId, projectId);

        String body = "{\"title\":\"" + api.fetchTodo(todoId).title() + "\",\"tasksof\":[]}";
        Response response = api.postJson("/todos/" + todoId, body);
        expectStatus(200, "POST /todos/" + todoId + " (clear tasksof via POST)", response);
        assertTrue(api.projectIdsOfTodo(todoId).isEmpty());
    }

    // helpers

    private Set<Todo> todosExcept(String id) {
        Set<Todo> others = new HashSet<>(api.fetchAllTodos());
        others.removeIf(todo -> todo.id().equals(id));
        return others;
    }

    private String curl(String... arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("curl", "-s", "-i"));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        process.waitFor();
        return output;
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