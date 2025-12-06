package com.hatrustsoft.agent.tools;

import com.google.adk.tools.FunctionImplementation;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;

/**
 * Tool: Lấy danh sách công nhân
 * API: GET /api/workers
 */
public class GetWorkersFunction implements FunctionImplementation {
    private final String backendUrl;
    private final OkHttpClient client;
    private final Gson gson;

    public GetWorkersFunction(String backendUrl) {
        this.backendUrl = backendUrl;
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        try {
            Request request = new Request.Builder()
                .url(backendUrl + "/api/workers")
                .get()
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "❌ Lỗi khi lấy dữ liệu công nhân: " + response.code();
                }

                String responseBody = response.body().string();
                
                // Parse JSON để đếm số lượng
                JsonObject[] workers = gson.fromJson(responseBody, JsonObject[].class);
                
                int total = workers.length;
                long online = 0;
                long offline = 0;
                
                for (JsonObject worker : workers) {
                    if (worker.has("isOnline") && worker.get("isOnline").getAsBoolean()) {
                        online++;
                    } else {
                        offline++;
                    }
                }

                return String.format(
                    "📊 Tổng quan công nhân:\n" +
                    "• Tổng số: %d người\n" +
                    "• ✅ Online: %d người\n" +
                    "• ⚪ Offline: %d người\n\n" +
                    "Chi tiết: %s",
                    total, online, offline, responseBody
                );
            }
        } catch (IOException e) {
            return "❌ Lỗi kết nối API: " + e.getMessage();
        }
    }
}
