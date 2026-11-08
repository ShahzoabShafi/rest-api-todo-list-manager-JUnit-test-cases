Feature: View all projects related to a TODO
  As a user,
  I want to view all projects related to a particular task,
  so that I can understand the broader context in which the task fits.

  Background:
    Given the server is running
    And the following TODOs exist:
      | title             | doneStatus | description         |
      | COMP 202 homework | false      | finish part 2       |
      | Study ECSE 316    | true       | do practice midterm |
      | Meal prep         | false      | make nasi lemak     |
    And the following projects exist:
      | title           | completed | active | description         |
      | Winter Semester | false     | true   | courses this term   |
      | Exam Prep       | false     | true   | midterm preparation |
      | Home Renovation | false     | false  | kitchen remodel     |
    And the TODO titled "COMP 202 homework" is a task of the project "Winter Semester"
    And the TODO titled "Study ECSE 316" is a task of the project "Winter Semester"
    And the TODO titled "Study ECSE 316" is a task of the project "Exam Prep"

  Scenario Outline: View the projects of a TODO that belongs to one project (Normal Flow)
    Given a TODO with the title "<title>" exists
    When a user requests the projects related to the TODO titled "<title>"
    Then the list of projects contains <count> project(s)
    And the list of projects contains the project "<project>"
    And the user is notified of the completion of the retrieval operation

    Examples:
      | title             | count | project         |
      | COMP 202 homework | 1     | Winter Semester |

  Scenario Outline: View the projects of a TODO that belongs to several projects (Alternative Flow)
    Given a TODO with the title "<title>" exists
    When a user requests the projects related to the TODO titled "<title>"
    Then the list of projects contains <count> project(s)
    And the list of projects contains the project "<firstProject>"
    And the list of projects contains the project "<secondProject>"
    And the user is notified of the completion of the retrieval operation

    Examples:
      | title          | count | firstProject    | secondProject |
      | Study ECSE 316 | 2     | Winter Semester | Exam Prep     |

  Scenario Outline: View the projects of a TODO that has no projects (Alternative Flow)
    Given a TODO with the title "<title>" exists
    And the TODO titled "<title>" is not a task of any project
    When a user requests the projects related to the TODO titled "<title>"
    Then the list of projects is empty
    And the user is notified of the completion of the retrieval operation

    Examples:
      | title     |
      | Meal prep |

  Scenario Outline: View the projects of a TODO that does not exist (Error Flow)
    Given no TODO with the id "<id>" exists
    When a user requests the projects related to the TODO with id "<id>"
    Then no list of projects is returned
    And the user is notified of the non-existence error with the message "<message>"

    Examples:
      | id   | message                                       |
      | 999  | Could not find an instance with todos/999     |
      | 1000 | Could not find an instance with todos/1000    |
