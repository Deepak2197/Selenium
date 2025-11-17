import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class DamsAPI {

    private final OkHttpClient client = new OkHttpClient();

    // ==============================
    // LOGIN API CALL
    // ==============================
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
        return json.getString("authorization");   // JWT Token
    }

    // ==============================
    // FETCH PLAN API
    // ==============================
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

    // ==============================
    // HTML REPORT GENERATOR
    // ==============================
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

    // ==============================
    // MAIN TEST
    // ==============================
    public static void main(String[] args) throws Exception {

        DamsAPI api = new DamsAPI();

        System.out.println("🔐 Logging in...");
        String token = "PUT-YOUR-TOKEN-HERE-FROM-CONSOLE";  // OR login API

        System.out.println("📡 Fetching Plans...");
        JSONArray plans = api.getPlans(token);

        System.out.println("📑 Generating HTML Report...");
        api.generateHTML(plans);

        System.out.println("🎉 Completed Successfully!");
    }
}
