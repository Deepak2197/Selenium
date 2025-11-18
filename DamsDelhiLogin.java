// IMPORTANT: Save this file as TestWithApi.java in the ROOT of your repository
// DO NOT use package declaration for GitHub Actions compatibility

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
                options.addArguments("--disable-software-rasterizer");
                options.addArguments("--disable-blink-features=AutomationControlled");
                options.addArguments("--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            } else {
                log("🔧 Configuring for local environment");
            }
            
            options.addArguments("--remote-allow-origins=*");
            options.addArguments("--disable-notifications");
            options.addArguments("--ignore-certificate-errors");
            
            log("🌐 Initializing Chrome WebDriver...");
            driver = new ChromeDriver(options);
            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            wait = new WebDriverWait(driver, Duration.ofSeconds(30));
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

            waitFor(4); 

            clickElement(By.xpath("//button[contains(@class, 'btndata') and normalize-space(text())='Logout & Continue']"),
                    "Logout & Continue button");

            // Enter PIN
            log("🔢 Entering PIN...");
            String[] pinDigits = {"2", "0", "0", "0"};
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input.otp-field")));
            List<WebElement> pinFields = driver.findElements(By.cssSelector("input.otp-field"));
            for (int i = 0; i < pinFields.size() && i < pinDigits.length; i++) {
                WebElement field = pinFields.get(i);
                wait.until(ExpectedConditions.elementToBeClickable(field));
                field.clear();
                field.sendKeys(pinDigits[i]);
                Thread.sleep(300);
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
        } finally {
            if (driver != null) {
                try {
                    String keepBrowserOpen = System.getenv("KEEP_BROWSER_OPEN");
                    if (!"true".equals(keepBrowserOpen)) {
                        driver.quit();
                        log("🔒 Browser closed");
                    }
                } catch (Exception e) {
                    System.err.println("Error closing browser: " + e.getMessage());
                }
            }
            
            log("✅ Test execution completed");
            log("📋 Total steps logged: " + reportLogs.size());
        }
    }

    private static void waitFor(int seconds) {
        try {
            log("⏳ Waiting " + seconds + " seconds...");
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
        reportLogs.add(logEntry);
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
            } else if (jwt != null) {
                String payload = decodeJWTPayload(jwt);
                String tsDateString = extractJsonValue(payload, "\"time[_sS]*stamp\"\\s*:\\s*\"([^\"]+)\"");
                if (tsDateString != null) {
                    String tsEpoch = toEpochMillis(tsDateString);
                    authMap.put("time_stamp", tsEpoch);
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
                case 0: break;
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
        } catch (Exception e) {
            // Silent fail
        }
        return null;
    }

    private static String callPlanAPI(Map<String, String> auth) {
        StringBuilder response = new StringBuilder();
        try {
            if (!auth.containsKey("jwt_token") || auth.get("jwt_token").isEmpty()) {
                log("❌ Missing JWT token");
                return "{\"status\":false,\"message\":\"Missing jwt_token\"}";
            }

            String jwt = auth.get("jwt_token");
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
                writer.println("<title>DAMS Automation Report - " + timestamp + "</title>");
                writer.println("<style>");
                writer.println("* { margin: 0; padding: 0; box-sizing: border-box; }");
                writer.println("body { font-family: system-ui, -apple-system, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; padding: 20px; }");
                writer.println(".container { max-width: 1400px; margin: 0 auto; }");
                writer.println(".header { background: white; border-radius: 16px; padding: 30px; margin-bottom: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); }");
                writer.println(".header h1 { color: #2d3748; font-size: 28px; margin-bottom: 10px; }");
                writer.println(".section { background: white; border-radius: 16px; padding: 25px; margin-bottom: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.08); }");
                writer.println(".section h2 { color: #2d3748; font-size: 20px; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 2px solid #e2e8f0; }");
                writer.println(".log { background: #f7fafc; padding: 10px 15px; margin: 6px 0; border-radius: 6px; font-size: 13px; font-family: 'Courier New', monospace; border-left: 3px solid #cbd5e0; }");
                writer.println(".log.success { border-left-color: #48bb78; background: #f0fff4; }");
                writer.println(".log.warning { border-left-color: #ed8936; background: #fffaf0; }");
                writer.println(".log.error { border-left-color: #f56565; background: #fff5f5; }");
                writer.println(".api-response { background: #1a202c; color: #e2e8f0; padding: 20px; border-radius: 8px; overflow-x: auto; max-height: 600px; font-family: 'Courier New', monospace; font-size: 12px; white-space: pre-wrap; word-break: break-all; }");
                writer.println(".stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px; margin-bottom: 20px; }");
                writer.println(".stat { background: linear-gradient(135deg, #667eea, #764ba2); color: white; padding: 20px; border-radius: 12px; text-align: center; }");
                writer.println(".stat .value { font-size: 32px; font-weight: bold; }");
                writer.println(".stat .label { font-size: 14px; opacity: 0.9; margin-top: 5px; }");
                writer.println("</style>");
                writer.println("</head><body>");
                
                writer.println("<div class='container'>");
                writer.println("<div class='header'>");
                writer.println("<h1>🎯 DAMS Automation Test Report</h1>");
                writer.println("<p style='color: #718096;'>Generated: " + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>");
                writer.println("</div>");
                
                // Statistics
                writer.println("<div class='section'>");
                writer.println("<h2>📊 Execution Statistics</h2>");
                writer.println("<div class='stats'>");
                
                long successCount = reportLogs.stream().filter(log -> log.contains("✅")).count();
                long warningCount = reportLogs.stream().filter(log -> log.contains("⚠️")).count();
                long errorCount = reportLogs.stream().filter(log -> log.contains("❌")).count();
                
                writer.println("<div class='stat'><div class='value'>" + reportLogs.size() + "</div><div class='label'>Total Logs</div></div>");
                writer.println("<div class='stat'><div class='value'>" + successCount + "</div><div class='label'>Success</div></div>");
                writer.println("<div class='stat'><div class='value'>" + warningCount + "</div><div class='label'>Warnings</div></div>");
                writer.println("<div class='stat'><div class='value'>" + errorCount + "</div><div class='label'>Errors</div></div>");
                writer.println("</div>");
                writer.println("</div>");
                
                // Logs
                writer.println("<div class='section'>");
                writer.println("<h2>📝 Execution Logs</h2>");
                for (String log : reportLogs) {
                    String cssClass = "log";
                    if (log.contains("✅")) cssClass += " success";
                    else if (log.contains("⚠️")) cssClass += " warning";
                    else if (log.contains("❌")) cssClass += " error";
                    
                    writer.println("<div class='" + cssClass + "'>" + escapeHtml(log) + "</div>");
                }
                writer.println("</div>");
                
                // API Response
                writer.println("<div class='section'>");
                writer.println("<h2>🔌 API Response</h2>");
                writer.println("<pre class='api-response'>" + escapeHtml(formatJson(apiResponse)) + "</pre>");
                writer.println("</div>");
                
                writer.println("</div>");
                writer.println("</body></html>");
            }
            
            System.out.println("✅ Report saved: " + htmlPath);
        } catch (Exception e) {
            System.err.println("❌ Report generation failed: " + e.getMessage());
        }
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private static String formatJson(String json) {
        try {
            StringBuilder formatted = new StringBuilder();
            int indent = 0;
            boolean inQuotes = false;
            
            for (char c : json.toCharArray()) {
                if (c == '"') inQuotes = !inQuotes;
                
                if (!inQuotes) {
                    if (c == '{' || c == '[') {
                        formatted.append(c).append('\n').append("  ".repeat(++indent));
                    } else if (c == '}' || c == ']') {
                        formatted.append('\n').append("  ".repeat(--indent)).append(c);
                    } else if (c == ',') {
                        formatted.append(c).append('\n').append("  ".repeat(indent));
                    } else {
                        formatted.append(c);
                    }
                } else {
                    formatted.append(c);
                }
            }
            return formatted.toString();
        } catch (Exception e) {
            return json;
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
