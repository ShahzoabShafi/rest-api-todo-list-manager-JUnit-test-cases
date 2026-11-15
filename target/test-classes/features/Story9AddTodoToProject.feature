@projects @story9
Feature: Add a todo to a project
  As a user,
  I want to add a task to a project,
  so that my work is organised under the right project.

  Background:
    Given the server is running
    And the following TODOs exist:
      | title             | doneStatus | description         |
      | COMP 202 homework | false      | finish part 2       |
      | Study ECSE 316    | true       | do practice midterm |
      | Meal prep         | false      | make nasi lemak     |
    And the following projects exist:
      | title            | completed | active | description          |
      | ECSE 429 Project | false     | true   | testing deliverables |
      | Capstone Design  | false     | true   | final year design    |
    And the TODO titled "Study ECSE 316" is a task of the project "Capstone Design"

  Scenario Outline: Add an existing todo to a project (Normal Flow)
    Given a project with the title "<project>" exists
    And the TODO titled "<todo>" is not a task of any project
    When a user adds the TODO titled "<todo>" to the project "<project>"
    Then the project "<project>" lists the TODO titled "<todo>" as a task
    And the TODO titled "<todo>" lists the project "<project>"
    And the user is notified of the completion of the association operation

    Examples:
      | project          | todo              |
      | ECSE 429 Project | COMP 202 homework |
      | Capstone Design  | Meal prep         |

  Scenario Outline: Create a new todo through a project (Alternate Flow)
    Given a project with the title "<project>" exists
    When a user adds a new task titled "<todo>" with description "<description>" to the project "<project>"
    Then the total number of todos increases by 1
    And a TODO with the title "<todo>" exists
    And the project "<project>" lists the TODO titled "<todo>" as a task
    And the TODO titled "<todo>" lists the project "<project>"
    And the user is notified of the completion of the creation operation

    Examples:
      | project          | todo           | description          |
      | ECSE 429 Project | Write Story 9  | feature and steps    |
      | Capstone Design  | Draft proposal | two pages for review |

  Scenario Outline: Add a todo that does not exist to a project (Error Flow)
    Given a project with the title "<project>" exists
    And no TODO with the id "<id>" exists
    When a user adds the TODO with id "<id>" to the project "<project>"
    Then the tasks of the project "<project>" are unchanged
    And the user is notified of the non-existence error with the message "<message>"

    Examples:
      | project          | id     | message                                   |
      | ECSE 429 Project | 999999 | Could not find thing matching value for id |
      | Capstone Design  | 888888 | Could not find thing matching value for id |

  Scenario Outline: Add a todo to a project that does not exist (Error Flow)
    Given no project with the id "<id>" exists
    And the TODO titled "<todo>" is not a task of any project
    When a user adds the TODO titled "<todo>" to the project with id "<id>"
    Then the TODO titled "<todo>" is not a task of any project
    And the user is notified of the non-existence error with the message "<message>"

    Examples:
      | id     | todo              | message                                                      |
      | 999999 | COMP 202 homework | Could not find parent thing for relationship projects/999999/tasks |
      | 888888 | Meal prep         | Could not find parent thing for relationship projects/888888/tasks |
