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
        expectStatus(404, "DELETE /projects/" + id + "(again)", response);

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
        assertEquals(title, response.xmlPath().getString("projects.title"));
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
                "active":"yes",
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


}