import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class DamsCompleteSolution {
    private static WebDriver driver;
    private static WebDriverWait wait;
    private static JavascriptExecutor js;
    
    // Tracking data
    private static List<CourseResult> courseResults = new ArrayList<>();
    private static int totalSuccessful = 0;
    private static int totalFailed = 0;
    
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private static SimpleDateFormat fileFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private static String executionStartTime;
    
    // API raw data
    private static String apiRawData = "";
    
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
            executionStartTime = fileFormat.format(new Date());

            System.out.println("╔════════════════════════════════════════════╗");
            System.out.println("║  DAMS COMPLETE AUTOMATION SOLUTION         ║");
            System.out.println("╚════════════════════════════════════════════╝\n");

            // STEP 1: Fetch API Data and Generate HTML
            System.out.println("STEP 1: Fetching API Data...");
            fetchAPIData();
            generateAPIHTML();

            // STEP 2: Start Selenium Automation
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
            System.out.println("║  Successful: " + totalSuccessful + "                              ║");
            System.out.println("║  Failed: " + totalFailed + "                                  ║");
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

    // ==================== UPDATED API DATA FETCHING WITH POST ====================
    
    private static void fetchAPIData() {
        System.out.println("📡 Fetching data from API using POST...");
        StringBuilder response = new StringBuilder();
        
        try {
            URL url = new URL("https://api.damsdelhi.com/v2_data_model/get_all_plan_by_category_id");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            // Set POST method
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            
            // Set all required headers from the browser request
            conn.setRequestProperty("Accept", "application/json, text/plain, */*");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br, zstd");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Origin", "https://www.damsdelhi.com");
            conn.setRequestProperty("Referer", "https://www.damsdelhi.com/");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1");
            conn.setRequestProperty("sec-fetch-dest", "empty");
            conn.setRequestProperty("sec-fetch-mode", "cors");
            conn.setRequestProperty("sec-fetch-site", "same-site");
            
            // Add custom headers
            conn.setRequestProperty("api_version", "1.0");
            conn.setRequestProperty("device_type", "web");
            conn.setRequestProperty("device_info", "web-browser");
            
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            
            // Send POST data (empty JSON object or add required payload)
            String jsonInputString = "{}";
            
            try(OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            int responseCode = conn.getResponseCode();
            System.out.println("   Response Code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                String inputLine;
                
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                apiRawData = response.toString();
                System.out.println("   ✓ Data fetched successfully");
                System.out.println("   Data length: " + apiRawData.length() + " characters");
                
                // Print first 500 characters as preview
                if (apiRawData.length() > 0) {
                    System.out.println("\n   📄 Response Preview:");
                    System.out.println("   " + apiRawData.substring(0, Math.min(500, apiRawData.length())) + "...\n");
                }
            } else {
                System.out.println("   ✗ Failed with response code: " + responseCode);
                
                // Try to read error stream
                try {
                    BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), "utf-8"));
                    String errorLine;
                    StringBuilder errorResponse = new StringBuilder();
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();
                    System.out.println("   Error Response: " + errorResponse.toString());
                } catch (Exception e) {
                    System.out.println("   Could not read error stream");
                }
            }
            
            conn.disconnect();
            
        } catch (Exception e) {
            System.out.println("   ✗ Error fetching data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== ENHANCED HTML GENERATION ====================
    
    private static void generateAPIHTML() {
        System.out.println("📄 Generating API Data HTML page...");
        
        try {
            String timestamp = fileFormat.format(new Date());
            String filename = "DAMS_API_Data_" + timestamp + ".html";
            
            StringBuilder html = new StringBuilder();
            
            // HTML Document Start
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang='en'>\n");
            html.append("<head>\n");
            html.append("    <meta charset='UTF-8'>\n");
            html.append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
            html.append("    <title>DAMS API Data - ").append(timestamp).append("</title>\n");
            html.append("    <style>\n");
            html.append("        * { margin: 0; padding: 0; box-sizing: border-box; }\n");
            html.append("        body {\n");
            html.append("            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n");
            html.append("            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n");
            html.append("            min-height: 100vh;\n");
            html.append("            padding: 40px 20px;\n");
            html.append("        }\n");
            html.append("        .container { max-width: 1400px; margin: 0 auto; }\n");
            html.append("        .header {\n");
            html.append("            background: white;\n");
            html.append("            border-radius: 20px;\n");
            html.append("            padding: 40px;\n");
            html.append("            margin-bottom: 30px;\n");
            html.append("            box-shadow: 0 10px 40px rgba(0,0,0,0.1);\n");
            html.append("            text-align: center;\n");
            html.append("        }\n");
            html.append("        .header h1 {\n");
            html.append("            color: #2d3748;\n");
            html.append("            font-size: 42px;\n");
            html.append("            font-weight: 700;\n");
            html.append("            margin-bottom: 10px;\n");
            html.append("        }\n");
            html.append("        .header .subtitle { color: #718096; font-size: 16px; }\n");
            html.append("        .content-box {\n");
            html.append("            background: white;\n");
            html.append("            border-radius: 20px;\n");
            html.append("            padding: 40px;\n");
            html.append("            box-shadow: 0 10px 40px rgba(0,0,0,0.1);\n");
            html.append("            margin-bottom: 30px;\n");
            html.append("        }\n");
            html.append("        .content-box h2 { color: #2d3748; margin-bottom: 20px; font-size: 24px; }\n");
            html.append("        .json-container {\n");
            html.append("            background: #1e293b;\n");
            html.append("            color: #e2e8f0;\n");
            html.append("            padding: 30px;\n");
            html.append("            border-radius: 15px;\n");
            html.append("            overflow-x: auto;\n");
            html.append("            font-family: 'Courier New', monospace;\n");
            html.append("            font-size: 14px;\n");
            html.append("            line-height: 1.6;\n");
            html.append("            max-height: 600px;\n");
            html.append("            overflow-y: auto;\n");
            html.append("        }\n");
            html.append("        .json-container pre {\n");
            html.append("            margin: 0;\n");
            html.append("            white-space: pre-wrap;\n");
            html.append("            word-wrap: break-word;\n");
            html.append("        }\n");
            html.append("        .stats {\n");
            html.append("            display: grid;\n");
            html.append("            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));\n");
            html.append("            gap: 20px;\n");
            html.append("            margin-bottom: 30px;\n");
            html.append("        }\n");
            html.append("        .stat-card {\n");
            html.append("            background: white;\n");
            html.append("            border-radius: 15px;\n");
            html.append("            padding: 25px;\n");
            html.append("            box-shadow: 0 5px 20px rgba(0,0,0,0.1);\n");
            html.append("            text-align: center;\n");
            html.append("        }\n");
            html.append("        .stat-card .label {\n");
            html.append("            color: #718096;\n");
            html.append("            font-size: 14px;\n");
            html.append("            margin-bottom: 10px;\n");
            html.append("            text-transform: uppercase;\n");
            html.append("            letter-spacing: 1px;\n");
            html.append("        }\n");
            html.append("        .stat-card .value { color: #667eea; font-size: 36px; font-weight: 700; }\n");
            html.append("        .btn-group { display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; }\n");
            html.append("        .copy-btn, .download-btn {\n");
            html.append("            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n");
            html.append("            color: white;\n");
            html.append("            border: none;\n");
            html.append("            padding: 12px 30px;\n");
            html.append("            border-radius: 10px;\n");
            html.append("            font-size: 16px;\n");
            html.append("            font-weight: 600;\n");
            html.append("            cursor: pointer;\n");
            html.append("            transition: all 0.3s;\n");
            html.append("        }\n");
            html.append("        .download-btn { background: linear-gradient(135deg, #48bb78 0%, #38a169 100%); }\n");
            html.append("        .copy-btn:hover, .download-btn:hover {\n");
            html.append("            transform: translateY(-2px);\n");
            html.append("            box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);\n");
            html.append("        }\n");
            html.append("        .api-info {\n");
            html.append("            background: #f7fafc;\n");
            html.append("            padding: 20px;\n");
            html.append("            border-radius: 10px;\n");
            html.append("            margin-bottom: 20px;\n");
            html.append("            font-family: monospace;\n");
            html.append("            font-size: 13px;\n");
            html.append("        }\n");
            html.append("        .api-info .method { color: #48bb78; font-weight: bold; }\n");
            html.append("        .api-info .url { color: #667eea; word-break: break-all; }\n");
            html.append("        .footer { text-align: center; color: white; padding: 20px; font-size: 14px; }\n");
            html.append("        @media (max-width: 768px) {\n");
            html.append("            .header h1 { font-size: 28px; }\n");
            html.append("            .content-box { padding: 25px 20px; }\n");
            html.append("        }\n");
            html.append("    </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("    <div class='container'>\n");
            
            // Header
            html.append("        <div class='header'>\n");
            html.append("            <h1>🎓 DAMS API Complete Data</h1>\n");
            html.append("            <p class='subtitle'>Full API Response from get_all_plan_by_category_id</p>\n");
            html.append("            <p class='subtitle' style='margin-top: 10px; font-weight: 600;'>")
                .append(new SimpleDateFormat("dd MMM yyyy, HH:mm:ss").format(new Date()))
                .append("</p>\n");
            html.append("        </div>\n");
            
            // API Info
            html.append("        <div class='content-box'>\n");
            html.append("            <h2>🔗 API Request Details</h2>\n");
            html.append("            <div class='api-info'>\n");
            html.append("                <div><span class='method'>POST</span> <span class='url'>https://api.damsdelhi.com/v2_data_model/get_all_plan_by_category_id</span></div>\n");
            html.append("                <div style='margin-top: 10px;'><strong>Headers:</strong></div>\n");
            html.append("                <div>• Content-Type: application/json</div>\n");
            html.append("                <div>• Origin: https://www.damsdelhi.com</div>\n");
            html.append("                <div>• User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 18_5...)</div>\n");
            html.append("                <div>• api_version: 1.0</div>\n");
            html.append("                <div>• device_type: web</div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            // Stats
            html.append("        <div class='stats'>\n");
            html.append("            <div class='stat-card'>\n");
            html.append("                <div class='label'>Data Size</div>\n");
            html.append("                <div class='value'>").append(String.format("%.2f", apiRawData.length() / 1024.0)).append(" KB</div>\n");
            html.append("            </div>\n");
            html.append("            <div class='stat-card'>\n");
            html.append("                <div class='label'>Characters</div>\n");
            html.append("                <div class='value'>").append(apiRawData.length()).append("</div>\n");
            html.append("            </div>\n");
            html.append("            <div class='stat-card'>\n");
            html.append("                <div class='label'>Status</div>\n");
            html.append("                <div class='value' style='font-size: 18px; color: #48bb78;'>✓ Success</div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            // Content Box
            html.append("        <div class='content-box'>\n");
            html.append("            <h2>📊 Complete API Response</h2>\n");
            html.append("            <div class='btn-group'>\n");
            html.append("                <button class='copy-btn' onclick='copyToClipboard()'>📋 Copy JSON Data</button>\n");
            html.append("                <button class='download-btn' onclick='downloadJSON()'>💾 Download JSON</button>\n");
            html.append("            </div>\n");
            html.append("            <div class='json-container' id='jsonData'>\n");
            html.append("                <pre>").append(escapeHtml(formatJSON(apiRawData))).append("</pre>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            // Footer
            html.append("        <div class='footer'>\n");
            html.append("            <p>🤖 Generated from DAMS API Automation</p>\n");
            html.append("            <p>📅 ").append(new SimpleDateFormat("EEEE, dd MMMM yyyy").format(new Date())).append("</p>\n");
            html.append("        </div>\n");
            html.append("    </div>\n");
            
            // JavaScript
            html.append("    <script>\n");
            html.append("        function copyToClipboard() {\n");
            html.append("            const jsonText = document.getElementById('jsonData').innerText;\n");
            html.append("            navigator.clipboard.writeText(jsonText).then(function() {\n");
            html.append("                alert('✅ JSON data copied to clipboard!');\n");
            html.append("            }).catch(function(err) {\n");
            html.append("                console.error('Copy failed:', err);\n");
            html.append("                alert('❌ Failed to copy. Please copy manually.');\n");
            html.append("            });\n");
            html.append("        }\n");
            html.append("        function downloadJSON() {\n");
            html.append("            const jsonText = document.getElementById('jsonData').innerText;\n");
            html.append("            const blob = new Blob([jsonText], { type: 'application/json' });\n");
            html.append("            const url = URL.createObjectURL(blob);\n");
            html.append("            const a = document.createElement('a');\n");
            html.append("            a.href = url;\n");
            html.append("            a.download = 'dams_api_data_").append(timestamp).append(".json';\n");
            html.append("            document.body.appendChild(a);\n");
            html.append("            a.click();\n");
            html.append("            document.body.removeChild(a);\n");
            html.append("            URL.revokeObjectURL(url);\n");
            html.append("        }\n");
            html.append("    </script>\n");
            html.append("</body>\n");
            html.append("</html>");
            
            // Write to file
            FileWriter writer = new FileWriter(filename);
            writer.write(html.toString());
            writer.close();
            
            System.out.println("   ✓ API HTML page created: " + filename);
            System.out.println("   File size: " + (html.length() / 1024) + " KB\n");
            
        } catch (Exception e) {
            System.out.println("   ✗ Error creating HTML: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String formatJSON(String json) {
        try {
            StringBuilder formatted = new StringBuilder();
            int indent = 0;
            boolean inString = false;
            
            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                
                if (c == '"' && (i == 0 || json.charAt(i-1) != '\\')) {
                    inString = !inString;
                }
                
                if (!inString) {
                    if (c == '{' || c == '[') {
                        formatted.append(c).append('\n');
                        indent++;
                        formatted.append("  ".repeat(indent));
                    } else if (c == '}' || c == ']') {
                        formatted.append('\n');
                        indent--;
                        formatted.append("  ".repeat(indent));
                        formatted.append(c);
                    } else if (c == ',') {
                        formatted.append(c).append('\n');
                        formatted.append("  ".repeat(indent));
                    } else if (c == ':') {
                        formatted.append(c).append(' ');
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

    // ==================== SELENIUM AUTOMATION ====================
    
    private static void setupDriver() {
        System.out.println("Setting up Chrome driver...");
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-notifications");
        
        String ciMode = System.getenv("CI");
        if ("true".equals(ciMode)) {
            System.out.println("Running in CI mode (headless)");
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
        } else {
            options.addArguments("--start-maximized");
        }
        
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        js = (JavascriptExecutor) driver;
        
        System.out.println("✓ Driver ready\n");
    }

    private static void login() {
        System.out.println("Starting login...");
        
        driver.get("https://www.damsdelhi.com/");
        sleep(3);
        
        try {
            WebElement signInBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[contains(text(), 'Sign in') or contains(text(), 'Sign In')]")));
            js.executeScript("arguments[0].click();", signInBtn);
            System.out.println("  ✓ Clicked: Sign In button");
            sleep(3);
        } catch (Exception e) {
            try {
                WebElement signInBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//a[contains(text(), 'Sign in') or contains(text(), 'Sign In')]")));
                js.executeScript("arguments[0].click();", hamburger);
                System.out.println("  ✓ Clicked: Hamburger Menu");
                hamburgerClicked = true;
                sleep(3);
            } catch (Exception e) {
                System.out.println("  ✗ Failed to click hamburger");
            }
            
            if (!hamburgerClicked) return;
            
            boolean cbtClicked = false;
            By[] cbtSelectors = {
                By.xpath("//div[contains(@class, 'Categories')]//div[contains(text(), 'CBT')]"),
                By.xpath("//div[contains(@class, 'Categories')]//*[contains(text(), 'CBT')]"),
                By.xpath("//button[contains(., 'CBT')]"),
                By.xpath("//*[@role='button' and contains(., 'CBT')]"),
                By.xpath("//*[contains(text(), 'CBT') and not(contains(text(), 'NEET'))]")
            };
            
            for (By selector : cbtSelectors) {
                try {
                    List<WebElement> cbtElements = driver.findElements(selector);
                    for (WebElement cbtElem : cbtElements) {
                        if (cbtElem.isDisplayed()) {
                            String elemText = cbtElem.getText().trim();
                            if (elemText.equals("CBT") || elemText.equalsIgnoreCase("cbt")) {
                                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", cbtElem);
                                sleep(1);
                                js.executeScript("arguments[0].click();", cbtElem);
                                System.out.println("  ✓ Clicked: CBT button");
                                cbtClicked = true;
                                sleep(3);
                                break;
                            }
                        }
                    }
                    if (cbtClicked) break;
                } catch (Exception e) {}
            }
            
            if (!cbtClicked) return;
            
            try {
                WebElement okBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//button[@type='button' and contains(@class, 'btn-danger') and contains(text(), 'OK')]")));
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", okBtn);
                sleep(1);
                js.executeScript("arguments[0].click();", okBtn);
                System.out.println("  ✓ Clicked: OK Button");
                sleep(3);
            } catch (Exception e) {
                System.out.println("  ℹ No OK button");
            }
            
            System.out.println("✓ Successfully navigated to CBT section\n");
            
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
    }

    private static List<String> discoverCBTCourses() {
        System.out.println("Discovering CBT courses...");
        List<String> courses = new ArrayList<>();
        
        try {
            sleep(5);
            js.executeScript("window.scrollTo(0, 0);");
            sleep(2);
            
            long lastHeight = (Long) js.executeScript("return document.body.scrollHeight");
            int stableCount = 0;
            
            while (stableCount < 3) {
                js.executeScript("window.scrollBy(0, 500);");
                sleep(1);
                long newHeight = (Long) js.executeScript("return document.body.scrollHeight");
                if (newHeight == lastHeight) {
                    stableCount++;
                } else {
                    stableCount = 0;
                    lastHeight = newHeight;
                }
            }
            
            js.executeScript("window.scrollTo(0, 0);");
            sleep(2);
            
            List<WebElement> buyNowButtons = driver.findElements(
                By.xpath("//button[@type='button' and contains(@class, 'butBtn') and contains(@class, 'modal_show')]"));
            
            System.out.println("  → Found " + buyNowButtons.size() + " Buy Now buttons");
            
            if (buyNowButtons.isEmpty()) {
                return courses;
            }
            
            int coursesToProcess = Math.min(3, buyNowButtons.size());
            System.out.println("  → Processing " + coursesToProcess + " courses (LIMITED TO 3)");
            
            for (int i = 0; i < coursesToProcess; i++) {
                WebElement button = buyNowButtons.get(i);
                try {
                    js.executeScript("arguments[0].scrollIntoView({block: 'center'});", button);
                    sleep(1);
                    
                    WebElement container = button.findElement(By.xpath("./ancestor::div[contains(@class, 'col')]"));
                    String courseName = "CBT Course " + (i + 1);
                    
                    try {
                        WebElement titleElem = container.findElement(
                            By.xpath(".//h3 | .//h4 | .//h5 | .//*[contains(@class, 'title')]"));
                        String title = titleElem.getText().trim();
                        if (!title.isEmpty() && title.length() > 10) {
                            courseName = title;
                        }
                    } catch (Exception e) {}
                    
                    courses.add(courseName);
                    System.out.println("  ✓ Found course: " + courseName);
                } catch (Exception e) {
                    System.out.println("  ⚠ Skipped course " + (i + 1));
                }
            }
            
            return new ArrayList<>(new LinkedHashSet<>(courses));
            
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
            return courses;
        }
    }

    private static void processCBTCourse(String courseName, int courseIndex) {
        String timestamp = timeFormat.format(new Date());
        String screenshotPath = null;
        String errorMsg = null;
        
        try {
            List<WebElement> buyButtons = driver.findElements(
                By.xpath("//button[contains(@class, 'butBtn') and contains(@class, 'modal_show')]"));
            
            if (courseIndex < buyButtons.size()) {
                WebElement buyBtn = buyButtons.get(courseIndex);
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", buyBtn);
                sleep(2);
                js.executeScript("arguments[0].click();", buyBtn);
                System.out.println("  ✓ Step 1: Clicked Buy Now");
                sleep(3);
            } else {
                throw new Exception("Buy button not found");
            }
            
            try {
                WebElement cbtModal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[@class='popup' and .//div[@id='cbt_hide']]")));
                WebElement cbtRadioLabel = cbtModal.findElement(
                    By.xpath(".//label[contains(normalize-space(), 'CBT (Center Based Test)')]"));
                js.executeScript("arguments[0].click();", cbtRadioLabel);
                System.out.println("  ✓ Clicked CBT option");
                sleep(1);
                
                WebElement modalOkButton = cbtModal.findElement(By.xpath(".//button[normalize-space()='OK']"));
                js.executeScript("arguments[0].click();", modalOkButton);
                System.out.println("  ✓ Clicked OK");
                sleep(3);
            } catch (Exception e) {
                System.out.println("  ℹ CBT Modal skipped");
            }
            
            try {
                WebElement flexBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//button[contains(@class, 'show_data_city')]")));
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", flexBtn);
                sleep(1);
                js.executeScript("arguments[0].click();", flexBtn);
                System.out.println("  ✓ Step 2: Clicked Flex Button");
                sleep(2);
            } catch (Exception e) {
                System.out.println("  ℹ Flex button skipped");
            }
            
            try {
                WebElement delhiBtn = driver.findElement(
                    By.xpath("//button[contains(text(), 'Delhi') or contains(@data-city, 'Delhi')]"));
                js.executeScript("arguments[0].click();", delhiBtn);
                System.out.println("  ✓ Step 3: Selected Delhi");
                sleep(2);
            } catch (Exception e) {
                System.out.println("  ℹ Delhi selection skipped");
            }
            
            try {
                WebElement redBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//button[contains(@class, 'btn-danger') and contains(@class, 'btn-block')]")));
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", redBtn);
                sleep(1);
                js.executeScript("arguments[0].click();", redBtn);
                System.out.println("  ✓ Step 4: Clicked Red Button");
                sleep(3);
            } catch (Exception e) {
                System.out.println("  ⚠ Red button not found");
            }
            
            try {
                WebElement paytm = null;
                By[] paytmSelectors = {
                    By.xpath("//label[.//span[contains(text(), 'Paytm')]]"),
                    By.xpath("//span[contains(text(), 'Paytm')]/ancestor::label"),
                    By.xpath("//input[@value='paytm']/parent::label")
                };
                
                for (By selector : paytmSelectors) {
                    try {
                        paytm = wait.until(ExpectedConditions.presenceOfElementLocated(selector));
                        if (paytm.isDisplayed()) break;
                    } catch (Exception e) {}
                }
                
                if (paytm != null) {
                    js.executeScript("arguments[0].click();", paytm);
                    System.out.println("  ✓ Step 5: Selected Paytm");
                    sleep(2);
                }
            } catch (Exception e) {
                System.out.println("  ℹ Paytm selection skipped");
            }
            
            try {
                WebElement paymentBtn = null;
                By[] paymentSelectors = {
                    By.xpath("//button[@type='button' and contains(@class, 'ant-btn-primary') and contains(@class, 'ant-btn-block')]"),
                    By.xpath("//button[contains(text(), 'Pay') or contains(text(), 'Proceed')]")
                };
                
                for (By selector : paymentSelectors) {
                    try {
                        paymentBtn = wait.until(ExpectedConditions.presenceOfElementLocated(selector));
                        if (paymentBtn.isDisplayed()) break;
                    } catch (Exception e) {}
                }
                
                if (paymentBtn != null) {
                    js.executeScript("arguments[0].click();", paymentBtn);
                    System.out.println("  ✓ Step 6: Clicked Payment Button");
                    sleep(2);
                }
            } catch (Exception e) {
                System.out.println("  ⚠ Payment button issue");
            }
            
            System.out.println("  ⏳ Step 7: Waiting for QR code...");
            WebDriverWait qrWait = new WebDriverWait(driver, Duration.ofSeconds(60));
            
            try {
                By qrLocator = By.xpath("//canvas | //img[contains(@class, 'qr') or contains(@class, 'QR')]");
                qrWait.until(ExpectedConditions.presenceOfElementLocated(qrLocator));
                System.out.println("  ✓ QR code detected");
                sleep(2);
            } catch (Exception e) {
                System.out.println("  ⚠ QR wait timeout");
            }
            
            String fileTimestamp = fileFormat.format(new Date());
            String filename = "screenshots/CBT_QR_" + courseName.replaceAll("[^a-zA-Z0-9]", "_") + 
                            "_" + fileTimestamp + ".png";
            
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            copyFile(screenshot, new File(filename));
            screenshotPath = filename;
            System.out.println("  ✓ Step 8: Screenshot saved: " + filename);
            
            closePaymentWindow();
            System.out.println("  ✓ Step 9: Closed payment window");
            
            courseResults.add(new CourseResult(courseName, "SUCCESS", timestamp, screenshotPath, null));
            totalSuccessful++;
            System.out.println("  ✅ Course processed successfully");
            
        } catch (Exception e) {
            errorMsg = e.getMessage();
            courseResults.add(new CourseResult(courseName, "FAILED", timestamp, screenshotPath, errorMsg));
            totalFailed++;
            System.out.println("  ❌ Failed: " + errorMsg);
        }
    }

    private static void returnToCBTSection() {
        try {
            System.out.println("\n  → Returning to CBT section...");
            driver.get("https://www.damsdelhi.com/");
            sleep(3);
            
            WebElement hamburger = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.className("humburgerIcon")));
            js.executeScript("arguments[0].click();", hamburger);
            sleep(2);
            
            List<WebElement> cbtElements = driver.findElements(
                By.xpath("//div[contains(@class, 'Categories')]//div[contains(text(), 'CBT')]"));
            for (WebElement cbtElem : cbtElements) {
                if (cbtElem.isDisplayed() && cbtElem.getText().trim().equals("CBT")) {
                    js.executeScript("arguments[0].click();", cbtElem);
                    sleep(2);
                    break;
                }
            }
            
            WebElement okBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[@type='button' and contains(@class, 'btn-danger') and contains(text(), 'OK')]")));
            js.executeScript("arguments[0].click();", okBtn);
            sleep(3);
        } catch (Exception e) {
            System.out.println("  ⚠ Error: " + e.getMessage());
        }
    }

    private static void closePaymentWindow() {
        try {
            By[] closeSelectors = {
                By.xpath("//span[contains(@class, 'ptm-cross') and @id='app-close-btn']"),
                By.id("app-close-btn")
            };
            
            for (By selector : closeSelectors) {
                try {
                    WebElement closeBtn = driver.findElement(selector);
                    js.executeScript("arguments[0].click();", closeBtn);
                    sleep(8);
                    break;
                } catch (Exception e) {}
            }
            
            try {
                WebElement skipBtn = driver.findElement(By.xpath("//button[contains(text(), 'Skip')]"));
                js.executeScript("arguments[0].click();", skipBtn);
                sleep(2);
            } catch (Exception e) {}
            
            try {
                WebElement modalBtn = driver.findElement(By.xpath("//span[contains(@class, 'ant-modal-close-x')]"));
                js.executeScript("arguments[0].click();", modalBtn);
                sleep(2);
            } catch (Exception e) {}
        } catch (Exception e) {}
    }

    private static void clickElement(By locator, String name) {
        try {
            WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", elem);
            sleep(1);
            js.executeScript("arguments[0].click();", elem);
            System.out.println("  ✓ Clicked: " + name);
        } catch (Exception e) {
            System.out.println("  ✗ Failed: " + name);
        }
    }

    private static void enterText(By locator, String text, String fieldName) {
        try {
            WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            elem.clear();
            elem.sendKeys(text);
            System.out.println("  ✓ Entered: " + fieldName);
        } catch (Exception e) {
            System.out.println("  ✗ Failed: " + fieldName);
        }
    }

    private static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void copyFile(File source, File dest) throws Exception {
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void generateDetailedReport() {
        System.out.println("\nGenerating automation report...");
        
        try {
            String timestamp = fileFormat.format(new Date());
            String filename = "DAMS_CBT_Report_" + timestamp + ".html";
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n<html>\n<head>\n");
            html.append("<meta charset='UTF-8'>\n");
            html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
            html.append("<title>DAMS CBT Report - ").append(timestamp).append("</title>\n");
            html.append("<style>\n");
            html.append("* { margin: 0; padding: 0; box-sizing: border-box; }\n");
            html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; padding: 40px 20px; }\n");
            html.append(".container { max-width: 1400px; margin: 0 auto; }\n");
            html.append(".header { background: white; border-radius: 20px; padding: 40px; margin-bottom: 30px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); text-align: center; }\n");
            html.append(".header h1 { color: #2d3748; font-size: 42px; font-weight: 700; margin-bottom: 10px; }\n");
            html.append(".summary { background: white; border-radius: 20px; padding: 40px; margin-bottom: 30px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); }\n");
            html.append(".stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-bottom: 25px; }\n");
            html.append(".stat-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 15px; }\n");
            html.append(".stat-card .label { font-size: 14px; opacity: 0.9; margin-bottom: 10px; text-transform: uppercase; }\n");
            html.append(".stat-card .value { font-size: 48px; font-weight: 700; }\n");
            html.append(".stat-card.success { background: linear-gradient(135deg, #48bb78 0%, #38a169 100%); }\n");
            html.append(".stat-card.failed { background: linear-gradient(135deg, #f56565 0%, #e53e3e 100%); }\n");
            html.append(".results { background: white; border-radius: 20px; padding: 40px; margin-bottom: 30px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); }\n");
            html.append("table { width: 100%; border-collapse: collapse; }\n");
            html.append("thead { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }\n");
            html.append("th { padding: 15px; text-align: left; font-weight: 600; }\n");
            html.append("td { padding: 15px; border-bottom: 1px solid #e2e8f0; }\n");
            html.append(".status-success { background: #c6f6d5; color: #22543d; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; }\n");
            html.append(".status-failed { background: #fed7d7; color: #742a2a; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; }\n");
            html.append(".screenshot-link { color: #667eea; text-decoration: none; font-weight: 600; }\n");
            html.append(".footer { text-align: center; color: white; padding: 20px; }\n");
            html.append("</style>\n</head>\n<body>\n");
            
            html.append("<div class='container'>\n");
            html.append("<div class='header'>\n");
            html.append("<h1>🎯 DAMS CBT Automation Report</h1>\n");
            html.append("<p style='color: #718096; margin-top: 10px;'>")
                .append(new SimpleDateFormat("dd MMM yyyy, HH:mm:ss").format(new Date()))
                .append("</p>\n");
            html.append("</div>\n");
            
            html.append("<div class='summary'>\n");
            html.append("<h2 style='color: #2d3748; margin-bottom: 25px;'>📊 Summary</h2>\n");
            html.append("<div class='stats-grid'>\n");
            html.append("<div class='stat-card'><div class='label'>Total</div><div class='value'>")
                .append(courseResults.size()).append("</div></div>\n");
            html.append("<div class='stat-card success'><div class='label'>Successful</div><div class='value'>")
                .append(totalSuccessful).append("</div></div>\n");
            html.append("<div class='stat-card failed'><div class='label'>Failed</div><div class='value'>")
                .append(totalFailed).append("</div></div>\n");
            html.append("</div>\n</div>\n");
            
            html.append("<div class='results'>\n");
            html.append("<h2 style='color: #2d3748; margin-bottom: 25px;'>📋 Course Results</h2>\n");
            html.append("<table>\n<thead>\n<tr>\n");
            html.append("<th>#</th><th>Course Name</th><th>Status</th><th>Time</th><th>Screenshot</th>\n");
            html.append("</tr>\n</thead>\n<tbody>\n");
            
            for (int i = 0; i < courseResults.size(); i++) {
                CourseResult result = courseResults.get(i);
                html.append("<tr>\n");
                html.append("<td>").append(i + 1).append("</td>\n");
                html.append("<td>").append(escapeHtml(result.courseName)).append("</td>\n");
                
                String statusClass = result.status.equals("SUCCESS") ? "status-success" : "status-failed";
                html.append("<td><span class='").append(statusClass).append("'>")
                    .append(result.status).append("</span></td>\n");
                
                html.append("<td>").append(result.timestamp).append("</td>\n");
                
                if (result.screenshotPath != null) {
                    html.append("<td><a href='").append(result.screenshotPath)
                        .append("' class='screenshot-link'>View QR</a></td>\n");
                } else {
                    html.append("<td>-</td>\n");
                }
                html.append("</tr>\n");
            }
            
            html.append("</tbody>\n</table>\n</div>\n");
            
            html.append("<div class='footer'>\n");
            html.append("<p>🤖 DAMS Automation System</p>\n");
            html.append("</div>\n</div>\n</body>\n</html>");
            
            FileWriter writer = new FileWriter(filename);
            writer.write(html.toString());
            writer.close();
            
            System.out.println("✓ Report saved: " + filename);
            
        } catch (Exception e) {
            System.out.println("✗ Report failed: " + e.getMessage());
        }
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}].click();", signInBtn);
                System.out.println("  ✓ Clicked: Sign In link");
                sleep(3);
            } catch (Exception e2) {
                System.out.println("  ✗ Could not find sign in element");
            }
        }
        
        enterText(By.xpath("//input[@type='tel' or @type='number' or contains(@placeholder, 'number')]"), 
                  "+919456628016", "Phone");
        sleep(2);
        
        clickElement(By.className("common-bottom-btn"), "Request OTP");
        sleep(3);
        
        try {
            WebElement logoutBtn = driver.findElement(
                By.xpath("//button[contains(@class, 'btndata') and contains(text(), 'Logout')]"));
            js.executeScript("arguments[0].click();", logoutBtn);
            System.out.println("  ✓ Clicked Logout popup");
            sleep(3);
        } catch (Exception e) {
            System.out.println("  ℹ No logout popup");
        }
        
        enterText(By.xpath("//input[@type='text' or @type='number' or contains(@placeholder, 'OTP')]"), 
                  "2000", "OTP");
        sleep(2);
        
        clickElement(By.className("common-bottom-btn"), "Submit OTP");
        sleep(5);
        
        System.out.println("✓ Login successful\n");
    }

    private static void navigateToCBTSectionViaHamburger() {
        System.out.println("Navigating to CBT section via Hamburger menu...");
        
        try {
            try {
                WebElement dropdown = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//button[contains(@class, 'SelectCat')]")));
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", dropdown);
                sleep(1);
                js.executeScript("arguments[0].click();", dropdown);
                System.out.println("  ✓ Clicked: Course Dropdown");
                sleep(3);
            } catch (Exception e) {
                System.out.println("  ⚠ Skipping dropdown");
            }
            
            try {
                List<WebElement> options = driver.findElements(
                    By.xpath("//span[contains(text(), 'NEET PG')] | //div[contains(text(), 'NEET PG')]"));
                for (WebElement option : options) {
                    if (option.isDisplayed()) {
                        js.executeScript("arguments[0].click();", option);
                        System.out.println("  ✓ Selected: NEET PG");
                        sleep(3);
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("  ⚠ Skipping NEET PG selection");
            }
            
            try {
                WebElement closeBtn = driver.findElement(
                    By.xpath("//button[@type='button' and @aria-label='Close'] | //span[contains(@class, 'ant-modal-close')]"));
                js.executeScript("arguments[0].click();", closeBtn);
                System.out.println("  ✓ Closed modal");
                sleep(2);
            } catch (Exception e) {
                System.out.println("  ℹ No modal to close");
            }
            
            boolean hamburgerClicked = false;
            try {
                WebElement hamburger = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.className("humburgerIcon")));
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", hamburger);
                sleep(1);
                js.executeScript("arguments[0
