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
import java.util.*;

public class DamsAutomationFull {

    // =======================================================
    //                OKHTTP CLIENT SECTION
    // =======================================================
    private final OkHttpClient client = new OkHttpClient();
    private static final String REPORT_DIR = "reports"; // Sabhi reports yahan save hongi

    public String loginAndGetToken(String userId, String deviceToken) throws IOException {
        // ... (Yeh function abhi bhi main mein use nahi ho raha hai, par ise chhod dete hain)
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("user_id", userId);
        bodyJson.put("device_token", deviceToken);

        RequestBody body = RequestBody.create(
                bodyJson.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://api.damsdelhi.com/v2_data_model/user_login_with_otp")
                .post(body)
                .addHeader("device_type", "3")
                .addHeader("api_version", "10")
                .build();

        Response response = client.newCall(request).execute();
        String resp = response.body().string();

        JSONObject json = new JSONObject(resp);
        return json.getString("authorization");
    }

    public JSONArray getPlans(String jwtToken) throws IOException {
        JSONObject reqBody = new JSONObject();
        reqBody.put("category_id", "1");

        RequestBody body = RequestBody.create(
                reqBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://api.damsdelhi.com/v2_data_model/get_all_plan_by_category_id")
                .post(body)
                .addHeader("authorization", jwtToken)
                .addHeader("device_type", "3")
                .addHeader("api_version", "10")
                .addHeader("user_id", "752847")
                .build();

        Response response = client.newCall(request).execute();
        String resp = response.body().string();

        JSONObject json = new JSONObject(resp);
        return json.getJSONArray("data");
    }

    public void generateHTML(JSONArray plans) throws IOException {
        // Timestamp format badla gaya hai taaki file name mein ":" na aaye
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "DAMS_CBT_Report_" + timestamp + ".html";
        Path filepath = Paths.get(REPORT_DIR, filename);

        // Directory banayein agar nahi hai
        Files.createDirectories(Paths.get(REPORT_DIR));

        FileWriter fw = new FileWriter(filepath.toFile());

        fw.write("<html><head><title>DAMS Plan Report</title>");
        fw.write("<style>body{font-family: Arial, sans-serif; margin: 20px;} table{border-collapse:collapse;width:80%;margin:auto;} td,th{border:1px solid #ddd;padding:12px;text-align:left;} th{background-color:#f2f2f2;} h2{text-align:center;}</style>");
        fw.write("</head><body>");

        fw.write("<h2>DAMS Plans Report</h2>");
        fw.write("<table><tr><th>Plan Name</th><th>Price</th><th>Validity</th></tr>");

        for (int i = 0; i < plans.length(); i++) {
            JSONObject p = plans.getJSONObject(i);
            fw.write("<tr>");
            fw.write("<td>" + p.getString("plan_name") + "</td>");
            fw.write("<td>" + p.getString("plan_price") + "</td>");
            fw.write("<td>" + p.getString("validity") + "</td>");
            fw.write("</tr>");
        }

        fw.write("</table></body></html>");
        fw.close();

        System.out.println("📄 HTML Report Created: " + filepath.toAbsolutePath());
    }

    // =======================================================
    //                   SELENIUM SECTION
    // =======================================================

    private WebDriver driver;
    private WebDriverWait wait;
    private WebDriverWait longWait;

    private String studentPhone = "+919456628016";
    private String studentOtp = "2000";

    public void initBrowser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        
        // CI/GitHub Actions ke liye zaroori options
        options.addArguments("--headless"); // Headless mode mein run karega
        options.addArguments("--no-sandbox"); // Sandbox disable karega
        options.addArguments("--disable-dev-shm-usage"); // Shared memory issues fix karega
        options.addArguments("--window-size=1920,1080"); // Window size set karega

        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        this.longWait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    public boolean loginToDamsSite() {
        try {
            System.out.println("🔐 Logging into damsdelhi.com ...");
            driver.get("https://www.damsdelhi.com/");
            Thread.sleep(3000);

            WebElement signinBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//a[contains(text(),'Sign in') or contains(text(),'Sign In')]")
                    )
            );

            signinBtn.click();
            Thread.sleep(2000);

            WebElement phoneField = wait.until(
                    ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='tel']"))
            );
            phoneField.sendKeys(studentPhone);

            WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(By.className("common-bottom-btn")));
            submitBtn.click();
            Thread.sleep(3000);

            WebElement otpField = wait.until(
                    ExpectedConditions.elementToBeClickable(By.xpath("//input"))
            );
            otpField.sendKeys(studentOtp);

            WebElement otpSubmit = wait.until(ExpectedConditions.elementToBeClickable(By.className("common-bottom-btn")));
            otpSubmit.click();
            Thread.sleep(5000);

            System.out.println("🎉 Login Successful!");
            return true;

        } catch (Exception e) {
            System.out.println("❌ Login failed: " + e.getMessage());
            e.printStackTrace(); // Poora error print karega
            takeScreenshot("DAMS_Login_Failed"); // Failure par bhi screenshot lega
            return false;
        }
    }

    public String takeScreenshot(String baseFilename) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = baseFilename + "_" + timestamp + ".png";
            Path filepath = Paths.get(REPORT_DIR, filename);

            // Directory banayein agar nahi hai
            Files.createDirectories(Paths.get(REPORT_DIR));

            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(source.toPath(), filepath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("📸 Screenshot Saved: " + filepath.toAbsolutePath());
            return filepath.toAbsolutePath().toString();

        } catch (Exception e) {
            System.out.println("❌ Failed to take screenshot: " + e.getMessage());
            return null;
        }
    }

    public void closeBrowser() {
        if (driver != null) driver.quit();
    }

    /**
     * API se mile plans ko console par ek formatted table mein print karta hai.
     * @param plans API se mila JSONArray
     */
    public void printPlansToConsole(JSONArray plans) {
        if (plans == null || plans.length() == 0) {
            System.out.println("No plans to display in console.");
            return;
        }

        // 1. Har column ki maximum width calculate karein
        int maxName = "Plan Name".length();
        int maxPrice = "Price".length();
        int maxValidity = "Validity".length();

        for (int i = 0; i < plans.length(); i++) {
            JSONObject p = plans.getJSONObject(i);
            String name = p.getString("plan_name");
            String price = p.getString("plan_price");
            String validity = p.getString("validity");

            if (name.length() > maxName) maxName = name.length();
            if (price.length() > maxPrice) maxPrice = price.length();
            if (validity.length() > maxValidity) maxValidity = validity.length();
        }

        // 2. Format string banayein (e.g., "%-20s | %-10s | %-15s%n")
        // Thoda extra padding (2)
        maxName += 2;
        maxPrice += 2;
        maxValidity += 2;
        String format = "%-" + maxName + "s | %-" + maxPrice + "s | %-" + maxValidity + "s%n";

        System.out.println("\n========= DAMS Plans Console Report =========\n");

        // 3. Header Print Karein
        System.out.printf(format, "Plan Name", "Price", "Validity");

        // 4. Separator Print Karein
        int totalWidth = maxName + maxPrice + maxValidity + 6; // 6 for " | " * 2
        System.out.println("-".repeat(totalWidth));

        // 5. Data Print Karein
        for (int i = 0; i < plans.length(); i++) {
            JSONObject p = plans.getJSONObject(i);
            System.out.printf(format,
                p.getString("plan_name"),
                p.getString("plan_price"),
                p.getString("validity")
            );
        }
        System.out.println("\n==============================================\n");
    }


    // =======================================================
    //                 MAIN COMBINED FLOW
    // =======================================================

    public static void main(String[] args) throws Exception {

        DamsAutomationFull full = new DamsAutomationFull();

        System.out.println("\n==============================");
        System.out.println("    STARTING FULL AUTOMATION");
        System.out.println("==============================\n");

        // 1️⃣ INIT BROWSER
        full.initBrowser();

        // 2️⃣ LOGIN SELENIUM
        boolean loginWeb = full.loginToDamsSite();

        if (!loginWeb) {
            System.out.println("❌ Cannot continue. Login Failed.");
            full.takeScreenshot("DAMS_Login_Failure"); // Screenshot lein failure par
            full.closeBrowser();
            return;
        }
        
        // Login successful hone par screenshot
        full.takeScreenshot("DAMS_Login_Success");

        // 3️⃣ API CALL – Fetch Plans Report
        System.out.println("📡 Fetching plans using OkHttp API...");

        // === YEH SABSE ZAROORI BADLAAV HAI ===
        // Token ko hardcode karne ke bajaaye Environment Variable se padhein
        String hardToken = System.getenv("DAMS_JWT_TOKEN");

        if (hardToken == null || hardToken.equals("PUT-YOUR-JWT-TOKEN-HERE") || hardToken.isEmpty()) {
            System.out.println("❌ Error: DAMS_JWT_TOKEN environment variable not set.");
            System.out.println("   Please set this as a GitHub Secret in your repository.");
            full.closeBrowser();
            return;
        } else {
             System.out.println("Token found successfully.");
        }

        try {
            JSONArray plans = full.getPlans(hardToken);
            
            // YAHAN NAYA METHOD CALL KIYA GAYA HAI
            full.printPlansToConsole(plans); // Console par table print karega
            
            full.generateHTML(plans); // Report generate karega ./reports/ folder mein
        } catch (Exception e) {
            System.out.println("❌ Failed to get plans or generate report: " + e.getMessage());
            e.printStackTrace();
        }

        // 4️⃣ FINAL SCREENSHOT (Optional, kyunki success par pehle hi le liya hai)
        // String screen = full.takeScreenshot("DAMS_Final_Capture");
        // System.out.println("📸 Final Screenshot Saved: " + screen);

        // 5️⃣ CLOSE BROWSER
        full.closeBrowser();

        System.out.println("\n🎉 FULL AUTOMATION COMPLETED SUCCESSFULLY!");
    }
}
