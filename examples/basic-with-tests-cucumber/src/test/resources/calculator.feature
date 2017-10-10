Feature: Calculator Operations

  Scenario Outline: Addition
    Given <lhs> + <rhs>
    Then The result should be <sum>
    Examples:
      | lhs | rhs | sum |
      | -1  | 1   | 0   |
      | 0   | 0   | 0   |
      | 1   | 0   | 1   |
      | 0   | 1   | 1   |
      | 1   | 2   | 3   |

  Scenario Outline: Subtraction
    Given <lhs> - <rhs>
    Then The result should be <result>
    Examples:
      | lhs | rhs | result |
      | -1  | 1   | -2     |
      | 0   | 0   | 0      |
      | 1   | 0   | 1      |
      | 0   | 1   | -1     |
      | 1   | 2   | -1     |
