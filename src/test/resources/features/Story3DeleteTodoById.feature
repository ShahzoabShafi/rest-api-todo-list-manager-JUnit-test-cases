Feature: Delete a TODO by its identification number
  As a user,
  I want to completely delete a specific task using its identification number when it is no longer needed,
  so that I can remove clutter from my task list.

  Background:
    Given the server is running
    And the following TODOs exist:
      | title             | doneStatus | description         |
      | COMP 202 homework | false      | finish part 2       |
      | Study ECSE 316    | true       | do practice midterm |
      | Meal prep         | false      | make nasi lemak     |
    And the following categories exist:
      | title  | description         |
      | School | school related work |
      | Home   | household tasks     |
    And the following projects exist:
      | title           | completed | active | description    |
      | Winter Semester | false     | true   | courses        |
    And the TODO titled "Study ECSE 316" is associated with the category "School"
    And the TODO titled "Study ECSE 316" is a task of the project "Winter Semester"

  Scenario Outline: Delete an existing TODO by id (Normal Flow)
    Given a TODO with the title "<title>" exists
    When a user deletes the TODO using the id of the TODO titled "<title>"
    Then the TODO titled "<title>" no longer exists
    And the total number of todos decreases by 1
    And the other todos are unchanged
    And the user is notified of the completion of the delete operation

    Examples:
      | title             |
      | COMP 202 homework |
      | Meal prep         |

  Scenario Outline: Delete a TODO that is linked to a category and a project (Alternative Flow)
    Given the TODO titled "<title>" is associated with the category "<category>"
    And the TODO titled "<title>" is a task of the project "<project>"
    When a user deletes the TODO using the id of the TODO titled "<title>"
    Then the TODO titled "<title>" no longer exists
    And the category "<category>" still exists
    And the project "<project>" still exists
    And the category "<category>" no longer lists the TODO titled "<title>"
    And the project "<project>" no longer lists the TODO titled "<title>" as a task
    And the user is notified of the completion of the delete operation

    Examples:
      | title          | category | project         |
      | Study ECSE 316 | School   | Winter Semester |

  Scenario Outline: Delete a TODO whose id does not exist (Error Flow)
    Given no TODO with the id "<id>" exists
    When a user deletes the TODO using the id "<id>"
    Then the total number of todos is unchanged
    And the user is notified of the non-existence error with the message "<message>"

    Examples:
      | id   | message                                          |
      | 999  | Could not find any instances with todos/999      |
      | 1000 | Could not find any instances with todos/1000     |

  Scenario Outline: Delete the same TODO twice (Error Flow)
    Given a TODO with the title "<title>" exists
    And the user has already deleted the TODO titled "<title>" using its id
    When a user deletes the TODO using the same id again
    Then the total number of todos is unchanged
    And the user is notified of the non-existence error with the message "<message>"

    Examples:
      | title     | message                                                 |
      | Meal prep | Could not find any instances with todos/<deleted id>    |
