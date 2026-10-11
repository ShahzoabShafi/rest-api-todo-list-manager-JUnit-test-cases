package api;

import io.restassured.response.Response;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class SystemSnapshot {

    private final TodoApi api;
    private final Map<String, Todo> todos = new HashMap<>();
    private final Map<String, List<String>> projectsOfTodo = new HashMap<>();
    private final Map<String, List<String>> categoriesOfTodo = new HashMap<>();
    private final Set<String> projectIds;
    private final Set<String> categoryIds;

    private SystemSnapshot(TodoApi api) {
        this.api = api;
        this.projectIds = new HashSet<>(api.projectIds());
        this.categoryIds = new HashSet<>(api.categoryIds());
        for (Todo todo : api.fetchAllTodos()) {
            todos.put(todo.id(), todo);
            projectsOfTodo.put(todo.id(), api.projectIdsOfTodo(todo.id()));
            categoriesOfTodo.put(todo.id(), api.categoryIdsOfTodo(todo.id()));
        }
    }

    static SystemSnapshot capture(TodoApi api) {
        return new SystemSnapshot(api);
    }

    void restore() {
        deleteTodosCreatedSinceSnapshot();
        deleteProjectsCreatedSinceSnapshot();
        deleteCategoriesCreatedSinceSnapshot();
        for (Todo original : todos.values()) {
            restoreTodo(original);
        }
    }

    private void deleteTodosCreatedSinceSnapshot() {
        for (String id : api.todoIds()) {
            if (!todos.containsKey(id)) {
                api.delete("/todos/" + id);
            }
        }
    }

    private void deleteProjectsCreatedSinceSnapshot() {
        for (String id : api.projectIds()) {
            if (!projectIds.contains(id)) {
                api.delete("/projects/" + id);
            }
        }
    }

    private void deleteCategoriesCreatedSinceSnapshot() {
        for (String id : api.categoryIds()) {
            if (!categoryIds.contains(id)) {
                api.delete("/categories/" + id);
            }
        }
    }

    private void restoreTodo(Todo original) {
        Todo current = api.fetchTodo(original.id());
        if (current.id() == null) {
            return;
        }
        if (!original.equals(current)) {
            api.postJson("/todos/" + original.id(),
                    Payloads.todo(original.title(), Boolean.parseBoolean(original.doneStatus()), original.description()));
        }
        restoreRelations(original.id(), "tasksof", projectsOfTodo.get(original.id()), api.projectIdsOfTodo(original.id()));
        restoreRelations(original.id(), "categories", categoriesOfTodo.get(original.id()), api.categoryIdsOfTodo(original.id()));
    }

    private void restoreRelations(String todoId, String relation, List<String> expected, List<String> actual) {
        String basePath = "/todos/" + todoId + "/" + relation;
        for (String relatedId : actual) {
            if (!expected.contains(relatedId)) {
                api.delete(basePath + "/" + relatedId);
            }
        }
        for (String relatedId : expected) {
            if (!actual.contains(relatedId)) {
                Response response = api.postJson(basePath, Payloads.idOnly(relatedId));
                if (response.getStatusCode() != 201) {
                    throw new IllegalStateException("Could not restore " + relation + " of todo " + todoId);
                }
            }
        }
    }
}
