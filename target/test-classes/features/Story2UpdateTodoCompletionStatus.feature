Feature: Update the completion status of a TODO
  As a user,
  I want to update the completion status of a task,
  so that I can keep my task information accurate and up to date.

  Background:
    Given the server is running
    And the following TODOs exist:
      | title             | doneStatus | description         |
      | COMP 202 homework | false      | finish part 2       |
      | Study ECSE 316    | true       | do practice midterm |
      | Meal prep         | false      | make nasi lemak     |

  Scenario Outline: Mark an incomplete TODO as done (Normal Flow)
    Given a TODO with the title "<title>" and doneStatus "false" exists
    When a user updates the doneStatus of the TODO titled "<title>" to "true"
    Then the TODO titled "<title>" has doneStatus "true"
    And the title and description of the TODO titled "<title>" are unchanged
    And the user is notified of the completion of the update operation

    Examples:
      | title             |
      | COMP 202 homework |
      | Meal prep         |

  Scenario Outline: Reopen a completed TODO by marking it as not done (Alternative Flow)
    Given a TODO with the title "<title>" and doneStatus "true" exists
    When a user updates the doneStatus of the TODO titled "<title>" to "false"
    Then the TODO titled "<title>" has doneStatus "false"
    And the title and description of the TODO titled "<title>" are unchanged
    And the user is notified of the completion of the update operation

    Examples:
      | title          |
      | Study ECSE 316 |

  Scenario Outline: Update the completion status of a TODO that does not exist (Error Flow)
    Given no TODO with the id "<id>" exists
    When a user updates the doneStatus of the TODO with id "<id>" to "true"
    Then no TODO is created or modified
    And the user is notified of the non-existence error with the message "<message>"

    Examples:
      | id   | message                                  |
      | 999  | No such todo entity instance with GUID or ID 999 found  |
      | 1000 | No such todo entity instance with GUID or ID 1000 found |

  Scenario Outline: Update the completion status of a TODO with an invalid value (Error Flow)
    Given a TODO with the title "<title>" exists
    When a user updates the doneStatus of the TODO titled "<title>" to "<invalidValue>"
    Then the TODO titled "<title>" keeps its original doneStatus
    And the user is notified of the validation error with the message "<message>"

    Examples:
      | title             | invalidValue | message                                                 |
      | COMP 202 homework | maybe        | Failed Validation: doneStatus should be BOOLEAN |
      | Meal prep         | 5            | Failed Validation: doneStatus should be BOOLEAN |
