package api;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
    void postWithMalformedJsonIsRejectedAndCreatesNothing(){
        int noOfProjectIdsBefore = api.projectIds().size();
        Response response = api.postJson("/projects", "{\"title\":\"Broken\"");
        // server declines request
        expectStatus(400,"POST /projects (malformed Json)",response);
        // confirms nothing was added
        assertEquals(noOfProjectIdsBefore,api.projectIds().size());
    }

    @Test
    void postWithMalformedXmlIsRejected(){
        int noOfProjectIdsBefore = api.projectIds().size();
        Response response = api.postXml("/projects", "<project><title>broken");
        // server declines request
        expectStatus(400,"POST /projects (malformed xml)",response);
        // confirms nothing was added
        assertEquals(noOfProjectIdsBefore,api.projectIds().size());
    }

    @Test
    void deleteProjectTwiceReturns404(){
        String id = api.createProject("Project 1");
        Response response = api.delete("/projects/" + id);
        expectStatus(200,"DELETE /projects/" + id,response);
        // delete the project with the same id again to check if it throws an error
        response = api.delete("/projects/" + id);
        // 404 proves that the id was deleted previously
        expectStatus(404,"DELETE /projects/" + id,response);

    }
}