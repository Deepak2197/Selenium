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

public class DamsAutomationFull {

    // =======================================================
    //                OKHTTP CLIENT SECTION
    // =======================================================
    private final OkHttpClient client = new OkHttpClient();
    private static final String REPORT_DIR = "reports";

    // ----------------------------------------------------------------
    //     NEW FUNCTION → get_all_events API
    // ----------------------------------------------------------------
    public JSONArray getEvents(String jwtToken) throws IOException {

        JSONObject reqBody = new JSONObject();
        reqBody.put("page", 1);

        RequestBody body = RequestBody.create(
                reqBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url("https://api.damsdelhi.com/v2_data_model/get_all_events")
                .post(body)
                .addHeader("authorization", jwtToken)
                .addHeader("device_type", "3")
                .addHeader("api_version", "10")
                .addHeader("user_id", "752847")
                .addHeader("device_token", "25714535808")
                .addHeader("device_info", "{\"browser\":\"Chrome 142\",\"os\":\"Windows 10\",\"deviceType\":\"browser\"}")
                .build();

        Response response = client.newCall(request).execute();
        String resp = response.body().string();

        JSONObject json = new JSONObject(resp);
        return json.getJSONArray("data");
    }

    // ----------------------------------------------------------------
    //     NEW FUNCTION → HTML REPORT FOR EVENTS
    // ----------------------------------------------------------------
    public void generateEventsHTML(JSONArray events) throws IOException {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "DAMS_Events_Report_" + timestamp + ".html";

        Path filepath = Paths.get(REPORT_DIR, filename);
        Files.createDirectories(Paths.get(REPORT_DIR));

        FileWriter fw = new FileWriter(filepath.toFile());

        fw.write("<html><head><title>DAMS Events Report</title>");
        fw.write("<style>body{font-family: Arial, sans-serif; margin: 20px;} "
                + "table{border-collapse:collapse;width:90%;margin:auto;} "
                + "td,th{border:1px solid #ddd;padding:10px;text-align:left;} "
                + "th{background-color:#f2f2f2;} h2{text-align:center;}</style>");
        fw.write("</head><body>");

        fw.write("<h2>DAMS Events List</h2>");
        fw.write("<table><tr><th>Event Name</th><th>Date</th><th>Description</th></tr>");

        for (int i = 0; i < events.length(); i++) {
            JSONObject e = events.getJSONObject(i);
            fw.write("<tr>");
            fw.write("<td>" + e.optString("event_name") + "</td>");
            fw.write("<td>" + e.optString("event_date") + "</td>");
            fw.write("<td>" + e.optString("event_description") + "</td>");
            fw.write("</tr>");
        }

        fw.write("</table></body></html>");
        fw.close();

        System.out.println("📄 HTML Events Report Created → " + filepath.toAbsolutePath());
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
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

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
            takeScreenshot("DAMS_Login_Failed");
            return false;
        }
    }

    public String takeScreenshot(String baseFilename) {

        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = baseFilename + "_" + timestamp + ".png";

            Path filepath = Paths.get(REPORT_DIR, filename);
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
            full.takeScreenshot("DAMS_Login_Failure");
            full.closeBrowser();
            return;
        }

        full.takeScreenshot("DAMS_Login_Success");

        // 3️⃣ API CALL – Fetch Events
        System.out.println("📡 Fetching events using OkHttp API...");

        String hardToken = System.getenv("DAMS_JWT_TOKEN");

        if (hardToken == null || hardToken.isEmpty()) {
            System.out.println("❌ Error: DAMS_JWT_TOKEN environment variable missing.");
            full.closeBrowser();
            return;
        }

        try {
            JSONArray events = full.getEvents(hardToken);
            full.generateEventsHTML(events);

        } catch (Exception e) {
            System.out.println("❌ Failed to fetch or generate event report: " + e.getMessage());
        }

        full.closeBrowser();

        System.out.println("\n🎉 FULL AUTOMATION COMPLETED SUCCESSFULLY!");
    }
}
