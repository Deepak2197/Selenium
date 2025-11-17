import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.awt.Desktop;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DamsMergedAutomation {

    // =================================================================
    // PART 1: API CONFIGURATION (Backend Data)
    // =================================================================
    private static final String API_URL = "https://api.damsdelhi.com/v2_data_model/get_all_plan_by_category_id";
    private static final String USER_ID = "161444";
    private static final String DEVICE_TOKEN = "61797743405";
    private static final String DEVICE_TYPE = "3";
    private static final String STREAM_ID = "1";
    private static final String API_VERSION = "25";
    private static final String DEVICE_INFO_JSON = "{\"model\":\"chrome_driver\",\"os\":\"windows\",\"app\":\"dams\",\"manufacturer\":\"selenium\"}";
    
    private static String apiResponseData = "";
    private static int apiResponseCode = 0;

    // =================================================================
    // PART 2: SELENIUM CONFIGURATION (Frontend Automation)
    // =================================================================
    private WebDriver driver;
    private WebDriverWait wait;
    private WebDriverWait longWait;
    private final String STUDENT_PHONE = "+919456628016";
    private final String STUDENT_OTP = "2000";

    // Constructor for Selenium Setup
    public DamsMergedAutomation() {
        System.out.println("⚙️ Setting up Chrome Driver...");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--remote-allow-origins=*");

        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        this.longWait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    // =================================================================
    // MAIN EXECUTION METHOD
    // =================================================================
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║   DAMS COMPLETE AUTOMATION SYSTEM          ║");
        System.out.println("╚════════════════════════════════════════════╝\n");

        // 
        // PHASE 1: API Data Extraction & Reporting
        System.out.println("🔵 PHASE 1: API REPORT GENERATION");
        runApiPhase();

        // 

[Image of login process flow diagram]

        // PHASE 2: Selenium Browser Automation
        System.out.println("\n🔵 PHASE 2: BROWSER AUTOMATION");
        runSeleniumPhase();
    }

    // =================================================================
    // PHASE 1 METHODS: API LOGIC
    // =================================================================
    private static void runApiPhase() {
        try {
            fetchDataFromApi();
            if (!apiResponseData.isEmpty()) {
                generateHtmlReport();
            } else {
                System.out.println("❌ No API data to report.");
            }
        } catch (Exception e) {
            System.out.println("❌ API Phase Error: " + e.getMessage());
        }
    }

    private static void fetchDataFromApi() {
        System.out.println("   Connecting to API Endpoint...");
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Origin", "https://www.damsdelhi.com");

            String jsonPayload = String.format(
                "{\"user_id\": \"%s\", \"device_token\": \"%s\", \"device_type\": \"%s\", \"stream_id\": \"%s\", \"api_version\": \"%s\", \"device_info\": %s}",
                USER_ID, DEVICE_TOKEN, DEVICE_TYPE, STREAM_ID, API_VERSION, DEVICE_INFO_JSON
            );

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            apiResponseCode = conn.getResponseCode();
            System.out.println("   Status Code: " + apiResponseCode);

            InputStream is = (apiResponseCode >= 200 && apiResponseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }

            apiResponseData = response.toString();
            System.out.println("   ✓ Data stored successfully (" + apiResponseData.length() + " chars)");

        } catch (Exception e) {
            System.out.println("   ❌ API Fetch Error: " + e.getMessage());
            apiResponseData = "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private static void generateHtmlReport() {
        System.out.println("   Generating HTML Report...");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "DAMS_API_Report_" + timestamp + ".html";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><title>DAMS API Data</title>");
        html.append("<style>body{font-family:'Segoe UI',sans-serif;background:#f4f7f6;padding:40px;}.container{background:white;padding:30px;border-radius:10px;box-shadow:0 4px 15px rgba(0,0,0,0.1);max-width:1200px;margin:auto;}.header{border-bottom:2px solid #eee;padding-bottom:20px;margin-bottom:20px;}h1{color:#333;margin:0;}.info{color:#777;font-size:14px;}.status-badge{display:inline-block;padding:5px 10px;border-radius:5px;color:white;font-weight:bold;background:" + (apiResponseCode == 200 ? "#28a745" : "#dc3545") + ";}.json-container{background:#2d2d2d;color:#f8f8f2;padding:20px;border-radius:8px;overflow-x:auto;font-family:Consolas,monospace;white-space:pre-wrap;word-wrap:break-word;}.string{color:#a5d6ff;}.number{color:#ff7b72;}.boolean{color:#79c0ff;}.key{color:#7ee787;}</style></head><body>");
        html.append("<div class='container'><div class='header'><h1>API Response Data</h1><p class='info'>Generated: " + new Date() + "</p><div class='status-badge'>Status: " + apiResponseCode + "</div></div>");
        html.append("<h3>Stored JSON Data:</h3><div class='json-container' id='json-display'>Loading...</div></div>");
        html.append("<script>const rawData = " + apiResponseData + "; function syntaxHighlight(json){if(typeof json!='string'){json=JSON.stringify(json,undefined,4);}return json.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/(\"(\\\\u[a-zA-Z0-9]{4}|\\\\[^u]|[^\\\\\"])*\"(\\s*:)?|\\b(true|false|null)\\b|-?\\d+(?:\\.\\d*)?(?:[eE][+\\-]?\\d+)?)/g,function(match){var cls='number';if(/^\"/.test(match)){if(/:$/.test(match)){cls='key';}else{cls='string';}}else if(/true|false/.test(match)){cls='boolean';}return '<span class=\"'+cls+'\">'+match+'</span>';});}document.getElementById('json-display').innerHTML=syntaxHighlight(rawData);</script></body></html>");

        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(html.toString());
            System.out.println("   ✓ Report created: " + fileName);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(new File(fileName).toURI());
        } catch (IOException e) {
            System.out.println("   ❌ HTML Write Error: " + e.getMessage());
        }
    }

    // =================================================================
    // PHASE 2 METHODS: SELENIUM LOGIC
    // =================================================================
    private static void runSeleniumPhase() {
        DamsMergedAutomation bot = new DamsMergedAutomation();
        try {
            // Step 1: Login
            if (bot.loginToDamsDelhi()) {
                bot.printPageDetails();
                
                // Step 2: Payment Flow
                if (bot.proceedToPayment()) {
                    System.out.println("\n✅ AUTOMATION COMPLETED SUCCESSFULLY");
                } else {
                    System.out.println("\n⚠️ Payment flow interrupted.");
                }
            } else {
                System.out.println("\n💥 Login failed. Aborting payment flow.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bot.closeBrowser();
        }
    }

    public boolean loginToDamsDelhi() {
        System.out.println("🔐 Starting Login...");
        try {
            driver.get("https://www.damsdelhi.com/");
            Thread.sleep(3000);

            // Click Sign In
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(), 'Sign in') or contains(text(), 'Sign In')]"))).click();
            System.out.println("   ✓ Clicked Sign In");
            Thread.sleep(2000);

            // Enter Phone
            WebElement phoneField = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='tel' or @type='number']")));
            phoneField.clear();
            phoneField.sendKeys(STUDENT_PHONE);
            System.out.println("   ✓ Entered Phone");
            Thread.sleep(1000);

            // Request OTP
            driver.findElement(By.className("common-bottom-btn")).click();
            Thread.sleep(2000);

            // Handle Logout Popup if present
            try {
                WebElement logoutBtn = driver.findElement(By.xpath("//button[contains(@class, 'btndata') and contains(text(), 'Logout')]"));
                if (logoutBtn.isDisplayed()) {
                    logoutBtn.click();
                    System.out.println("   ✓ Handled Logout Popup");
                    Thread.sleep(2000);
                }
            } catch (Exception e) { /* Ignore */ }

            // Enter OTP
            WebElement otpField = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[contains(@placeholder, 'OTP') or contains(@placeholder, 'otp')]")));
            otpField.clear();
            otpField.sendKeys(STUDENT_OTP);
            System.out.println("   ✓ Entered OTP");
            Thread.sleep(1000);

            // Submit OTP
            driver.findElement(By.className("common-bottom-btn")).click();
            System.out.println("   ✓ Submitted OTP");
            Thread.sleep(5000);

            // Verify Dashboard
            if (!driver.getCurrentUrl().toLowerCase().contains("dashboard")) {
                System.out.println("   ℹ️ Navigating to Dashboard manually...");
                try {
                    driver.findElement(By.xpath("//a[contains(text(), 'Dashboard') or contains(text(), 'Profile')]")).click();
                    Thread.sleep(3000);
                } catch (Exception e) { 
                    System.out.println("   ⚠️ Could not click Dashboard link, continuing...");
                }
            }
            return true;

        } catch (Exception e) {
            System.out.println("   ❌ Login Error: " + e.getMessage());
            return false;
        }
    }

    public boolean proceedToPayment() {
        System.out.println("\n💳 Starting Payment Process...");
        try {
            Thread.sleep(2000);
            
            // 1. Click "GO PRO" (Robust Search)
            System.out.println("   Searching for GO PRO...");
            WebElement goProBtn = findGoProButton();
            if (goProBtn != null) {
                highlightElement(goProBtn);
                clickElement(goProBtn, "GO PRO");
                Thread.sleep(4000);
            }

            // 2. Click "Buy Now"
            WebElement buyNowBtn = longWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("h5.buy-now-btn")));
            highlightElement(buyNowBtn);
            clickElement(buyNowBtn, "Buy Now");
            Thread.sleep(3000);

            // 3. Select "12 Months"
            WebElement twelveMonths = longWait.until(ExpectedConditions.elementToBeClickable(By.xpath("//h3[contains(text(), '12 Months') or text()='12 Months']")));
            clickElement(twelveMonths, "12 Months Plan");
            Thread.sleep(2000);

            // 3b. Handle Cart Conflict Modal
            try {
                WebElement yesBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[.//span[text()='Yes'] or contains(text(), 'Yes')]")));
                clickElement(yesBtn, "Yes (Cart Modal)");
                Thread.sleep(3000);
            } catch (Exception e) { /* No modal */ }

            // 4. Click Today Button
            clickElement(wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[contains(@class, 'today-button')]"))), "Today Button");
            Thread.sleep(2000);

            // 5. Click Continue
            clickElement(longWait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[contains(text(), 'Continue')]"))), "Continue");
            Thread.sleep(3000);

            // 6. Click h6 (if applicable)
            try {
                List<WebElement> h6s = driver.findElements(By.tagName("h6"));
                if (!h6s.isEmpty()) {
                    clickElement(h6s.get(0), "Offer/Item Selection");
                    Thread.sleep(2000);
                }
            } catch(Exception e) {}

            // 7. Select Payment Method (Paytm)
            WebElement paytmRadio = longWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@type='radio' and @value='Paytm']")));
            clickElement(paytmRadio, "Paytm Radio");
            Thread.sleep(2000);

            // 8. Find and Click "Pay Now" (Ant Design)
            System.out.println("   Searching for Pay Now button...");
            WebElement payNowBtn = findPayNowButton();
            
            if(payNowBtn != null) {
                highlightElement(payNowBtn);
                String oldUrl = driver.getCurrentUrl();
                
                clickElement(payNowBtn, "Pay Now ($0)");
                Thread.sleep(1000);
                clickElement(payNowBtn, "Pay Now ($0) Confirm"); // Double click sometimes needed
                
                // Wait for navigation
                try {
                    new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> !d.getCurrentUrl().equals(oldUrl));
                    System.out.println("   ✅ Navigation detected!");
                } catch (TimeoutException t) {
                    System.out.println("   ⚠️ URL didn't change, checking page content...");
                }
                Thread.sleep(5000);
                
                takeScreenshot("Payment_Success");
                return true;
            } else {
                System.out.println("   ❌ Could not find Pay Now button.");
                return false;
            }

        } catch (Exception e) {
            System.out.println("   ❌ Payment Error: " + e.getMessage());
            takeScreenshot("Payment_Error");
            return false;
        }
    }

    // =================================================================
    // HELPER METHODS
    // =================================================================
    
    // Robust finder for GO PRO
    private WebElement findGoProButton() {
        try {
            return longWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("button.btn")));
        } catch (Exception e) {
            try {
                return driver.findElement(By.xpath("//button[@class='btn'][.//strong[contains(text(), 'Go Pro')]]"));
            } catch (Exception ex) { return null; }
        }
    }

    // Robust finder for Pay Now (Ant Design)
    private WebElement findPayNowButton() {
        By[] locators = {
            By.xpath("//button[contains(@class, 'ant-btn-primary') and contains(@class, 'ant-btn-block') and contains(., 'Pay Now')]"),
            By.xpath("//div[contains(@class, 'ant-modal')]//button[contains(text(), 'Pay Now')]"),
            By.cssSelector("button.ant-btn.ant-btn-primary.ant-btn-block")
        };
        
        for (By loc : locators) {
            try { return longWait.until(ExpectedConditions.elementToBeClickable(loc)); } 
            catch (Exception e) {}
        }
        return null;
    }

    private void clickElement(WebElement el, String name) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", el);
            el.click();
            System.out.println("   ✓ Clicked " + name);
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
            System.out.println("   ✓ Clicked " + name + " (JS Force)");
        }
    }

    private void highlightElement(WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].style.border='3px solid red';", element);
        } catch(Exception e) {}
    }

    private void takeScreenshot(String name) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String filename = "DAMS_" + name + "_" + System.currentTimeMillis() + ".png";
            Files.copy(src.toPath(), new File(filename).toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("   📷 Screenshot saved: " + filename);
        } catch (IOException e) {
            System.out.println("   ❌ Screenshot failed");
        }
    }

    private void printPageDetails() {
        System.out.println("   📄 Current URL: " + driver.getCurrentUrl());
        System.out.println("   📄 Page Title: " + driver.getTitle());
    }

    private void closeBrowser() {
        if (driver != null) {
            System.out.println("\n🌐 Closing Browser...");
            driver.quit();
        }
    }
}
