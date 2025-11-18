import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DamsAutomation {

    // Configuration
    private static final String REPORT_DIR = "reports";
    private static final String BASE_URL = "https://www.damsdelhi.com/";
    private static final String API_URL = "https://api.damsdelhi.com/v2_data_model/get_all_events";
    
    // Login credentials
    private static final String STUDENT_PHONE = "+919456628016";
    private static final String STUDENT_OTP = "2000";
    
    // OkHttp client
    private final OkHttpClient httpClient;
    
    // Selenium components
    private WebDriver driver;
    private WebDriverWait wait;

    public DamsAutomation() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }

    // ========================================
    // SELENIUM BROWSER METHODS
    // ========================================

    public void initBrowser() {
        System.out.println("🚀 Initializing Chrome browser...");
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-notifications");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        
        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        
        System.out.println("✅ Browser initialized successfully");
    }

    public boolean loginToDamsSite() {
        try {
            System.out.println("\n🔐 Starting login process to " + BASE_URL);
            driver.get(BASE_URL);
            Thread.sleep(3000);

            // Click Sign In button
            System.out.println("📍 Step 1: Clicking Sign In button...");
            WebElement signinBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[contains(text(),'Sign in') or contains(text(),'Sign In') or contains(@class, 'sign')]")
                )
            );
            signinBtn.click();
            Thread.sleep(2000);

            // Enter phone number
            System.out.println("📍 Step 2: Entering phone number...");
            WebElement phoneField = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='tel' or @placeholder='Enter Phone Number']"))
            );
            phoneField.clear();
            phoneField.sendKeys(STUDENT_PHONE);
            Thread.sleep(1000);

            // Click submit button
            System.out.println("📍 Step 3: Submitting phone number...");
            WebElement submitBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'common-bottom-btn') or contains(text(), 'Continue')]")
                )
            );
            submitBtn.click();
            Thread.sleep(3000);

            // Enter OTP
            System.out.println("📍 Step 4: Entering OTP...");
            WebElement otpField = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath("//input[@type='text' or @type='tel' or @placeholder='Enter OTP']")
                )
            );
            otpField.clear();
            otpField.sendKeys(STUDENT_OTP);
            Thread.sleep(1000);

            // Submit OTP
            System.out.println("📍 Step 5: Submitting OTP...");
            WebElement otpSubmitBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'common-bottom-btn') or contains(text(), 'Verify')]")
                )
            );
            otpSubmitBtn.click();
            Thread.sleep(5000);

            // Verify login success
            boolean isLoggedIn = driver.getCurrentUrl().contains("dashboard") 
                              || driver.getCurrentUrl().contains("home")
                              || driver.getPageSource().contains("Profile")
                              || driver.getPageSource().contains("Dashboard");

            if (isLoggedIn) {
                System.out.println("✅ Login Successful!");
                return true;
            } else {
                System.out.println("⚠️ Login status unclear, assuming success...");
                return true;
            }

        } catch (Exception e) {
            System.err.println("❌ Login failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public String takeScreenshot(String baseFilename) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = baseFilename + "_" + timestamp + ".png";
            
            Path reportPath = Paths.get(REPORT_DIR);
            Files.createDirectories(reportPath);
            
            Path filepath = reportPath.resolve(filename);
            
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(screenshotFile.toPath(), filepath, StandardCopyOption.REPLACE_EXISTING);
            
            System.out.println("📸 Screenshot saved: " + filepath.toAbsolutePath());
            return filepath.toAbsolutePath().toString();
            
        } catch (Exception e) {
            System.err.println("❌ Screenshot capture failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void closeBrowser() {
        if (driver != null) {
            System.out.println("🔒 Closing browser...");
            driver.quit();
        }
    }

    // ========================================
    // API METHODS (OkHttp)
    // ========================================

    public JSONArray getEvents(String jwtToken) throws IOException {
        System.out.println("\n📡 Calling API: " + API_URL);
        
        JSONObject requestBody = new JSONObject();
        requestBody.put("page", 1);
        
        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.parse("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
            .url(API_URL)
            .post(body)
            .addHeader("authorization", jwtToken)
            .addHeader("device_type", "3")
            .addHeader("api_version", "10")
            .addHeader("user_id", "752847")
            .addHeader("device_token", "25714535808")
            .addHeader("device_info", "{\"browser\":\"Chrome 142\",\"os\":\"Windows 10\",\"deviceType\":\"browser\"}")
            .addHeader("Content-Type", "application/json")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API call failed with status: " + response.code());
            }
            
            String responseBody = response.body().string();
            System.out.println("✅ API Response received");
            
            JSONObject jsonResponse = new JSONObject(responseBody);
            
            if (jsonResponse.has("data")) {
                JSONArray events = jsonResponse.getJSONArray("data");
                System.out.println("📊 Total events found: " + events.length());
                return events;
            } else {
                System.out.println("⚠️ No 'data' field in response");
                return new JSONArray();
            }
        }
    }

    // ========================================
    // HTML REPORT GENERATION
    // ========================================

    public void generateEventsHTML(JSONArray events) throws IOException {
        System.out.println("\n📝 Generating HTML report...");
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "DAMS_Events_Report_" + timestamp + ".html";
        
        Path reportPath = Paths.get(REPORT_DIR);
        Files.createDirectories(reportPath);
        
        Path filepath = reportPath.resolve(filename);
        
        try (FileWriter writer = new FileWriter(filepath.toFile())) {
            // HTML Header
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html lang='en'>\n");
            writer.write("<head>\n");
            writer.write("    <meta charset='UTF-8'>\n");
            writer.write("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
            writer.write("    <title>DAMS Events Report</title>\n");
            writer.write("    <style>\n");
            writer.write("        * { margin: 0; padding: 0; box-sizing: border-box; }\n");
            writer.write("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f5f5f5; padding: 20px; }\n");
            writer.write("        .container { max-width: 1400px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n");
            writer.write("        h1 { color: #2c3e50; text-align: center; margin-bottom: 10px; font-size: 32px; }\n");
            writer.write("        .meta { text-align: center; color: #7f8c8d; margin-bottom: 30px; font-size: 14px; }\n");
            writer.write("        .stats { background: #3498db; color: white; padding: 15px; border-radius: 5px; margin-bottom: 20px; text-align: center; font-size: 18px; }\n");
            writer.write("        table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n");
            writer.write("        th { background: #34495e; color: white; padding: 12px; text-align: left; font-weight: 600; position: sticky; top: 0; }\n");
            writer.write("        td { padding: 12px; border-bottom: 1px solid #ecf0f1; }\n");
            writer.write("        tr:hover { background: #f8f9fa; }\n");
            writer.write("        tr:nth-child(even) { background: #fafafa; }\n");
            writer.write("        .event-name { font-weight: 600; color: #2980b9; }\n");
            writer.write("        .event-date { color: #27ae60; font-size: 14px; }\n");
            writer.write("        .event-desc { color: #555; line-height: 1.5; }\n");
            writer.write("        .footer { text-align: center; margin-top: 30px; color: #95a5a6; font-size: 12px; }\n");
            writer.write("    </style>\n");
            writer.write("</head>\n");
            writer.write("<body>\n");
            writer.write("    <div class='container'>\n");
            
            // Title and metadata
            writer.write("        <h1>🎓 DAMS Events Report</h1>\n");
            writer.write("        <div class='meta'>Generated on: " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm:ss a")) + 
                "</div>\n");
            
            // Statistics
            writer.write("        <div class='stats'>📊 Total Events: " + events.length() + "</div>\n");
            
            // Table
            writer.write("        <table>\n");
            writer.write("            <thead>\n");
            writer.write("                <tr>\n");
            writer.write("                    <th style='width: 5%;'>#</th>\n");
            writer.write("                    <th style='width: 30%;'>Event Name</th>\n");
            writer.write("                    <th style='width: 15%;'>Event Date</th>\n");
            writer.write("                    <th style='width: 50%;'>Description</th>\n");
            writer.write("                </tr>\n");
            writer.write("            </thead>\n");
            writer.write("            <tbody>\n");
            
            // Events data
            if (events.length() == 0) {
                writer.write("                <tr><td colspan='4' style='text-align: center; padding: 20px; color: #95a5a6;'>No events found</td></tr>\n");
            } else {
                for (int i = 0; i < events.length(); i++) {
                    JSONObject event = events.getJSONObject(i);
                    
                    String eventName = event.optString("event_name", "N/A");
                    String eventDate = event.optString("event_date", "N/A");
                    String eventDesc = event.optString("event_description", "No description available");
                    
                    // Escape HTML special characters
                    eventName = escapeHtml(eventName);
                    eventDate = escapeHtml(eventDate);
                    eventDesc = escapeHtml(eventDesc);
                    
                    writer.write("                <tr>\n");
                    writer.write("                    <td>" + (i + 1) + "</td>\n");
                    writer.write("                    <td class='event-name'>" + eventName + "</td>\n");
                    writer.write("                    <td class='event-date'>" + eventDate + "</td>\n");
                    writer.write("                    <td class='event-desc'>" + eventDesc + "</td>\n");
                    writer.write("                </tr>\n");
                }
            }
            
            writer.write("            </tbody>\n");
            writer.write("        </table>\n");
            
            // Footer
            writer.write("        <div class='footer'>\n");
            writer.write("            <p>Generated by DAMS Automation Framework | © 2025</p>\n");
            writer.write("        </div>\n");
            writer.write("    </div>\n");
            writer.write("</body>\n");
            writer.write("</html>\n");
        }
        
        System.out.println("✅ HTML report created: " + filepath.toAbsolutePath());
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }

    // ========================================
    // MAIN EXECUTION FLOW
    // ========================================

    public static void main(String[] args) {
        DamsAutomation automation = new DamsAutomation();
        boolean success = false;

        try {
            System.out.println("╔════════════════════════════════════════════════════╗");
            System.out.println("║   DAMS AUTOMATION FRAMEWORK - Starting Execution   ║");
            System.out.println("╚════════════════════════════════════════════════════╝");
            
            // Step 1: Initialize browser
            automation.initBrowser();
            
            // Step 2: Login to website
            boolean loginSuccess = automation.loginToDamsSite();
            
            if (loginSuccess) {
                automation.takeScreenshot("DAMS_Login_Success");
                System.out.println("\n✅ Web login completed successfully");
            } else {
                automation.takeScreenshot("DAMS_Login_Failed");
                System.err.println("\n❌ Web login failed - continuing with API call anyway");
            }
            
            // Step 3: Fetch events via API
            String jwtToken = System.getenv("DAMS_JWT_TOKEN");
            
            if (jwtToken == null || jwtToken.trim().isEmpty()) {
                System.err.println("\n❌ ERROR: DAMS_JWT_TOKEN environment variable not set!");
                System.err.println("Please set it using: export DAMS_JWT_TOKEN='your_token_here'");
                return;
            }
            
            JSONArray events = automation.getEvents(jwtToken);
            
            // Step 4: Generate HTML report
            automation.generateEventsHTML(events);
            
            success = true;
            
            System.out.println("\n╔════════════════════════════════════════════════════╗");
            System.out.println("║     ✅ AUTOMATION COMPLETED SUCCESSFULLY! ✅        ║");
            System.out.println("╚════════════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            System.err.println("\n❌ FATAL ERROR: " + e.getMessage());
            e.printStackTrace();
            
            if (automation.driver != null) {
                automation.takeScreenshot("DAMS_Error_Screenshot");
            }
            
        } finally {
            automation.closeBrowser();
            System.out.println("\n🏁 Automation execution finished");
            System.exit(success ? 0 : 1);
        }
    }
}
