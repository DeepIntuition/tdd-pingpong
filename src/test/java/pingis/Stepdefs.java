package pingis;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class Stepdefs {
    
    WebDriver driver;
    String baseUrl;

    @Test
    public void setupTest() {
    }

    @Before
    public void setUp() throws Exception {
        if(driver == null) driver = new HtmlUnitDriver();
        baseUrl = "http://localhost:8080/";
    }

    private void get(String page){
        driver.get(baseUrl + page);
    }

    private boolean contains(String s){
        return driver.getPageSource().contains(s);
    }

    @Given("^.* navigates to the login form$")
    public void navigates_to_the_login_form() throws Throwable {
        get("login");
    }

    @When("^.* inputs their username (.*) and password (.*)$")
    public void inputs_username_and_password(String username, String password) throws Throwable {
        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
    }

    @And("submits the .*")
    public void submits() throws Throwable {
        driver.findElement(By.id("submit")).click();
    }

    @Then("^.* is successfully authenticated$")
    public void successfully_authenticated() throws Throwable {
        assertFalse(contains("error"));
    }

    @Then("^.* is not authenticated$")
    public void not_authenticated() throws Throwable {
        assertTrue(contains("error"));
    }

    @After
    public void tearDown() {
        driver.close();
        driver.quit();
    }
}
