package api;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;

public class TodoApi {

    public static final String BASE_URL = "http://localhost:4567";

    public RequestSpecification request() {
        return given();
    }

    public String url(String path) {
        return BASE_URL + path;
    }

    public Response get(String path) {
        return given().get(url(path));
    }

    public Response getAs(String path, ContentType accept) {
        return given().accept(accept).get(url(path));
    }

    public Response getFiltered(String path, String... keysAndValues) {
        RequestSpecification spec = given();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            spec = spec.queryParam(keysAndValues[i], keysAndValues[i + 1]);
        }
        return spec.get(url(path));
    }

    public Response head(String path) {
        return given().head(url(path));
    }

    public Response options(String path) {
        return given().options(url(path));
    }

    public Response patch(String path) {
        return given().patch(url(path));
    }

    public Response put(String path) {
        return given().put(url(path));
    }

    public Response delete(String path) {
        return given().delete(url(path));
    }

    public Response postJson(String path, String body) {
        return given().contentType(ContentType.JSON).body(body).post(url(path));
    }

    public Response putJson(String path, String body) {
        return given().contentType(ContentType.JSON).body(body).put(url(path));
    }

    public Response postXml(String path, String body) {
        return given().contentType(ContentType.XML).body(body).post(url(path));
    }

    public Response putXml(String path, String body) {
        return given().contentType(ContentType.XML).body(body).put(url(path));
    }

    public String createTodo(String title) {
        return createTodo(title, false, "");
    }

    public String createTodo(String title, boolean doneStatus, String description) {
        Response response = postJson("/todos", Payloads.todo(title, doneStatus, description));
        return idFromCreated(response, "create todo");
    }

    public String createProject(String title) {
        Response response = postJson("/projects", "{\"title\":\"" + title + "\"}");
        return idFromCreated(response, "create project");
    }

    public String createCategory(String title) {
        Response response = postJson("/categories", "{\"title\":\"" + title + "\"}");
        return idFromCreated(response, "create category");
    }

    public void linkTodoToProject(String todoId, String projectId) {
        Response response = postJson("/todos/" + todoId + "/tasksof", Payloads.idOnly(projectId));
        requireStatus(201, response, "link todo to project");
    }

    public void linkTodoToCategory(String todoId, String categoryId) {
        Response response = postJson("/todos/" + todoId + "/categories", Payloads.idOnly(categoryId));
        requireStatus(201, response, "link todo to category");
    }

    public Todo fetchTodo(String id) {
        JsonPath body = get("/todos/" + id).jsonPath();
        return new Todo(
                body.getString("todos[0].id"),
                body.getString("todos[0].title"),
                body.getString("todos[0].doneStatus"),
                body.getString("todos[0].description"));
    }

    public Set<Todo> fetchAllTodos() {
        List<Map<String, Object>> items = get("/todos").jsonPath().getList("todos");
        Set<Todo> todos = new HashSet<>();
        for (Map<String, Object> item : items) {
            todos.add(new Todo(
                    String.valueOf(item.get("id")),
                    String.valueOf(item.get("title")),
                    String.valueOf(item.get("doneStatus")),
                    String.valueOf(item.get("description"))));
        }
        return todos;
    }

    public List<String> todoIds() {
        return get("/todos").jsonPath().getList("todos.id");
    }

    public List<String> projectIds() {
        return get("/projects").jsonPath().getList("projects.id");
    }

    public List<String> categoryIds() {
        return get("/categories").jsonPath().getList("categories.id");
    }

    public List<String> projectIdsOfTodo(String todoId) {
        return get("/todos/" + todoId + "/tasksof").jsonPath().getList("projects.id");
    }

    public List<String> categoryIdsOfTodo(String todoId) {
        return get("/todos/" + todoId + "/categories").jsonPath().getList("categories.id");
    }

    public List<String> todoIdsOfProject(String projectId) {
        return get("/projects/" + projectId + "/tasks").jsonPath().getList("todos.id");
    }

    public List<String> todoIdsOfCategory(String categoryId) {
        return get("/categories/" + categoryId + "/todos").jsonPath().getList("todos.id");
    }

    private String idFromCreated(Response response, String action) {
        requireStatus(201, response, action);
        return response.jsonPath().getString("id");
    }

    private void requireStatus(int expected, Response response, String action) {
        if (response.getStatusCode() != expected) {
            throw new IllegalStateException("Test setup failed to " + action + ": expected status "
                    + expected + " but received " + response.getStatusCode());
        }
    }
}
