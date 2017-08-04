@dev
Feature: Content check for multiple editor tabs

  Scenario Outline: <user> can examine content in multiple editors
    Given <user> navigates to the task page at <page>
    When <user> clicks the <tab_name> tab
    Then the page contains <content>

    Examples:
      |user   |tab_name                     |content             |page       |
      |User   |test/CalculatorTest.java     |testMultiplication  |task/1/0/  |
      |User   |src/Calculator.java          |Calculator          |task/1/1/  |
