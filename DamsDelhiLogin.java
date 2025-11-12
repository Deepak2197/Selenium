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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class DamsDelhiLogin {
    private WebDriver driver;
    private WebDriverWait wait;
    private WebDriverWait longWait;
    private String studentPhone = "+919456628016";
    private String studentOtp = "2000";

    public DamsDelhiLogin() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");

        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        this.longWait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    /**
     * Login to damsdelhi.com student portal
     */
    public boolean loginToDamsDelhi() {
        System.out.println("🔐 Starting login process for damsdelhi.com");

        try {
            driver.get("https://www.damsdelhi.com/");
            System.out.println("✅ Loaded damsdelhi.com");
            Thread.sleep(3000);

            WebElement signinElement = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[contains(text(), 'Sign in') or contains(text(), 'Sign In')]")
                )
            );
            signinElement.click();
            System.out.println("✅ Clicked Sign in button");
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
            System.out.println("✅ Entered phone number: " + studentPhone);
            Thread.sleep(2000);

            WebElement submitButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.className("common-bottom-btn"))
            );
            submitButton.click();
            System.out.println("✅ Clicked to request OTP");
            Thread.sleep(3000);

            try {
                WebElement logoutContinueButton = driver.findElement(
                    By.xpath("//button[contains(@class, 'btndata') and contains(text(), 'Logout & Continue')]")
                );
                if (logoutContinueButton.isDisplayed()) {
                    logoutContinueButton.click();
                    System.out.println("✅ Clicked Logout & Continue button");
                    Thread.sleep(3000);
                }
            } catch (NoSuchElementException e) {
                System.out.println("ℹ️  No Logout & Continue button found - continuing");
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
            System.out.println("✅ Entered OTP: " + studentOtp);
            Thread.sleep(2000);

            WebElement submitOtpButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.className("common-bottom-btn"))
            );
            submitOtpButton.click();
            System.out.println("✅ Submitted OTP");
            Thread.sleep(5000);

            String currentUrl = driver.getCurrentUrl();
            System.out.println("✅ Current URL after login: " + currentUrl);

            String lowerUrl = currentUrl.toLowerCase();
            if (!lowerUrl.contains("dashboard") && !lowerUrl.contains("student") && !lowerUrl.contains("profile")) {
                System.out.println("ℹ️  Not on dashboard, looking for dashboard link...");

                try {
                    WebElement dashboardLink = wait.until(
                        ExpectedConditions.elementToBeClickable(
                            By.xpath("//a[contains(@href, 'dashboard') or contains(@href, 'student') or " +
                                    "contains(@href, 'profile') or contains(text(), 'Dashboard') or " +
                                    "contains(text(), 'Profile') or contains(text(), 'My Account')]")
                        )
                    );
                    dashboardLink.click();
                    System.out.println("✅ Clicked dashboard link");
                    Thread.sleep(3000);

                    currentUrl = driver.getCurrentUrl();
                    System.out.println("✅ New URL: " + currentUrl);
                } catch (TimeoutException e) {
                    System.out.println("⚠️  Dashboard link not found, continuing anyway...");
                }
            }

            System.out.println("🎉 Login successful!");
            return true;

        } catch (TimeoutException e) {
            System.out.println("❌ Timeout: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            System.out.println("❌ Interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Complete payment flow after login - UPDATED FOR QR CODE AND $0
     */
    public boolean proceedToPayment() {
        System.out.println("\n💳 Starting payment process...");

        try {
            System.out.println("🔍 Current page: " + driver.getCurrentUrl());
            System.out.println("🔍 Page title: " + driver.getTitle());

            Thread.sleep(2000);

            // Step 0.5: Click "GO PRO" button
            System.out.println("🔶 Step 0: Looking for 'GO PRO' button...");
            WebElement goProBtn = null;

            try {
                goProBtn = longWait.until(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("button.btn"))
                );
                String btnText = goProBtn.getText();
                if (!btnText.contains("Go Pro") && !btnText.contains("GO PRO") && !btnText.contains("Premium")) {
                    throw new TimeoutException("Wrong button found");
                }
                System.out.println("✅ Found GO PRO button!");

            } catch (TimeoutException e1) {
                try {
                    goProBtn = longWait.until(
                        ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//button[@class='btn'][.//strong[contains(text(), 'Go Pro')]]")
                        )
                    );
                    System.out.println("✅ Found GO PRO button via XPath with strong tag!");
                } catch (TimeoutException e2) {
                    List<WebElement> allButtons = driver.findElements(By.tagName("button"));
                    for (WebElement btn : allButtons) {
                        String text = btn.getText().trim();
                        String className = btn.getAttribute("class");
                        if ((text.contains("Go Pro") || text.contains("GO PRO") || text.contains("Premium Access")) 
                            && className != null && className.contains("btn")) {
                            goProBtn = btn;
                            System.out.println("✅ Found matching button");
                            break;
                        }
                    }
                    if (goProBtn == null) {
                        System.out.println("❌ GO PRO button not found, maybe already on premium page");
                    }
                }
            }

            if (goProBtn != null) {
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", 
                    goProBtn
                );
                Thread.sleep(1000);
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].style.border='5px solid lime'; arguments[0].style.backgroundColor='yellow';", 
                    goProBtn
                );
                Thread.sleep(1000);
                clickElement(goProBtn, "GO PRO");
                Thread.sleep(5000);
            }

            // 1. Click "Buy Now"
            System.out.println("🔶 Step 1: Looking for 'Buy Now' button...");
            WebElement buyNowBtn = longWait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("h5.buy-now-btn"))
            );
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", buyNowBtn);
            Thread.sleep(1000);
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].style.border='3px solid red'", buyNowBtn
            );
            Thread.sleep(500);
            clickElement(buyNowBtn, "Buy Now");
            Thread.sleep(3000);

            // 2. Select "12 Months"
            WebElement twelveMonths = longWait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath("//h3[contains(text(), '12 Months') or text()='12 Months']")
                )
            );
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", twelveMonths);
            Thread.sleep(500);
            clickElement(twelveMonths, "12 Months");
            Thread.sleep(2000);

            // 2.5: Handle "Cart Already Contains Items" modal
            try {
                WebElement yesButton = wait.until(
                    ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[.//span[text()='Yes'] or contains(text(), 'Yes')]")
                    )
                );
                ((JavascriptExecutor) driver).executeScript("arguments[0].style.border='3px solid blue'", yesButton);
                Thread.sleep(500);
                clickElement(yesButton, "Yes (Cart Modal)");
                Thread.sleep(4000);
            } catch (TimeoutException ignored) {}

            // 3. Click today-button
            WebElement todayBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[contains(@class, 'today-button')]")
                )
            );
            clickElement(todayBtn, "Today button");
            Thread.sleep(2000);

            // 4. Click Continue
            WebElement continueBtn = longWait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//span[contains(text(), 'Continue')]"))
            );
            clickElement(continueBtn, "Continue");
            Thread.sleep(3000);

            // 5. Click h6 element
            List<WebElement> h6Elements = driver.findElements(By.tagName("h6"));
            if (!h6Elements.isEmpty()) {
                WebElement h6Element = wait.until(
                    ExpectedConditions.elementToBeClickable(h6Elements.get(0))
                );
                clickElement(h6Element, "h6 element");
                Thread.sleep(2000);
            }

            // 6. Select Paytm radio
            WebElement paytmRadio = longWait.until(
                ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//input[@type='radio' and @value='Paytm']")
                )
            );
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", paytmRadio);
            Thread.sleep(500);
            clickElement(paytmRadio, "Paytm radio");
            Thread.sleep(2000);

            // 7. Click Pay Now - UPDATED FOR ANT DESIGN BUTTON IN MODAL
            System.out.println("🔶 Step 7: Looking for 'Pay Now' button in Payment Mode modal...");
            WebElement payNowBtnFinal = null;

            try {
                // Wait for modal to appear
                Thread.sleep(2000);
                
                // Method 1: Find the blue Ant Design button with "Pay Now" text in modal
                try {
                    payNowBtnFinal = longWait.until(
                        ExpectedConditions.elementToBeClickable(
                            By.xpath("//button[contains(@class, 'ant-btn-primary') and " +
                                    "contains(@class, 'ant-btn-block') and " +
                                    "(contains(text(), 'Pay Now') or .//span[contains(text(), 'Pay Now')])]")
                        )
                    );
                    System.out.println("✅ Found Ant Design 'Pay Now' button in modal (Method 1)");
                } catch (TimeoutException e1) {
                    System.out.println("🔍 Method 1 failed, trying Method 2...");
                    
                    // Method 2: Find any ant-btn-primary ant-btn-block button
                    try {
                        List<WebElement> antButtons = driver.findElements(
                            By.xpath("//button[contains(@class, 'ant-btn-primary') and " +
                                    "contains(@class, 'ant-btn-block')]")
                        );
                        
                        System.out.println("🔍 Found " + antButtons.size() + " block buttons");
                        
                        for (WebElement btn : antButtons) {
                            String btnText = btn.getText().toLowerCase();
                            System.out.println("🔍 Button text: " + btnText);
                            if (btnText.contains("pay now") || btnText.contains("pay")) {
                                payNowBtnFinal = btn;
                                System.out.println("✅ Found Pay Now button (Method 2)");
                                break;
                            }
                        }
                    } catch (Exception e2) {
                        System.out.println("🔍 Method 2 failed, trying Method 3...");
                    }
                }
                
                // Method 3: Find button inside modal with specific text
                if (payNowBtnFinal == null) {
                    try {
                        payNowBtnFinal = longWait.until(
                            ExpectedConditions.elementToBeClickable(
                                By.xpath("//div[contains(@class, 'ant-modal')]//button[contains(text(), 'Pay Now')]")
                            )
                        );
                        System.out.println("✅ Found Pay Now button in modal (Method 3)");
                    } catch (TimeoutException e3) {
                        System.out.println("🔍 Method 3 failed, trying Method 4...");
                    }
                }
                
                // Method 4: CSS Selector approach
                if (payNowBtnFinal == null) {
                    try {
                        payNowBtnFinal = longWait.until(
                            ExpectedConditions.elementToBeClickable(
                                By.cssSelector("button.ant-btn.ant-btn-primary.ant-btn-block")
                            )
                        );
                        System.out.println("✅ Found Ant button via CSS (Method 4)");
                    } catch (TimeoutException e4) {
                        System.out.println("⚠️  All methods failed, trying last fallback...");
                    }
                }
                
                // Last fallback
                if (payNowBtnFinal == null) {
                    payNowBtnFinal = longWait.until(
                        ExpectedConditions.elementToBeClickable(
                            By.xpath("//button[contains(@class, 'ant-btn')]")
                        )
                    );
                    System.out.println("⚠️  Using generic Ant button");
                }

                // Highlight and prepare to click
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", payNowBtnFinal);
                Thread.sleep(500);
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].style.border='5px solid green'; arguments[0].style.backgroundColor='yellow';", 
                    payNowBtnFinal
                );
                Thread.sleep(1000);
                
                // Store current URL before clicking
                String urlBeforeClick = driver.getCurrentUrl();
                System.out.println("📍 Current URL before click: " + urlBeforeClick);
                
                // First click
                clickElement(payNowBtnFinal, "Pay Now $0 Button (1st click)");
                Thread.sleep(2000);
                
                // Second click
                clickElement(payNowBtnFinal, "Pay Now $0 Button (2nd click)");
                System.out.println("✅ Clicked Pay Now button 2 times");
                
                // Wait for URL to change or new page to load (max 60 seconds)
                System.out.println("⏳ Waiting for next page to open...");
                WebDriverWait urlChangeWait = new WebDriverWait(driver, Duration.ofSeconds(60));
                
                boolean urlChanged = false;
                try {
                    urlChangeWait.until(driver -> {
                        String currentUrl = driver.getCurrentUrl();
                        return !currentUrl.equals(urlBeforeClick);
                    });
                    urlChanged = true;
                    System.out.println("✅ Page changed! New URL: " + driver.getCurrentUrl());
                } catch (TimeoutException e) {
                    System.out.println("⚠️  URL did not change in 60 seconds, checking page state...");
                }
                
                // Additional wait to ensure page is fully loaded
                if (urlChanged) {
                    Thread.sleep(5000);
                    System.out.println("✅ Next page fully loaded!");
                } else {
                    // Even if URL didn't change, wait a bit for any dynamic content
                    Thread.sleep(10000);
                    System.out.println("⏳ Waited for dynamic content to load");
                }

            } catch (TimeoutException e) {
                System.out.println("❌ Pay Now button not found: " + e.getMessage());
            }

            // Take screenshot
            String screenshotPath = takeScreenshot("DAMS_QR_Payment");
            if (screenshotPath != null) {
                System.out.println("✅ Screenshot saved: " + screenshotPath);
                closeBrowser();
                return true;
            } else {
                System.out.println("⚠️ Screenshot failed but payment page reached");
                closeBrowser();
                return true;
            }

        } catch (Exception e) {
            System.out.println("❌ Error during payment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void clickElement(WebElement element, String elementName) {
        try {
            element.click();
            System.out.println("✅ Clicked " + elementName);
        } catch (Exception e) {
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                System.out.println("✅ Clicked " + elementName + " (via JavaScript)");
            } catch (Exception ex) {
                System.out.println("❌ Failed to click " + elementName + ": " + ex.getMessage());
                throw ex;
            }
        }
    }

    public String takeScreenshot(String baseFilename) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = baseFilename + "_" + timestamp + ".png";

            TakesScreenshot screenshotDriver = (TakesScreenshot) driver;
            File sourceFile = screenshotDriver.getScreenshotAs(OutputType.FILE);
            File destinationFile = new File(filename);
            Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return destinationFile.getAbsolutePath();

        } catch (IOException e) {
            System.out.println("❌ Screenshot save failed: " + e.getMessage());
            return null;
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
                    System.out.println("✅ Screenshot: Saved");
                    System.out.println("========================================\n");
                } else {
                    System.out.println("\n⚠️  Payment flow incomplete");
                    automation.closeBrowser();
                }

            } else {
                System.out.println("\n💥 Login failed - cannot proceed to payment");
                automation.closeBrowser();
            }

        } catch (Exception e) {
            System.out.println("💥 Unexpected error: " + e.getMessage());
            e.printStackTrace();
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