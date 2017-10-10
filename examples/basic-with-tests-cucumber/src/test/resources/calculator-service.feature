Feature: Remote Server/Client contract

  Background:
    And a calculator client against http://localhost:8080

  Scenario Outline: Addition
    Given a remote request to add <lhs> and <rhs>
    Then The response should be <sum>
    Examples:
      | lhs | rhs | sum |
      | -1  | 1   | 0   |
      | 0   | 0   | 0   |
      | 1   | 0   | 1   |
      | 0   | 1   | 1   |
      | 1   | 2   | 3   |

  Scenario Outline: Subtraction
    Given a remote request to subtract <rhs> from <lhs>
    Then The response should be <result>
    Examples:
      | lhs | rhs | result |
      | -1  | 1   | -2     |
      | 0   | 0   | 0      |
      | 1   | 0   | 1      |
      | 0   | 1   | -1     |
      | 1   | 2   | -1     |
