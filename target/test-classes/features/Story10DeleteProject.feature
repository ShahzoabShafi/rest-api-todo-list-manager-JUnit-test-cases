@projects @story10
Feature: Delete a project
  As a user,
  I want to delete a project I no longer need,
  so that my project list only shows current work.

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
      | Old Club Plans   | false     | false  | club ideas on hold   |
      | Summer Trip      | false     | false  | plan the travel      |
    And the TODO titled "COMP 202 homework" is a task of the project "ECSE 429 Project"
    And the TODO titled "Study ECSE 316" is a task of the project "Capstone Design"

  Scenario Outline: Delete a project that has tasks (Normal Flow)
    Given the project "<project>" lists the TODO titled "<todo>" as a task
    When a user deletes the project titled "<project>"
    Then the project titled "<project>" no longer exists
    And the number of projects decreases by 1
    And a TODO with the title "<todo>" exists
    And the TODO titled "<todo>" is not a task of any project
    And the user is notified of the completion of the delete operation

    Examples:
      | project          | todo              |
      | ECSE 429 Project | COMP 202 homework |
      | Capstone Design  | Study ECSE 316    |

  Scenario Outline: Delete a project that has no tasks (Alternate Flow)
    Given a project with the title "<project>" exists
    And the project "<project>" has no tasks
    When a user deletes the project titled "<project>"
    Then the project titled "<project>" no longer exists
    And the number of projects decreases by 1
    And the total number of todos is unchanged
    And the user is notified of the completion of the delete operation

    Examples:
      | project        |
      | Old Club Plans |
      | Summer Trip    |

  Scenario Outline: Delete the same project twice (Error Flow)
    Given a project with the title "<project>" exists
    And the user has already deleted the project titled "<project>"
    When a user deletes the same project again
    Then the number of projects is unchanged
    And the user is notified of the non-existence error with the message "<message>"

    Examples:
      | project         | message                                            |
      | Old Club Plans  | Could not find any instances with projects/<deleted id> |
      | Capstone Design | Could not find any instances with projects/<deleted id> |

  Scenario Outline: Delete a project that does not exist (Error Flow)
    Given no project with the id "<id>" exists
    When a user deletes the project with id "<id>"
    Then the number of projects is unchanged
    And the user is notified of the non-existence error with the message "<message>"

    Examples:
      | id     | message                                       |
      | 999999 | Could not find any instances with projects/999999 |
      | 888888 | Could not find any instances with projects/888888 |
