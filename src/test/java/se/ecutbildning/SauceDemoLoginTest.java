package se.ecutbildning;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Selenium WebDriver-tester för inloggningen på https://www.saucedemo.com/.
 *
 * Varje test öppnar Chrome, gör sitt test och stänger Chrome efteråt.
 * Mitt hopp är att detta ska göra testerna tydliga och minskar risken att ett test påverkar nästa test.
 */
class SauceDemoLoginTest {

    private static final String SAUCEDEMO_URL = "https://www.saucedemo.com/";
    private static final String VALID_USERNAME = "standard_user";
    private static final String VALID_PASSWORD = "secret_sauce";
    private static final boolean DEMO_MODE = Boolean.parseBoolean(System.getProperty("demoMode", "false"));

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void startBrowser() {
        /*
         * Den här funktionen körs före varje test. Det är därflr jag lagt den i @BeforeEach delen.
         * Här startar vi Chrome och ställer in Selenium så att testet kan styra webbläsaren.
         */
        ChromeOptions options = new ChromeOptions();

        /*
         * Headless betyder att Chrome körs utan synligt fönster. Nämndes i kursen att det annars tar massa resurser
         * Standard är true eftersom det passar bäst i GitHub Actions.
         * Om man vill se Chrome lokalt kan man köra Maven med -Dheadless=false.
         */
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));
        if (headless) {
            options.addArguments("--headless=new");
        }

        options.addArguments("--window-size=1440,900");

        /*
         * De här argumenten gör Chrome stabilare i automatiska testmiljöer.
         */
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        driver = new ChromeDriver(options);

        /*
         * WebDriverWait används för att vänta på element på sidan.
         * Det nämndes i kursen att det är bättre än Thread.sleep eftersom vi bara väntar så länge som behövs.
         */
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void closeBrowser() {
        /*
         * Den här funktionen körs efter varje test. Det är därför jag satte den i @AfterEach delen av våra tester.
         * Om Chrome har startats stänger vi den, så den inte ligger kvar i bakgrunden.
         */
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void loginShouldSucceedWithCorrectCredentials() {
        /*
         * För G-kravet:
         * Testa att inloggningen lyckas med rätt användarnamn och rätt lösenord.
         */
        openLoginPage();
        pauseInDemoMode();
        logIn(VALID_USERNAME, VALID_PASSWORD);
        pauseInDemoMode();

        /*
         * Efter lyckad inloggning hamnar användaren på inventory-sidan.
         * Där visas produkterna, och rubriken på sidan är "Products".
         */
        wait.until(ExpectedConditions.urlContains("inventory.html"));
        WebElement pageTitle = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("title")));

        assertTrue(driver.getCurrentUrl().contains("inventory.html"));
        assertEquals("Products", pageTitle.getText());
    }

    @Test
    void loginShouldFailWithWrongUsername() {
        /*
         * Här ger jag mig på VG-kravet:
         * Testa att fel användarnamn ger ett felmeddelande.
         * Lösenordet är rätt här, så felet är bara användarnamnet.
         */
        openLoginPage();
        pauseInDemoMode();
        logIn("wrong_user", VALID_PASSWORD);
        pauseInDemoMode();

        String message = getErrorMessage();
        pauseInDemoMode();

        assertTrue(message.contains("Username and password do not match"));
    }

    @Test
    void loginShouldFailWithWrongPassword() {
        /*
         * också en del av VG-kravet:
         * Testa att fel lösenord ger ett felmeddelande.
         * Användarnamnet är rätt här, så felet är bara lösenordet.
         */
        openLoginPage();
        pauseInDemoMode();
        logIn(VALID_USERNAME, "wrong_password");
        pauseInDemoMode();

        String message = getErrorMessage();
        pauseInDemoMode();

        assertTrue(message.contains("Username and password do not match"));
    }

    private void openLoginPage() {
        /*
         * Öppnar SauceDemo och väntar tills fältet för användarnamn syns.
         * Då börjar testet inte skriva innan sidan är redo.
         */
        driver.get(SAUCEDEMO_URL);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("user-name")));
    }

    private void logIn(String username, String password) {
        /*
         * Fyller i användarnamn och lösenord och klickar på login.
         * Funktionen används i flera tester så att vi slipper upprepa samma kod om och om igen.
         */
        WebElement usernameInput = driver.findElement(By.id("user-name"));
        WebElement passwordInput = driver.findElement(By.id("password"));
        WebElement loginButton = driver.findElement(By.id("login-button"));

        usernameInput.clear();
        usernameInput.sendKeys(username);

        passwordInput.clear();
        passwordInput.sendKeys(password);

        loginButton.click();
    }

    private String getErrorMessage() {
        /*
         * Hämtar felmeddelandet som visas vid misslyckad inloggning.
         * Vi väntar på meddelandet eftersom sidan kan behöva en kort stund efter klicket.
         */
        WebElement errorBox = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-test='error']"))
        );

        return errorBox.getText();
    }

    private void pauseInDemoMode() {
        /*
         * Demo-läge som jag gjorde för min egen skull. Den används bara när man vill titta på testet lokalt.
         *man hade faktiskt kunnat ta bort den, hade den mest för min egen skull
         * I vanliga testkörningar pausas inget, så CI går snabbare.
         */
        if (!DEMO_MODE) {
            return;
        }

        try {
            Thread.sleep(1500);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
