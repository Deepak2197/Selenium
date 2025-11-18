// NO PACKAGE DECLARATION - Keep in Root Directory
import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.io.FileHandler;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.time.ZoneId;

public class TestWithApi {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static List<String> reportLogs = new ArrayList<>();
    private static String screenshotDir = System.getProperty("user.dir") + "/test-report/screenshots/";
    private static String reportDir = System.getProperty("user.dir") + "/test-report/";

    // --- Constants ---
    private static final String API_URL = "https://api.damsdelhi.com/v2_data_model/get_all_plan_by_category_id";
    private static final String USER_ID_OVERRIDE = "161444";
    private static final String DEVICE_TOKEN_OVERRIDE = "61797743405";
    private static final String DEVICE_TYPE = "3";
    private static final String STREAM_ID = "1";
    private static final String API_VERSION = "25";
    private static final String DEVICE_INFO_MOCK = "{\"model\":\"chrome_driver\",\"os\":\"linux\",\"app\":\"dams\"}";

    public static void main(String[] args) {
        log("🚀 DAMS Automation Started");
        log("Environment: " + (System.getenv("CI") != null ? "GitHub Actions (CI)" : "Local"));

        try {
            // --- 1. Setup Chrome ---
            ChromeOptions options = new ChromeOptions();
            if (System.getenv("CI") != null) {
                options.addArguments("--headless=new");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--window-size=1920,1080");
                options.addArguments("--remote-allow-origins=*");
            } else {
                options.addArguments("--remote-allow-origins=*");
            }
            options.addArguments("--disable-notifications");

            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            wait = new WebDriverWait(driver, Duration.ofSeconds(25));
            log("✅ WebDriver initialized");

            // --- 2. Login ---
            log("📍 Navigating to DAMS...");
            driver.get("https://www.damsdelhi.com/");

            clickElement(By.className("loginbtnSignupbtn"), "Login Button");
            
            WebElement phoneInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".react-international-phone-input")));
            phoneInput.sendKeys("7897897897");
            clickElement(By.className("common-bottom-btn"), "Submit Phone");
            
            waitFor(4);

            // Optional Logout Check
            try {
                WebElement logoutBtn = driver.findElement(By.xpath("//button[contains(text(),'Logout & Continue')]"));
                if(logoutBtn.isDisplayed()) {
                    logoutBtn.click();
                    log("✅ Clicked 'Logout & Continue'");
                }
            } catch (Exception e) { /* Ignore if not found */ }

            // PIN
            log("🔢 Entering PIN...");
            List<WebElement> pinFields = driver.findElements(By.cssSelector("input.otp-field"));
            String[] pin = {"2", "0", "0", "0"};
            for(int i=0; i<pinFields.size(); i++) pinFields.get(i).sendKeys(pin[i]);
            
            clickElement(By.xpath("//button[contains(text(),'Verify & Proceed')]"), "Verify Button");
            log("✅ Login Successful");
            
            waitFor(3);

            // --- 3. Navigation Flow ---
            log("📚 Selecting Category...");
            clickElement(By.xpath("//button[normalize-space()='NEET PG NEXT']"), "Category");
            
            String label = "NURSING (DSSSB, SGPGI, ESIC, RBB, KGMU)";
            clickElement(By.xpath("//span[text()='" + label + "']/parent::label"), "Sub-Category");
            
            waitFor(2);
            
            // --- 4. Purchase Flow ---
            log("💳 Selecting Plan...");
            clickElementJS(By.xpath("//button[contains(., 'Premium') and contains(., 'Go Pro')]"), "Go Pro");
            clickElementJS(By.xpath("//button[contains(@class, 'plan-actions')]//h5[contains(text(), 'Buy Now')]"), "Buy Now");
            clickElementJS(By.xpath("//button[contains(@class, 'boxrate')]//h3[normalize-space(text())='3 Months']/parent::button"), "3 Months Plan");
            clickElementJS(By.xpath("//button[contains(@class,'btn-danger')]//h6[contains(normalize-space(.), 'Place Order')]"), "Place Order");
            
            waitFor(2);
            
            clickElement(By.xpath("//label[.//span[normalize-space(text())='Paytm']]"), "Paytm");
            clickElement(By.xpath("//button[.//span[normalize-space(text())='Pay Now']]"), "Pay Now");

            // --- 5. Data Capture ---
            waitFor(5);
            takeScreenshot("PaymentScreen");
            
            Map<String, String> auth = extractAuthFromStorage();
            log("🔐 Auth Extracted");

            // --- 6. API Call ---
            log("🌐 Calling API...");
            String response = callPlanAPI(auth);
            log("📥 Response received");

            // --- 7. Report ---
            generateHTMLReport(response);

        } catch (Exception e) {
            log("❌ FATAL ERROR: " + e.getMessage());
            e.printStackTrace();
            takeScreenshot("Error");
            generateHTMLReport("{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}");
            throw new RuntimeException(e);
        } finally {
            if(driver != null) driver.quit();
        }
    }

    // --- Helpers ---
    
    private static void clickElement(By locator, String name) {
        try {
            WebElement el = wait.until(ExpectedConditions.elementToBeClickable(locator));
            ((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", el);
            Thread.sleep(500);
            el.click();
            log("✅ Clicked " + name);
        } catch(Exception e) {
            clickElementJS(locator, name);
        }
    }

    private static void clickElementJS(By locator, String name) {
        try {
            WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            ((JavascriptExecutor)driver).executeScript("arguments[0].click();", el);
            log("✅ Clicked (JS) " + name);
        } catch(Exception e) {
            log("❌ Failed to click " + name);
            throw new RuntimeException(e);
        }
    }

    private static void waitFor(int sec) {
        try { Thread.sleep(sec * 1000); } catch(Exception e) {}
    }

    private static void log(String msg) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + time + "] " + msg);
        System.out.flush();
        reportLogs.add("[" + time + "] " + msg);
    }

    private static void takeScreenshot(String name) {
        try {
            new File(screenshotDir).mkdirs();
            File src = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            FileHandler.copy(src, new File(screenshotDir + name + ".png"));
        } catch(Exception e) {
            log("Screenshot failed");
        }
    }

    // --- API & Auth ---

    private static Map<String, String> extractAuthFromStorage() {
        Map<String, String> map = new HashMap<>();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        try {
            String jwt = (String) js.executeScript("return window.localStorage.getItem('jwt_token');");
            if(jwt != null) map.put("jwt_token", jwt);
            
            map.put("user_id", USER_ID_OVERRIDE);
            map.put("device_token", DEVICE_TOKEN_OVERRIDE);
            map.put("time_stamp", String.valueOf(System.currentTimeMillis()));
        } catch(Exception e) {
            log("Auth extract failed");
        }
        return map;
    }

    private static String callPlanAPI(Map<String, String> auth) {
        try {
            if(!auth.containsKey("jwt_token")) return "{\"error\": \"No JWT Token\"}";
            
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + auth.get("jwt_token"));
            conn.setRequestProperty("user_id", auth.get("user_id"));
            conn.setRequestProperty("device_token", auth.get("device_token"));
            conn.setRequestProperty("device_type", DEVICE_TYPE);
            conn.setRequestProperty("time_stamp", auth.get("time_stamp"));
            conn.setRequestProperty("stream_id", STREAM_ID);
            conn.setRequestProperty("api_version", API_VERSION);
            conn.setRequestProperty("device_info", DEVICE_INFO_MOCK);

            conn.setDoOutput(true);
            String payload = String.format("{\"user_id\": \"%s\", \"cat_id\": \"188\"}", auth.get("user_id"));
            
            try(OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes("utf-8"));
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while((line = br.readLine()) != null) response.append(line);
            return response.toString();
        } catch(Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private static void generateHTMLReport(String response) {
        try {
            new File(reportDir).mkdirs();
            String path = reportDir + "DAMS_Report_" + System.currentTimeMillis() + ".html";
            FileWriter fw = new FileWriter(path);
            fw.write("<html><body style='font-family:sans-serif;padding:20px;'>");
            fw.write("<h2>Automation Report</h2>");
            fw.write("<h3>Logs</h3><pre style='background:#eee;padding:10px;'>" + String.join("\n", reportLogs) + "</pre>");
            fw.write("<h3>API Response</h3><pre style='background:#222;color:#0f0;padding:10px;'>" + response + "</pre>");
            fw.write("</body></html>");
            fw.close();
            log("✅ Report Saved: " + path);
        } catch(Exception e) {
            log("❌ Report Gen Failed");
        }
    }
}
