package com.hatrustsoft.agent.tools;

import com.google.adk.tools.FunctionImplementation;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;

/**
 * Tool: Kiểm tra trạng thái mũ bảo hộ
 * API: GET /api/helmet/all hoặc /api/location/map-data-realtime
 */
public class GetHelmetStatusFunction implements FunctionImplementation {
    private final String backendUrl;
    private final OkHttpClient client;

    public GetHelmetStatusFunction(String backendUrl) {
        this.backendUrl = backendUrl;
        this.client = new OkHttpClient();
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String macAddress = (String) arguments.get("mac_address");
        
        if (macAddress == null || macAddress.isEmpty()) {
            return "❌ Vui lòng cung cấp địa chỉ MAC của mũ bảo hộ";
        }

        try {
            // Gọi API lấy realtime data
            Request request = new Request.Builder()
                .url(backendUrl + "/api/location/map-data-realtime")
                .get()
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "❌ Lỗi khi lấy dữ liệu mũ: " + response.code();
                }

                String responseBody = response.body().string();
                
                // Tìm mũ theo MAC address trong response
                if (responseBody.contains(macAddress)) {
                    return String.format(
                        "🔍 Trạng thái mũ %s:\n%s",
                        macAddress,
                        responseBody
                    );
                } else {
                    return String.format(
                        "⚠️ Không tìm thấy mũ với MAC %s. " +
                        "Có thể mũ đang offline hoặc MAC không đúng.",
                        macAddress
                    );
                }
            }
        } catch (IOException e) {
            return "❌ Lỗi kết nối API: " + e.getMessage();
        }
    }
}
