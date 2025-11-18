// IMPORTANT: Save this file as TestWithApi.java in the ROOT of your repository
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

    // Constants
    private static final String DEVICE_TOKEN_FALLBACK = "61797743405"; 
    private static final String DEVICE_TYPE = "3";
    private static final String API_URL = "https://api.damsdelhi.com/v2_data_model/get_all_plan_by_category_id";
    private static final String USER_ID_OVERRIDE = "161444";
    private static final String DEVICE_TOKEN_OVERRIDE = "61797743405";
    private static final String STREAM_ID = "1";
    private static final String API_VERSION = "25";
    private static final String DEVICE_INFO_MOCK = "{\"model\":\"chrome_driver\",\"os\":\"linux\",\"app\":\"dams\"}";

    public static void main(String[] args) {
        log("🚀 DAMS Automation Started");
        log("Environment: " + (System.getenv("CI") != null ? "GitHub Actions (CI)" : "Local"));
        
        try {
            // Setup Chrome Options
            ChromeOptions options = new ChromeOptions();
            
            String ciEnv = System.getenv("CI");
            if ("true".equals(ciEnv)) {
                log("🔧 Configuring for CI environment (headless mode)");
                options.addArguments("--headless=new");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--disable-gpu");
                options.addArguments("--window-size=1920,1080");
                options.addArguments("--disable-extensions");
                options.addArguments("--remote-allow-origins=*");
                options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            } else {
                log("🔧 Configuring for local environment");
                options.addArguments("--remote-allow-origins=*");
            }
            
            options.addArguments("--disable-notifications");
            options.addArguments("--ignore-certificate-errors");
            
            log("🌐 Initializing Chrome WebDriver...");
            driver = new ChromeDriver(options);
            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            log("✅ WebDriver initialized successfully");

            // --- STEP 1: Navigation and Login ---
            log("📍 STEP 1: Navigating to DAMS website");
            driver.get("https://www.damsdelhi.com/");
            waitFor(3);
            log("✅ Page loaded: " + driver.getCurrentUrl());

            log("🔐 STEP 2: Starting login process");
            clickElement(By.className("loginbtnSignupbtn"), "Login/Signup button");
            
            WebElement phoneInput = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector(".react-international-phone-input")));
            phoneInput.clear();
            phoneInput.sendKeys("7897897897");
            
            clickElement(By.className("common-bottom-btn"), "Mobile submit button");
            log("✅ Mobile number entered");

            waitFor(3); 

            // --- FIX: Handle "Logout & Continue" optionally ---
            // This button only appears if logged in elsewhere. If not found, we shouldn't crash.
            try {
                log("❓ Checking for 'Logout & Continue' popup...");
                WebElement logoutBtn = new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class, 'btndata') and normalize-space(text())='Logout & Continue']")));
                logoutBtn.click();
                log("✅ Clicked 'Logout & Continue'");
            } catch (Exception e) {
                log("ℹ️ 'Logout & Continue' not found (Continuing normal flow)");
            }

            // Enter PIN
            log("🔢 Entering PIN...");
            String[] pinDigits = {"2", "0", "0", "0"};
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input.otp-field")));
            List<WebElement> pinFields = driver.findElements(By.cssSelector("input.otp-field"));
            
            for (int i = 0; i < pinFields.size() && i < pinDigits.length; i++) {
                WebElement field = pinFields.get(i);
                field.sendKeys(pinDigits[i]);
                Thread.sleep(100); 
            }
            log("✅ PIN entered");

            clickElement(By.xpath("//button[contains(@class, 'common-bottom-btn') and normalize-space(text())='Verify & Proceed']"),
                    "Verify & Proceed button");
            log("✅ Login successful");
            
            waitFor(3);

            // --- STEP 3: Navigate to Course Selection ---
            log("📚 STEP 3: Selecting course category");
            WebElement categoryButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='NEET PG NEXT']")));
            categoryButton.click();
            log("✅ Clicked NEET PG NEXT");

            waitFor(2);

            String labelText = "NURSING (DSSSB, SGPGI, ESIC, RBB, KGMU)";
            WebElement radioLabel = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[text()='" + labelText + "']/parent::label")));
            radioLabel.click();
            log("✅ Selected: " + labelText);

            waitFor(2);

            // --- STEP 4: Plan Selection ---
            log("💳 STEP 4: Selecting plan");
            By goProButton = By.xpath("//button[contains(., 'Premium') and contains(., 'Go Pro')]");
            clickElementJS(goProButton, "Go Pro button");
            
            waitFor(2);
            
            By buyNowButton = By.xpath("//button[contains(@class, 'plan-actions')]//h5[contains(text(), 'Buy Now')]");
            clickElementJS(buyNowButton, "Buy Now button");

            waitFor(2);
            
            By planButton = By.xpath("//button[contains(@class, 'boxrate')]//h3[normalize-space(text())='3 Months']/parent::button");
            clickElementJS(planButton, "3 Months plan");

            waitFor(2);
            
            By placeOrderButton = By.xpath("//button[contains(@class,'btn-danger')]//h6[contains(normalize-space(.), 'Place Order')]");
            clickElementJS(placeOrderButton, "Place Order button");
            log("✅ Order placed");

            waitFor(3);
            
            // --- STEP 5: Payment Method ---
            log("💰 STEP 5: Selecting payment method");
            By paytmRadioLocator = By.xpath("//label[.//span[normalize-space(text())='Paytm']]");
            clickElement(paytmRadioLocator, "Paytm option");

            waitFor(2);
            
            By payNowLocator = By.xpath("//button[.//span[normalize-space(text())='Pay Now']]");
            clickElement(payNowLocator, "Pay Now button");
            log("✅ Proceeding to payment");

            // --- STEP 6: Capture Data ---
            log("📸 STEP 6: Capturing screenshot and extracting data");
            waitFor(6);
            
            String screenshotPath = takeScreenshot("PaymentConfirmation");
            if (screenshotPath != null) {
                log("✅ Screenshot saved: " + new File(screenshotPath).getName());
            }

            Map<String, String> authHeaders = extractAuthFromStorage();
            log("🔐 Authentication data extracted");

            // --- STEP 7: API Call ---
            log("🌐 STEP 7: Calling API");
            String apiResponse = callPlanAPI(authHeaders);
            
            if (apiResponse.contains("\"status\":true")) {
                log("✅ API call successful");
                int planCount = countOccurrences(apiResponse, "\"id\":");
                log("📊 Plans retrieved: " + planCount);
            } else {
                log("⚠️ API response indicates an issue");
            }

            // --- STEP 8: Generate Report ---
            log("📝 STEP 8: Generating HTML report");
            generateHTMLReport(apiResponse);
            log("✅ Report generated successfully");

        } catch (Exception e) {
            log("❌ FATAL ERROR: " + e.getMessage());
            e.printStackTrace();
            takeScreenshot("FatalError");
            
            try {
                generateHTMLReport("{\"status\":false,\"message\":\"Fatal error: " + 
                    e.getMessage().replaceAll("\"", "'") + "\"}");
            } catch (Exception reportError) {
                System.err.println("Could not generate error report: " + reportError.getMessage());
            }
            // Throw exception to fail the GitHub Action
            throw new RuntimeException(e);
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                    log("🔒 Browser closed");
                } catch (Exception e) {
                    System.err.println("Error closing browser: " + e.getMessage());
                }
            }
            
            log("✅ Test execution completed");
        }
    }

    private static void waitFor(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void clickElement(By locator, String elementName) {
        try {
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
            Thread.sleep(500);
            element.click();
            log("✅ Clicked: " + elementName);
        } catch (Exception e) {
            log("⚠️ Standard click failed for " + elementName + ", trying JavaScript");
            try {
                WebElement element = driver.findElement(locator);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                log("✅ Clicked (JS): " + elementName);
            } catch (Exception ex) {
                log("❌ Failed to click: " + elementName);
                throw new RuntimeException("Could not click element: " + elementName, ex);
            }
        }
    }

    private static void clickElementJS(By locator, String elementName) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
            Thread.sleep(500);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            log("✅ Clicked (JS): " + elementName);
        } catch (Exception e) {
            log("❌ JS click failed: " + elementName);
            throw new RuntimeException("Could not click element: " + elementName, e);
        }
    }

    private static String takeScreenshot(String fileName) {
        try {
            File screenshotDirFile = new File(screenshotDir);
            screenshotDirFile.mkdirs();
            
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File destFile = new File(screenshotDir + fileName + ".png");
            FileHandler.copy(srcFile, destFile);
            return destFile.getAbsolutePath();
        } catch (Exception e) {
            log("❌ Screenshot failed: " + e.getMessage());
            return null;
        }
    }

    private static void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String logEntry = "[" + timestamp + "] " + message;
        System.out.println(logEntry);
        System.out.flush(); // Ensure it appears in CI logs immediately
        reportLogs.add(logEntry);
    }

    private static Map<String, String> extractAuthFromStorage() {
        Map<String, String> authMap = new HashMap<>();
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            String jwt = (String) js.executeScript(
                "return window.localStorage.getItem('jwt_token') || window.sessionStorage.getItem('jwt_token');");
            String timeStamp = (String) js.executeScript(
                "return window.localStorage.getItem('time_stamp') || window.sessionStorage.getItem('time_stamp');");

            if (jwt != null) {
                authMap.put("jwt_token", jwt);
                log("✅ JWT token found");
            } else {
                log("❌ JWT token not found");
            }
            
            authMap.put("user_id", USER_ID_OVERRIDE);
            authMap.put("device_token", DEVICE_TOKEN_OVERRIDE);
            
            if (timeStamp != null && !timeStamp.trim().isEmpty()) {
                authMap.put("time_stamp", timeStamp);
            } else {
                authMap.put("time_stamp", String.valueOf(System.currentTimeMillis()));
            }

        } catch (Exception e) {
            log("⚠️ Auth extraction failed: " + e.getMessage());
        }
        return authMap;
    }

    private static String callPlanAPI(Map<String, String> auth) {
        StringBuilder response = new StringBuilder();
        try {
            String jwt = auth.get("jwt_token");
            if (jwt == null) jwt = ""; // Prevent null pointer, API will handle invalid token
            
            String userId = auth.getOrDefault("user_id", USER_ID_OVERRIDE);
            String deviceToken = auth.getOrDefault("device_token", DEVICE_TOKEN_OVERRIDE);
            String timeStamp = auth.getOrDefault("time_stamp", String.valueOf(System.currentTimeMillis()));
            
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            
            conn.setRequestProperty("Content-Type", "application/json"); 
            conn.setRequestProperty("Accept", "application/json"); 
            conn.setRequestProperty("Authorization", "Bearer " + jwt);
            conn.setRequestProperty("user_id", userId);
            conn.setRequestProperty("device_token", deviceToken);
            conn.setRequestProperty("device_type", DEVICE_TYPE);
            conn.setRequestProperty("time_stamp", timeStamp);
            conn.setRequestProperty("stream_id", STREAM_ID);
            conn.setRequestProperty("api_version", API_VERSION);
            conn.setRequestProperty("device_info", DEVICE_INFO_MOCK);

            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            String categoryId = "188";
            String jsonInputString = String.format(
                "{\"user_id\": \"%s\", \"cat_id\": \"%s\"}", 
                userId, categoryId
            );
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int status = conn.getResponseCode();
            log("API Status: " + status);

            InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
            
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                }
            }

        } catch (Exception e) {
            log("⚠️ API call failed: " + e.getMessage());
            return "{\"status\":false,\"message\":\"" + e.getMessage() + "\"}";
        }
        return response.toString();
    }

    private static void generateHTMLReport(String apiResponse) {
        try {
            File reportDirFile = new File(reportDir);
            reportDirFile.mkdirs();
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String htmlPath = reportDir + "DAMS_Report_" + timestamp + ".html";
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(htmlPath))) {
                writer.println("<!DOCTYPE html>");
                writer.println("<html lang='en'><head>");
                writer.println("<meta charset='UTF-8'>");
                writer.println("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
                writer.println("<title>DAMS Automation Report</title>");
                writer.println("<style>body{font-family:sans-serif;padding:20px;}</style>");
                writer.println("</head><body>");
                writer.println("<h1>Automation Report</h1>");
                writer.println("<h2>Logs</h2>");
                for(String l : reportLogs) writer.println("<div>"+l+"</div>");
                writer.println("<h2>API Response</h2>");
                writer.println("<pre>" + apiResponse + "</pre>");
                writer.println("</body></html>");
            }
            System.out.println("✅ Report saved: " + htmlPath);
        } catch (Exception e) {
            System.err.println("❌ Report generation failed: " + e.getMessage());
        }
    }

    private static int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}
