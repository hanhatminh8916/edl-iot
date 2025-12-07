package com.hatrustsoft.agent;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.FunctionDeclaration;
import com.google.adk.tools.Parameters;
import com.google.adk.tools.Property;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import com.hatrustsoft.agent.tools.*;

/**
 * IoT Dashboard Voice Control Agent
 * Hỗ trợ điều khiển dashboard bằng giọng nói tiếng Việt
 */
public class IoTDashboardAgent {

    // Field expected by Dev UI to load the agent dynamically
    public static final BaseAgent ROOT_AGENT = initAgent();

    private static final String BACKEND_API_URL = 
        System.getenv().getOrDefault("IOT_BACKEND_URL", "https://edl-safework-iot-bf3ee691c9f6.herokuapp.com");

    public static BaseAgent initAgent() {
        return LlmAgent.builder()
            .name("iot-dashboard-control")
            .description("Trợ lý AI điều khiển dashboard IoT bằng giọng nói tiếng Việt")
            .model("gemini-2.0-flash-exp")
            .instruction("""
                Bạn là trợ lý AI cho hệ thống giám sát an toàn công nhân xây dựng.
                Bạn có thể:
                
                1. **Kiểm tra trạng thái công nhân**:
                   - Số lượng công nhân đang online/offline
                   - Vị trí hiện tại của từng công nhân
                   - Mức pin của mũ bảo hộ
                   
                2. **Theo dõi cảnh báo**:
                   - Cảnh báo ngã (FALL)
                   - Yêu cầu trợ giúp (HELP_REQUEST)
                   - Điện áp/dòng điện bất thường
                   
                3. **Phân tích dữ liệu**:
                   - Thống kê theo thời gian
                   - Hiệu suất làm việc
                   - Vùng nguy hiểm
                
                **Lưu ý**:
                - Luôn trả lời bằng tiếng Việt
                - Ngắn gọn, dễ hiểu
                - Ưu tiên thông tin an toàn
                - Sử dụng emojis phù hợp (⚠️, ✅, 🔋, 📍, etc.)
                
                **Backend API**: """ + BACKEND_API_URL + """
                """)
            
            // Tool 1: Lấy danh sách công nhân
            .tool(FunctionDeclaration.builder()
                .name("get_workers")
                .description("Lấy danh sách tất cả công nhân và trạng thái của họ")
                .parameters(Parameters.builder().build())
                .implementation(new GetWorkersFunction(BACKEND_API_URL))
                .build())
            
            // Tool 2: Kiểm tra trạng thái mũ
            .tool(FunctionDeclaration.builder()
                .name("get_helmet_status")
                .description("Kiểm tra trạng thái chi tiết của một mũ bảo hộ")
                .parameters(Parameters.builder()
                    .addProperty("mac_address", Property.builder()
                        .type(Type.STRING)
                        .description("Địa chỉ MAC của mũ bảo hộ (vd: F4DD40BA2010)")
                        .build())
                    .addRequired("mac_address")
                    .build())
                .implementation(new GetHelmetStatusFunction(BACKEND_API_URL))
                .build())
            
            // Tool 3: Lấy cảnh báo gần đây
            .tool(FunctionDeclaration.builder()
                .name("get_recent_alerts")
                .description("Lấy danh sách cảnh báo nguy hiểm gần đây")
                .parameters(Parameters.builder()
                    .addProperty("limit", Property.builder()
                        .type(Type.INTEGER)
                        .description("Số lượng cảnh báo cần lấy (mặc định 10)")
                        .build())
                    .build())
                .implementation(new GetRecentAlertsFunction(BACKEND_API_URL))
                .build())
            
            // Tool 4: Lấy dữ liệu bản đồ
            .tool(FunctionDeclaration.builder()
                .name("get_map_data")
                .description("Lấy vị trí hiện tại của tất cả công nhân trên bản đồ")
                .parameters(Parameters.builder().build())
                .implementation(new GetMapDataFunction(BACKEND_API_URL))
                .build())
            
            // Tool 5: Dashboard overview
            .tool(FunctionDeclaration.builder()
                .name("get_dashboard_overview")
                .description("Lấy tổng quan dashboard (tổng số công nhân, active, alerts, hiệu suất)")
                .parameters(Parameters.builder().build())
                .implementation(new GetDashboardOverviewFunction(BACKEND_API_URL))
                .build())
            
            .build();
    }

    public static void main(String[] args) {
        System.out.println("IoT Dashboard Agent initialized successfully!");
        System.out.println("Backend API: " + BACKEND_API_URL);
    }
}
