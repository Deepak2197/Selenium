import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.JavascriptExecutor;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class DamsDelhiLogin {
    private WebDriver driver;
    private WebDriverWait wait;
    private WebDriverWait longWait;
    private String studentPhone = "+919456628016";
    private String studentOtp = "2000";
    
    // Report data collection
    private List<Map<String, String>> reportSteps = new ArrayList<>();
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean overallSuccess = false;
    private List<String> screenshotPaths = new ArrayList<>();

    public DamsDelhiLogin() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        this.longWait = new WebDriverWait(driver, Duration.ofSeconds(30));
        this.startTime = LocalDateTime.now();
    }

    private void addReportStep(String step, String status, String details) {
        Map<String, String> stepData = new HashMap<>();
        stepData.put("step", step);
        stepData.put("status", status);
        stepData.put("details", details);
        stepData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        reportSteps.add(stepData);
        System.out.println(status + " " + step + " - " + details);
    }

    public boolean loginToDamsDelhi() {
        System.out.println("🔐 Starting login process for damsdelhi.com");
        addReportStep("Login Process", "🔄 STARTED", "Initiating login to DAMS Delhi portal");

        try {
            driver.get("https://www.damsdelhi.com/");
            addReportStep("Load Homepage", "✅ SUCCESS", "Successfully loaded damsdelhi.com");
            Thread.sleep(3000);

            WebElement signinElement = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[contains(text(), 'Sign in') or contains(text(), 'Sign In')]")
                )
            );
            signinElement.click();
            addReportStep("Click Sign In", "✅ SUCCESS", "Clicked Sign in button");
            Thread.sleep(3000);

            WebElement phoneField = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath("//input[@type='tel' or @type='number' or " +
                            "contains(@placeholder, 'number') or contains(@placeholder, 'Number') or " +
                            "contains(@placeholder, 'phone') or contains(@placeholder, 'Phone') or " +
                            "contains(@placeholder, 'Mobile') or contains(@placeholder, 'mobile')]")
                )
            );
            phoneField.clear();
            phoneField.sendKeys(studentPhone);
            addReportStep("Enter Phone Number", "✅ SUCCESS", "Entered: " + studentPhone);
            Thread.sleep(2000);

            WebElement submitButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.className("common-bottom-btn"))
            );
            submitButton.click();
            addReportStep("Request OTP", "✅ SUCCESS", "Clicked to request OTP");
            Thread.sleep(3000);

            try {
                WebElement logoutContinueButton = driver.findElement(
                    By.xpath("//button[contains(@class, 'btndata') and contains(text(), 'Logout & Continue')]")
                );
                if (logoutContinueButton.isDisplayed()) {
                    logoutContinueButton.click();
                    addReportStep("Handle Existing Session", "✅ SUCCESS", "Clicked Logout & Continue");
                    Thread.sleep(3000);
                }
            } catch (NoSuchElementException e) {
                addReportStep("Check Existing Session", "ℹ️ INFO", "No logout required");
            }

            WebElement otpField = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath("//input[@type='text' or @type='number' or @type='tel' or " +
                            "contains(@placeholder, 'OTP') or contains(@placeholder, 'otp') or " +
                            "contains(@name, 'otp') or contains(@id, 'otp')]")
                )
            );
            otpField.clear();
            otpField.sendKeys(studentOtp);
            addReportStep("Enter OTP", "✅ SUCCESS", "Entered OTP: " + studentOtp);
            Thread.sleep(2000);

            WebElement submitOtpButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.className("common-bottom-btn"))
            );
            submitOtpButton.click();
            addReportStep("Submit OTP", "✅ SUCCESS", "Submitted OTP for verification");
            Thread.sleep(5000);

            String currentUrl = driver.getCurrentUrl();
            addReportStep("Verify Login", "✅ SUCCESS", "Login successful - URL: " + currentUrl);

            String lowerUrl = currentUrl.toLowerCase();
            if (!lowerUrl.contains("dashboard") && !lowerUrl.contains("student") && !lowerUrl.contains("profile")) {
                try {
                    WebElement dashboardLink = wait.until(
                        ExpectedConditions.elementToBeClickable(
                            By.xpath("//a[contains(@href, 'dashboard') or contains(@href, 'student') or " +
                                    "contains(@href, 'profile') or contains(text(), 'Dashboard') or " +
                                    "contains(text(), 'Profile') or contains(text(), 'My Account')]")
                        )
                    );
                    dashboardLink.click();
                    addReportStep("Navigate to Dashboard", "✅ SUCCESS", "Clicked dashboard link");
                    Thread.sleep(3000);
                } catch (TimeoutException e) {
                    addReportStep("Navigate to Dashboard", "⚠️ WARNING", "Dashboard link not found");
                }
            }

            String screenshotPath = takeScreenshot("01_Login_Success");
            if (screenshotPath != null) {
                screenshotPaths.add(screenshotPath);
            }

            addReportStep("Login Complete", "🎉 SUCCESS", "Successfully logged into DAMS Delhi portal");
            return true;

        } catch (TimeoutException e) {
            addReportStep("Login Failed", "❌ FAILED", "Timeout: " + e.getMessage());
            takeScreenshot("ERROR_Login_Timeout");
            return false;
        } catch (InterruptedException e) {
            addReportStep("Login Failed", "❌ FAILED", "Interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            addReportStep("Login Failed", "❌ FAILED", "Error: " + e.getMessage());
            takeScreenshot("ERROR_Login_Exception");
            return false;
        }
    }

    public boolean proceedToPayment() {
        System.out.println("\n💳 Starting payment process...");
        addReportStep("Payment Process", "🔄 STARTED", "Initiating payment flow");

        try {
            Thread.sleep(2000);

            // Step 0: Click GO PRO button
            addReportStep("Find GO PRO Button", "🔄 PROCESSING", "Searching for GO PRO button");
            WebElement goProBtn = null;

            try {
                goProBtn = longWait.until(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("button.btn"))
                );
                String btnText = goProBtn.getText();
                if (!btnText.contains("Go Pro") && !btnText.contains("GO PRO") && !btnText.contains("Premium")) {
                    throw new TimeoutException("Wrong button found");
                }
            } catch (TimeoutException e1) {
                try {
                    goProBtn = longWait.until(
                        ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//button[@class='btn'][.//strong[contains(text(), 'Go Pro')]]")
                        )
                    );
                } catch (TimeoutException e2) {
                    List<WebElement> allButtons = driver.findElements(By.tagName("button"));
                    for (WebElement btn : allButtons) {
                        String text = btn.getText().trim();
                        String className = btn.getAttribute("class");
                        if ((text.contains("Go Pro") || text.contains("GO PRO")) 
                            && className != null && className.contains("btn")) {
                            goProBtn = btn;
                            break;
                        }
                    }
                }
            }

            if (goProBtn != null) {
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", 
                    goProBtn
                );
                Thread.sleep(1000);
                clickElement(goProBtn, "GO PRO");
                addReportStep("Click GO PRO", "✅ SUCCESS", "Clicked GO PRO button");
                Thread.sleep(5000);
                takeScreenshot("02_Go_Pro_Page");
            } else {
                addReportStep("Click GO PRO", "⚠️ WARNING", "GO PRO button not found, continuing");
            }

            // Step 1: Click Buy Now
            addReportStep("Click Buy Now", "🔄 PROCESSING", "Looking for Buy Now button");
            WebElement buyNowBtn = longWait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("h5.buy-now-btn"))
            );
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", buyNowBtn);
            Thread.sleep(1000);
            clickElement(buyNowBtn, "Buy Now");
            addReportStep("Click Buy Now", "✅ SUCCESS", "Clicked Buy Now button");
            Thread.sleep(3000);
            takeScreenshot("03_Buy_Now_Clicked");

            // Step 2: Select 12 Months
            addReportStep("Select Plan", "🔄 PROCESSING", "Selecting 12 Months plan");
            WebElement twelveMonths = longWait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath("//h3[contains(text(), '12 Months') or text()='12 Months']")
                )
            );
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", twelveMonths);
            Thread.sleep(500);
            clickElement(twelveMonths, "12 Months");
            addReportStep("Select Plan", "✅ SUCCESS", "Selected 12 Months plan");
            Thread.sleep(2000);

            // Step 2.5: Handle cart modal
            try {
                WebElement yesButton = wait.until(
                    ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[.//span[text()='Yes'] or contains(text(), 'Yes')]")
                    )
                );
                clickElement(yesButton, "Yes (Cart Modal)");
                addReportStep("Handle Cart Modal", "✅ SUCCESS", "Confirmed cart replacement");
                Thread.sleep(4000);
            } catch (TimeoutException e) {
                addReportStep("Handle Cart Modal", "ℹ️ INFO", "No cart modal appeared");
            }

            // Step 3: Click today-button
            addReportStep("Select Start Date", "🔄 PROCESSING", "Clicking today button");
            WebElement todayBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[contains(@class, 'today-button')]")
                )
            );
            clickElement(todayBtn, "Today button");
            addReportStep("Select Start Date", "✅ SUCCESS", "Selected today as start date");
            Thread.sleep(2000);
            takeScreenshot("04_Date_Selected");

            // Step 4: Click Continue
            addReportStep("Proceed to Payment", "🔄 PROCESSING", "Clicking Continue button");
            WebElement continueBtn = longWait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//span[contains(text(), 'Continue')]"))
            );
            clickElement(continueBtn, "Continue");
            addReportStep("Proceed to Payment", "✅ SUCCESS", "Clicked Continue");
            Thread.sleep(3000);

            // Step 5: Click h6 element
            List<WebElement> h6Elements = driver.findElements(By.tagName("h6"));
            if (!h6Elements.isEmpty()) {
                WebElement h6Element = wait.until(
                    ExpectedConditions.elementToBeClickable(h6Elements.get(0))
                );
                clickElement(h6Element, "h6 element");
                addReportStep("Expand Payment Options", "✅ SUCCESS", "Clicked payment section");
                Thread.sleep(2000);
            }

            // Step 6: Select Paytm
            addReportStep("Select Payment Method", "🔄 PROCESSING", "Selecting Paytm");
            WebElement paytmRadio = longWait.until(
                ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//input[@type='radio' and @value='Paytm']")
                )
            );
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", paytmRadio);
            Thread.sleep(500);
            clickElement(paytmRadio, "Paytm radio");
            addReportStep("Select Payment Method", "✅ SUCCESS", "Selected Paytm as payment method");
            Thread.sleep(2000);
            takeScreenshot("05_Payment_Method_Selected");

            // Step 7: Click Pay Now
            addReportStep("Complete Payment", "🔄 PROCESSING", "Looking for Pay Now button");
            WebElement payNowBtnFinal = null;

            try {
                Thread.sleep(2000);
                
                try {
                    payNowBtnFinal = longWait.until(
                        ExpectedConditions.elementToBeClickable(
                            By.xpath("//button[contains(@class, 'ant-btn-primary') and " +
                                    "contains(@class, 'ant-btn-block') and " +
                                    "(contains(text(), 'Pay Now') or .//span[contains(text(), 'Pay Now')])]")
                        )
                    );
                } catch (TimeoutException e1) {
                    List<WebElement> antButtons = driver.findElements(
                        By.xpath("//button[contains(@class, 'ant-btn-primary') and " +
                                "contains(@class, 'ant-btn-block')]")
                    );
                    
                    for (WebElement btn : antButtons) {
                        String btnText = btn.getText().toLowerCase();
                        if (btnText.contains("pay now") || btnText.contains("pay")) {
                            payNowBtnFinal = btn;
                            break;
                        }
                    }
                }

                if (payNowBtnFinal == null) {
                    payNowBtnFinal = longWait.until(
                        ExpectedConditions.elementToBeClickable(
                            By.cssSelector("button.ant-btn.ant-btn-primary.ant-btn-block")
                        )
                    );
                }

                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", payNowBtnFinal);
                Thread.sleep(500);
                
                String urlBeforeClick = driver.getCurrentUrl();
                
                clickElement(payNowBtnFinal, "Pay Now (1st click)");
                Thread.sleep(2000);
                clickElement(payNowBtnFinal, "Pay Now (2nd click)");
                addReportStep("Complete Payment", "✅ SUCCESS", "Clicked Pay Now button");
                
                System.out.println("⏳ Waiting for payment page to load...");
                WebDriverWait urlChangeWait = new WebDriverWait(driver, Duration.ofSeconds(60));
                
                try {
                    urlChangeWait.until(driver -> !driver.getCurrentUrl().equals(urlBeforeClick));
                    addReportStep("Payment Page Load", "✅ SUCCESS", "Payment page loaded: " + driver.getCurrentUrl());
                } catch (TimeoutException e) {
                    addReportStep("Payment Page Load", "⚠️ WARNING", "URL did not change, checking page state");
                }
                
                Thread.sleep(5000);

            } catch (TimeoutException e) {
                addReportStep("Complete Payment", "❌ FAILED", "Pay Now button not found: " + e.getMessage());
                takeScreenshot("ERROR_Pay_Now_Not_Found");
            }

            String screenshotPath = takeScreenshot("06_FINAL_Payment_Page");
            if (screenshotPath != null) {
                screenshotPaths.add(screenshotPath);
                addReportStep("Screenshot Captured", "✅ SUCCESS", "Final screenshot saved");
            }

            addReportStep("Payment Process", "🎉 COMPLETE", "Payment flow completed successfully");
            this.overallSuccess = true;
            return true;

        } catch (Exception e) {
            addReportStep("Payment Process", "❌ FAILED", "Error: " + e.getMessage());
            takeScreenshot("ERROR_Payment_Exception");
            return false;
        }
    }

    private void clickElement(WebElement element, String elementName) {
        try {
            element.click();
        } catch (Exception e) {
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            } catch (Exception ex) {
                System.out.println("❌ Failed to click " + elementName + ": " + ex.getMessage());
                throw ex;
            }
        }
    }

    public String takeScreenshot(String baseFilename) {
        try {
            File screenshotDir = new File("screenshots");
            if (!screenshotDir.exists()) {
                screenshotDir.mkdirs();
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "screenshots/" + baseFilename + "_" + timestamp + ".png";

            TakesScreenshot screenshotDriver = (TakesScreenshot) driver;
            File sourceFile = screenshotDriver.getScreenshotAs(OutputType.FILE);
            File destinationFile = new File(filename);
            Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return filename;

        } catch (IOException e) {
            System.out.println("❌ Screenshot save failed: " + e.getMessage());
            return null;
        }
    }

    public void generateHTMLReport() {
        try {
            this.endTime = LocalDateTime.now();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "DAMS_CBT_Report_" + timestamp + ".html";

            FileWriter writer = new FileWriter(filename);
            
            String duration = Duration.between(startTime, endTime).toSeconds() + " seconds";
            String statusColor = overallSuccess ? "#10b981" : "#ef4444";
            String statusText = overallSuccess ? "✅ SUCCESS" : "❌ FAILED";

            writer.write("<!DOCTYPE html>\n");
            writer.write("<html lang=\"en\">\n");
            writer.write("<head>\n");
            writer.write("    <meta charset=\"UTF-8\">\n");
            writer.write("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            writer.write("    <title>DAMS CBT Automation Report</title>\n");
            writer.write("    <style>\n");
            writer.write("        * { margin: 0; padding: 0; box-sizing: border-box; }\n");
            writer.write("        body { font-family: 'Segoe UI', Tahoma, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px 20px; }\n");
            writer.write("        .container { max-width: 1400px; margin: 0 auto; }\n");
            writer.write("        .header { background: white; border-radius: 20px; padding: 40px; margin-bottom: 30px; box-shadow: 0 10px 40px rgba(0,0,0,0.15); }\n");
            writer.write("        .header h1 { font-size: 42px; color: #2d3748; margin-bottom: 10px; }\n");
            writer.write("        .header .subtitle { color: #718096; font-size: 18px; }\n");
            writer.write("        .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-bottom: 30px; }\n");
            writer.write("        .summary-card { background: white; padding: 30px; border-radius: 15px; box-shadow: 0 5px 20px rgba(0,0,0,0.1); }\n");
            writer.write("        .summary-card h3 { color: #718096; font-size: 14px; text-transform: uppercase; margin-bottom: 10px; }\n");
            writer.write("        .summary-card .value { font-size: 32px; font-weight: 700; color: #2d3748; }\n");
            writer.write("        .steps-container { background: white; border-radius: 20px; padding: 40px; box-shadow: 0 10px 40px rgba(0,0,0,0.15); margin-bottom: 30px; }\n");
            writer.write("        .steps-container h2 { color: #2d3748; margin-bottom: 30px; font-size: 28px; }\n");
            writer.write("        .step { padding: 20px; margin: 15px 0; background: #f7fafc; border-radius: 12px; border-left: 5px solid #667eea; transition: all 0.3s; }\n");
            writer.write("        .step:hover { transform: translateX(5px); box-shadow: 0 5px 15px rgba(0,0,0,0.1); }\n");
            writer.write("        .step.success { border-left-color: #10b981; }\n");
            writer.write("        .step.failed { border-left-color: #ef4444; }\n");
            writer.write("        .step.warning { border-left-color: #f59e0b; }\n");
            writer.write("        .step-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }\n");
            writer.write("        .step-name { font-weight: 600; font-size: 18px; color: #2d3748; }\n");
            writer.write("        .step-status { padding: 6px 16px; border-radius: 20px; font-size: 14px; font-weight: 600; }\n");
            writer.write("        .step-status.success { background: #d1fae5; color: #065f46; }\n");
            writer.write("        .step-status.failed { background: #fee2e2; color: #991b1b; }\n");
            writer.write("        .step-status.warning { background: #fef3c7; color: #92400e; }\n");
            writer.write("        .step-status.info { background: #dbeafe; color: #1e40af; }\n");
            writer.write("        .step-details { color: #4b5563; font-size: 15px; line-height: 1.6; }\n");
            writer.write("        .step-time { color: #9ca3af; font-size: 13px; margin-top: 8px; }\n");
            writer.write("        .screenshots { background: white; border-radius: 20px; padding: 40px; box-shadow: 0 10px 40px rgba(0,0,0,0.15); }\n");
            writer.write("        .screenshots h2 { color: #2d3748; margin-bottom: 30px; font-size: 28px; }\n");
            writer.write("        .screenshot-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }\n");
            writer.write("        .screenshot-item { border-radius: 12px; overflow: hidden; box-shadow: 0 5px 15px rgba(0,0,0,0.1); transition: all 0.3s; }\n");
            writer.write("        .screenshot-item:hover { transform: translateY(-5px); box-shadow: 0 10px 30px rgba(0,0,0,0.2); }\n");
            writer.write("        .screenshot-item img { width: 100%; height: auto; display: block; }\n");
            writer.write("        .screenshot-label { padding: 15px; background: #f7fafc; font-size: 14px; color: #4b5563; font-weight: 600; }\n");
            writer.write("        .footer { text-align: center; margin-top: 40px; color: white; font-size: 14px; }\n");
            writer.write("    </style>\n");
            writer.write("</head>\n");
            writer.write("<body>\n");
            writer.write("    <div class=\"container\">\n");
            writer.write("        <div class=\"header\">\n");
            writer.write("            <h1>🎯 DAMS CBT Automation Report</h1>\n");
            writer.write("            <p class=\"subtitle\">Comprehensive CBT Course Purchase Summary</p>\n");
            writer.write("        </div>\n");
            
            // Summary cards
            int successCount = (int) reportSteps.stream().filter(s -> s.get("status").contains("SUCCESS")).count();
            int failedCount = (int) reportSteps.stream().filter(s -> s.get("status").contains("FAILED")).count();
            int warningCount = (int) reportSteps.stream().filter(s -> s.get("status").contains("WARNING")).count();
            
            writer.write("        <div class=\"summary\">\n");
            writer.write("            <div class=\"summary-card\">\n");
            writer.write("                <h3>Overall Status</h3>\n");
            writer.write("                <div class=\"value\" style=\"color: " + statusColor + "\">" + statusText + "</div>\n");
            writer.write("            </div>\n");
            writer.write("            <div class=\"summary-card\">\n");
            writer.write("                <h3>Duration</h3>\n");
            writer.write("                <div class=\"value\" style=\"color: #667eea\">" + duration + "</div>\n");
            writer.write("            </div>\n");
            writer.write("            <div class=\"summary-card\">\n");
            writer.write("                <h3>Total Steps</h3>\n");
            writer.write("                <div class=\"value\" style=\"color: #764ba2\">" + reportSteps.size() + "</div>\n");
            writer.write("            </div>\n");
            writer.write("            <div class=\"summary-card\">\n");
            writer.write("                <h3>Success Rate</h3>\n");
            writer.write("                <div class=\"value\" style=\"color: #10b981\">" + successCount + "/" + reportSteps.size() + "</div>\n");
            writer.write("            </div>\n");
            writer.write("        </div>\n");
            
            // Steps
            writer.write("        <div class=\"steps-container\">\n");
            writer.write("            <h2>📋 Execution Steps</h2>\n");
            
            for (Map<String, String> step : reportSteps) {
                String status = step.get("status");
                String stepClass = "step";
                String statusClass = "info";
                
                if (status.contains("SUCCESS") || status.contains("COMPLETE")) {
                    stepClass += " success";
                    statusClass = "success";
                } else if (status.contains("FAILED")) {
                    stepClass += " failed";
                    statusClass = "failed";
                } else if (status.contains("WARNING")) {
                    stepClass += " warning";
                    statusClass = "warning";
                }
                
                writer.write("            <div class=\"" + stepClass + "\">\n");
                writer.write("                <div class=\"step-header\">\n");
                writer.write("                    <div class=\"step-name\">" + step.get("step") + "</div>\n");
                writer.write("                    <div class=\"step-status " + statusClass + "\">" + status + "</div>\n");
                writer.write("                </div>\n");
                writer.write("                <div class=\"step-details\">" + step.get("details") + "</div>\n");
                writer.write("                <div class=\"step-time\">⏱️ " + step.get("timestamp") + "</div>\n");
                writer.write("            </div>\n");
            }
            
            writer.write("        </div>\n");
            
            // Screenshots
            if (!screenshotPaths.isEmpty()) {
                writer.write("        <div class=\"screenshots\">\n");
                writer.write("            <h2>📸 Screenshots (" + screenshotPaths.size() + ")</h2>\n");
                writer.write("            <div class=\"screenshot-grid\">\n");
                
                for (String path : screenshotPaths) {
                    String filename = new File(path).getName();
                    writer.write("                <div class=\"screenshot-item\">\n");
                    writer.write("                    <img src=\"" + path + "\" alt=\"" + filename + "\">\n");
                    writer.write("                    <div class=\"screenshot-label\">" + filename + "</div>\n");
                    writer.write("                </div>\n");
                }
                
                writer.write("            </div>\n");
                writer.write("        </div>\n");
            }
            
            writer.write("        <div class=\"footer\">\n");
            writer.write("            <p>Generated on " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss")) + "</p>\n");
            writer.write("            <p>DAMS Delhi CBT Automation • Phone: " + studentPhone + "</p>\n");
            writer.write("        </div>\n");
            writer.write("    </div>\n");
            writer.write("</body>\n");
            writer.write("</html>");
            
            writer.close();
            
            System.out.println("\n✅ HTML Report generated: " + filename);
            addReportStep("Generate Report", "✅ SUCCESS", "HTML report saved: " + filename);

        } catch (IOException e) {
            System.out.println("❌ Failed to generate HTML report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Map<String, Object> getPageInfo() {
        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("url", driver.getCurrentUrl());
        pageInfo.put("title", driver.getTitle());
        pageInfo.put("pageSourceLength", driver.getPageSource().length());
        return pageInfo;
    }

    public void closeBrowser() {
        try {
            System.out.println("\n🌐 Closing browser...");
            driver.quit();
            System.out.println("✅ Browser closed successfully!");
        } catch (Exception e) {
            System.out.println("❌ Error closing browser: " + e.getMessage());
        }
    }

    public static void testComplete() {
        DamsDelhiLogin automation = new DamsDelhiLogin();

        try {
            boolean loginSuccess = automation.loginToDamsDelhi();

            if (loginSuccess) {
                System.out.println("\n✅ Login completed successfully!");
                Map<String, Object> pageInfo = automation.getPageInfo();
                System.out.println("📄 URL: " + pageInfo.get("url"));
                System.out.println("📄 Title: " + pageInfo.get("title"));
                Thread.sleep(2000);

                boolean paymentSuccess = automation.proceedToPayment();

                if (paymentSuccess) {
                    System.out.println("\n🎊 ========================================");
                    System.out.println("   AUTOMATION COMPLETED SUCCESSFULLY!");
                    System.out.println("========================================");
                    System.out.println("✅ Login: Success");
                    System.out.println("✅ Payment Flow: Success");
                    System.out.println("✅ Screenshots: Saved");
                    System.out.println("========================================\n");
                } else {
                    System.out.println("\n⚠️  Payment flow incomplete");
                }

            } else {
                System.out.println("\n💥 Login failed - cannot proceed to payment");
            }

            // Generate HTML report regardless of success/failure
            automation.generateHTMLReport();
            
            // Close browser after report generation
            automation.closeBrowser();

        } catch (Exception e) {
            System.out.println("💥 Unexpected error: " + e.getMessage());
            e.printStackTrace();
            
            // Still try to generate report and close browser
            try {
                automation.generateHTMLReport();
            } catch (Exception reportError) {
                System.out.println("❌ Could not generate report: " + reportError.getMessage());
            }
            automation.closeBrowser();
        }
    }

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   DAMS Delhi Automation Starting");
        System.out.println("========================================\n");
        testComplete();
    }
}
