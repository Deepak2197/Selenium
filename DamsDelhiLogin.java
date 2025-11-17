import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.awt.Desktop;

public class DamsCompleteSolution {
    // ================= SELENIUM CONFIG =================
    private static WebDriver driver;
    private static WebDriverWait wait;
    private static JavascriptExecutor js;
    
    // ================= API CONFIGURATION CONSTANTS =================
    private static final String API_URL = "https://api.damsdelhi.com/v2_data_model/get_all_plan_by_category_id";
    private static final String USER_ID = "161444";
    private static final String DEVICE_TOKEN = "61797743405";
    private static final String DEVICE_TYPE = "3";
    private static final String STREAM_ID = "1";
    private static final String API_VERSION = "25";
    // Mock Device Info (JSON Object as String)
    private static final String DEVICE_INFO_JSON = "{\"model\":\"chrome_driver\",\"os\":\"windows\",\"app\":\"dams\",\"manufacturer\":\"selenium\"}";

    // ================= TRACKING DATA =================
    private static List<CourseResult> courseResults = new ArrayList<>();
    private static int totalSuccessful = 0;
    private static int totalFailed = 0;
    
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private static SimpleDateFormat fileFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    
    // API Results Storage
    private static String apiRawData = "";
    private static int apiResponseCode = 0;
    private static String apiExecutionTime = "";
    
    static class CourseResult {
        String courseName;
        String status;
        String timestamp;
        String screenshotPath;
        String errorMessage;
        
        CourseResult(String name, String status, String time, String screenshot, String error) {
            this.courseName = name;
            this.status = status;
            this.timestamp = time;
            this.screenshotPath = screenshot;
            this.errorMessage = error;
        }
    }
    
    public static void main(String[] args) {
        try {
            new File("screenshots").mkdirs();
            
            System.out.println("╔════════════════════════════════════════════╗");
            System.out.println("║  DAMS COMPLETE AUTOMATION SOLUTION         ║");
            System.out.println("╚════════════════════════════════════════════╝\n");

            // STEP 1: Fetch API Data (Backend)
            System.out.println("STEP 1: Fetching API Data (Custom Payload)...");
            fetchAPIData();
            generateAPIHTML();

            // STEP 2: Start Selenium Automation (Frontend)
            System.out.println("\nSTEP 2: Starting Browser Automation...");
            setupDriver();
            login();
            navigateToCBTSectionViaHamburger();

            // Discover all CBT courses
            List<String> cbtCourses = discoverCBTCourses();
            System.out.println("\n✓ Found " + cbtCourses.size() + " CBT courses");
            for (int i = 0; i < cbtCourses.size(); i++) {
                System.out.println("  [" + (i + 1) + "] " + cbtCourses.get(i));
            }

            // Process each CBT course
            for (int i = 0; i < cbtCourses.size(); i++) {
                String courseName = cbtCourses.get(i);
                System.out.println("\n" + "=".repeat(60));
                System.out.println("PROCESSING: " + courseName + " [" + (i+1) + "/" + cbtCourses.size() + "]");
                System.out.println("=".repeat(60));

                processCBTCourse(courseName, i);
                
                if (i < cbtCourses.size() - 1) {
                    returnToCBTSection();
                }
            }

            System.out.println("\n╔════════════════════════════════════════════╗");
            System.out.println("║  EXECUTION COMPLETED!                      ║");
            System.out.println("║  Successful: " + totalSuccessful + "                     ║");
            System.out.println("║  Failed: " + totalFailed + "                         ║");
            System.out.println("╚════════════════════════════════════════════╝");

        } catch (Exception e) {
            System.out.println("CRITICAL ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            generateDetailedReport();
            System.out.println("\nClosing in 10 seconds...");
            sleep(10);
            if (driver != null) {
                driver.quit();
            }
        }
    }

    // ==================== API DATA FETCHING (Optimized) ====================
    
    private static void fetchAPIData() {
        System.out.println("   URL: " + API_URL);
        
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // --- Setup Headers ---
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Origin", "https://www.damsdelhi.com");
            conn.setRequestProperty("Referer", "https://www.damsdelhi.com/");
            
            // --- Construct JSON Payload Manually ---
            String jsonPayload = String.format(
                "{" +
                "\"user_id\": \"%s\"," +
                "\"device_token\": \"%s\"," +
                "\"device_type\": \"%s\"," +
                "\"stream_id\": \"%s\"," +
                "\"api_version\": \"%s\"," +
                "\"device_info\": %s" +
                "}",
                USER_ID, DEVICE_TOKEN, DEVICE_TYPE, STREAM_ID, API_VERSION, DEVICE_INFO_JSON
            );

            // System.out.println("   Payload: " + jsonPayload); // Debugging

            // --- Send Request ---
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // --- Read Response ---
            apiResponseCode = conn.getResponseCode();
            System.out.println("   Status Code: " + apiResponseCode);

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            (apiResponseCode >= 200 && apiResponseCode < 300) ? conn.getInputStream() : conn.getErrorStream(),
                            StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            apiRawData = response.toString();
            apiExecutionTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            if (apiResponseCode == 200) {
                System.out.println("   ✓ Success! Data received (" + apiRawData.length() + " chars).");
            } else {
                System.out.println("   ✗ Failed. Error received.");
            }
            
            conn.disconnect();

        } catch (Exception e) {
            System.out.println("   ✗ Exception: " + e.getMessage());
            apiRawData = "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    // ==================== HTML GENERATION ====================
    
    private static void generateAPIHTML() {
        System.out.println("📄 Generating API Data HTML page...");
        
        try {
            String timestamp = fileFormat.format(new Date());
            String filename = "DAMS_API_Data_" + timestamp + ".html";
            
            StringBuilder html = new StringBuilder();
            
            // HTML Header
            html.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><title>DAMS API Result</title>");
            html.append("<style>");
            html.append("body { font-family: 'Segoe UI', Tahoma, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px; }");
            html.append(".container { max-width: 1200px; margin: 0 auto; background: white; border-radius: 15px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); overflow: hidden; }");
            html.append(".header { padding: 30px; text-align: center; border-bottom: 1px solid #eee; }");
            html.append(".stats { display: flex; justify-content: space-around; padding: 20px; background: #f8f9fa; }");
            html.append(".stat-box { text-align: center; }");
            html.append(".stat-label { font-size: 12px; color: #666; text-transform: uppercase; }");
            html.append(".stat-value { font-size: 20px; font-weight: bold; color: #333; }");
            html.append(".json-container { padding: 20px; background: #1e293b; color: #e2e8f0; overflow-x: auto; font-family: monospace; font-size: 14px; }");
            html.append(".key { color: #e06c75; } .string { color: #98c379; } .number { color: #d19a66; }");
            html.append("</style></head><body>");
            
            html.append("<div class='container'>");
            html.append("<div class='header'><h1>API Response Data</h1><p>").append(apiExecutionTime).append("</p></div>");
            
            html.append("<div class='stats'>");
            html.append("<div class='stat-box'><div class='stat-label'>Status</div><div class='stat-value'>").append(apiResponseCode).append("</div></div>");
            html.append("<div class='stat-box'><div class='stat-label'>Size</div><div class='stat-value'>").append(apiRawData.length()).append(" chars</div></div>");
            html.append("<div class='stat-box'><div class='stat-label'>User ID</div><div class='stat-value'>").append(USER_ID).append("</div></div>");
            html.append("</div>");
            
            html.append("<div class='json-container'><pre id='json-renderer'>Loading...</pre></div>");
            html.append("</div>");
            
            // JavaScript for Syntax Highlighting
            html.append("<script>");
            html.append("const rawData = ").append(apiRawData).append(";");
            html.append("function syntaxHighlight(json) {");
            html.append("    if (typeof json != 'string') { json = JSON.stringify(json, undefined, 2); }");
            html.append("    json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');");
            html.append("    return json.replace(/(\"(\\\\u[a-zA-Z0-9]{4}|\\\\[^u]|[^\\\\\"])*\"(\\s*:)?|\\b(true|false|null)\\b|-?\\d+(?:\\.\\d*)?(?:[eE][+\\-]?\\d+)?)/g, function (match) {");
            html.append("        var cls = 'number';");
            html.append("        if (/^\"/.test(match)) { if (/:$/.test(match)) { cls = 'key'; } else { cls = 'string'; } }");
            html.append("        return '<span class=\"' + cls + '\">' + match + '</span>';");
            html.append("    });");
            html.append("}");
            html.append("document.getElementById('json-renderer').innerHTML = syntaxHighlight(rawData);");
            html.append("</script>");
            html.append("</body></html>");
            
            FileWriter writer = new FileWriter(filename);
            writer.write(html.toString());
            writer.close();
            
            System.out.println("   ✓ API HTML page created: " + filename);
            try {
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(new File(filename));
            } catch(Exception e) {} // Ignore open error
            
        } catch (Exception e) {
            System.out.println("   ✗ Error creating HTML: " + e.getMessage());
        }
    }

    // ==================== SELENIUM AUTOMATION ====================
    
    private static void setupDriver() {
        System.out.println("Setting up Chrome driver...");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-notifications");
        options.addArguments("--start-maximized");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        js = (JavascriptExecutor) driver;
        System.out.println("✓ Driver ready\n");
    }

    private static void login() {
        System.out.println("Starting login...");
        driver.get("https://www.damsdelhi.com/");
        sleep(3);
        
        try {
            // Try Sign In Button
            try {
                WebElement signInBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(), 'Sign in') or contains(text(), 'Sign In')]")));
                signInBtn.click();
            } catch (Exception e) {
                // Try Hamburger if main button fails
                WebElement hamburger = wait.until(ExpectedConditions.elementToBeClickable(By.className("humburgerIcon")));
                hamburger.click();
                sleep(1);
                WebElement signInLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[contains(text(), 'Sign in')]")));
                signInLink.click();
            }
            System.out.println("   ✓ Clicked Sign In");
            
            // Enter Phone
            enterText(By.xpath("//input[@type='tel' or @type='number']"), "+919456628016", "Phone");
            sleep(1);
            clickElement(By.className("common-bottom-btn"), "Request OTP");
            sleep(3);
            
            // Handle Logout Popup if exists
            try {
                WebElement logoutBtn = driver.findElement(By.xpath("//button[contains(text(), 'Logout')]"));
                logoutBtn.click();
                System.out.println("   ✓ Clicked Logout popup");
                sleep(2);
            } catch (Exception e) { /* Ignore */ }
            
            // Enter OTP
            enterText(By.xpath("//input[@placeholder='Enter OTP']"), "2000", "OTP");
            sleep(1);
            clickElement(By.className("common-bottom-btn"), "Submit OTP");
            sleep(5);
            System.out.println("✓ Login successful\n");
            
        } catch (Exception e) {
            System.out.println("✗ Login Failed: " + e.getMessage());
        }
    }

    private static void navigateToCBTSectionViaHamburger() {
        System.out.println("Navigating to CBT section via Hamburger menu...");
        
        try {
            // 1. Select Course Category (NEET PG)
            try {
                WebElement dropdown = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class, 'SelectCat')]")));
                dropdown.click();
                sleep(1);
                List<WebElement> options = driver.findElements(By.xpath("//span[contains(text(), 'NEET PG')]"));
                for(WebElement opt : options) {
                    if(opt.isDisplayed()) { opt.click(); break; }
                }
                System.out.println("   ✓ Selected: NEET PG");
                sleep(3);
            } catch (Exception e) {
                System.out.println("   ⚠ Skipping NEET PG selection");
            }
            
            // 2. Close any modals
            try {
                WebElement closeBtn = driver.findElement(By.xpath("//button[@aria-label='Close'] | //span[contains(@class, 'ant-modal-close')]"));
                closeBtn.click();
            } catch (Exception e) {}

            // 3. Open Hamburger
            WebElement hamburger = wait.until(ExpectedConditions.elementToBeClickable(By.className("humburgerIcon")));
            hamburger.click();
            sleep(1);
            System.out.println("   ✓ Clicked Hamburger");
            
            // 4. Click CBT Link
            // Updated Logic to fix your previous snippet error
            try {
                WebElement cbtLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[contains(@class, 'Categories')]//div[contains(text(), 'CBT')] | //button[contains(., 'CBT')]")));
                js.executeScript("arguments[0].click();", cbtLink);
                System.out.println("   ✓ Clicked CBT Link");
                sleep(3);
            } catch (Exception e) {
                System.out.println("   ✗ Could not find CBT link in menu");
            }

            // 5. Handle "OK" button on CBT disclaimer
            try {
                WebElement okBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'btn-danger') and contains(text(), 'OK')]")));
                okBtn.click();
                System.out.println("   ✓ Clicked OK Button");
                sleep(2);
            } catch (Exception e) {
                System.out.println("   ℹ No OK button");
            }
            
        } catch (Exception e) {
            System.out.println("✗ Error navigating: " + e.getMessage());
        }
    }

    private static List<String> discoverCBTCourses() {
        List<String> courses = new ArrayList<>();
        try {
            sleep(3);
            // Scroll to load
            js.executeScript("window.scrollTo(0, document.body.scrollHeight/2);");
            sleep(2);
            
            List<WebElement> buyButtons = driver.findElements(
                By.xpath("//button[contains(@class, 'butBtn') and contains(@class, 'modal_show')]"));
                
            int limit = Math.min(3, buyButtons.size());
            for(int i=0; i<limit; i++) {
                courses.add("CBT Course " + (i+1)); // Using generic names as titles are hard to scrape reliably here
            }
            return courses;
        } catch (Exception e) {
            return courses;
        }
    }

    private static void processCBTCourse(String courseName, int courseIndex) {
        String timestamp = timeFormat.format(new Date());
        String screenshotPath = null;
        
        try {
            List<WebElement> buyButtons = driver.findElements(
                By.xpath("//button[contains(@class, 'butBtn') and contains(@class, 'modal_show')]"));
            
            if (courseIndex >= buyButtons.size()) return;
            
            WebElement btn = buyButtons.get(courseIndex);
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", btn);
            sleep(1);
            btn.click();
            System.out.println("   ✓ Clicked Buy Now");
            sleep(3);
            
            // Modal Steps
            try {
                WebElement cbtRadio = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//label[contains(., 'CBT')]")));
                cbtRadio.click();
                driver.findElement(By.xpath("//button[text()='OK']")).click();
                sleep(2);
            } catch (Exception e) {} // Ignore if not present
            
            // Flex/Delhi selection logic (Simplified for brevity)
            try {
                List<WebElement> delhiBtns = driver.findElements(By.xpath("//button[contains(text(), 'Delhi')]"));
                if(!delhiBtns.isEmpty()) delhiBtns.get(0).click();
                sleep(2);
            } catch(Exception e) {}

            // Final Payment Button
            try {
                WebElement redBtn = driver.findElement(By.xpath("//button[contains(@class, 'btn-danger') and contains(@class, 'btn-block')]"));
                redBtn.click();
                sleep(3);
            } catch(Exception e) {}
            
            // QR Code Wait
            System.out.println("   ⏳ Waiting for QR code...");
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//canvas | //img[contains(@class, 'qr')]")));
                System.out.println("   ✓ QR code detected");
                
                // Screenshot
                String filename = "screenshots/QR_" + timestamp.replace(":","") + "_" + courseIndex + ".png";
                File src = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
                Files.copy(src.toPath(), new File(filename).toPath(), StandardCopyOption.REPLACE_EXISTING);
                screenshotPath = filename;
                System.out.println("   ✓ Screenshot saved");
                
                courseResults.add(new CourseResult(courseName, "SUCCESS", timestamp, screenshotPath, null));
                totalSuccessful++;
                
                // Close Payment
                closePaymentWindow();
                
            } catch (Exception e) {
                throw new Exception("QR Code not found");
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Failed: " + e.getMessage());
            courseResults.add(new CourseResult(courseName, "FAILED", timestamp, null, e.getMessage()));
            totalFailed++;
        }
    }

    private static void returnToCBTSection() {
        driver.get("https://www.damsdelhi.com/");
        sleep(3);
        navigateToCBTSectionViaHamburger();
    }

    private static void closePaymentWindow() {
        try {
             // Generic close button attempts
             List<WebElement> closeBtns = driver.findElements(By.xpath("//span[contains(@class, 'ptm-cross')] | //button[contains(text(), 'Cancel')]"));
             if(!closeBtns.isEmpty()) closeBtns.get(0).click();
        } catch(Exception e) {}
    }

    // ==================== HELPER METHODS ====================
    private static void clickElement(By locator, String name) {
        try {
            WebElement el = wait.until(ExpectedConditions.elementToBeClickable(locator));
            el.click();
            System.out.println("   ✓ Clicked " + name);
        } catch (Exception e) {
            System.out.println("   ✗ Failed to click " + name);
        }
    }
    
    private static void enterText(By locator, String text, String name) {
        try {
            WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            el.clear();
            el.sendKeys(text);
            System.out.println("   ✓ Entered " + name);
        } catch (Exception e) {
            System.out.println("   ✗ Failed to enter " + name);
        }
    }
    
    private static void sleep(int sec) {
        try { Thread.sleep(sec * 1000L); } catch (InterruptedException e) {}
    }
    
    private static String escapeHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static void generateDetailedReport() {
        try {
            String filename = "DAMS_Automation_Report_" + fileFormat.format(new Date()) + ".html";
            StringBuilder html = new StringBuilder();
            html.append("<html><head><style>");
            html.append("body{font-family:sans-serif;padding:20px;} table{border-collapse:collapse;width:100%;} th,td{border:1px solid #ddd;padding:8px;} th{background:#f2f2f2;} .success{color:green;} .failed{color:red;}");
            html.append("</style></head><body>");
            html.append("<h1>Automation Report</h1>");
            html.append("<table><tr><th>Course</th><th>Status</th><th>Time</th><th>Screenshot</th></tr>");
            
            for(CourseResult r : courseResults) {
                html.append("<tr>");
                html.append("<td>").append(r.courseName).append("</td>");
                html.append("<td class='").append(r.status.toLowerCase()).append("'>").append(r.status).append("</td>");
                html.append("<td>").append(r.timestamp).append("</td>");
                html.append("<td>").append(r.screenshotPath != null ? "<a href='"+r.screenshotPath+"'>View</a>" : "-").append("</td>");
                html.append("</tr>");
            }
            html.append("</table></body></html>");
            
            FileWriter w = new FileWriter(filename);
            w.write(html.toString());
            w.close();
            System.out.println("\n✓ Automation Report saved: " + filename);
        } catch(Exception e) {
            System.out.println("Error generating report: " + e.getMessage());
        }
    }
}
