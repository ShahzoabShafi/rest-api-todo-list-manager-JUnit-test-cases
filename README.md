# Rest API Todo List Manager – ECSE 429 Project Part A

Exploratory testing and a JUnit 5 unit test suite for the **Rest API Todo List Manager**
(`runTodoManagerRestAPI-1.5.5.jar`, http://localhost:4567).

| Team member | Student ID | Area tested |
|---|---|---|
| Yessine Chaari | 261179816 | Todos endpoints |
| Shahzoab Shafi | 261160765 | Projects endpoints |

## Video

Full suite run, random order with seeds 20260930 (twice) and 12345, and a run with the server stopped:
https://drive.google.com/file/d/1bc0XAvL0clSKOjVljvLknxv122-DQI7T/view?usp=sharing

## Requirements

- Java 21 (JDK)
- Maven 3.8+
- curl (used by `CommandLineQueryTest` and the session script)
- `runTodoManagerRestAPI-1.5.5.jar` (from myCourses or https://github.com/eviltester/thingifier/releases)

## Running the tests

1. Start the application under test in its own terminal and leave it running:

   ```bash
   java -jar runTodoManagerRestAPI-1.5.5.jar
   ```

2. From the project root, run the whole suite:

   ```bash
   mvn test
   ```

The first line of the output shows the random-order seed (`Random order seed: 20260930`),
and every test prints the requests it sent, the expected and received status codes, and PASS or FAIL.

**Expected result:** `Tests run: 143, Failures: 19`. The 19 failures are the
`bugN_expected_…` tests in `KnownBugsTest`, one per bug. Each one asserts the documented
behavior and fails because the bug is real. Its partner `bugN_actual_…` test passes and shows
what the API actually does.

If the server is not running, every test fails with
`Todo manager is not running at http://localhost:4567`.

### Useful variations

```bash
# a different random order
mvn test -Djunit.jupiter.execution.order.random.seed=12345

# one test class
mvn test -Dtest=ProjectsTest

# only the bug tests that show the documented (failing) behavior
mvn test -Dgroups=expected-behavior

# only the bug tests that show the actual (passing) behavior
mvn test -Dgroups=actual-behavior
```

The server keeps its data in memory, so restarting it restores the default data.

## Random order

`src/test/resources/junit-platform.properties` runs both test classes and test methods in a
pseudo-random order. The fixed seed makes a run repeatable: the same seed gives the same order,
and another seed gives a different one.

```properties
junit.jupiter.testmethod.order.default=org.junit.jupiter.api.MethodOrderer$Random
junit.jupiter.testclass.order.default=org.junit.jupiter.api.ClassOrderer$Random
junit.jupiter.execution.order.random.seed=20260930
```

## Repository structure

```
pom.xml                          Maven build (JUnit 5.11, REST Assured 5.5, Surefire 3.5)
README.md
src/test/java/api/
  BaseApiTest.java               checks the server is running, saves and restores system state
  SystemSnapshot.java            captures todos, projects, categories and links; restores them
  TodoApi.java                   HTTP helpers (JSON and XML) and setup helpers
  Payloads.java, Todo.java       request bodies with unique titles; todo record for comparisons
  ResultReporter.java            prints the seed and PASS/FAIL for each test
  TodosTest.java                 55 tests: /todos, /todos/:id, /todos/:id/categories, /todos/:id/tasksof
  ProjectsTest.java              35 tests: /projects, /projects/:id, /projects/:id/tasks, /projects/:id/categories
  KnownBugsTest.java             38 tests: an expected/actual pair for each of bugs 1–19
  UndocumentedApiTest.java       10 tests: OPTIONS, PATCH, unknown query parameters, Swagger and docs,
                                 creating a todo through /projects/:id/tasks
  CommandLineQueryTest.java      5 tests: runs curl as an external process
src/test/resources/
  junit-platform.properties      random order and seed
session/
  projects_session.sh            replays every command of the projects exploratory session
docs/
  session notes, bug summary reports and the written report (PDF)
```

## Exploratory session script

`session/projects_session.sh` replays the 49 curl commands of the projects session in order.
Each command is labelled with the session note it belongs to. Run it against a freshly started
server, because the ids in later notes depend on the earlier commands:

```bash
java -jar runTodoManagerRestAPI-1.5.5.jar     # fresh server, default data
bash session/projects_session.sh | tee projects_session_output.txt
```

The similar `session/todos_session.sh` script replays the 50 curl commands of the todos session 
and works in a similar way.

## Bugs found

The 19 bugs are documented in the bug summary reports in `docs/`. Each bug has a summary,
description, impact, steps to reproduce and the `KnownBugsTest` tests that reproduce it.
Bugs 1–10 are on the todos endpoints and bugs 11–19 are on the projects endpoints.

## Report

The written report is in the `docs` folder. It describes the 
exploratory testing, the unit test suite, the bugs found, and the lessons learned.