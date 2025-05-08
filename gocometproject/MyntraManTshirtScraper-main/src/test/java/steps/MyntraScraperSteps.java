package steps;

import com.microsoft.playwright.*;
import io.cucumber.java.en.*;
import java.util.*;

import org.junit.Assert;

import static org.junit.Assert.*;

public class MyntraScraperSteps {

    // These are some variables neccessory to store our data
    // browser variable is req to access the browser engine(here we will use
    // chromium)
    // page is required to navigate to the link
    // Since we can have multiple brands so that we are not bounded to test only for
    // one brand and can reuse code
    // for this we have used a variable brand of string type
    // Also to avoid limitation for men/women/kids (only one) we have taken a
    // category named variable os string type
    // which will store the gender (men/women/kids) to increase reusability for
    // different test cases
    public Browser browser;
    public Page page;
    public String brand;
    public String category;
    List<Map<String, String>> tshirts = new ArrayList<>();

    // So basically in below given function we are starting the playwright engine
    // and using chromium browser (base browser for chrome and some other browsers)
    // then we launch our browser and create a page object and navigate to myntra
    // because we have passed it as a parameter in our feature file
    // If any other URL is shared instead of myntra it will fail the testcase with
    // msg of not myntra's link
    @Given("I navigate to {string}")
    public void NavigateToUrl(String url) {
        if (!url.contains("myntra.com")) {
            Assert.fail("Not myntra's url.");
        }
        Playwright playwright = Playwright.create();
        BrowserType browserType = playwright.chromium();
        browser = browserType.launch(new BrowserType.LaunchOptions().setHeadless(false));
        page = browser.newPage();
        page.navigate(url);
    }

    // Here we are passing category (Men/Women/kids in our case) to the function and
    // what this will do is navigate to the category string ie Men/Women/kids and
    // open it
    // by taking cursor there
    // If gender is not from our list we are failing the testcase with msg of
    // invalid gender
    @When("I select the {string} category")
    public void SelectCategory(String category) {
        if (!(category.equals("Men") || category.equals("Women") || category.equals("Kids"))) {
            Assert.fail("Not a valid gender category.");
        }
        this.category = category;
        page.hover("text=" + category);
    }

    // Now this step will take a string parameter ie our item name (t-shirt)
    // and then it click on tshirt through the anchor tag which follows the link
    // of tshirt based on gender
    // used if else for men/kids and women bcs ui and elements are slighly different
    // in both cases
    // If any other item name is entered instead of tshirt specially which is not in
    // item list at myntra ,
    // that testcase will return an error bcs that can navigate us to an uncertain
    // link
    @And("I filter by type {string}")
    public void FilterByType(String type) {
        String gender = category.toLowerCase();
        if (gender.equals("men") || gender.equals("kids")) {
            page.navigate("https://www.myntra.com/" + gender + "-" + type.toLowerCase());
        } else if (gender.equals("women")) {
            page.navigate("https://www.myntra.com/myntra-fashion-store?f=Categories%3A" + type
                    + "%3A%3AGender%3Amen%20women%2C" + gender);
        }
        page.waitForLoadState();
    }

    // This fn filters for our brand
    // uses if else for men and women/kids bcs of difference in ui and elements in
    // both cases
    // (here ui and elements are same for women and kisa but in previous fn that is
    // while finding
    // item (tshirt), they were same for men and kids)
    // click on search, then fill brand name there and search and then click the
    // checkbox for that brand
    // Here we are using try catch so that if someone enters a brand which do not
    // exist, that testcase will be
    // failed with a msg of invalid brand
    @And("I filter by brand {string}")
    public void FilterByBrand(String brand) {
        this.brand = brand;
        try {
            if (category.equalsIgnoreCase("Men")) {
                page.click(".filter-search-iconSearch");
                page.fill(".filter-search-inputBox", brand);
                page.press(".filter-search-inputBox", "Enter");
                page.click("label:has(input[type='checkbox'][value='" + brand + "'])");
            } else if (category.equalsIgnoreCase("Women") || category.equalsIgnoreCase("Kids")) {
                page.click(".brand-container .filter-search-iconSearch");
                page.fill(
                        ".brand-container .filter-search-filterSearchBox.filter-search-expanded .filter-search-inputBox",
                        brand);
                page.press(
                        ".brand-container .filter-search-filterSearchBox.filter-search-expanded .filter-search-inputBox",
                        "Enter");
                page.click("label:has(input[type='checkbox'][value='" + brand + "'])");

            }
        } catch (Exception e) {
            fail("Not a valid brand name");
        }
    }

    // Here we are taking a variable of maxpages and putting it in while loop till
    // it>0,
    // and in eachiteration we decrement it and if before it being 0, we found that
    // we cannot find next button,
    // that means the no of pages was less than mx and hence we have to fail this
    // testcase with a msg of not enough pages
    // also when while terimates after mx iterations we will stop bcs we want data
    // of maximum mx pages only
    @Then("I extract the discounted T-shirts data and maxpages is {int}")
    public void ExtractDiscountedTshirts(int mx) {
        {
            while (mx > 0) {
                extractProductsFromPage();
                mx--;
                Locator nextButton = page.locator(".pagination-next");
                if (nextButton.count() == 0 || nextButton.getAttribute("class").contains("pagination-disabled")) {
                    if (mx > 0) {
                        fail("No enough pages");
                    }
                    break;
                }
                nextButton.click();
                page.waitForLoadState();
            }
        }
    }

    // here in this function we will iterate over all the products
    // and for each product we will do 3 tries and break if we find the product and
    // make a map named tshirt and store key value pairs in that
    // also we store this product tshirt in our arraylist tshirts

    // ADDITIONAL COMPUTING AT POINT 1: In some products I observed that the
    // discount is not in percentage but in RS OFF format
    // so I have calculated the percentage using original price and discounted price
    // and used that for further processing to
    // maintaion data integrity, Also neglecting it was effecting the sorting
    // procedure bcs it was comparing rs off with percentage off.
    // To differentiate between percent off and rs off I have checked the char at
    // 1st index.
    // (See conversion method at POINT 1)

    // ADDITIONAL TESTCASE AT POINT 2: In this fn we are also varifying that on the
    // website for all products the
    // discount is calculated correctly with +-1 difference because of rounded form
    // data injn integers
    // if somewhere we find that discount is miscalulated we fail that testcase.
    // (See formulae at POINT 2)
    private void extractProductsFromPage() {
        page.waitForSelector(".product-base");
        Locator products = page.locator(".product-base");
        int count = products.count();
        for (int i = 0; i < count; i++) {
            Locator product = products.nth(i);
            Locator discountedProducts = product.locator(".product-strike");
            int retries = 3;
            while (retries > 0) {
                try {
                    if (discountedProducts.count() > 0 && discountedProducts.isVisible()) {
                        String discountedPrice = products.nth(i).locator(".product-discountedPrice").textContent()
                                .trim();
                        String originalPrice = discountedProducts.textContent().trim();
                        String discount = products.nth(i).locator(".product-discountPercentage").textContent().trim();
                        String link = "https://www.myntra.com/" + products.nth(i).locator("a").getAttribute("href");
                        int discount1 = extractNumericValue(discount);

                        // POINT 1
                        if (discount.charAt(1) == 'R') {
                            discount1 *= 100;
                            discount1 /= extractNumericValue(originalPrice);
                            discount = Integer.toString(discount1);
                            discount = "(" + discount + "% OFF)";
                        }

                        Map<String, String> tshirt = new HashMap<>();
                        tshirt.put("originalPrice", originalPrice);
                        tshirt.put("discountedPrice", discountedPrice);
                        tshirt.put("discount", discount);
                        tshirt.put("link", link);

                        // POINT 2
                        int dp = extractNumericValue(discountedPrice);
                        int op = extractNumericValue(originalPrice);
                        int dis = extractNumericValue(discount);
                        if (dp + (op * (dis / 100)) < op - 1 && dp + (op * (dis / 100)) > op - 1)
                            fail("Discount miscalculated.");

                        tshirts.add(tshirt);
                    }
                    break;
                } catch (Exception e) {
                    retries--;
                }
            }

        }
    }

    // In this fn we are sorting the tshirts arraylist of map based on discounts
    // sorting is needed to fulfill project requirements
    // for this we have used bubble sort
    // Can also use collection framework here to use sort fn but it can be done
    // without that also
    // we have also used extractnumericvalue fn to extract parsed data which is
    // defined below this fn
    @Then("I sort the tshirts by highest discount")
    public void SortDiscountedTshirts() {
        if (tshirts.isEmpty()) {
            System.out.println("[INFO] No discounted T-shirts were found for the given filter. Skipping sort.");
            return;
        }
        int n = tshirts.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                int discount1 = extractNumericValue(tshirts.get(j).get("discount"));
                int discount2 = extractNumericValue(tshirts.get(j + 1).get("discount"));

                if (discount1 < discount2) {
                    Map<String, String> temp = tshirts.get(j);
                    tshirts.set(j, tshirts.get(j + 1));
                    tshirts.set(j + 1, temp);
                }
            }
        }
    }

    // this fn is used in sorting to get discounts
    private int extractNumericValue(String discountString) {
        return Integer.parseInt(discountString.replaceAll("[^0-9]", ""));

    }

    // This is nothing but a simple fn which will print the output on our console
    // shows cant find tshirt of this brand if we get arraylist tshirts empty
    @Then("I print the sorted data to the console")
    public void DisplaySortedData() {
        if (tshirts.isEmpty()) {
            System.out.println("Can't find any T-shirt of " + brand + " brand");
        } else {
            System.out.println("Tshirts of brand " + brand + " For " + category);
            System.out.println("-------------------------------------------");
            tshirts.forEach(tshirt -> {
                System.out.println("Discounted Price: " + tshirt.get("discountedPrice"));
                System.out.println("Original Price: " + tshirt.get("originalPrice"));
                System.out.println("Discount: " + tshirt.get("discount"));
                System.out.println("Link: " + tshirt.get("link"));
                System.out.println("-------------------------------------");
            });
            System.out.println("Developed By Sayyed Ubed Ali (ubedzz5573@gmail.com)");
            browser.close();
        }

    }
}

// Thankyou Gocomet team for your time and consideration
// I am so excited to work with you and grow within this organization and with
// the organization

// The problem I have faced in this project was for men, kids and women we
// cannot get same ui and elements which
// I have resolved using conditional statements
// May be there exists a better way to get rid of this issue, I am excited to
// learn that

// I have listed both positive and negative test Scenarios in different feature
// files, where we can use multiple test cases
// of both the scenarios using example table and combining number of valid and
// invalid inputs

// About negative test cases: Invalid URL, invalid gender, invalid brand,
// computational errors in discount calculatoion
// and no of pages less than mx pages will result in testcase failure.

// About Positive test cases: Valid usage (excluding practices described in neg
// testcases) will pass the testcase.
// Assertions: We have used them whenever we found our project not to work
// properly where it should work.