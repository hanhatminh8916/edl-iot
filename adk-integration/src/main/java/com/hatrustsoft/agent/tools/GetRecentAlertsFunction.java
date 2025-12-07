package com.hatrustsoft.agent.tools;

import com.google.adk.tools.FunctionImplementation;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;

/**
 * Tool: Lấy cảnh báo gần đây
 * API: GET /api/alerts/recent?limit=X
 */
public class GetRecentAlertsFunction implements FunctionImplementation {
    private final String backendUrl;
    private final OkHttpClient client;

    public GetRecentAlertsFunction(String backendUrl) {
        this.backendUrl = backendUrl;
        this.client = new OkHttpClient();
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        int limit = 10; // Default
        
        if (arguments.containsKey("limit")) {
            Object limitObj = arguments.get("limit");
            if (limitObj instanceof Number) {
                limit = ((Number) limitObj).intValue();
            }
        }

        try {
            Request request = new Request.Builder()
                .url(backendUrl + "/api/alerts/recent?limit=" + limit)
                .get()
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "❌ Lỗi khi lấy cảnh báo: " + response.code();
                }

                String responseBody = response.body().string();
                
                // Check if empty or no alerts
                if (responseBody.equals("[]") || responseBody.trim().isEmpty()) {
                    return "✅ Hiện tại không có cảnh báo nguy hiểm nào!";
                }

                return String.format(
                    "⚠️ Danh sách %d cảnh báo gần đây:\n%s",
                    limit,
                    responseBody
                );
            }
        } catch (IOException e) {
            return "❌ Lỗi kết nối API: " + e.getMessage();
        }
    }
}
