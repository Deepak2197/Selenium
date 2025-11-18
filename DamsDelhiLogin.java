package SelenuimTest.SelenuimTest1;

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

    // --- Constants & API Configuration ---
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
            // --- 1. Setup Chrome Options (CI & Local) ---
            ChromeOptions options = new ChromeOptions();
            
            if (System.getenv("CI") != null) {
                log("🔧 Configuring for CI environment (Headless Mode)");
                options.addArguments("--headless=new");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--disable-gpu");
                options.addArguments("--window-size=1920,1080");
                options.addArguments("--remote-allow-origins=*");
                options.addArguments("--disable-extensions");
            } else {
                log("🔧 Configuring for Local environment");
                options.addArguments("--remote-allow-origins=*");
            }
            
            // Common options
            options.addArguments("--disable-notifications");
            options.addArguments("--ignore-certificate-errors");

            driver = new ChromeDriver(options);
            if (System.getenv("CI") == null) {
                driver.manage().window().maximize();
            }
            
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            log("✅ WebDriver initialized successfully");

            // --- 2. Navigation and Login ---
            log("📍 Navigating to DAMS website...");
            driver.get("https://www.damsdelhi.com/");

            clickElement(By.className("loginbtnSignupbtn"), "Login/Signup button");
            
            WebElement phoneInput = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".react-international-phone-input")));
            phoneInput.clear();
            phoneInput.sendKeys("7897897897");
            
            clickElement(By.className("common-bottom-btn"), "Mobile Submit button");
            log("✅ Mobile number entered");

            waitFor(4); 

            // Handle "Logout & Continue" Popup (Optional check)
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
                wait.until(ExpectedConditions.elementToBeClickable(field)).sendKeys(pinDigits[i]);
            }
            log("✅ PIN entered");

            clickElement(By.xpath("//button[contains(@class, 'common-bottom-btn') and normalize-space(text())='Verify & Proceed']"),
                    "Verify & Proceed button");
            log("✅ Login successful");
            
            waitFor(3);

            // --- 3. Navigate to Course Selection ---
            log("📚 Selecting course category...");
            WebElement categoryButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[normalize-space()='NEET PG NEXT']")));
            categoryButton.click();
            log("✅ Clicked NEET PG NEXT");

            String labelText = "NURSING (DSSSB, SGPGI, ESIC, RBB, KGMU)";
            WebElement radioLabel = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[text()='" + labelText + "']/parent::label")));
            radioLabel.click();
            log("✅ Selected Category: " + labelText);

            waitFor(2);

            // --- 4. Plan Selection ---
            log("💳 Selecting Plan...");
            By goProButton = By.xpath("//button[contains(., 'Premium') and contains(., 'Go Pro')]");
            clickElementJS(goProButton, "Go Pro button");

            By buyNowButton = By.xpath("//button[contains(@class, 'plan-actions')]//h5[contains(text(), 'Buy Now')]");
            clickElementJS(buyNowButton, "Buy Now button");

            By planButton = By.xpath("//button[contains(@class, 'boxrate')]//h3[normalize-space(text())='3 Months']/parent::button");
            clickElementJS(planButton, "3 Months Plan");

            By placeOrderButton = By.xpath("//button[contains(@class,'btn-danger')]//h6[contains(normalize-space(.), 'Place Order')]");
            clickElementJS(placeOrderButton, "Place Order button");
            log("✅ Order placed");

            waitFor(3);
            
            // --- 5. Payment Method ---
            log("💰 Selecting Payment Method...");
            By paytmRadioLocator = By.xpath("//label[.//span[normalize-space(text())='Paytm']]");
            clickElement(paytmRadioLocator, "Paytm Option");

            By payNowLocator = By.xpath("//button[.//span[normalize-space(text())='Pay Now']]");
            clickElement(payNowLocator, "Pay Now button");
            log("✅ Proceeding to payment page");

            // --- 6. Capture Data & Screenshot ---
            waitFor(5);
            
            String screenshotPath = takeScreenshot("PaymentConfirmation");
            if(screenshotPath != null) log("📸 Screenshot captured: " + new File(screenshotPath).getName());

            Map<String, String> authHeaders = extractAuthFromStorage();
            log("🔐 Authentication Data Extracted");

            // --- 7. Call API ---
            log("🌐 Calling API...");
            String apiResponse = callPlanAPI(authHeaders);
            log("📥 API Response Length: " + apiResponse.length());
            
            // --- 8. Generate Report ---
            generateHTMLReport(apiResponse);
            log("✅ Execution Completed Successfully");

        } catch (Exception e) {
            log("❌ FATAL ERROR: " + e.getMessage());
            e.printStackTrace();
            takeScreenshot("FatalError");
            generateHTMLReport("{\"error\": \"FATAL ERROR: " + e.getMessage().replace("\"", "'") + "\"}");
            throw new RuntimeException(e); // Fail the GitHub Action
        } finally {
            if (driver != null) {
                driver.quit();
                log("🔒 Browser Closed");
            }
        }
    }

    // --- Utility Methods ---

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
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
            Thread.sleep(500);
            element.click();
            log("✅ Clicked: " + elementName);
        } catch (Exception e) {
            log("⚠️ Standard click failed for " + elementName + ", trying JS click...");
            clickElementJS(locator, elementName);
        }
    }

    private static void clickElementJS(By locator, String elementName) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
            Thread.sleep(500);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            log("✅ Clicked (JS): " + elementName);
        } catch (Exception e) {
            log("❌ Failed to click: " + elementName);
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
        System.out.flush(); 
        reportLogs.add(logEntry);
    }

    // --- Authentication & API Methods ---

    private static Map<String, String> extractAuthFromStorage() {
        Map<String, String> authMap = new HashMap<>();
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            String jwt = (String) js.executeScript("return window.localStorage.getItem('jwt_token') || window.sessionStorage.getItem('jwt_token');");
            String timeStamp = (String) js.executeScript("return window.localStorage.getItem('time_stamp') || window.sessionStorage.getItem('time_stamp');");

            if (jwt != null) {
                authMap.put("jwt_token", jwt);
                log("✅ JWT Token found");
            } else {
                log("❌ JWT Token NOT found in storage");
            }
            
            authMap.put("user_id", USER_ID_OVERRIDE);
            authMap.put("device_token", DEVICE_TOKEN_OVERRIDE);
            
            // Timestamp Logic
            if (timeStamp != null && !timeStamp.trim().isEmpty()) {
                authMap.put("time_stamp", timeStamp);
            } else if (jwt != null) {
                // Try to decode JWT for timestamp
                String payload = decodeJWTPayload(jwt);
                String tsDateString = extractJsonValue(payload, "\"time[_sS]*stamp\"\\s*:\\s*\"([^\"]+)\"");
                if (tsDateString != null) {
                    authMap.put("time_stamp", toEpochMillis(tsDateString));
                } else {
                    authMap.put("time_stamp", String.valueOf(System.currentTimeMillis()));
                }
            } else {
                authMap.put("time_stamp", String.valueOf(System.currentTimeMillis()));
            }

        } catch (Exception e) {
            log("⚠️ Auth extraction failed: " + e.getMessage());
        }
        return authMap;
    }

    private static String decodeJWTPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;
            String base64Payload = parts[1].replace('-', '+').replace('_', '/');
            switch (base64Payload.length() % 4) {
                case 2: base64Payload += "=="; break;
                case 3: base64Payload += "="; break;
            }
            byte[] decoded = Base64.getDecoder().decode(base64Payload);
            return new String(decoded, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    private static String toEpochMillis(String dateTimeString) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, formatter);
            long epochMillis = localDateTime.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
            return String.valueOf(epochMillis);
        } catch (Exception e) {
            return String.valueOf(System.currentTimeMillis());
        }
    }

    private static String extractJsonValue(String text, String regex) {
        if (text == null) return null;
        try {
            Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            if (m.find()) return m.group(1);
        } catch (Exception e) { }
        return null;
    }

    private static String callPlanAPI(Map<String, String> auth) {
        StringBuilder response = new StringBuilder();
        try {
            if (!auth.containsKey("jwt_token") || auth.get("jwt_token").isEmpty()) {
                log("❌ Missing JWT Token - API Call Aborted");
                return "{\"status\":false,\"message\":\"Missing jwt_token\"}";
            }

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            
            // Headers
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + auth.get("jwt_token"));
            conn.setRequestProperty("user_id", auth.get("user_id"));
            conn.setRequestProperty("device_token", auth.get("device_token"));
            conn.setRequestProperty("device_type", DEVICE_TYPE);
            conn.setRequestProperty("time_stamp", auth.get("time_stamp"));
            conn.setRequestProperty("stream_id", STREAM_ID);
            conn.setRequestProperty("api_version", API_VERSION);
            conn.setRequestProperty("device_info", DEVICE_INFO_MOCK);

            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);

            // JSON Payload
            String jsonInputString = String.format(
                "{\"user_id\": \"%s\", \"cat_id\": \"%s\"}", 
                auth.get("user_id"), "188"
            );
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int status = conn.getResponseCode();
            log("🔄 API Status Code: " + status);

            InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                }
            }
        } catch (Exception e) {
            log("⚠️ API Call Failed: " + e.getMessage());
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
                writer.println("<html><head><title>DAMS Automation Report</title>");
                writer.println("<style>body{font-family:sans-serif;padding:20px;background:#f4f4f9;}");
                writer.println("h2{color:#333;} .log{background:#fff;padding:8px;margin:5px 0;border-left:4px solid #007bff;font-family:monospace;}");
                writer.println("pre{background:#222;color:#0f0;padding:15px;border-radius:5px;overflow-x:auto;}</style></head><body>");
                writer.println("<h2>📊 Automation Test Report</h2>");
                
                writer.println("<h3>Execution Logs:</h3>");
                for(String log : reportLogs) writer.println("<div class='log'>" + log + "</div>");
                
                writer.println("<h3>API Response:</h3>");
                writer.println("<pre>" + formatJson(apiResponse) + "</pre>");
                writer.println("</body></html>");
            }
            System.out.println("📄 Report Generated: " + htmlPath);
        } catch (Exception e) {
            System.err.println("❌ Report Generation Failed: " + e.getMessage());
        }
    }

    private static String formatJson(String json) {
        if(json == null) return "";
        return json.replace(",", ",\n").replace("{", "{\n").replace("}", "\n}");
    }
}
