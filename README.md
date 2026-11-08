# Rest API Todo List Manager – ECSE 429 Project Parts A and B

Part A: exploratory testing and a JUnit 5 unit test suite.
Part B: Gherkin story tests (Cucumber, JavaScript) for the Todos user stories.

Application under test: **Rest API Todo List Manager**
(`runTodoManagerRestAPI-1.5.5.jar`, http://localhost:4567).

| Team member | Student ID | Area tested |
|---|---|---|
| Yessine Chaari | 261179816 | Todos endpoints (Part A), Todos user stories (Part B) |
| Shahzoab Shafi | 261160765 | Projects endpoints |

## Videos

- **Part A**, full suite run, random order with seeds 20260930 (twice) and 12345, and a run with the server stopped:
  https://drive.google.com/file/d/1bc0XAvL0clSKOjVljvLknxv122-DQI7T/view?usp=sharing
- **Part B**, story test suite run: _link to be added_

## Requirements

- Java 21 (JDK)
- Maven 3.8+ (Part A)
- Node.js 18 or later, which includes npm (Part B). Check with `node -v` and `npm -v`.
- curl (used by `CommandLineQueryTest` and the session scripts)
- `runTodoManagerRestAPI-1.5.5.jar` (from myCourses or https://github.com/eviltester/thingifier/releases)

## Starting the application

Both suites test the running server. Start it in its own terminal and leave it running:

```bash
java -jar runTodoManagerRestAPI-1.5.5.jar
```

The server keeps its data in memory, so restarting it restores the default data and resets the
id counters. **Restart it before running each suite** (see the note under Part B).

## Part A: unit test suite (JUnit 5)

From the project root, run the whole suite:

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

### Random order

`src/test/resources/junit-platform.properties` runs both test classes and test methods in a
pseudo-random order. The fixed seed makes a run repeatable: the same seed gives the same order,
and another seed gives a different one.

```properties
junit.jupiter.testmethod.order.default=org.junit.jupiter.api.MethodOrderer$Random
junit.jupiter.testclass.order.default=org.junit.jupiter.api.ClassOrderer$Random
junit.jupiter.execution.order.random.seed=20260930
```

## Part B: story tests (Cucumber)

The five Todos user stories are written as Gherkin feature files, one per story:

| Story | Feature file |
|---|---|
| 1. Filter todos by title | `Story1FilterTodosByTitle.feature` |
| 2. Update the completion status of a todo | `Story2UpdateTodoCompletionStatus.feature` |
| 3. Delete a todo by id | `Story3DeleteTodoById.feature` |
| 4. Associate a todo with a category | `Story4AssociateTodoWithCategory.feature` |
| 5. View the projects of a todo | `Story5ViewProjectsOfTodo.feature` |

Each feature has a Background, and Scenario Outlines labelled Normal Flow, Alternative Flow or
Error Flow. Together they expand to 36 scenarios. Every scenario starts from an empty system: a
`Before` hook deletes all todos, categories and projects through the API.

### Run

1. Start (or restart) the server as described above.
2. In a second terminal, from the project root, install the dependencies once:

   ```bash
   npm install
   ```

3. Run all the story tests:

   ```bash
   npx cucumber-js
   ```

   Run one story only:

   ```bash
   npx cucumber-js src/test/resources/features/Story4AssociateTodoWithCategory.feature
   ```

The tests call the server at http://127.0.0.1:4567. If it is not running, every scenario fails.

**Expected result:** `36 scenarios (31 passed, 5 failed)` and
`342 steps (330 passed, 7 skipped, 5 failed)`. The 5 failures are:

| Scenario | Cause |
|---|---|
| Story 4, Associate a todo with a category (Normal Flow), 2 examples | New bug: a todo linked with `POST /todos/:id/categories` is not listed by `GET /categories/:id/todos` |
| Story 5, View the projects of a todo that does not exist (Error Flow), 2 examples | Known bug 2 of Part A: `GET /todos/:id/tasksof` returns projects for an id that does not exist |
| Story 2, Update the completion status of a todo that does not exist, id `1000` | Test data collision, not an API bug (see below) |

> **Note on ids.** The error flows of Stories 2 to 5 use the ids `999` and `1000` as ids that do
> not exist. The server's id counter keeps growing during a run and the clean-up cannot reset it,
> so on a server that has already created that many todos (for example after other test runs)
> the Story 2 example with id `1000` fails because a todo with that id exists. Restarting the
> server before the run avoids it.
>
> The scenarios delete **all** todos, categories and projects, so do not run them against a
> server that holds data you want to keep.

## Repository structure

```
pom.xml                          Maven build (JUnit 5.11, REST Assured 5.5, Surefire 3.5)
package.json, package-lock.json  Node dependencies for the story tests (Cucumber, chai, chai-http)
cucumber.js                      Cucumber configuration
README.md
src/test/java/api/               Part A: JUnit 5 tests
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
  junit-platform.properties      Part A: random order and seed
  features/                      Part B: the five Gherkin feature files
  steps/                         Part B: step definitions
    CommonStepDefs.js            Before hook, shared Background steps, shared error-message check
    Story1StepDefs.js … Story5StepDefs.js   steps specific to each story
  support/
    api.js                       Part B: HTTP client (chai-http) and helpers
session/
  projects_session.sh            replays every command of the projects exploratory session
  todos_session.sh               replays every command of the todos exploratory session
docs/
  session notes, bug summary reports and the written reports (PDF)
```

## Exploratory session scripts

`session/projects_session.sh` replays the 49 curl commands of the projects session in order.
Each command is labelled with the session note it belongs to. Run it against a freshly started
server, because the ids in later notes depend on the earlier commands:

```bash
java -jar runTodoManagerRestAPI-1.5.5.jar     # fresh server, default data
bash session/projects_session.sh | tee projects_session_output.txt
```

The similar `session/todos_session.sh` script replays the 50 curl commands of the todos session
and works in the same way.

## Bugs found

**Part A.** The 19 bugs are documented in the bug summary reports in `docs/`. Each bug has a
summary, description, impact, steps to reproduce and the `KnownBugsTest` tests that reproduce it.
Bugs 1–10 are on the todos endpoints and bugs 11–19 are on the projects endpoints.

**Part B.** The story tests found one new bug, documented in the Cucumber story tests bug summary
report in `docs/`: `GET /categories/:id/todos` omits todos linked via `POST /todos/:id/categories`
(Story 4). The failing Story 5 error flow is Part A bug 2 and is not repeated.

## Reports

The written reports are in the `docs` folder. The Part A report describes the exploratory
testing, the unit test suite, the bugs found, and the lessons learned. The Part B report
describes the story test suite, its structure, the source code repository and the results of
its execution.
