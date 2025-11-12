import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class DamsDelhiLogin {
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
            System.out.println("║  DAMS CBT AUTOMATION - ALL CBT COURSES    ║");
            System.out.println("╚════════════════════════════════════════════╝\n");

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
                js.executeScript("arguments[0].click();", signInBtn);
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
                System.out.println("  ⚠ Skipping dropdown: " + e.getMessage());
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
                js.executeScript("arguments[0].click();", hamburger);
                System.out.println("  ✓ Clicked: Hamburger Menu");
                hamburgerClicked = true;
                sleep(3);
            } catch (Exception e) {
                System.out.println("  ✗ Failed to click hamburger: " + e.getMessage());
            }
            
            if (!hamburgerClicked) {
                System.out.println("  ✗ Could not open hamburger menu!");
                return;
            }
            
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
                } catch (Exception e) {
                    System.out.println("    Trying next selector...");
                }
            }
            
            if (!cbtClicked) {
                System.out.println("  ✗ Could not click CBT button!");
                return;
            }
            
            try {
                WebElement okBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//button[@type='button' and contains(@class, 'btn-danger') and contains(text(), 'OK')]")));
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", okBtn);
                sleep(1);
                js.executeScript("arguments[0].click();", okBtn);
                System.out.println("  ✓ Clicked: OK Button (Red)");
                sleep(3);
            } catch (Exception e) {
                System.out.println("  ℹ No OK button to click");
            }
            
            System.out.println("✓ Successfully navigated to CBT section\n");
            
        } catch (Exception e) {
            System.out.println("✗ Error navigating to CBT section: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<String> discoverCBTCourses() {
        System.out.println("Discovering CBT courses...");
        List<String> courses = new ArrayList<>();
        
        try {
            System.out.println("  → Waiting for CBT page to load completely...");
            sleep(5);
            
            js.executeScript("window.scrollTo(0, 0);");
            sleep(2);
            
            long lastHeight = (Long) js.executeScript("return document.body.scrollHeight");
            int stableCount = 0;
            
            System.out.println("  → Scrolling to load all courses...");
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
                System.out.println("  ✗ No Buy Now buttons found!");
                return courses;
            }
            
            int coursesToProcess = Math.min(3, buyNowButtons.size());
            System.out.println("  → Processing EXACTLY " + coursesToProcess + " courses (LIMITED TO 3)");
            
            for (int i = 0; i < coursesToProcess; i++) {
                WebElement button = buyNowButtons.get(i);
                try {
                    js.executeScript("arguments[0].scrollIntoView({block: 'center'});", button);
                    sleep(1);
                    
                    WebElement container = button.findElement(By.xpath("./ancestor::div[contains(@class, 'col')]"));
                    
                    String courseName = "";
                    
                    try {
                        WebElement titleElem = container.findElement(
                            By.xpath(".//h3 | .//h4 | .//h5 | .//*[contains(@class, 'title') or contains(@class, 'heading')]"));
                        courseName = titleElem.getText().trim();
                        System.out.println("  → Method 1: Found title: " + courseName);
                    } catch (Exception e) {}
                    
                    if (courseName.isEmpty()) {
                        try {
                            WebElement linkElem = container.findElement(
                                By.xpath(".//a[string-length(normalize-space(text())) > 15]"));
                            courseName = linkElem.getText().trim();
                            System.out.println("  → Method 2: Found link text: " + courseName);
                        } catch (Exception e) {}
                    }
                    
                    if (courseName.isEmpty()) {
                        String allText = container.getText();
                        String[] lines = allText.split("\n");
                        for (String line : lines) {
                            line = line.trim();
                            if (isValidCBTCourseName(line)) {
                                courseName = line;
                                System.out.println("  → Method 3: Found from text: " + courseName);
                                break;
                            }
                        }
                    }
                    
                    if (!courseName.isEmpty() && isValidCBTCourseName(courseName)) {
                        courses.add(courseName);
                        System.out.println("  ✓ Found course: " + courseName);
                    } else {
                        courseName = "CBT Course " + (i + 1);
                        courses.add(courseName);
                        System.out.println("  → Using generic name: " + courseName);
                    }
                } catch (Exception e) {
                    System.out.println("  ⚠ Skipped course " + (i + 1) + ": " + e.getMessage());
                }
            }
            
            List<String> uniqueCourses = new ArrayList<>(new LinkedHashSet<>(courses));
            return uniqueCourses;
            
        } catch (Exception e) {
            System.out.println("✗ Error discovering courses: " + e.getMessage());
            e.printStackTrace();
            return courses;
        }
    }

    private static boolean isValidCBTCourseName(String text) {
        if (text == null || text.length() < 10) return false;
        
        String lower = text.toLowerCase();
        
        if (!lower.contains("all india") && !lower.contains("dams") && 
            !lower.contains("neet") && !lower.contains("mds") && 
            !lower.contains("fmge") && !lower.contains("combo") && 
            !lower.contains("cbt") && !lower.contains("test")) {
            return false;
        }
        
        String[] invalid = {
            "test instructions", "buy now", "registration", "exam date", 
            "noida", "delhi", "select", "choose", "click here", "view details",
            "registration last date", "download app", "app store", "google play",
            "view qr", "screenshot"
        };
        
        for (String term : invalid) {
            if (lower.equals(term) || lower.contains("₹")) {
                return false;
            }
        }
        
        return true;
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
                throw new Exception("Buy button not found for index " + courseIndex);
            }
            
            try {
                WebElement cbtModal = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[@class='popup' and .//div[@id='cbt_hide']]")));
                System.out.println("  ✓ CBT Modal detected");
                
                WebElement cbtRadioLabel = cbtModal.findElement(
                    By.xpath(".//label[contains(normalize-space(), 'CBT (Center Based Test)')]"));
                js.executeScript("arguments[0].click();", cbtRadioLabel);
                System.out.println("  ✓ Clicked 'CBT (Center Based Test)'");
                sleep(1);
                
                WebElement modalOkButton = cbtModal.findElement(
                    By.xpath(".//button[normalize-space()='OK']"));
                js.executeScript("arguments[0].click();", modalOkButton);
                System.out.println("  ✓ Clicked OK on CBT modal");
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
                    By.xpath("//input[@value='paytm']/parent::label"),
                    By.xpath("//*[contains(text(), 'Paytm')]")
                };
                
                for (By selector : paytmSelectors) {
                    try {
                        paytm = wait.until(ExpectedConditions.presenceOfElementLocated(selector));
                        if (paytm.isDisplayed()) {
                            break;
                        }
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
                    By.xpath("//button[contains(text(), 'Pay') or contains(text(), 'Proceed')]"),
                    By.xpath("//button[contains(@class, 'btn-primary') and contains(@class, 'btn-block')]")
                };
                
                for (By selector : paymentSelectors) {
                    try {
                        paymentBtn = wait.until(ExpectedConditions.presenceOfElementLocated(selector));
                        if (paymentBtn.isDisplayed()) {
                            break;
                        }
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
            
            System.out.println("  ⏳ Step 7: Waiting for QR code (max 60s)...");
            WebDriverWait qrWait = new WebDriverWait(driver, Duration.ofSeconds(60));
            
            try {
                By qrLocator = By.xpath("//canvas | //img[contains(@class, 'qr') or contains(@class, 'QR') or contains(@src, 'data:image')]");
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
            System.out.println("  ❌ Course processing failed: " + errorMsg);
            e.printStackTrace();
        }
    }

    private static void returnToCBTSection() {
        try {
            System.out.println("\n  → Returning to CBT section...");
            
            driver.get("https://www.damsdelhi.com/");
            sleep(3);
            
            boolean hamburgerClicked = false;
            try {
                WebElement hamburger = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.className("humburgerIcon")));
                js.executeScript("arguments[0].click();", hamburger);
                System.out.println("  ✓ Clicked Hamburger");
                hamburgerClicked = true;
                sleep(2);
            } catch (Exception e) {
                System.out.println("  ✗ Failed hamburger");
            }
            
            if (!hamburgerClicked) return;
            
            boolean cbtClicked = false;
            By[] cbtSelectors = {
                By.xpath("//div[contains(@class, 'Categories')]//div[contains(text(), 'CBT')]"),
                By.xpath("//div[contains(@class, 'Categories')]//*[contains(text(), 'CBT')]"),
                By.xpath("//*[contains(text(), 'CBT') and not(contains(text(), 'NEET'))]")
            };
            
            for (By selector : cbtSelectors) {
                try {
                    List<WebElement> cbtElements = driver.findElements(selector);
                    for (WebElement cbtElem : cbtElements) {
                        if (cbtElem.isDisplayed() && cbtElem.getText().trim().equals("CBT")) {
                            js.executeScript("arguments[0].click();", cbtElem);
                            System.out.println("  ✓ Clicked CBT");
                            cbtClicked = true;
                            sleep(2);
                            break;
                        }
                    }
                    if (cbtClicked) break;
                } catch (Exception e) {}
            }
            
            if (!cbtClicked) {
                System.out.println("  ✗ Failed to click CBT");
                return;
            }
            
            try {
                WebElement okBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//button[@type='button' and contains(@class, 'btn-danger') and contains(text(), 'OK')]")));
                js.executeScript("arguments[0].click();", okBtn);
                System.out.println("  ✓ Clicked OK Button");
                sleep(3);
            } catch (Exception e) {
                System.out.println("  ✗ Failed OK button");
            }
            
        } catch (Exception e) {
            System.out.println("  ⚠ Error returning to CBT: " + e.getMessage());
        }
    }

    private static void closePaymentWindow() {
        try {
            By[] closeSelectors = {
                By.xpath("//span[contains(@class, 'ptm-cross') and @id='app-close-btn']"),
                By.id("app-close-btn"),
                By.xpath("//span[contains(@class, 'ptm-cross')]")
            };
            
            for (By selector : closeSelectors) {
                try {
                    WebElement closeBtn = driver.findElement(selector);
                    js.executeScript("arguments[0].click();", closeBtn);
                    System.out.println("  ✓ Closed payment window");
                    sleep(8);
                    break;
                } catch (Exception e) {}
            }
            
            By[] skipSelectors = {
                By.xpath("//button[contains(@class, 'ptm-feedback-btn') and contains(text(), 'Skip')]"),
                By.xpath("//button[contains(text(), 'Skip')]")
            };
            
            for (By selector : skipSelectors) {
                try {
                    WebElement skipBtn = driver.findElement(selector);
                    js.executeScript("arguments[0].click();", skipBtn);
                    sleep(2);
                    break;
                } catch (Exception e) {}
            }
            
            By[] modalSelectors = {
                By.xpath("//span[contains(@class, 'ant-modal-close-x')]"),
                By.xpath("//button[contains(@class, 'ant-modal-close')]")
            };
            
            for (By selector : modalSelectors) {
                try {
                    WebElement modalBtn = driver.findElement(selector);
                    js.executeScript("arguments[0].click();", modalBtn);
                    sleep(2);
                    break;
                } catch (Exception e) {}
            }
            
        } catch (Exception e) {
            System.out.println("  ⚠ Issue closing payment");
        }
    }

    private static void clickElement(By locator, String name) {
        try {
            WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", elem);
            sleep(1);
            js.executeScript("arguments[0].click();", elem);
            System.out.println("  ✓ Clicked: " + name);
        } catch (Exception e) {
            System.out.println("  ✗ Failed to click: " + name);
        }
    }

    private static void enterText(By locator, String text, String fieldName) {
        try {
            WebElement elem = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            elem.clear();
            elem.sendKeys(text);
            System.out.println("  ✓ Entered: " + fieldName);
        } catch (Exception e) {
            System.out.println("  ✗ Failed to enter: " + fieldName);
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
        System.out.println("\nGenerating detailed HTML report...");
        
        try {
            String timestamp = fileFormat.format(new Date());
            String filename = "DAMS_CBT_Report_" + timestamp + ".html";
            
            double successRate = courseResults.isEmpty() ? 0 : 
                (totalSuccessful * 100.0 / courseResults.size());
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n<html>\n<head>\n");
            html.append("<meta charset='UTF-8'>\n");
            html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
            html.append("<title>DAMS CBT Automation Report - ").append(timestamp).append("</title>\n");
            html.append("<style>\n");
            html.append("* { margin: 0; padding: 0; box-sizing: border-box; }\n");
            html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; padding: 40px 20px; }\n");
            html.append(".container { max-width: 1400px; margin: 0 auto; }\n");
            html.append(".header { background: white; border-radius: 20px; padding: 40px; margin-bottom: 30px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); text-align: center; }\n");
            html.append(".header h1 { color: #2d3748; font-size: 42px; font-weight: 700; margin-bottom: 10px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }\n");
            html.append(".header .subtitle { color: #718096; font-size: 16px; margin-top: 5px; }\n");
            html.append(".header .timestamp { color: #a0aec0; font-size: 14px; margin-top: 10px; font-weight: 600; }\n");
            html.append(".summary { background: white; border-radius: 20px; padding: 40px; margin-bottom: 30px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); }\n");
            html.append(".summary h2 { color: #2d3748; font-size: 28px; font-weight: 600; margin-bottom: 25px; }\n");
            html.append(".stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-bottom: 25px; }\n");
            html.append(".stat-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 15px; box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4); }\n");
            html.append(".stat-card .label { font-size: 14px; opacity: 0.9; margin-bottom: 10px; text-transform: uppercase; letter-spacing: 1px; }\n");
            html.append(".stat-card .value { font-size: 48px; font-weight: 700; }\n");
            html.append(".stat-card.success { background: linear-gradient(135deg, #48bb78 0%, #38a169 100%); }\n");
            html.append(".stat-card.failed { background: linear-gradient(135deg, #f56565 0%, #e53e3e 100%); }\n");
            html.append(".stat-card.rate { background: linear-gradient(135deg, #ed8936 0%, #dd6b20 100%); }\n");
            html.append(".info-section { background: #f7fafc; border-left: 4px solid #667eea; padding: 20px; border-radius: 10px; margin-top: 20px; }\n");
            html.append(".info-section p { color: #4a5568; margin: 8px 0; line-height: 1.6; }\n");
            html.append(".info-section strong { color: #2d3748; }\n");
            html.append(".results { background: white; border-radius: 20px; padding: 40px; margin-bottom: 30px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); }\n");
            html.append(".results h2 { color: #2d3748; font-size: 28px; font-weight: 600; margin-bottom: 25px; }\n");
            html.append(".table-wrapper { overflow-x: auto; }\n");
            html.append("table { width: 100%; border-collapse: collapse; min-width: 800px; }\n");
            html.append("thead { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }\n");
            html.append("th { padding: 15px; text-align: left; font-weight: 600; text-transform: uppercase; font-size: 12px; letter-spacing: 1px; }\n");
            html.append("tbody tr { border-bottom: 1px solid #e2e8f0; transition: all 0.3s; }\n");
            html.append("tbody tr:hover { background: #f7fafc; transform: scale(1.01); }\n");
            html.append("td { padding: 15px; vertical-align: top; }\n");
            html.append(".course-name { font-weight: 600; color: #2d3748; max-width: 400px; }\n");
            html.append(".status-badge { display: inline-block; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; text-transform: uppercase; }\n");
            html.append(".status-success { background: #c6f6d5; color: #22543d; }\n");
            html.append(".status-failed { background: #fed7d7; color: #742a2a; }\n");
            html.append(".screenshot-link { color: #667eea; text-decoration: none; font-weight: 600; padding: 8px 16px; background: #edf2f7; border-radius: 8px; display: inline-block; transition: all 0.3s; }\n");
            html.append(".screenshot-link:hover { background: #667eea; color: white; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4); }\n");
            html.append(".error-msg { color: #e53e3e; font-size: 12px; font-style: italic; background: #fff5f5; padding: 8px 12px; border-radius: 6px; display: inline-block; max-width: 300px; }\n");
            html.append(".no-data { color: #a0aec0; font-style: italic; }\n");
            html.append(".timestamp-cell { color: #718096; font-size: 13px; white-space: nowrap; }\n");
            html.append(".index-cell { font-weight: 700; color: #667eea; font-size: 16px; }\n");
            html.append(".footer { text-align: center; color: white; margin-top: 40px; padding: 20px; }\n");
            html.append(".footer p { opacity: 0.9; margin: 5px 0; }\n");
            html.append(".print-data { background: white; border-radius: 20px; padding: 40px; margin-bottom: 30px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); }\n");
            html.append(".print-data h2 { color: #2d3748; font-size: 28px; font-weight: 600; margin-bottom: 25px; }\n");
            html.append(".print-item { background: #f7fafc; padding: 15px 20px; margin: 10px 0; border-radius: 10px; border-left: 4px solid #667eea; }\n");
            html.append(".print-item .key { color: #667eea; font-weight: 600; margin-right: 10px; }\n");
            html.append(".print-item .value { color: #2d3748; }\n");
            html.append("@media print { body { background: white; } .container { max-width: 100%; } }\n");
            html.append("@media (max-width: 768px) {\n");
            html.append("  .header h1 { font-size: 32px; }\n");
            html.append("  .summary, .results, .print-data { padding: 25px 20px; }\n");
            html.append("  .stat-card .value { font-size: 36px; }\n");
            html.append("  table { font-size: 14px; }\n");
            html.append("  th, td { padding: 10px; }\n");
            html.append("}\n");
            html.append("</style>\n</head>\n<body>\n");
            
            html.append("<div class='container'>\n");
            html.append("<div class='header'>\n");
            html.append("<h1>🎯 DAMS CBT Automation Report</h1>\n");
            html.append("<p class='subtitle'>Comprehensive CBT Course Purchase Summary</p>\n");
            html.append("<p class='timestamp'>Generated: ").append(new SimpleDateFormat("dd MMM yyyy, HH:mm:ss").format(new Date())).append("</p>\n");
            html.append("</div>\n");
            
            html.append("<div class='summary'>\n");
            html.append("<h2>📊 Execution Summary</h2>\n");
            html.append("<div class='stats-grid'>\n");
            
            html.append("<div class='stat-card'>\n");
            html.append("<div class='label'>Total Courses</div>\n");
            html.append("<div class='value'>").append(courseResults.size()).append("</div>\n");
            html.append("</div>\n");
            
            html.append("<div class='stat-card success'>\n");
            html.append("<div class='label'>Successful</div>\n");
            html.append("<div class='value'>").append(totalSuccessful).append("</div>\n");
            html.append("</div>\n");
            
            html.append("<div class='stat-card failed'>\n");
            html.append("<div class='label'>Failed</div>\n");
            html.append("<div class='value'>").append(totalFailed).append("</div>\n");
            html.append("</div>\n");
            
            html.append("<div class='stat-card rate'>\n");
            html.append("<div class='label'>Success Rate</div>\n");
            html.append("<div class='value'>").append(String.format("%.1f", successRate)).append("%</div>\n");
            html.append("</div>\n");
            
            html.append("</div>\n");
            
            html.append("<div class='info-section'>\n");
            html.append("<p><strong>📅 Execution Start Time:</strong> ").append(executionStartTime).append("</p>\n");
            html.append("<p><strong>🌐 Website:</strong> https://www.damsdelhi.com/</p>\n");
            html.append("<p><strong>📱 Phone Number:</strong> +919456628016</p>\n");
            html.append("<p><strong>🔐 OTP Used:</strong> 2000</p>\n");
            html.append("<p><strong>🎓 Course Type:</strong> NEET PG - CBT (Center Based Test)</p>\n");
            html.append("<p><strong>📍 Selected City:</strong> Delhi</p>\n");
            html.append("<p><strong>💳 Payment Method:</strong> Paytm</p>\n");
            html.append("</div>\n");
            html.append("</div>\n");
            
            html.append("<div class='results'>\n");
            html.append("<h2>📋 Detailed Course Results</h2>\n");
            html.append("<div class='table-wrapper'>\n");
            html.append("<table>\n");
            html.append("<thead>\n");
            html.append("<tr>\n");
            html.append("<th>#</th>\n");
            html.append("<th>Course Name</th>\n");
            html.append("<th>Status</th>\n");
            html.append("<th>Time</th>\n");
            html.append("<th>Screenshot</th>\n");
            html.append("<th>Error Details</th>\n");
            html.append("</tr>\n");
            html.append("</thead>\n");
            html.append("<tbody>\n");
            
            for (int i = 0; i < courseResults.size(); i++) {
                CourseResult result = courseResults.get(i);
                html.append("<tr>\n");
                html.append("<td class='index-cell'>").append(i + 1).append("</td>\n");
                html.append("<td class='course-name'>").append(escapeHtml(result.courseName)).append("</td>\n");
                
                String statusClass = result.status.equals("SUCCESS") ? "status-success" : "status-failed";
                String statusIcon = result.status.equals("SUCCESS") ? "✅" : "❌";
                html.append("<td><span class='status-badge ").append(statusClass).append("'>")
                    .append(statusIcon).append(" ").append(result.status).append("</span></td>\n");
                
                html.append("<td class='timestamp-cell'>").append(result.timestamp).append("</td>\n");
                
                if (result.screenshotPath != null && !result.screenshotPath.isEmpty()) {
                    html.append("<td><a href='").append(result.screenshotPath)
                        .append("' class='screenshot-link' target='_blank'>🖼️ View QR Code</a></td>\n");
                } else {
                    html.append("<td class='no-data'>No screenshot</td>\n");
                }
                
                if (result.errorMessage != null && !result.errorMessage.isEmpty()) {
                    html.append("<td><span class='error-msg'>⚠️ ").append(escapeHtml(result.errorMessage)).append("</span></td>\n");
                } else {
                    html.append("<td class='no-data'>-</td>\n");
                }
                html.append("</tr>\n");
            }
            
            html.append("</tbody>\n");
            html.append("</table>\n");
            html.append("</div>\n");
            html.append("</div>\n");
            
            html.append("<div class='print-data'>\n");
            html.append("<h2>📄 Complete Data Dump</h2>\n");
            
            html.append("<div class='print-item'>\n");
            html.append("<span class='key'>Total Courses Processed:</span>\n");
            html.append("<span class='value'>").append(courseResults.size()).append("</span>\n");
            html.append("</div>\n");
            
            html.append("<div class='print-item'>\n");
            html.append("<span class='key'>Successful Purchases:</span>\n");
            html.append("<span class='value'>").append(totalSuccessful).append("</span>\n");
            html.append("</div>\n");
            
            html.append("<div class='print-item'>\n");
            html.append("<span class='key'>Failed Purchases:</span>\n");
            html.append("<span class='value'>").append(totalFailed).append("</span>\n");
            html.append("</div>\n");
            
            html.append("<div class='print-item'>\n");
            html.append("<span class='key'>Success Rate:</span>\n");
            html.append("<span class='value'>").append(String.format("%.2f%%", successRate)).append("</span>\n");
            html.append("</div>\n");
            
            html.append("<div class='print-item'>\n");
            html.append("<span class='key'>Execution Start:</span>\n");
            html.append("<span class='value'>").append(executionStartTime).append("</span>\n");
            html.append("</div>\n");
            
            html.append("<div class='print-item'>\n");
            html.append("<span class='key'>Report Generated:</span>\n");
            html.append("<span class='value'>").append(timestamp).append("</span>\n");
            html.append("</div>\n");
            
            for (int i = 0; i < courseResults.size(); i++) {
                CourseResult result = courseResults.get(i);
                html.append("<div class='print-item'>\n");
                html.append("<span class='key'>Course ").append(i + 1).append(":</span>\n");
                html.append("<span class='value'>").append(escapeHtml(result.courseName)).append("</span><br>\n");
                html.append("<span class='key'>Status:</span> <span class='value'>").append(result.status).append("</span><br>\n");
                html.append("<span class='key'>Time:</span> <span class='value'>").append(result.timestamp).append("</span><br>\n");
                if (result.screenshotPath != null) {
                    html.append("<span class='key'>Screenshot:</span> <span class='value'>").append(result.screenshotPath).append("</span><br>\n");
                }
                if (result.errorMessage != null) {
                    html.append("<span class='key'>Error:</span> <span class='value'>").append(escapeHtml(result.errorMessage)).append("</span>\n");
                }
                html.append("</div>\n");
            }
            
            html.append("</div>\n");
            
            html.append("<div class='footer'>\n");
            html.append("<p>🤖 Generated by DAMS CBT Automation System</p>\n");
            html.append("<p>⚡ Powered by Selenium WebDriver</p>\n");
            html.append("<p>📧 All rights reserved © 2024</p>\n");
            html.append("</div>\n");
            
            html.append("</div>\n");
            html.append("</body>\n</html>");
            
            FileWriter writer = new FileWriter(filename);
            writer.write(html.toString());
            writer.close();
            
            System.out.println("✓ Detailed report saved: " + filename);
            System.out.println("  → Total Courses: " + courseResults.size());
            System.out.println("  → Successful: " + totalSuccessful);
            System.out.println("  → Failed: " + totalFailed);
            System.out.println("  → Success Rate: " + String.format("%.2f%%", successRate));
            
        } catch (Exception e) {
            System.out.println("✗ Report generation failed: " + e.getMessage());
            e.printStackTrace();
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
}
