# 🚀 LLM Analytics Integration - Deployment Guide

## 📋 Tổng quan

Hệ thống đã được tích hợp với **Safety Analytics LLM API** để cung cấp khả năng phân tích dữ liệu bằng AI.

## ✅ Các file đã tạo

### Backend (Java/Spring Boot)
1. **`LlmAnalyticsService.java`** - Service gọi LLM API
2. **`LlmAnalyticsController.java`** - REST endpoints cho frontend

### Frontend (JavaScript)
3. **`llm-analytics.js`** - Client library
4. **`ai-analytics.html`** - Demo UI

### Configuration
5. **`application.properties`** - Thêm config cho LLM API

---

## 🔧 Cài đặt và Cấu hình

### Bước 1: Set Environment Variables

Thêm vào Heroku Config Vars hoặc file `.env`:

```bash
# LLM API Configuration
LLM_API_BASE_URL=https://api.safety-analytics.com
LLM_API_KEY=sk-safety-your-api-key-here
```

Hoặc update trực tiếp trong `application.properties`:

```properties
llm.api.base-url=https://api.safety-analytics.com
llm.api.key=sk-safety-xyz123...
llm.api.timeout=30
```

### Bước 2: Verify Dependencies

Đảm bảo `pom.xml` có WebFlux dependency (đã có sẵn):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

### Bước 3: Build và Deploy

```bash
# Build project
mvn clean package

# Test locally
java -jar target/bfe-foraiot-0.0.1-SNAPSHOT.jar

# Deploy to Heroku
git add .
git commit -m "Add LLM Analytics integration"
git push heroku main
```

### Bước 4: Set API Key trên Heroku

```bash
heroku config:set LLM_API_KEY=sk-safety-your-real-api-key
heroku config:set LLM_API_BASE_URL=https://api.safety-analytics.com
```

---

## 📡 API Endpoints đã tạo

### 1. Natural Language Query
```http
POST /api/analytics/query
Content-Type: application/json

{
  "query": "Có bao nhiêu cảnh báo hôm nay?",
  "executeQueries": true,
  "includeData": true
}
```

### 2. Auto-generate Insights
```http
POST /api/analytics/insights
Content-Type: application/json

{
  "timeRange": "30d",
  "department": "Xây dựng"
}
```

### 3. Root Cause Analysis
```http
GET /api/analytics/root-cause/{alertId}?includeContext=true
```

### 4. Risk Prediction
```http
GET /api/analytics/risk-prediction/{workerId}?horizonDays=7
```

### 5. Generate Report
```http
POST /api/analytics/report
Content-Type: application/json

{
  "reportType": "weekly",
  "timeRange": "7d",
  "audience": "management"
}
```

### 6. Health Check
```http
GET /api/analytics/health
```

---

## 🎨 Frontend Integration

### Cách sử dụng trong HTML pages hiện có

Thêm vào bất kỳ HTML page nào:

```html
<!-- Add script -->
<script src="js/llm-analytics.js"></script>

<!-- Add AI Query Box -->
<div class="ai-query-section">
    <input type="text" id="aiQuery" placeholder="Hỏi AI về dữ liệu...">
    <button onclick="askAI()">Hỏi AI</button>
    <div id="aiResponse"></div>
</div>

<script>
async function askAI() {
    const query = document.getElementById('aiQuery').value;
    const response = await askQuestion(query);
    displayLlmResponse(response, 'aiResponse');
}
</script>
```

### Ví dụ tích hợp vào `dashboard.html`

```html
<!-- Thêm AI Insights Card -->
<div class="dashboard-card">
    <h3>🤖 AI Insights</h3>
    <button onclick="showDailyInsights()">Get Today's Insights</button>
    <div id="daily-insights"></div>
</div>

<script src="js/llm-analytics.js"></script>
<script>
async function showDailyInsights() {
    const insights = await generateInsights('1d', null);
    displayLlmResponse(insights, 'daily-insights');
}
</script>
```

### Ví dụ tích hợp vào `alerts.html`

Thêm button "Analyze Root Cause" cho mỗi alert:

```javascript
async function analyzeAlertCause(alertId) {
    const analysis = await analyzeRootCause(alertId);
    
    // Show in modal
    showModal({
        title: '🔍 Root Cause Analysis',
        content: `
            <p><strong>Summary:</strong> ${analysis.analysis.summary}</p>
            <h4>Insights:</h4>
            <ul>
                ${analysis.analysis.insights.map(i => `<li>${i}</li>`).join('')}
            </ul>
            <h4>Recommendations:</h4>
            ${analysis.analysis.recommendations.map(r => `
                <div class="recommendation ${r.priority.toLowerCase()}">
                    <strong>${r.action}</strong>
                    <p>${r.impact}</p>
                </div>
            `).join('')}
        `
    });
}
```

---

## 🧪 Testing

### 1. Test Backend Endpoints

```bash
# Health check
curl http://localhost:8080/api/analytics/health

# Test query
curl -X POST http://localhost:8080/api/analytics/query \
  -H "Content-Type: application/json" \
  -d '{"query":"Có bao nhiêu cảnh báo hôm nay?","executeQueries":true}'

# Test insights
curl -X POST http://localhost:8080/api/analytics/insights \
  -H "Content-Type: application/json" \
  -d '{"timeRange":"7d"}'
```

### 2. Test Frontend

Mở browser và truy cập:
```
http://localhost:8080/ai-analytics.html
```

Thử các câu hỏi:
- "Có bao nhiêu cảnh báo hôm nay?"
- "Công nhân nào có nguy cơ cao nhất?"
- "Phòng ban nào có nhiều sự cố nhất?"

---

## 📊 Use Cases

### Use Case 1: Dashboard với AI Insights
```javascript
// Auto-load insights khi mở dashboard
document.addEventListener('DOMContentLoaded', async () => {
    const insights = await generateInsights('7d', null);
    
    // Display in dashboard
    const insightsCard = document.getElementById('ai-insights-card');
    insightsCard.innerHTML = `
        <h3>💡 AI Insights (7 days)</h3>
        <ul>
            ${insights.insights.map(i => `<li>${i}</li>`).join('')}
        </ul>
    `;
});
```

### Use Case 2: Alert Details với Root Cause
```javascript
// Khi click vào alert, show root cause analysis
async function showAlertDetails(alertId) {
    // Load alert data
    const alert = await fetch(`/api/alerts/${alertId}`).then(r => r.json());
    
    // Get AI analysis
    const analysis = await analyzeRootCause(alertId);
    
    // Combine and display
    showDetailModal(alert, analysis);
}
```

### Use Case 3: Employee Profile với Risk Prediction
```javascript
// Trong employee profile page
async function loadEmployeeRisk(workerId) {
    const risk = await predictWorkerRisk(workerId, 7);
    
    // Display risk score
    document.getElementById('risk-score').innerHTML = `
        <div class="risk-indicator">
            <h4>Risk Score: ${risk.prediction.risk_score}/100</h4>
            <p>${risk.prediction.summary}</p>
        </div>
    `;
}
```

### Use Case 4: Weekly Report Generation
```javascript
// Schedule weekly report
async function generateWeeklyReport() {
    const report = await generateReport('weekly', '7d', 'management');
    
    // Send report via email hoặc save to database
    await fetch('/api/reports/save', {
        method: 'POST',
        body: JSON.stringify({
            title: 'Weekly Safety Report',
            content: report.report_markdown,
            generated_at: new Date().toISOString()
        })
    });
}
```

---

## ⚠️ Error Handling

Tất cả functions đều có error handling built-in:

```javascript
try {
    const response = await askQuestion("...");
    displayLlmResponse(response, 'container');
} catch (error) {
    console.error('LLM API error:', error);
    
    // Show user-friendly error
    showNotification('Không thể kết nối với AI service', 'error');
}
```

---

## 🔒 Security

1. **API Key**: Never expose API key trong frontend code
2. **Rate Limiting**: Backend sẽ handle rate limiting
3. **Input Validation**: Tất cả inputs đều được validate
4. **Error Messages**: Không expose sensitive info trong error messages

---

## 📈 Monitoring

Monitor LLM API calls trong logs:

```bash
# Heroku logs
heroku logs --tail | grep "LLM"

# Look for:
# 🤖 Sending NL query to LLM API: ...
# ✅ LLM response received: intent=...
# ❌ LLM API error: ...
```

---

## 🚀 Next Steps

1. **Get API Key** từ Safety Analytics
2. **Set environment variables** trên Heroku
3. **Deploy** application
4. **Test** endpoints
5. **Integrate** vào các HTML pages hiện có
6. **Monitor** usage và performance

---

## 📞 Support

Nếu gặp vấn đề:
1. Check logs: `heroku logs --tail`
2. Verify API key: `heroku config:get LLM_API_KEY`
3. Test health endpoint: `curl https://your-app.herokuapp.com/api/analytics/health`

---

## 🎉 Kết luận

Hệ thống đã sẵn sàng tích hợp LLM Analytics! Chỉ cần:
1. ✅ Set API key
2. ✅ Deploy
3. ✅ Enjoy AI-powered analytics! 🚀
