# IoT Voice Control Agent - Setup Guide

## 📋 Prerequisites

1. **Java 17+** installed
   ```bash
   java -version
   ```

2. **Maven 3.8+** installed
   ```bash
   mvn -version
   ```

3. **Google AI Studio API Key**
   - Get from: https://ai.google.dev/gemini-api/docs/api-key
   - Free tier: 1,500 requests/day

---

## 🚀 Quick Start

### Step 1: Set environment variables

**Windows (PowerShell)**:
```powershell
$env:GOOGLE_GENAI_USE_VERTEXAI = "FALSE"
$env:GOOGLE_API_KEY = "YOUR_API_KEY_HERE"
$env:IOT_BACKEND_URL = "https://edl-safework-iot-bf3ee691c9f6.herokuapp.com"
```

**Linux/Mac**:
```bash
export GOOGLE_GENAI_USE_VERTEXAI=FALSE
export GOOGLE_API_KEY=YOUR_API_KEY_HERE
export IOT_BACKEND_URL=https://edl-safework-iot-bf3ee691c9f6.herokuapp.com
```

### Step 2: Build project

```bash
cd J:/IOT/BFE_forAIOT/adk-integration
mvn clean compile
```

### Step 3: Run Dev UI (Text mode)

```bash
mvn exec:java \
    -Dexec.mainClass="com.google.adk.web.AdkWebServer" \
    -Dexec.args="--adk.agents.source-dir=." \
    -Dexec.classpathScope="compile"
```

### Step 4: Open browser

Navigate to: http://localhost:8080

Select agent: **iot-dashboard-control**

---

## 🎤 Voice Control Examples

### Basic Commands (Vietnamese)

1. **Kiểm tra công nhân**
   ```
   "Có bao nhiêu công nhân đang online?"
   "Hiển thị danh sách công nhân"
   ```

2. **Kiểm tra cảnh báo**
   ```
   "Có cảnh báo nguy hiểm nào không?"
   "Hiển thị 5 cảnh báo gần nhất"
   ```

3. **Kiểm tra mũ bảo hộ**
   ```
   "Kiểm tra pin của mũ F4DD40BA2010"
   "Trạng thái của mũ F4DD40BA2010"
   ```

4. **Xem bản đồ**
   ```
   "Hiển thị vị trí tất cả công nhân"
   "Công nhân đang ở đâu?"
   ```

5. **Tổng quan**
   ```
   "Cho tôi xem tổng quan dashboard"
   "Hiệu suất làm việc hôm nay thế nào?"
   ```

---

## 🔧 Available Tools

| Tool Name | API Endpoint | Description |
|-----------|-------------|-------------|
| `get_workers` | `/api/workers` | Danh sách công nhân + trạng thái |
| `get_helmet_status` | `/api/location/map-data-realtime` | Chi tiết 1 mũ bảo hộ |
| `get_recent_alerts` | `/api/alerts/recent?limit=X` | Cảnh báo gần đây |
| `get_map_data` | `/api/location/map-data-realtime` | Vị trí realtime |
| `get_dashboard_overview` | `/api/dashboard/overview` | Tổng quan dashboard |

---

## 📱 Integration with Web Dashboard

### Option 1: Embed ADK Dev UI in iframe

Add to your `dashboard.html`:

```html
<div id="voice-assistant">
    <button onclick="toggleVoiceAssistant()">
        🎤 Voice Assistant
    </button>
    <iframe 
        id="adk-frame" 
        src="http://localhost:8080" 
        style="width: 400px; height: 600px; display: none;"
    ></iframe>
</div>

<script>
function toggleVoiceAssistant() {
    const frame = document.getElementById('adk-frame');
    frame.style.display = frame.style.display === 'none' ? 'block' : 'none';
}
</script>
```

### Option 2: Custom WebSocket Integration

Create your own UI that connects to ADK's WebSocket API (more complex but fully customizable).

---

## 🌐 Deploy to Production

### Option 1: Run locally alongside Spring Boot

```bash
# Terminal 1: Spring Boot
cd J:/IOT/BFE_forAIOT
mvn spring-boot:run

# Terminal 2: ADK Agent
cd J:/IOT/BFE_forAIOT/adk-integration
mvn exec:java -Dexec.mainClass="com.google.adk.web.AdkWebServer"
```

### Option 2: Deploy ADK to Cloud Run (Google Cloud)

See: https://google.github.io/adk-docs/deploy/cloud-run/

### Option 3: Package as JAR and run on Heroku

1. Create separate Heroku app for ADK agent
2. Set GOOGLE_API_KEY config var
3. Deploy using Heroku Java buildpack

---

## 🎯 Benefits

✅ **Hands-free control** - Điều khiển dashboard không cần chạm tay  
✅ **Fast response** - Realtime streaming từ Gemini 2.0 Flash  
✅ **Vietnamese support** - Agent hiểu và trả lời tiếng Việt  
✅ **Multi-modal** - Hỗ trợ text, voice, và video (tùy chọn)  
✅ **Easy integration** - Tích hợp dễ dàng với Spring Boot backend  

---

## 📚 Next Steps

1. **Test text mode** - Thử các câu lệnh trong Dev UI
2. **Enable voice** - Click microphone button để dùng giọng nói
3. **Add more tools** - Tích hợp thêm APIs (send alert, update config, etc.)
4. **Custom UI** - Tạo giao diện riêng cho voice assistant
5. **Production deploy** - Deploy lên Cloud Run hoặc Heroku

---

## 🆘 Troubleshooting

### Agent không hiển thị trong dropdown?
- Chạy `mvn compile` trước
- Chạy `mvn exec:java` từ thư mục `adk-integration`
- Check console logs

### API call failed?
- Verify `IOT_BACKEND_URL` đúng
- Check backend đang chạy
- Enable CORS if needed

### Voice không hoạt động?
- Allow microphone permission trong browser
- Dùng HTTPS hoặc localhost
- Đeo tai nghe để tránh echo

---

## 📞 Support

- Google ADK Docs: https://google.github.io/adk-docs/
- Gemini API Key: https://ai.google.dev/gemini-api/docs/api-key
- Issues: Contact your development team
