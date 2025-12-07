package com.hatrustsoft.agent.tools;

import com.google.adk.tools.FunctionImplementation;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;

/**
 * Tool: Lấy dữ liệu bản đồ realtime
 * API: GET /api/location/map-data-realtime
 */
public class GetMapDataFunction implements FunctionImplementation {
    private final String backendUrl;
    private final OkHttpClient client;

    public GetMapDataFunction(String backendUrl) {
        this.backendUrl = backendUrl;
        this.client = new OkHttpClient();
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        try {
            Request request = new Request.Builder()
                .url(backendUrl + "/api/location/map-data-realtime")
                .get()
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "❌ Lỗi khi lấy dữ liệu bản đồ: " + response.code();
                }

                String responseBody = response.body().string();
                
                return String.format(
                    "📍 Vị trí realtime của tất cả công nhân:\n%s",
                    responseBody
                );
            }
        } catch (IOException e) {
            return "❌ Lỗi kết nối API: " + e.getMessage();
        }
    }
}
