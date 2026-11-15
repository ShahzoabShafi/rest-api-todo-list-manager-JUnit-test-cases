@projects @story8
Feature: Update a project's details
  As a user,
  I want to update a project's details,
  so that the information about my work stays accurate.

  Background:
    Given the server is running
    And the following projects exist:
      | title            | completed | active | description          |
      | ECSE 429 Project | false     | true   | testing deliverables |
      | Capstone Design  | false     | false  | final year design    |

  Scenario Outline: Change the description of a project (Normal Flow)
    Given a project with the title "<title>" exists
    When a user updates the description of the project titled "<title>" to "<newDescription>"
    Then the project titled "<title>" has description "<newDescription>"
    And the title, active and completed status of the project titled "<title>" are unchanged
    And the user is notified of the completion of the update operation

    Examples:
      | title            | newDescription          |
      | ECSE 429 Project | add the Part B features |
      | Capstone Design  | paused until next term  |

  Scenario Outline: Mark a project as completed (Alternate Flow)
    Given a project with the title "<title>" exists
    When a user updates the completed status of the project titled "<title>" to "true"
    Then the project titled "<title>" has completed "true"
    And the title, description and active status of the project titled "<title>" are unchanged
    And the user is notified of the completion of the update operation

    Examples:
      | title            |
      | ECSE 429 Project |
      | Capstone Design  |

  Scenario Outline: Update a project that does not exist (Error Flow)
    Given no project with the id "<id>" exists
    When a user updates the description of the project with id "<id>" to "new desc"
    Then the number of projects is unchanged
    And the user is notified of the non-existence error with the message "<message>"

    Examples:
      | id     | message                                                    |
      | 999999 | No such project entity instance with GUID or ID 999999 found |
      | 888888 | No such project entity instance with GUID or ID 888888 found |

  Scenario Outline: Update a project with an invalid completed value (Error Flow)
    Given a project with the title "<title>" exists
    When a user updates the completed status of the project titled "<title>" to "<invalidValue>"
    Then the project titled "<title>" keeps all of its original details
    And the user is notified of the validation error with the message "<message>"

    Examples:
      | title            | invalidValue | message                                        |
      | ECSE 429 Project | yes          | Failed Validation: completed should be BOOLEAN |
      | Capstone Design  | TRUE         | Failed Validation: completed should be BOOLEAN |
