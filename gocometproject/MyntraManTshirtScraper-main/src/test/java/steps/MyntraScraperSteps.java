package steps;
import com.microsoft.playwright.*;
import io.cucumber.java.en.*;
import java.util.*;
import static org.junit.Assert.*;


public class MyntraScraperSteps {

    //These are some variables neccessory to store our data
    //browser variable is req to access the browser engine(here we will use chromium)
    //page is required to navigate to the link
    //Since we can have multiple brands so that we are not bounded to test only for one brand and can reuse code
    //for this we have used a variable brand of string type
    //Also to avoid limitation for men/women (only one) we have taken a category named variable os string type
    //which will store the gender (men/women) to increase reusability for different test cases
    public Browser browser;
    public Page page;
    public String brand;
    public String category;
    public Boolean valid;
    List<Map<String, String>> tshirts = new ArrayList<>();


    //So basically in below given function we are starting the playwright engine
    // and using chromium browser (base browser for chrome and some other browsers)
    // then we launch our browser and create a page object and navigate to myntra 
    // because we have passed it as a parameter in our feature file
    //Exception is given because we dont want our project to crash
    @Given("I navigate to {string}")
    public void NavigateToUrl(String url) {
        valid=true;
        if (!url.contains("myntra.com")) {
                System.err.println("This is not Myntra's URL.");
                valid=false;
                return;
            }
        try{
            Playwright playwright = Playwright.create();
            BrowserType browserType = playwright.chromium();
            browser = browserType.launch(new BrowserType.LaunchOptions().setHeadless(false));
            page = browser.newPage();
            page.navigate(url);
        }catch (Exception e) {
           if(valid) fail("Error in going to URL: " + e.getMessage());
        }
    }


    // Here we are passing category (Men/Women/kids in our case) to the function and 
    //what this will do is navigate to the category string ie Men/Women/kids and open it
    // by taking cursor there 
    @When("I select the {string} category")
    public void SelectCategory(String category) {
        if(!(category.equals("Men") || category.equals("Women") || category.equals("Kids"))){
            valid=false;
        }
        try {
            this.category=category;
            page.hover("text=" + category);
        } catch (Exception e) {
            if(valid) fail("Error in going to this gender: " + e.getMessage());
        }
    }


    // Now this step will take a string parameter ie our item name (t-shirt)
    //and then it click on tshirt through the anchor tag which follows the link
    //of tshirt based on gender
    //used if else for men/kids and women bcs ui and elements are slighly different in both cases
    //Similar to prev fn here also I have added try catch block to prevent project failure
    //if someone puts an invalid gender it will show an error msg
    @And("I filter by type {string}")
    public void FilterByType(String type) {
        try {
            String gender = category.toLowerCase();
    
            if (gender.equals("men") || gender.equals("kids")) {
                page.navigate("https://www.myntra.com/"+ gender +"-" + type.toLowerCase());
            } else if (gender.equals("women")) {
                page.navigate("https://www.myntra.com/myntra-fashion-store?f=Categories%3A" + type + "%3A%3AGender%3Amen%20women%2C"+gender);
            }
            page.waitForLoadState(); 
        } catch (Exception e) {
            if(!valid) System.err.println("Website dont have this item type " + e.getMessage());
            if(valid) fail("cant work with correct inputs");
        }
    }
    
//This fn filters for our brand 
//uses if else for men and women/kids bcs of difference in ui and elements in both cases
//(here ui and elements are same for women and kisa but in previous fn that is while finding
// item (tshirt), they were same for men and kids)
//click on search, then fill brand name there and search and then click the checkbox for that brand
//here also we will use try catch block to avoid project failure
    @And("I filter by brand {string}")
    public void FilterByBrand(String brand) {
        this.brand = brand;
        try {
            if(category.equalsIgnoreCase("Men")){
            page.click(".filter-search-iconSearch");
            page.fill(".filter-search-inputBox", brand);
            page.press(".filter-search-inputBox", "Enter");
            page.locator("input[type='checkbox'][value='" + brand + "']").dispatchEvent("click"); 
        }
        else if(category.equalsIgnoreCase("Women") || category.equalsIgnoreCase("Kids")){
        page.click(".brand-container .filter-search-iconSearch");
        page.fill(".brand-container .filter-search-filterSearchBox.filter-search-expanded .filter-search-inputBox", brand);
        page.press(".brand-container .filter-search-filterSearchBox.filter-search-expanded .filter-search-inputBox", "Enter");
        page.locator("input[type='checkbox'][value='" + brand + "']").dispatchEvent("click");
        }
        } catch (Exception e) {
            if(!valid) System.err.println("Dont have this brand brand: " + e.getMessage());
            if(valid) fail("Dont works with valid inputs");
        }
    }


    //here we are on products page with our filters applied
    //now we will run a infinite while loop and when we find that in an 
    //iteration we cannot find next button we will break the loop
    //in each iteration we will call extractdiscountfn which is defined below (refer there for its description)
    @Then("I extract the discounted T-shirts data")
    public void ExtractDiscountedTshirts() {
        try {
            while (true) {
                extractProductsFromPage();

                Locator nextButton = page.locator(".pagination-next");
                if (nextButton.count() == 0 || nextButton.getAttribute("class").contains("pagination-disabled")) {
                    break;
                }

                nextButton.click();
                page.waitForLoadState();
            }
        } catch (Exception e) {
            if(valid) fail("dont work with valid data");
        }
    }

    //here in this function we will iterate over all the products
    //and for each product we will do 3 tries and break if we find the product and 
    // make a map named tshirt and store key value pairs in that
    //also we store this product tshirt in our arraylist tshirts
    private void extractProductsFromPage() {
        try {
            page.waitForSelector(".product-base");
            Locator products = page.locator(".product-base");
            int count = products.count();

            for (int i = 0; i < count; i++) {
                try {
                    int retries = 3;
                    while (retries > 0) {
                        try {
                            Locator discountedProducts = products.nth(i).locator(".product-strike");
                            if (discountedProducts.count() > 0 && discountedProducts.isVisible()) {
                                String discountedPrice = products.nth(i).locator(".product-discountedPrice").textContent().trim();
                                String originalPrice = discountedProducts.textContent().trim();
                                String discount = products.nth(i).locator(".product-discountPercentage").textContent().trim();
                                String link = "https://www.myntra.com/" + products.nth(i).locator("a").getAttribute("href");

                                Map<String, String> tshirt = new HashMap<>();
                                tshirt.put("originalPrice", originalPrice);
                                tshirt.put("discountedPrice", discountedPrice);
                                tshirt.put("discount", discount);
                                tshirt.put("link", link);

                                tshirts.add(tshirt);
                            }
                            break; 
                        } catch (Exception e) {
                            retries--;
                        }
                    }
                } catch (Exception e) {
                    if(valid) fail("Error in extracting item.");
                }
            }
        } catch (Exception e) {
            if(valid) fail("Error in extracting items.");
        }
    }


//In this fn we are sorting the tshirts arraylist of map based on discounts
//sorting is needed to fulfill project requirements
//for this we have used bubble sort
//Can also use collection framework here to use sort fn but it can be done without that also
//we have also used extractnumericvalue fn to extract parsed data which is defined below this fn
    @Then("I sort the tshirts by highest discount")
    public void SortDiscountedTshirts() {
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

    //this fn is used in sorting to get discounts
    private int extractNumericValue(String discountString) {
        if(!valid) return 0;
        try {
            return Integer.parseInt(discountString.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            if(valid) fail("error in extracting numeric value");
            return 0;
        }
    }


    //This is nothing but a simplefn which will print the output on our console
    //shows cant find tshirt of this brand if we get arraylist tshirts empty
    @Then("I print the sorted data to the console")
    public void DisplaySortedData() {
        if(tshirts.isEmpty()){
            System.out.println("Can't find any T-shirt of "+brand+" brand");
        }else{
            System.out.println("DISCOUNTS FOR BRAND: " + brand);
            System.out.println("*****************************");
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


//Thankyou Gocomet team for your time and consideration
//I am so excited to work with you and grow within this organization and with the organization

//The problem I have faced in this project was for men, kids and women we cannot get same ui and elements which
//I have resolved using conditional statements
//May be there exists a better way to get rid of this issue, I am excited to learn that

//I have listed both positive and negative test Scenarios in different feature files, where we can use multiple test cases
//of both the scenarios using example table and combining number of valid and invalid inputs

//About negative test cases: We will print only the first point of failure that is if we have a testcase where all url,
//gender and brand name are invalid, the fn should prinjt only the first point of failure that is only the invalid url msg,
//other fns will be return without any output.

//Valid Boolean Use: Whenever we find some invalid data like invalid link or gender, in that case our project should not work and if same
//happens like if it not works it is fine but if it works we will fail that testcase and also viceversa means if our project dont work even
//with valid data, it will fail.

//Assertions: We have used thwm whenever we found our project not to work properly where it should work.