@projects @story6
Feature: Create a project
  As a user,
  I want to create a project,
  so that I can group related tasks together.

  Background:
    Given the server is running
    And the following projects exist:
      | title           | completed | active | description       |
      | Winter Semester | false     | true   | courses this term |

  Scenario Outline: Create a project with all fields (Normal Flow)
    When a user creates a project with title "<title>", description "<description>" and active "<active>"
    Then a project titled "<title>" exists with description "<description>" and active "<active>"
    And the number of projects increases by 1
    And the user is notified of the completion of the creation operation

    Examples:
      | title       | description     | active |
      | ECSE 429    | testing course  | true   |
      | Summer Trip | plan the travel | false  |

  Scenario Outline: Create a project with only a title (Alternate Flow)
    When a user creates a project with only the title "<title>"
    Then a project titled "<title>" exists with description "" and active "false"
    And the project titled "<title>" is not completed
    And the number of projects increases by 1
    And the user is notified of the completion of the creation operation

    Examples:
      | title          |
      | Grocery Run    |
      | Thesis Writing |

  Scenario Outline: Create a project with an invalid active value (Error Flow)
    When a user creates a project with title "<title>", description "<description>" and active "<invalidActive>"
    Then the number of projects is unchanged
    And the user is notified of the validation error with the message "<message>"

    Examples:
      | title       | description    | invalidActive | message                                     |
      | Bad Value   | should not add | yes           | Failed Validation: active should be BOOLEAN |
      | Shouty Bool | should not add | TRUE          | Failed Validation: active should be BOOLEAN |
