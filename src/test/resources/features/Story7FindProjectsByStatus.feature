@projects @story7
Feature: Find projects by status
  As a user,
  I want to find projects by their status,
  so that I can focus on the ones that are still active.

  Background:
    Given the server is running
    And the following projects exist:
      | title            | completed | active | description           |
      | ECSE 429 Project | false     | true   | testing deliverables  |
      | Capstone Design  | false     | true   | final year design     |
      | Finished Lab     | true      | true   | lab report handed in  |
      | Old Club Plans   | false     | false  | club ideas on hold    |

  Scenario Outline: Find projects by active status (Normal Flow)
    When a user filters the list of projects by active "<active>"
    Then every project in the filtered list has active "<active>"
    And the filtered list of projects includes "<included>"
    And the filtered list of projects does not include "<excluded>"
    And the user is notified of the completion of the filtering operation

    Examples:
      | active | included         | excluded         |
      | true   | ECSE 429 Project | Old Club Plans   |
      | false  | Old Club Plans   | Capstone Design  |

  Scenario Outline: Find projects by active and completed status (Alternate Flow)
    When a user filters the list of projects by active "<active>" and completed "<completed>"
    Then every project in the filtered list has active "<active>" and completed "<completed>"
    And the filtered list of projects includes "<included>"
    And the filtered list of projects does not include "<excluded>"
    And the user is notified of the completion of the filtering operation

    Examples:
      | active | completed | included        | excluded         |
      | true   | true      | Finished Lab    | ECSE 429 Project |
      | true   | false     | Capstone Design | Finished Lab     |

  Scenario Outline: Find projects by a title that does not exist (Error Flow)
    Given no project with the title "<title>" exists
    When a user filters the list of projects by title "<title>"
    Then the filtered list of projects is empty
    And no error is returned for an unmatched filter
    And the user is notified of the completion of the filtering operation

    Examples:
      | title     |
      | Nope      |
      | Gym Plans |
