package com.hatrustsoft.agent.tools;

import com.google.adk.tools.FunctionImplementation;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;

/**
 * Tool: Lấy tổng quan dashboard
 * API: GET /api/dashboard/overview
 */
public class GetDashboardOverviewFunction implements FunctionImplementation {
    private final String backendUrl;
    private final OkHttpClient client;

    public GetDashboardOverviewFunction(String backendUrl) {
        this.backendUrl = backendUrl;
        this.client = new OkHttpClient();
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        try {
            Request request = new Request.Builder()
                .url(backendUrl + "/api/dashboard/overview")
                .get()
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "❌ Lỗi khi lấy tổng quan dashboard: " + response.code();
                }

                String responseBody = response.body().string();
                
                return String.format(
                    "📊 Tổng quan Dashboard:\n%s",
                    responseBody
                );
            }
        } catch (IOException e) {
            return "❌ Lỗi kết nối API: " + e.getMessage();
        }
    }
}
