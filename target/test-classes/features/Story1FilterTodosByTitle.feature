Feature: Filter TODOs by title name
  As a user,
  I want to filter the list of todos by title name,
  so that I can quickly find the tasks I am looking for.

  Background:
    Given the server is running
    And the following TODOs exist:
      | title             | doneStatus | description         |
      | COMP 202 homework | false      | finish part 2       |
      | Study ECSE 316    | true       | do practice midterm |
      | Meal prep         | false      | make nasi lemak     |
      | Meal prep         | true       | make lasagna        |

  Scenario Outline: Filter TODOs by title name (Normal Flow)
    Given a TODO with the title "<title>" exists
    When a user filters the list of todos by title name "<title>"
    Then the filtered list contains <count> todo(s)
    And every todo in the filtered list has the title "<title>"
    And the user is notified of the completion of the filtering operation

    Examples:
      | title             | count |
      | COMP 202 homework | 1     |
      | Study ECSE 316    | 1     |
      | Meal prep         | 2     |

  Scenario Outline: Filter TODOs by a substring of the title name (Alternative Flow)
    Given at least one TODO with a title containing "<substring>" exists
    When a user filters the list of todos by the title substring "<substring>"
    Then the filtered list contains <count> todo(s)
    And every todo in the filtered list has a title that contains "<substring>"
    And the user is notified of the completion of the filtering operation

    Examples:
      | substring | count |
      | COMP      | 1     |
      | 316       | 1     |
      | Meal      | 2     |
      | prep      | 2     |

  Scenario Outline: Filter TODOs by a title name that does not exist (Error Flow)
    Given no TODO with the title "<title>" exists
    When a user filters the list of todos by title name "<title>"
    Then the filtered list of todos is empty
    And the user is notified of the non-existence error with the message "<message>"

    Examples:
      | title        | message                             |
      | Gym workout  | No todos found with title Gym workout |
      | ECSE 999     | No todos found with title ECSE 999    |
      | Meal prepare | No todos found with title Meal prepare |