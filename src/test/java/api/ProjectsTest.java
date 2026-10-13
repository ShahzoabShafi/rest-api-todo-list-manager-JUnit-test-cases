package api;

import io.restassured.response.Response;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static api.Payloads.uniqueTitle;
import static org.junit.jupiter.api.Assertions.*;


class ProjectsTest extends BaseApiTest {

    @Test
    void getAllProjects() {
        String title = Payloads.uniqueTitle("listed");
        api.createProject(title);
        Response response = api.get("/projects");
        expectStatus(200, "GET /projects", response);
        List<String> titles = response.jsonPath().getList("projects.title");
        assertTrue(titles.contains(title), "GET /projects should list the project just created");
    }


    @Test
    void postCreatesProject() {
        Response response = api.postJson("/projects", "{\"title\":\"School\"}");
        expectStatus(201, "POST /projects", response);
        String id = response.jsonPath().getString("id");
        Response fetched = api.get("/projects/" + id);
        expectStatus(200, "GET /projects/" + id, fetched);
        assertEquals("School", fetched.jsonPath().getString("projects[0].title"));
    }

    @Test
    void getUnknownProjectReturns404() {
        Response response = api.get("/projects/999999");
        expectStatus(404, "GET /projects/999999", response);
        String responseBody = response.asString();
        assertTrue(responseBody.contains("Could not find an instance with projects/999999"), "Expected response body to contain the error message, but actual body was: " + responseBody);
    }

    @Test
    void postWithMalformedJsonIsRejectedAndCreatesNothing() {
        int noOfProjectIdsBefore = api.projectIds().size();
        Response response = api.postJson("/projects", "{\"title\":\"Broken\"");
        // server declines request
        expectStatus(400, "POST /projects (malformed Json)", response);
        // confirms nothing was added
        assertEquals(noOfProjectIdsBefore, api.projectIds().size());
    }

    @Test
    void postWithMalformedXmlIsRejected() {
        int noOfProjectIdsBefore = api.projectIds().size();
        Response response = api.postXml("/projects", "<project><title>broken");
        // server declines request
        expectStatus(400, "POST /projects (malformed xml)", response);
        // confirms nothing was added
        assertEquals(noOfProjectIdsBefore, api.projectIds().size());
    }

    @Test
    void deleteProjectTwiceReturns404() {
        String id = api.createProject("Project 1");
        Response response = api.delete("/projects/" + id);
        expectStatus(200, "DELETE /projects/" + id, response);
        // delete the project with the same id again to check if it throws an error
        response = api.delete("/projects/" + id);
        // 404 proves that the id was deleted previously
        expectStatus(404, "DELETE /projects/" + id + " (again)", response);
        expectStatus(404, "GET /projects/" + id + " (after delete)", api.get("/projects/" + id));

    }

    @Test
    void postAmendsOnlyTheGivenField() {
        //create a new project
        Response projectCreated = api.postJson("/projects", "{\"title\":\"New Project\",\"description\":\"keep me\",\"active\":true}");
        expectStatus(201, "POST /projects", projectCreated);
        String id = projectCreated.jsonPath().getString("id");
        // rename the project using the id
        Response renameResponse = api.postJson("/projects/" + id, "{\"title\":\"New Project renamed\"}");
        expectStatus(200, "POST /projects/" + id + " Project renamed", renameResponse);
        // get data for the id
        Response getProjects = api.get("/projects/" + id);
        // check if the project was renamed and the description is still the same
        assertEquals("New Project renamed", getProjects.jsonPath().getString("projects[0].title"));
        assertEquals("keep me", getProjects.jsonPath().getString("projects[0].description"));
    }


    @Test
    void putPatchAndDeleteOnProjectsCollectionReturn405() {
        Response response = api.put("/projects");
        expectStatus(405, "PUT /projects method is refused", response);
        response = api.patch("/projects");
        expectStatus(405, "PATCH /projects method is refused", response);
        Response deleteResponse = api.delete("/projects");
        expectStatus(405, "DELETE /projects", deleteResponse);
    }

    @Test
    void getAllProjectsAsXml() {
        String id = api.createProject(uniqueTitle("xml-list"));
        Response allProjectsAsXmlResponse = api.getAs("/projects", ContentType.XML);
        expectStatus(200, "GET /projects as XML", allProjectsAsXmlResponse);
        assertTrue(allProjectsAsXmlResponse.getContentType().contains("xml"));
        assertTrue(allProjectsAsXmlResponse.xmlPath().getList("projects.project.id").contains(id));
    }

    @Test
    void headProjects() {
        Response headResponse = api.head("/projects");
        expectStatus(200, "HEAD /projects", headResponse);
        assertTrue(headResponse.asString().isEmpty());
    }

    @Test
        // command line query
    void filterProjectsByQueryParameters() {
        //create two new project with one active and the other not active
        Response project1Created = api.postJson("/projects", "{\"title\":\"New Project 1\",\"description\":\"keep me\",\"active\":true}");
        expectStatus(201, "POST /projects", project1Created);
        String project1Id = project1Created.jsonPath().getString("id");

        Response project2Created = api.postJson("/projects", "{\"title\":\"New Project 1\",\"description\":\"keep me\",\"active\":false}");
        expectStatus(201, "POST /projects", project2Created);
        String project2Id = project2Created.jsonPath().getString("id");

        Response response = api.getFiltered("/projects", "active", "true");
        expectStatus(200, "GET /projects?active=true", response);

        assertTrue(response.jsonPath().getList("projects.id").contains(project1Id));
        assertFalse(response.jsonPath().getList("projects.id").contains(project2Id));
    }

    @Test
    void postWithXmlBodyAndXmlResponse() {
        String title = uniqueTitle("xml-both");
        Response response = api.request()
                .contentType(ContentType.XML)
                .accept(ContentType.XML)
                .body("<project><title>" + title + "</title></project>")
                .post(api.url("/projects"));
        expectStatus(201, "POST /projects (xml in, xml out)", response);
        assertTrue(response.getContentType().contains("xml"));
        assertEquals(title, response.xmlPath().getString("project.title"));
    }

    @Test
    void postWithIdOrInvalidBooleanIsRejectedAndCreatesNothing() {
        int noOfProjectIdsBefore = api.projectIds().size();
        Response project1CreateResponse = api.postJson("/projects", """
                {
                "id":"99",
                "title":"x"
                }
                """);
        expectStatus(400, "POST /projects (client id)", project1CreateResponse);
        Response project2CreateResponse = api.postJson("/projects", """
                {
                "title":"x",
                "active":"yes"
                }
                """);
        expectStatus(400, "POST /projects (active not boolean)", project2CreateResponse);
        int noOfProjectIdsAfter = api.projectIds().size();
        assertEquals(noOfProjectIdsBefore, noOfProjectIdsAfter);
    }

    @Test
    void headProjectById() {
        // create a project
        Response projectCreatedResponse = api.postJson("/projects", """
                {
                "title":"project 1",
                "Description":"lorem ipsum",
                "active": true
                }
                """);
        expectStatus(201, "POST /projects project created", projectCreatedResponse);
        String projectId = projectCreatedResponse.jsonPath().getString("id");
        Response headResponse = api.head("/projects/" + projectId);
        expectStatus(200, "HEAD /projects/" + projectId, headResponse);
    }

    @Test
    void getProjectByIdAsJsonAndXml() {
        String title = uniqueTitle("json-xml");
        String id = api.createProject(title);
        Response jsonResponse = api.get("/projects/" + id);
        expectStatus(200, "GET /projects/" + id, jsonResponse);
        assertEquals(title, jsonResponse.jsonPath().getString("projects[0].title"));
        Response xmlResponse = api.getAs("/projects/" + id, ContentType.XML);
        expectStatus(200, "GET AS XML /projects/" + id, xmlResponse);
        assertEquals(title, xmlResponse.xmlPath().getString("projects.project.title"));
    }

    @Test
    void amendWithJsonBodyAndXmlResponse() {
        String title = uniqueTitle("amendWithJsonBodyAndXmlResponse");
        String id = api.createProject(title);
        String newTitle = uniqueTitle("renamed");
        Response response = api.request()
                .contentType(ContentType.JSON) //what i am sending
                .accept(ContentType.XML) // what i want back
                .body("{\"title\":\"" + newTitle + "\"}")
                .post(api.url("/projects/" + id));
        expectStatus(200, "POST /projects/" + id + " (JSON in, XML out)", response);
        assertTrue(response.getContentType().contains("xml")); // did i get xml back?
        assertEquals(newTitle, response.xmlPath().getString("project.title")); // did the title get renamed?
    }

    @Test
    void putWithXmlBody() {
        String title = uniqueTitle("putWithXmlBody");
        String id = api.createProject(title);
        Response response = api.putXml("/projects/" + id, "<project><title>new title</title></project>");
        expectStatus(200, "PUT XML /projects/" + id, response);
        Response getProject = api.get("/projects/" + id);
        // only title is asserted since put wipes other fields (known bug)
        assertEquals("new title", getProject.jsonPath().getString("projects[0].title"));
    }

    @Test
    void amendWithMalformedJsonOrInvalidBooleanLeavesProjectUnchanged() {
        String title = uniqueTitle("unchanged");
        String description = "must survive bad requests";
        Response created = api.postJson("/projects",
                "{\"title\":\"" + title + "\",\"description\":\"" + description + "\"}");
        expectStatus(201, "POST /projects", created);
        String id = created.jsonPath().getString("id");

        Response malformed = api.postJson("/projects/" + id, "{\"title\":\"Broken\"");
        expectStatus(400, "POST /projects/" + id + " (malformed json)", malformed);

        Response invalidBoolean = api.postJson("/projects/" + id, "{\"completed\":\"yes\"}");
        expectStatus(400, "POST /projects/" + id + " (completed not boolean)", invalidBoolean);

        Response fetched = api.get("/projects/" + id);
        expectStatus(200, "GET /projects/" + id, fetched);
        assertEquals(title, fetched.jsonPath().getString("projects[0].title"));
        assertEquals(description, fetched.jsonPath().getString("projects[0].description"));
    }

    @Test
    void putAndPostOnUnknownProjectReturn404() {
        Response unknownPost = api.postJson("/projects/999999", "{\"title\":\"new title\",\"description\":\"lorem ipsum\",\"active\":false}");
        expectStatus(404, "POST /projects/999999 on unknown project", unknownPost);
        Response unknownPut = api.putJson("/projects/999999", "{\"title\":\"new title\",\"description\":\"lorem ipsum\",\"active\":false}");
        expectStatus(404, "PUT /projects/999999 on unknown project", unknownPut);
    }

    @Test
    void patchOnProjectReturns405() {
        Response createResponse = api.postJson("/projects", "{\"title\":\"new title\",\"description\":\"lorem ipsum\",\"active\":false}");
        expectStatus(201, "POST /projects", createResponse);
        String id = createResponse.jsonPath().getString("id");
        Response patchResponse = api.patch("/projects/" + id);
        expectStatus(405, "PATCH /projects/" + id, patchResponse);
    }

    @Test
    void deleteProjectKeepsItsTodosAndOtherProjects() {
        String projectToDelete = api.createProject(uniqueTitle("to-delete"));
        String otherProject = api.createProject(uniqueTitle("bystander"));
        String todoId = api.createTodo(uniqueTitle("linked-todo"));
        Response link = api.postJson("/projects/" + projectToDelete + "/tasks", "{\"id\":\"" + todoId + "\"}");
        expectStatus(201, "POST /projects/" + projectToDelete + "/tasks", link);

        Response deleted = api.delete("/projects/" + projectToDelete);
        expectStatus(200, "DELETE /projects/" + projectToDelete, deleted);

        expectStatus(200, "GET /todos/" + todoId + " (todo survives)", api.get("/todos/" + todoId));
        expectStatus(200, "GET /projects/" + otherProject + " (other project untouched)", api.get("/projects/" + otherProject));
        assertFalse(api.projectIdsOfTodo(todoId).contains(projectToDelete),
                "the todo should no longer point to the deleted project");
    }

}