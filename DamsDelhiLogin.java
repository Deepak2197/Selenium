package SelenuimTest.SelenuimTest1;

//This is the correct code for api implementation
import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; 
import java.util.*;
import java.util.regex.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
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

    // Constants
    // UPDATED: Device token requested by user (used as fallback and override source)
    private static final String DEVICE_TOKEN_FALLBACK = "61797743405"; 
    private static final String DEVICE_TYPE = "3"; // device_type as 3
    private static final String API_URL = "https://api.damsdelhi.com/v2_data_model/get_all_plan_by_category_id";
    
    // NEW/UPDATED CONSTANTS based on user's working Postman request
    private static final String USER_ID_OVERRIDE = "161444"; // Required user_id
    private static final String DEVICE_TOKEN_OVERRIDE = "61797743405"; // Required device_token
    private static final String STREAM_ID = "1";
    private static final String API_VERSION = "25";
    // Mocking device_info since we can't run JSON.stringify(device_info) in Java
    private static final String DEVICE_INFO_MOCK = "{\"model\":\"chrome_driver\",\"os\":\"windows\",\"app\":\"dams\"}";


    public static void main(String[] args) {
        // Set up WebDriver and wait times
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10)); // Increased implicit wait
        wait = new WebDriverWait(driver, Duration.ofSeconds(30)); // Increased explicit wait

        log("🚀 Test Execution Started at " + LocalDateTime.now());

        try {
            // --- 1. Navigation and Login ---
            driver.get("https://www.damsdelhi.com/");
            log("Opened DAMS website");

            clickElement(By.className("loginbtnSignupbtn"), "'Login/Signup' button");
            driver.findElement(By.cssSelector(".react-international-phone-input")).sendKeys("7897897897");
            driver.findElement(By.className("common-bottom-btn")).click();
            log("Entered mobile number successfully");

            // Wait for OTP/PIN screen load
            waitFor(4); 

            clickElement(By.xpath("//button[contains(@class, 'btndata') and normalize-space(text())='Logout & Continue']"),
                    "'Logout & Continue' button");

            String[] pinDigits = {"2", "0", "0", "0"};
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input.otp-field")));
            List<WebElement> pinFields = driver.findElements(By.cssSelector("input.otp-field"));
            for (int i = 0; i < pinFields.size() && i < pinDigits.length; i++) {
                WebElement field = pinFields.get(i);
                wait.until(ExpectedConditions.elementToBeClickable(field)).sendKeys(pinDigits[i]);
            }
            log("Entered 4-digit PIN");

            clickElement(By.xpath("//button[contains(@class, 'common-bottom-btn') and normalize-space(text())='Verify & Proceed']"),
                    "'Verify & Proceed' button");
            log("Login successful");
            
            // Wait for dashboard to load before proceeding
            waitFor(2);

            // --- 2. Navigate to Plan Purchase Flow ---
            WebElement categoryButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[normalize-space()='NEET PG NEXT']")));
            categoryButton.click();
            log("Clicked NEET PG NEXT category");

            String labelText = "NURSING (DSSSB, SGPGI, ESIC, RBB, KGMU)";
            WebElement radioLabel = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[text()='" + labelText + "']/parent::label")));
            radioLabel.click();
            log("Selected category: " + labelText);

            By goProButton = By.xpath("//button[contains(., 'Premium') and contains(., 'Go Pro')]");
            WebElement buttonA = wait.until(ExpectedConditions.elementToBeClickable(goProButton));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", buttonA);
            log("Clicked 'Go Pro' button");
            
            By buyNowButton = By.xpath("//button[contains(@class, 'plan-actions')]//h5[contains(text(), 'Buy Now')]");
            WebElement buttonB = wait.until(ExpectedConditions.elementToBeClickable(buyNowButton));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", buttonB);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", buttonB);
            log("Clicked 'Buy Now' button");

            By planButton = By.xpath("//button[contains(@class, 'boxrate')]//h3[normalize-space(text())='3 Months']/parent::button");
            WebElement planBtn = wait.until(ExpectedConditions.elementToBeClickable(planButton));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", planBtn);
            log("Selected '3 Months' plan");

            By placeOrderButton = By.xpath("//button[contains(@class,'btn-danger')]//h6[contains(normalize-space(.), 'Place Order')]");
            WebElement placeOrderBtn = wait.until(ExpectedConditions.elementToBeClickable(placeOrderButton));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", placeOrderBtn);
            log("Clicked 'Place Order' button");

            By paytmRadioLocator = By.xpath("//label[.//span[normalize-space(text())='Paytm']]");
            clickElement(paytmRadioLocator, "Paytm Radio Button");
            log("Selected Paytm as payment option");

            By payNowLocator = By.xpath("//button[.//span[normalize-space(text())='Pay Now']]");
            clickElement(payNowLocator, "'Pay Now' button");

            // --- 3. Capture Screenshot & Extract Auth ---
            waitFor(5); // Wait for the payment page to fully load and ensure storage is updated.
            String screenshotPath = takeScreenshot("PaymentConfirmation");
            log("Screenshot captured: " + screenshotPath);

            Map<String, String> authHeaders = extractAuthFromStorage();
            log("--- Extracted Headers for API Call ---");
            // These logs show the values being used, including the overridden ones
            log("Authorization: Bearer " + authHeaders.getOrDefault("jwt_token", "MISSING"));
            log("user_id: " + authHeaders.getOrDefault("user_id", "MISSING"));
            log("device_token: " + authHeaders.getOrDefault("device_token", "MISSING (Using Fallback)"));
            log("time_stamp: " + authHeaders.getOrDefault("time_stamp", "MISSING"));
            log("---------------------------------------");

            // --- 4. Call API ---
            String apiResponse = callPlanAPI(authHeaders);
            log("API Response Body: " + apiResponse);

            generateHTMLReport();
            log("✅ HTML report generated successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            log("❌ ERROR: " + e.getMessage());
            takeScreenshot("ErrorOccurred");
        } finally {
            if (driver != null) {
                // driver.quit(); // Keep browser open for inspection if needed during dev
            }
            log("Browser closed. Test finished.");
        }
    }

    private static void waitFor(int seconds) {
        try {
            log("💤 Waiting for " + seconds + " seconds...");
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void clickElement(By locator, String elementName) {
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        element.click();
        log("Clicked " + elementName);
    }

    private static String takeScreenshot(String fileName) {
        try {
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File destFile = new File(screenshotDir + fileName + ".png");
            destFile.getParentFile().mkdirs();
            FileHandler.copy(srcFile, destFile);
            return destFile.getAbsolutePath();
        } catch (IOException e) {
            log("❌ Screenshot failed: " + e.getMessage());
            return null;
        }
    }

    private static void log(String message) {
        System.out.println(message);
        reportLogs.add(LocalDateTime.now() + " ➜ " + message);
    }

    private static Map<String, String> extractAuthFromStorage() {
        Map<String, String> authMap = new HashMap<>();
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            String jwt = (String) js.executeScript(
                    "return window.localStorage.getItem('jwt_token') || window.sessionStorage.getItem('jwt_token');");
            String userId = (String) js.executeScript(
                    "return window.localStorage.getItem('id') || window.sessionStorage.getItem('id');");
            String deviceToken = (String) js.executeScript(
                    "return window.localStorage.getItem('device_token') || window.sessionStorage.getItem('device_token');");
            String timeStamp = (String) js.executeScript(
                    "return window.localStorage.getItem('time_stamp') || window.sessionStorage.getItem('time_stamp');");

            // 1. JWT Token (Mandatory)
            if (jwt != null) {
                authMap.put("jwt_token", jwt);
                log("✅ Found jwt_token in storage.");
            } else {
                log("❌ jwt_token NOT found in storage.");
            }
            
            // 2. User ID (Override with requested value)
            if (userId != null && !userId.trim().isEmpty()) {
                log("⚠️ Overriding stored user_id (" + userId + ") with requested value: " + USER_ID_OVERRIDE);
            }
            authMap.put("user_id", USER_ID_OVERRIDE);

            // 3. Device Token (Override with requested value)
            if (deviceToken != null && !deviceToken.trim().isEmpty()) {
                log("⚠️ Overriding stored device_token (" + deviceToken + ") with requested value: " + DEVICE_TOKEN_OVERRIDE);
            } else {
                log("⚠️ No device_token found. Using requested value: " + DEVICE_TOKEN_OVERRIDE);
            }
            authMap.put("device_token", DEVICE_TOKEN_OVERRIDE);
            
            // 4. Time Stamp (from storage or JWT payload)
            if (timeStamp != null && !timeStamp.trim().isEmpty()) {
                // If found in storage, we assume it's the epoch time (or close enough) and use it directly.
                authMap.put("time_stamp", timeStamp);
                log("✅ Found time_stamp in storage: " + timeStamp);
            } else if (jwt != null) {
                String payload = decodeJWTPayload(jwt);
                String tsDateString = extractJsonValue(payload, "\"time[_sS]*stamp\"\\s*:\\s*\"([^\"]+)\"");

                if (tsDateString != null) {
                    // Convert date string to epoch milliseconds, assuming UTC
                    String tsEpoch = toEpochMillis(tsDateString);

                    authMap.put("time_stamp", tsEpoch);
                    log("✅ Decoded JWT payload, CONVERTED timeStamp (" + tsDateString + ") to epoch (UTC assumed): " + tsEpoch);
                } else {
                    String fallbackTs = String.valueOf(System.currentTimeMillis());
                    authMap.put("time_stamp", fallbackTs);
                    log("⚠️ Could not find timeStamp in JWT payload. Using current system time as fallback: " + fallbackTs);
                }
            } else {
                String fallbackTs = String.valueOf(System.currentTimeMillis());
                authMap.put("time_stamp", fallbackTs);
                log("⚠️ No time_stamp found and no JWT to decode. Using current system time as fallback: " + fallbackTs);
            }

        } catch (Exception e) {
            log("⚠️ Failed to extract auth from storage: " + e.getMessage());
        }
        return authMap;
    }

    private static String decodeJWTPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;
            // Pad the Base64 string if necessary (JWT uses URL-safe Base64 without padding)
            String base64Payload = parts[1].replace('-', '+').replace('_', '/');
            switch (base64Payload.length() % 4) {
                case 0: break; // No padding needed
                case 2: base64Payload += "=="; break;
                case 3: base64Payload += "="; break;
                default: throw new IllegalArgumentException("Illegal base64url string!");
            }
            
            byte[] decoded = Base64.getDecoder().decode(base64Payload);
            return new String(decoded, "UTF-8");
        } catch (Exception e) {
            log("⚠️ JWT decode failed: " + e.getMessage());
            return null;
        }
    }

    // Converts date string format to epoch milliseconds, assuming UTC.
    private static String toEpochMillis(String dateTimeString) {
        try {
            // The format from the log is "2025-10-24 11:13:58"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, formatter);
            
            // Convert to epoch milliseconds assuming the extracted time is in UTC.
            long epochMillis = localDateTime.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
            
            return String.valueOf(epochMillis);
        } catch (Exception e) {
            log("⚠️ Failed to parse/convert date string to epoch: " + dateTimeString + ". Error: " + e.getMessage());
            return String.valueOf(System.currentTimeMillis());
        }
    }


    private static String extractJsonValue(String text, String regex) {
        if (text == null) return null;
        try {
            // Case-insensitive matching for the key
            Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            if (m.find()) return m.group(1);
        } catch (Exception e) {
            log("⚠️ Regex extraction failed: " + e.getMessage());
        }
        return null;
    }

    private static String callPlanAPI(Map<String, String> auth) {
        StringBuilder response = new StringBuilder();
        try {
            // CRITICAL CHECK: Ensure JWT is present
            if (!auth.containsKey("jwt_token") || auth.get("jwt_token").isEmpty()) {
                log("❌ Missing jwt_token. API call aborted, cannot authenticate.");
                return "{\"status\":false,\"message\":\"Missing jwt_token, API request blocked by test script.\"}}";
            }

            String jwt = auth.get("jwt_token");
            String userId = auth.getOrDefault("user_id", USER_ID_OVERRIDE); // Using override constant
            String deviceToken = auth.getOrDefault("device_token", DEVICE_TOKEN_OVERRIDE); // Using override constant
            String timeStamp = auth.getOrDefault("time_stamp", String.valueOf(System.currentTimeMillis()));
            
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            
            // --- UPDATED HEADERS based on user's working Postman request ---
            conn.setRequestProperty("Content-Type", "application/json"); 
            conn.setRequestProperty("Accept", "application/json"); 
            conn.setRequestProperty("Authorization", "Bearer " + jwt);
            conn.setRequestProperty("user_id", userId);
            conn.setRequestProperty("device_token", deviceToken);
            conn.setRequestProperty("device_type", DEVICE_TYPE);
            conn.setRequestProperty("time_stamp", timeStamp);
            conn.setRequestProperty("stream_id", STREAM_ID); // New header
            conn.setRequestProperty("api_version", API_VERSION); // New header
            conn.setRequestProperty("device_info", DEVICE_INFO_MOCK); // New header

            conn.setDoOutput(true);
            conn.setConnectTimeout(15000); // 15 seconds connection timeout
            conn.setReadTimeout(15000); // 15 seconds read timeout

            // --- FIX: Sending JSON Payload (key changed to cat_id, value changed to 188) ---
            String categoryId = "188"; // Updated to 188 as requested
            String jsonInputString = String.format(
                "{\"user_id\": \"%s\", \"cat_id\": \"%s\"}", 
                userId, categoryId
            );
            log("➡️ Sending API request with JSON payload: " + jsonInputString);
            
            // Send request data (JSON)
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Read response
            int status = conn.getResponseCode();
            log("🔄 API Response Status: " + status);

            // Determine which stream to read (input for 2xx, error for others)
            InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
            
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                }
            } else {
                 response.append("No response stream available (Status: ").append(status).append(")");
            }

        } catch (IOException e) {
            log("⚠️ API network call failed (IO Exception): " + e.getMessage());
            return "{\"status\":false,\"message\":\"API network failure: " + e.getMessage().replaceAll("\"", "'") + "\"}";
        } catch (Exception e) {
            log("⚠️ API call failed (General Exception): " + e.getMessage());
            return "{\"status\":false,\"message\":\"API general failure: " + e.getMessage().replaceAll("\"", "'") + "\"}";
        }
        return response.toString();
    }

    private static void generateHTMLReport() {
        String htmlPath = System.getProperty("user.dir") + "/test-report/TestReport.html";
        try (PrintWriter writer = new PrintWriter(new FileWriter(htmlPath))) {
            writer.println("<html><head><title>Automation Test Report</title>");
            writer.println("<style>");
            writer.println("body{font-family:Arial;background:#f8f9fa;padding:20px;}");
            writer.println("h2{color:#2c3e50;} .log{background:#fff;padding:10px;margin-bottom:10px;border-radius:8px;box-shadow:0 0 4px #ddd; font-size: 0.9em; white-space: pre-wrap;}");
            writer.println("</style></head><body>");
            writer.println("<h2>📊 DAMS Automation Report</h2>");
            for (String log : reportLogs) writer.println("<div class='log'>" + log + "</div>");
            writer.println("<p><b>Report generated on:</b> " + LocalDateTime.now() + "</p>");
            writer.println("</body></html>");
            System.out.println("📄 Report saved at: " + htmlPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
