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
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DamsAutomationFull {

    // =======================================================
    //                  OKHTTP CLIENT SECTION
    // =======================================================
    private final OkHttpClient client = new OkHttpClient();

    public String loginAndGetToken(String userId, String deviceToken) throws IOException {

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
        String filename = "DAMS_Plans_Report_" + LocalDateTime.now() + ".html";
        FileWriter fw = new FileWriter(filename);

        fw.write("<html><head><title>DAMS Plan Report</title>");
        fw.write("<style>table{border-collapse:collapse;width:100%;} td,th{border:1px solid #000;padding:8px;}</style>");
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

        System.out.println("📄 HTML Report Created: " + filename);
    }

    // =======================================================
    //                     SELENIUM SECTION
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
            return false;
        }
    }

    public String takeScreenshot(String baseFilename) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = baseFilename + "_" + timestamp + ".png";

            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File dest = new File(filename);
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return dest.getAbsolutePath();

        } catch (Exception e) {
            return null;
        }
    }

    public void closeBrowser() {
        if (driver != null) driver.quit();
    }

    // =======================================================
    //                      MAIN COMBINED FLOW
    // =======================================================

    public static void main(String[] args) throws Exception {

        DamsAutomationFull full = new DamsAutomationFull();

        System.out.println("\n==============================");
        System.out.println("   STARTING FULL AUTOMATION");
        System.out.println("==============================\n");

        // 1️⃣ INIT BROWSER
        full.initBrowser();

        // 2️⃣ LOGIN SELENIUM
        boolean loginWeb = full.loginToDamsSite();

        if (!loginWeb) {
            System.out.println("❌ Cannot continue. Login Failed.");
            full.closeBrowser();
            return;
        }

        // 3️⃣ API CALL – Fetch Plans Report
        System.out.println("📡 Fetching plans using OkHttp API...");

        String hardToken = "PUT-YOUR-JWT-TOKEN-HERE";

        JSONArray plans = full.getPlans(hardToken);

        full.generateHTML(plans);

        // 4️⃣ SCREENSHOT
        String screen = full.takeScreenshot("DAMS_Final_Capture");

        System.out.println("📸 Screenshot Saved: " + screen);

        // 5️⃣ CLOSE BROWSER
        full.closeBrowser();

        System.out.println("\n🎉 FULL AUTOMATION COMPLETED SUCCESSFULLY!");
    }
}

