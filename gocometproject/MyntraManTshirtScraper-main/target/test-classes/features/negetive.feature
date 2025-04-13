Feature: To Scrape discounted T-shirts on Myntra for particular gender and particular brand (Negative Testcases are managed here)

  Scenario Outline: Extract discounted T-shirts based on discount in descending order
    Given I navigate to "<links>"
    When I select the "<gender>" category
    And I filter by type "Tshirts"
    And I filter by brand "<brand>"
    Then I extract the discounted T-shirts data
    Then I sort the tshirts by highest discount
    Then I print the sorted data to the console

  Examples:
    | brand    | gender   | links                    |
    | Bike     | Bits     | https://www.leetcode.com |
    | Bajaj    | NoWomen  | https://www.ubed.com     |
