Feature: Associate a TODO with a category
  As a user,
  I want to associate a task with a specific category,
  so that I can classify my activities and find them more easily.

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
      | Urgent | needs attention now |

  Scenario Outline: Associate a TODO with a category (Normal Flow)
    Given a TODO with the title "<title>" exists
    And a category with the title "<category>" exists
    And the TODO titled "<title>" has no categories
    When a user associates the TODO titled "<title>" with the category "<category>"
    Then the categories of the TODO titled "<title>" contain "<category>"
    And the todos of the category "<category>" contain the TODO titled "<title>"
    And the user is notified of the completion of the association operation

    Examples:
      | title             | category |
      | COMP 202 homework | School   |
      | Meal prep         | Home     |

  Scenario Outline: Associate a TODO that already has a category with an additional category (Alternative Flow)
    Given the TODO titled "<title>" is already associated with the category "<firstCategory>"
    When a user associates the TODO titled "<title>" with the category "<secondCategory>"
    Then the categories of the TODO titled "<title>" contain "<firstCategory>"
    And the categories of the TODO titled "<title>" contain "<secondCategory>"
    And the TODO titled "<title>" has 2 categories
    And the user is notified of the completion of the association operation

    Examples:
      | title             | firstCategory | secondCategory |
      | COMP 202 homework | School        | Urgent         |
      | Meal prep         | Home          | Urgent         |

  Scenario Outline: Associate a TODO with a category that does not exist (Error Flow)
    Given a TODO with the title "<title>" exists
    And no category with the id "<categoryId>" exists
    When a user associates the TODO titled "<title>" with the category id "<categoryId>"
    Then the TODO titled "<title>" has no new categories
    And the user is notified of the non-existence error with the message "<message>"

    Examples:
      | title             | categoryId | message                                    |
      | COMP 202 homework | 999        | Could not find thing matching value for id |
      | Meal prep         | 1000       | Could not find thing matching value for id |

  Scenario Outline: Associate a TODO that does not exist with a category (Error Flow)
    Given no TODO with the id "<todoId>" exists
    And a category with the title "<category>" exists
    When a user associates the TODO with id "<todoId>" with the category "<category>"
    Then the todos of the category "<category>" are unchanged
    And the user is notified of the non-existence error with the message "<message>"

    Examples:
      | todoId | category | message                                         |
      | 999    | School   | Could not find parent thing for relationship todos/999/categories |
      | 1000   | Home     | Could not find parent thing for relationship todos/1000/categories |
