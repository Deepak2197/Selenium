import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.json.*;

public class DamsEnhancedAPIFetcher {
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
    
    // Enhanced API data storage
    private static JSONObject fullAPIResponse = null;
    private static JSONArray allCoursesData = new JSONArray();
    private static Map<String, JSONObject> courseDetailsMap = new HashMap<>();
    
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
            System.out.println("║  DAMS - ENHANCED API DATA FETCHER         ║");
            System.out.println("╚════════════════════════════════════════════╝\n");

            // Fetch comprehensive data from API
            fetchComprehensiveAPIData();
            
            // Generate enhanced HTML report with all API data
            generateEnhancedHTMLReport();

            System.out.println("\n╔════════════════════════════════════════════╗");
            System.out.println("║  API DATA EXTRACTION COMPLETED!            ║");
            System.out.println("║  Total Courses Found: " + allCoursesData.length() + "                   ║");
            System.out.println("╚════════════════════════════════════════════╝");

        } catch (Exception e) {
            System.out.println("CRITICAL ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void fetchComprehensiveAPIData() {
        System.out.println("Fetching comprehensive data from DAMS API...\n");
        
        // Fetch data for multiple categories
        String[] categoryIds = {"1", "2", "3", "4", "5"}; // Try multiple categories
        
        for (String categoryId : categoryIds) {
            System.out.println("→ Fetching Category ID: " + categoryId);
            fetchAPIDataForCategory(categoryId);
            System.out.println();
        }
        
        System.out.println("✓ API data collection complete!");
        System.out.println("  → Total unique courses collected: " + allCoursesData.length());
    }

    private static void fetchAPIDataForCategory(String categoryId) {
        try {
            URL url = new URL("https://api.damsdelhi.com/v2_data_model/get_all_plan_by_category_id");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("api_version", "1.0");
            conn.setRequestProperty("authorization", "");
            conn.setRequestProperty("device_info", "Web Browser");
            conn.setRequestProperty("device_token", "");
            conn.setRequestProperty("device_type", "web");
            conn.setRequestProperty("stream_id", "");
            conn.setRequestProperty("user_id", "");
            conn.setRequestProperty("Origin", "https://www.damsdelhi.com");
            conn.setRequestProperty("Referer", "https://www.damsdelhi.com/");
            conn.setDoOutput(true);
            
            // Send POST request
            String jsonInputString = "{\"category_id\": \"" + categoryId + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            int responseCode = conn.getResponseCode();
            System.out.println("  → Response Code: " + responseCode);
            
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                // Parse JSON response
                JSONObject jsonResponse = new JSONObject(response.toString());
                
                if (fullAPIResponse == null) {
                    fullAPIResponse = jsonResponse;
                }
                
                // Extract courses from various possible structures
                extractCoursesFromResponse(jsonResponse, categoryId);
                
            } else {
                System.out.println("  ✗ API request failed with code: " + responseCode);
            }
            
        } catch (Exception e) {
            System.out.println("  ✗ Error fetching category " + categoryId + ": " + e.getMessage());
        }
    }

    private static void extractCoursesFromResponse(JSONObject response, String categoryId) {
        try {
            // Try multiple possible JSON structures
            JSONArray courses = null;
            
            if (response.has("data")) {
                Object dataObj = response.get("data");
                if (dataObj instanceof JSONArray) {
                    courses = (JSONArray) dataObj;
                } else if (dataObj instanceof JSONObject) {
                    JSONObject dataObject = (JSONObject) dataObj;
                    if (dataObject.has("plans")) {
                        courses = dataObject.getJSONArray("plans");
                    } else if (dataObject.has("courses")) {
                        courses = dataObject.getJSONArray("courses");
                    } else if (dataObject.has("items")) {
                        courses = dataObject.getJSONArray("items");
                    }
                }
            } else if (response.has("plans")) {
                courses = response.getJSONArray("plans");
            } else if (response.has("courses")) {
                courses = response.getJSONArray("courses");
            } else if (response.has("result")) {
                Object resultObj = response.get("result");
                if (resultObj instanceof JSONArray) {
                    courses = (JSONArray) resultObj;
                }
            }
            
            if (courses != null && courses.length() > 0) {
                System.out.println("  ✓ Found " + courses.length() + " courses in category " + categoryId);
                
                for (int i = 0; i < courses.length(); i++) {
                    JSONObject course = courses.getJSONObject(i);
                    
                    // Add category info
                    course.put("source_category_id", categoryId);
                    
                    // Store in map with unique identifier
                    String courseId = course.optString("id", 
                                    course.optString("plan_id", 
                                    course.optString("course_id", "unknown_" + i)));
                    
                    courseDetailsMap.put(courseId, course);
                    allCoursesData.put(course);
                }
            } else {
                System.out.println("  ℹ No courses found in category " + categoryId);
            }
            
        } catch (Exception e) {
            System.out.println("  ⚠ Error extracting courses: " + e.getMessage());
        }
    }

    private static void generateEnhancedHTMLReport() {
        System.out.println("\nGenerating enhanced HTML report with complete API data...");
        
        try {
            String timestamp = fileFormat.format(new Date());
            String filename = "DAMS_Complete_API_Report_" + timestamp + ".html";
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n<html>\n<head>\n");
            html.append("<meta charset='UTF-8'>\n");
            html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
            html.append("<title>DAMS Complete API Report - ").append(timestamp).append("</title>\n");
            html.append("<style>\n");
            addEnhancedStyles(html);
            html.append("</style>\n");
            html.append("<script>\n");
            addEnhancedJavaScript(html);
            html.append("</script>\n");
            html.append("</head>\n<body>\n");
            
            html.append("<div class='container'>\n");
            
            // Header Section
            addHeaderSection(html, timestamp);
            
            // Statistics Dashboard
            addStatisticsDashboard(html);
            
            // Search and Filter Section
            addSearchFilterSection(html);
            
            // Courses Display
            addCoursesSection(html);
            
            // Raw JSON Data Section (Expandable)
            addRawDataSection(html);
            
            // Footer
            addFooterSection(html, timestamp);
            
            html.append("</div>\n");
            html.append("</body>\n</html>");
            
            FileWriter writer = new FileWriter(filename);
            writer.write(html.toString());
            writer.close();
            
            System.out.println("✓ Enhanced HTML report saved: " + filename);
            System.out.println("  → Total courses displayed: " + allCoursesData.length());
            System.out.println("  → Open the file in your browser to view complete data!");
            
        } catch (Exception e) {
            System.out.println("✗ HTML generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addEnhancedStyles(StringBuilder html) {
        html.append("* { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("body { font-family: 'Segoe UI', system-ui, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; padding: 20px; }\n");
        html.append(".container { max-width: 1800px; margin: 0 auto; }\n");
        
        // Header Styles
        html.append(".header { background: white; border-radius: 20px; padding: 40px; margin-bottom: 25px; box-shadow: 0 10px 50px rgba(0,0,0,0.15); text-align: center; }\n");
        html.append(".header h1 { color: #2d3748; font-size: 48px; font-weight: 800; margin-bottom: 10px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }\n");
        html.append(".header .subtitle { color: #718096; font-size: 18px; margin: 10px 0; }\n");
        html.append(".header .timestamp { color: #a0aec0; font-size: 14px; font-weight: 600; }\n");
        
        // Stats Dashboard
        html.append(".stats-dashboard { background: white; border-radius: 20px; padding: 30px; margin-bottom: 25px; box-shadow: 0 10px 50px rgba(0,0,0,0.15); }\n");
        html.append(".stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }\n");
        html.append(".stat-box { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 25px; border-radius: 15px; color: white; text-align: center; box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4); }\n");
        html.append(".stat-box .label { font-size: 13px; opacity: 0.95; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 10px; }\n");
        html.append(".stat-box .value { font-size: 42px; font-weight: 800; }\n");
        html.append(".stat-box.active { background: linear-gradient(135deg, #48bb78 0%, #38a169 100%); }\n");
        html.append(".stat-box.inactive { background: linear-gradient(135deg, #f56565 0%, #e53e3e 100%); }\n");
        html.append(".stat-box.categories { background: linear-gradient(135deg, #ed8936 0%, #dd6b20 100%); }\n");
        
        // Search and Filter
        html.append(".search-filter { background: white; border-radius: 20px; padding: 30px; margin-bottom: 25px; box-shadow: 0 10px 50px rgba(0,0,0,0.15); }\n");
        html.append(".search-box { position: relative; margin-bottom: 20px; }\n");
        html.append(".search-box input { width: 100%; padding: 18px 50px 18px 20px; border: 2px solid #e2e8f0; border-radius: 12px; font-size: 16px; transition: all 0.3s; }\n");
        html.append(".search-box input:focus { outline: none; border-color: #667eea; box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1); }\n");
        html.append(".search-icon { position: absolute; right: 20px; top: 50%; transform: translateY(-50%); color: #a0aec0; font-size: 20px; }\n");
        html.append(".filter-buttons { display: flex; gap: 10px; flex-wrap: wrap; }\n");
        html.append(".filter-btn { padding: 10px 20px; border: 2px solid #e2e8f0; background: white; border-radius: 25px; cursor: pointer; transition: all 0.3s; font-size: 14px; font-weight: 600; color: #4a5568; }\n");
        html.append(".filter-btn:hover { border-color: #667eea; color: #667eea; transform: translateY(-2px); }\n");
        html.append(".filter-btn.active { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border-color: transparent; }\n");
        
        // Courses Grid
        html.append(".courses-section { margin-bottom: 25px; }\n");
        html.append(".courses-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(400px, 1fr)); gap: 25px; }\n");
        html.append(".course-card { background: white; border-radius: 18px; padding: 30px; box-shadow: 0 5px 25px rgba(0,0,0,0.12); transition: all 0.4s; cursor: pointer; border: 3px solid transparent; position: relative; overflow: hidden; }\n");
        html.append(".course-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 5px; background: linear-gradient(90deg, #667eea 0%, #764ba2 100%); }\n");
        html.append(".course-card:hover { transform: translateY(-10px); box-shadow: 0 20px 50px rgba(0,0,0,0.2); border-color: #667eea; }\n");
        
        // Course Card Elements
        html.append(".course-header { display: flex; justify-content: space-between; align-items: start; margin-bottom: 20px; }\n");
        html.append(".course-id { background: #edf2f7; color: #4a5568; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 700; }\n");
        html.append(".status-badge { padding: 6px 14px; border-radius: 20px; font-size: 11px; font-weight: 700; text-transform: uppercase; }\n");
        html.append(".status-badge.active { background: #c6f6d5; color: #22543d; }\n");
        html.append(".status-badge.inactive { background: #fed7d7; color: #742a2a; }\n");
        html.append(".course-title { font-size: 22px; font-weight: 700; color: #2d3748; margin-bottom: 15px; line-height: 1.4; min-height: 65px; }\n");
        html.append(".course-badges { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 15px; }\n");
        html.append(".badge { padding: 5px 12px; border-radius: 15px; font-size: 11px; font-weight: 600; background: #edf2f7; color: #4a5568; }\n");
        html.append(".price-section { margin: 20px 0; display: flex; align-items: center; gap: 15px; }\n");
        html.append(".current-price { font-size: 32px; font-weight: 800; color: #48bb78; }\n");
        html.append(".original-price { font-size: 18px; color: #a0aec0; text-decoration: line-through; }\n");
        html.append(".discount-badge { background: #f56565; color: white; padding: 5px 12px; border-radius: 15px; font-size: 12px; font-weight: 700; }\n");
        html.append(".course-description { color: #718096; font-size: 14px; line-height: 1.7; margin-bottom: 20px; max-height: 100px; overflow: hidden; }\n");
        html.append(".course-details { display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px; padding: 20px; background: #f7fafc; border-radius: 12px; margin: 20px 0; }\n");
        html.append(".detail-item { font-size: 13px; }\n");
        html.append(".detail-item .label { color: #718096; font-weight: 600; display: block; margin-bottom: 5px; }\n");
        html.append(".detail-item .value { color: #2d3748; font-weight: 700; }\n");
        html.append(".view-json-btn { width: 100%; padding: 14px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none; border-radius: 10px; font-weight: 700; cursor: pointer; transition: all 0.3s; }\n");
        html.append(".view-json-btn:hover { transform: scale(1.02); box-shadow: 0 8px 25px rgba(102, 126, 234, 0.5); }\n");
        
        // Raw Data Section
        html.append(".raw-data-section { background: white; border-radius: 20px; padding: 30px; margin-bottom: 25px; box-shadow: 0 10px 50px rgba(0,0,0,0.15); }\n");
        html.append(".raw-data-section h2 { color: #2d3748; font-size: 28px; margin-bottom: 20px; }\n");
        html.append(".json-container { background: #1a202c; color: #48bb78; padding: 25px; border-radius: 12px; font-family: 'Courier New', monospace; font-size: 13px; overflow-x: auto; max-height: 600px; overflow-y: auto; }\n");
        html.append(".copy-btn { padding: 10px 20px; background: #4299e1; color: white; border: none; border-radius: 8px; cursor: pointer; margin-bottom: 15px; font-weight: 600; }\n");
        html.append(".copy-btn:hover { background: #3182ce; }\n");
        
        // Footer
        html.append(".footer { text-align: center; color: white; padding: 30px; margin-top: 30px; }\n");
        html.append(".footer p { margin: 8px 0; font-size: 14px; opacity: 0.95; }\n");
        
        // Responsive
        html.append("@media (max-width: 768px) { .courses-grid { grid-template-columns: 1fr; } .course-details { grid-template-columns: 1fr; } }\n");
    }

    private static void addEnhancedJavaScript(StringBuilder html) {
        html.append("let allCourses = [];\n");
        html.append("let filteredCourses = [];\n\n");
        
        html.append("function searchCourses() {\n");
        html.append("  const input = document.getElementById('searchInput').value.toLowerCase();\n");
        html.append("  const cards = document.querySelectorAll('.course-card');\n");
        html.append("  let count = 0;\n");
        html.append("  cards.forEach(card => {\n");
        html.append("    const text = card.textContent.toLowerCase();\n");
        html.append("    if (text.includes(input)) {\n");
        html.append("      card.style.display = 'block';\n");
        html.append("      count++;\n");
        html.append("    } else {\n");
        html.append("      card.style.display = 'none';\n");
        html.append("    }\n");
        html.append("  });\n");
        html.append("  document.getElementById('visibleCount').textContent = count;\n");
        html.append("}\n\n");
        
        html.append("function filterByStatus(status) {\n");
        html.append("  const cards = document.querySelectorAll('.course-card');\n");
        html.append("  const buttons = document.querySelectorAll('.filter-btn');\n");
        html.append("  buttons.forEach(btn => btn.classList.remove('active'));\n");
        html.append("  event.target.classList.add('active');\n");
        html.append("  let count = 0;\n");
        html.append("  cards.forEach(card => {\n");
        html.append("    if (status === 'all' || card.dataset.status === status) {\n");
        html.append("      card.style.display = 'block';\n");
        html.append("      count++;\n");
        html.append("    } else {\n");
        html.append("      card.style.display = 'none';\n");
        html.append("    }\n");
        html.append("  });\n");
        html.append("  document.getElementById('visibleCount').textContent = count;\n");
        html.append("}\n\n");
        
        html.append("function copyJSON() {\n");
        html.append("  const jsonText = document.getElementById('rawJSON').textContent;\n");
        html.append("  navigator.clipboard.writeText(jsonText).then(() => {\n");
        html.append("    alert('JSON data copied to clipboard!');\n");
        html.append("  });\n");
        html.append("}\n\n");
        
        html.append("function viewCourseJSON(index) {\n");
        html.append("  const course = allCourses[index];\n");
        html.append("  alert('Course JSON:\\n\\n' + JSON.stringify(course, null, 2));\n");
        html.append("}\n");
    }

    private static void addHeaderSection(StringBuilder html, String timestamp) {
        html.append("<div class='header'>\n");
        html.append("<h1>🎓 DAMS Complete API Data Report</h1>\n");
        html.append("<p class='subtitle'>Comprehensive Course Catalog with Full API Details</p>\n");
        html.append("<p class='timestamp'>Generated: ").append(new SimpleDateFormat("dd MMM yyyy, HH:mm:ss").format(new Date())).append("</p>\n");
        html.append("</div>\n");
    }

    private static void addStatisticsDashboard(StringBuilder html) {
        int activeCourses = 0;
        int inactiveCourses = 0;
        Set<String> categories = new HashSet<>();
        
        for (int i = 0; i < allCoursesData.length(); i++) {
            try {
                JSONObject course = allCoursesData.getJSONObject(i);
                String status = course.optString("status", course.optString("is_active", "1"));
                if ("1".equals(status) || "active".equalsIgnoreCase(status)) {
                    activeCourses++;
                } else {
                    inactiveCourses++;
                }
                
                String category = course.optString("category", course.optString("category_name", ""));
                if (!category.isEmpty()) {
                    categories.add(category);
                }
            } catch (Exception e) {}
        }
        
        html.append("<div class='stats-dashboard'>\n");
        html.append("<div class='stats-grid'>\n");
        
        html.append("<div class='stat-box'>\n");
        html.append("<div class='label'>📚 Total Courses</div>\n");
        html.append("<div class='value' id='visibleCount'>").append(allCoursesData.length()).append("</div>\n");
        html.append("</div>\n");
        
        html.append("<div class='stat-box active'>\n");
        html.append("<div class='label'>✅ Active Courses</div>\n");
        html.append("<div class='value'>").append(activeCourses).append("</div>\n");
        html.append("</div>\n");
        
        html.append("<div class='stat-box inactive'>\n");
        html.append("<div class='label'>❌ Inactive Courses</div>\n");
        html.append("<div class='value'>").append(inactiveCourses).append("</div>\n");
        html.append("</div>\n");
        
        html.append("<div class='stat-box categories'>\n");
        html.append("<div class='label'>📂 Categories</div>\n");
        html.append("<div class='value'>").append(categories.size()).append("</div>\n");
        html.append("</div>\n");
        
        html.append("</div>\n");
        html.append("</div>\n");
    }

    private static void addSearchFilterSection(StringBuilder html) {
        html.append("<div class='search-filter'>\n");
        html.append("<div class='search-box'>\n");
        html.append("<input type='text' id='searchInput' placeholder='🔍 Search by name, ID, category, price, status...' onkeyup='searchCourses()'>\n");
        html.append("<span class='search-icon'>🔍</span>\n");
        html.append("</div>\n");
        
        html.append("<div class='filter-buttons'>\n");
        html.append("<button class='filter-btn active' onclick='filterByStatus(\"all\")'>All Courses</button>\n");
        html.append("<button class='filter-btn' onclick='filterByStatus(\"active\")'>✅ Active Only</button>\n");
        html.append("<button class='filter-btn' onclick='filterByStatus(\"inactive\")'>❌ Inactive Only</button>\n");
        html.append("</div>\n");
        html.append("</div>\n");
    }

    private static void addCoursesSection(StringBuilder html) {
        html.append("<div class='courses-section'>\n");
        html.append("<div class='courses-grid'>\n");
        
        html.append("<script>\nallCourses = [\n");
        
        for (int i = 0; i < allCoursesData.length(); i++) {
            try {
                JSONObject course = allCoursesData.getJSONObject(i);
                
                // Store in JavaScript array
                html.append(course.toString().replace("\\", "\\\\").replace("'", "\\'"));
                if (i < allCoursesData.length() - 1) {
                    html.append(",\n");
                }
                
                String courseId = course.optString("id", 
                                course.optString("plan_id", 
                                course.optString("course_id", "ID-" + (i+1))));
                
                String status = course.optString("status", course.optString("is_active", "1"));
                boolean isActive = "1".equals(status) || "active".equalsIgnoreCase(status);
                
                html.append("</script>\n");
                
                html.append("<div class='course-card' data-status='").append(isActive ? "active" : "inactive").append("'>\n");
                
                // Course Header with ID and Status
                html.append("<div class='course-header'>\n");
                html.append("<span class='course-id'>ID: ").append(escapeHtml(courseId)).append("</span>\n");
                html.append("<span class='status-badge ").append(isActive ? "active" : "inactive").append("'>");
                html.append(isActive ? "✅ ACTIVE" : "❌ INACTIVE").append("</span>\n");
                html.append("</div>\n");
                
                // Course Title
                String title = course.optString("title", 
                              course.optString("name", 
                              course.optString("plan_name", 
                              course.optString("course_name", "Course " + (i+1)))));
                html.append("<div class='course-title'>").append(escapeHtml(title)).append("</div>\n");
                
                // Badges Section
                html.append("<div class='course-badges'>\n");
                
                if (course.has("category") && !course.isNull("category")) {
                    html.append("<span class='badge'>📚 ").append(escapeHtml(course.getString("category"))).append("</span>\n");
                }
                if (course.has("category_name") && !course.isNull("category_name")) {
                    html.append("<span class='badge'>📚 ").append(escapeHtml(course.getString("category_name"))).append("</span>\n");
                }
                if (course.has("type") && !course.isNull("type")) {
                    html.append("<span class='badge'>🎯 ").append(escapeHtml(course.getString("type"))).append("</span>\n");
                }
                if (course.has("plan_type") && !course.isNull("plan_type")) {
                    html.append("<span class='badge'>🎯 ").append(escapeHtml(course.getString("plan_type"))).append("</span>\n");
                }
                if (course.has("mode") && !course.isNull("mode")) {
                    html.append("<span class='badge'>💻 ").append(escapeHtml(course.getString("mode"))).append("</span>\n");
                }
                
                html.append("</div>\n");
                
                // Price Section
                html.append("<div class='price-section'>\n");
                String currentPrice = course.optString("price", 
                                    course.optString("amount", 
                                    course.optString("final_price", 
                                    course.optString("selling_price", ""))));
                
                if (!currentPrice.isEmpty()) {
                    html.append("<span class='current-price'>₹").append(escapeHtml(currentPrice)).append("</span>");
                    
                    String originalPrice = course.optString("original_price", 
                                         course.optString("mrp", 
                                         course.optString("actual_price", "")));
                    
                    if (!originalPrice.isEmpty() && !originalPrice.equals(currentPrice)) {
                        html.append("<span class='original-price'>₹").append(escapeHtml(originalPrice)).append("</span>");
                        
                        try {
                            double current = Double.parseDouble(currentPrice);
                            double original = Double.parseDouble(originalPrice);
                            if (original > current) {
                                int discount = (int) (((original - current) / original) * 100);
                                html.append("<span class='discount-badge'>").append(discount).append("% OFF</span>");
                            }
                        } catch (Exception e) {}
                    }
                }
                html.append("</div>\n");
                
                // Description
                if (course.has("description") && !course.isNull("description")) {
                    String desc = course.getString("description");
                    if (!desc.isEmpty()) {
                        html.append("<div class='course-description'>").append(escapeHtml(desc)).append("</div>\n");
                    }
                }
                
                // Detailed Information Grid
                html.append("<div class='course-details'>\n");
                
                // Collect all available details
                addDetailIfExists(html, course, "validity", "⏳ Validity");
                addDetailIfExists(html, course, "valid_days", "⏳ Valid Days", " days");
                addDetailIfExists(html, course, "duration", "⏱️ Duration");
                addDetailIfExists(html, course, "tests_count", "📝 Tests");
                addDetailIfExists(html, course, "total_tests", "📝 Total Tests");
                addDetailIfExists(html, course, "subjects", "📖 Subjects");
                addDetailIfExists(html, course, "language", "🌐 Language");
                addDetailIfExists(html, course, "center", "📍 Center");
                addDetailIfExists(html, course, "created_at", "📅 Created");
                addDetailIfExists(html, course, "updated_at", "🔄 Updated");
                addDetailIfExists(html, course, "source_category_id", "🔖 Category ID");
                
                // Add any custom fields
                Iterator<String> keys = course.keys();
                int fieldCount = 0;
                while (keys.hasNext() && fieldCount < 20) {
                    String key = keys.next();
                    if (!isCommonField(key) && !course.isNull(key)) {
                        try {
                            String value = course.get(key).toString();
                            if (!value.isEmpty() && value.length() < 100) {
                                html.append("<div class='detail-item'>\n");
                                html.append("<span class='label'>").append(escapeHtml(formatFieldName(key))).append("</span>\n");
                                html.append("<span class='value'>").append(escapeHtml(value)).append("</span>\n");
                                html.append("</div>\n");
                                fieldCount++;
                            }
                        } catch (Exception e) {}
                    }
                }
                
                html.append("</div>\n");
                
                // View JSON Button
                html.append("<button class='view-json-btn' onclick='viewCourseJSON(").append(i).append(")'>📋 View Complete JSON</button>\n");
                
                html.append("</div>\n");
                
                html.append("<script>\n");
                
            } catch (Exception e) {
                System.out.println("  ⚠ Error processing course " + i + ": " + e.getMessage());
            }
        }
        
        html.append("];\n</script>\n");
        html.append("</div>\n");
        html.append("</div>\n");
    }

    private static void addDetailIfExists(StringBuilder html, JSONObject course, String key, String label) {
        addDetailIfExists(html, course, key, label, "");
    }

    private static void addDetailIfExists(StringBuilder html, JSONObject course, String key, String label, String suffix) {
        if (course.has(key) && !course.isNull(key)) {
            try {
                String value = course.getString(key);
                if (!value.isEmpty()) {
                    html.append("<div class='detail-item'>\n");
                    html.append("<span class='label'>").append(label).append("</span>\n");
                    html.append("<span class='value'>").append(escapeHtml(value)).append(suffix).append("</span>\n");
                    html.append("</div>\n");
                }
            } catch (Exception e) {}
        }
    }

    private static boolean isCommonField(String key) {
        String[] commonFields = {
            "id", "plan_id", "course_id", "title", "name", "plan_name", "course_name",
            "description", "price", "amount", "final_price", "selling_price",
            "original_price", "mrp", "actual_price", "category", "category_name",
            "type", "plan_type", "mode", "status", "is_active", "validity",
            "valid_days", "duration", "tests_count", "total_tests", "subjects",
            "language", "center", "created_at", "updated_at", "source_category_id"
        };
        
        for (String field : commonFields) {
            if (field.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static String formatFieldName(String key) {
        return key.replace("_", " ").toUpperCase();
    }

    private static void addRawDataSection(StringBuilder html) {
        html.append("<div class='raw-data-section'>\n");
        html.append("<h2>📊 Complete Raw API Response</h2>\n");
        html.append("<button class='copy-btn' onclick='copyJSON()'>📋 Copy All JSON Data</button>\n");
        html.append("<div class='json-container'>\n");
        html.append("<pre id='rawJSON'>");
        
        try {
            JSONObject completeData = new JSONObject();
            completeData.put("total_courses", allCoursesData.length());
            completeData.put("generated_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            completeData.put("courses", allCoursesData);
            
            if (fullAPIResponse != null) {
                completeData.put("original_api_response", fullAPIResponse);
            }
            
            html.append(escapeHtml(completeData.toString(2)));
        } catch (Exception e) {
            html.append("Error generating JSON: ").append(e.getMessage());
        }
        
        html.append("</pre>\n");
        html.append("</div>\n");
        html.append("</div>\n");
    }

    private static void addFooterSection(StringBuilder html, String timestamp) {
        html.append("<div class='footer'>\n");
        html.append("<p>🤖 DAMS Enhanced API Data Fetcher</p>\n");
        html.append("<p>📅 Generated: ").append(new SimpleDateFormat("dd MMM yyyy, HH:mm:ss").format(new Date())).append("</p>\n");
        html.append("<p>📊 Total Courses Extracted: ").append(allCoursesData.length()).append("</p>\n");
        html.append("<p>🌐 API Endpoint: https://api.damsdelhi.com/v2_data_model/get_all_plan_by_category_id</p>\n");
        html.append("<p>✨ All course data including IDs, status, pricing, validity, and custom fields</p>\n");
        html.append("</div>\n");
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
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

    private static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
