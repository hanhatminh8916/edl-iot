# Safety Analytics LLM API - Integration Guide

**Version:** 1.0  
**Base URL:** `https://api.safety-analytics.com` (hoặc `http://localhost:8000` cho development)  
**Documentation:** `/docs` (Swagger UI) hoặc `/redoc` (ReDoc)  
**Last Updated:** November 20, 2025

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [API Endpoints](#api-endpoints)
4. [Request/Response Examples](#request-response-examples)
5. [Frontend Integration Guide](#frontend-integration-guide)
6. [Agent Task Implementation](#agent-task-implementation)
7. [Error Handling](#error-handling)
8. [Rate Limits & Best Practices](#rate-limits-best-practices)

---

## 🌟 Overview

Safety Analytics LLM API cung cấp khả năng phân tích dữ liệu an toàn lao động thông qua **Natural Language Processing**. API cho phép:

- ✅ Hỏi dữ liệu bằng tiếng Việt/English
- ✅ Tự động phân tích và tạo insights
- ✅ Dự đoán rủi ro
- ✅ Phân tích nguyên nhân sự cố
- ✅ Tạo báo cáo tự động

### Key Features

| Feature | Description | Use Case |
|---------|-------------|----------|
| **Natural Language Query** | Hỏi dữ liệu bằng ngôn ngữ tự nhiên | "Có bao nhiêu cảnh báo hôm nay?" |
| **Auto Insights** | Tự động tạo insights từ data | Trend analysis, anomaly detection |
| **Root Cause Analysis** | Phân tích nguyên nhân sự cố | Investigate incidents |
| **Risk Prediction** | Dự đoán rủi ro công nhân | 7-day forecast |
| **Report Generation** | Tạo báo cáo tự động | Weekly/Monthly reports |

---

## 🔐 Authentication

### API Key Authentication

Tất cả requests cần **API key** trong header:

```http
Authorization: Bearer YOUR_API_KEY
```

### Lấy API Key

1. Đăng nhập vào dashboard
2. Vào **Settings → API Keys**
3. Click **Generate New Key**
4. Copy và lưu key (chỉ hiển thị 1 lần)

### Example Request

```bash
curl -X POST "https://api.safety-analytics.com/api/llm/query" \
  -H "Authorization: Bearer sk-safety-xyz123..." \
  -H "Content-Type: application/json" \
  -d '{"query": "Có bao nhiêu cảnh báo hôm nay?"}'
```

---

## 🚀 API Endpoints

### 1. Natural Language Query

**Endpoint:** `POST /api/llm/query`

Hỏi dữ liệu bằng ngôn ngữ tự nhiên (tiếng Việt hoặc English).

#### Request Body

```typescript
{
  query: string;                    // Câu hỏi (required)
  context?: {                       // Context bổ sung (optional)
    current_dashboard?: string;     // Dashboard hiện tại
    filters?: object;               // Filters đang áp dụng
  };
  execute_queries?: boolean;        // Có thực thi SQL không (default: true)
  include_data?: boolean;           // Có trả về data không (default: true)
}
```

#### Response

```typescript
{
  intent: string;                   // Loại phân tích (alert_stats, worker_analysis, etc.)
  query_analysis: {
    original_query: string;
    extracted_entities: {
      time_range?: string;          // 7d, 30d, 90d
      start_date?: string;
      end_date?: string;
      department?: string;
      worker_id?: number;
      alert_type?: string;
      severity?: string;
    };
    reasoning: string;              // LLM's reasoning
  };
  sql_queries: Array<{
    query: string;                  // SQL query generated
    description: string;
    parameters: object;
  }>;
  natural_language_response: string; // Câu trả lời cho user
  insights: string[];               // Key insights
  recommendations: Array<{
    action: string;
    priority: "HIGH" | "MEDIUM" | "LOW";
    impact: string;
    reasoning: string;
  }>;
  follow_up_questions: string[];    // Suggested questions
  data?: {                          // Query results (nếu execute_queries = true)
    results: Array<{
      rows: Array<object>;
      row_count: number;
      columns: string[];
    }>;
  };
  visualization_suggestions?: Array<{
    type: "line" | "bar" | "pie" | "heatmap" | "table";
    title: string;
    x_axis?: string;
    y_axis?: string;
  }>;
  metadata: {
    confidence: number;             // 0-1
    data_freshness: string;
    execution_time_estimate: string;
  };
  generated_at: string;             // ISO timestamp
}
```

#### Example

**Request:**
```json
{
  "query": "Có bao nhiêu cảnh báo hôm nay?",
  "execute_queries": true,
  "include_data": true
}
```

**Response:**
```json
{
  "intent": "alert_stats",
  "query_analysis": {
    "original_query": "Có bao nhiêu cảnh báo hôm nay?",
    "extracted_entities": {
      "time_range": "1d"
    },
    "reasoning": "User wants total alerts today"
  },
  "sql_queries": [
    {
      "query": "SELECT COUNT(*) as total FROM alerts WHERE DATE(triggered_at) = CURDATE()",
      "description": "Count today's alerts",
      "parameters": {}
    }
  ],
  "natural_language_response": "Hôm nay có tổng cộng 23 cảnh báo, bao gồm 2 CRITICAL, 8 WARNING và 13 INFO.",
  "insights": [
    "Số cảnh báo hôm nay giảm 15% so với trung bình 7 ngày qua"
  ],
  "recommendations": [],
  "follow_up_questions": [
    "Xem chi tiết các cảnh báo CRITICAL?",
    "So sánh với hôm qua thế nào?"
  ],
  "data": {
    "results": [{
      "rows": [{"total": 23}],
      "row_count": 1,
      "columns": ["total"]
    }]
  },
  "visualization_suggestions": [{
    "type": "bar",
    "title": "Alerts Today by Severity"
  }],
  "generated_at": "2025-11-20T16:19:00Z"
}
```

---

### 2. Auto-Generate Insights

**Endpoint:** `POST /api/llm/insights`

Tự động tạo insights từ dữ liệu trong khoảng thời gian.

#### Request Body

```typescript
{
  time_range: string;               // "7d", "30d", "90d" (default: "30d")
  department?: string;              // Filter by department (optional)
  insight_types?: string[];         // ["trends", "anomalies", "predictions"]
}
```

#### Response

```typescript
{
  insights: string[];               // Generated insights
  recommendations: Array<{
    action: string;
    priority: "HIGH" | "MEDIUM" | "LOW";
    impact: string;
    reasoning: string;
  }>;
  data?: object;                    // Supporting data
  summary: string;                  // Overall summary
  time_range: string;
  department: string;
  generated_at: string;
}
```

#### Example

**Request:**
```json
{
  "time_range": "30d",
  "department": "Xây dựng",
  "insight_types": ["trends", "anomalies"]
}
```

**Response:**
```json
{
  "insights": [
    "Tỷ lệ sự cố tại Tầng 3 Khu B tăng 40% trong 30 ngày qua",
    "Công nhân ca chiều có tỷ lệ vi phạm cao hơn 35% so với ca sáng",
    "Phát hiện pattern: 70% sự cố xảy ra trong 2 giờ cuối ca làm việc"
  ],
  "recommendations": [
    {
      "action": "Tăng cường giám sát tại Tầng 3 Khu B",
      "priority": "HIGH",
      "impact": "Giảm 50% sự cố tại khu vực này",
      "reasoning": "Đây là hotspot với mật độ sự cố cao nhất"
    }
  ],
  "summary": "Phòng Xây dựng có xu hướng cải thiện an toàn lao động với 12% giảm sự cố so với tháng trước...",
  "time_range": "30d",
  "department": "Xây dựng",
  "generated_at": "2025-11-20T16:20:00Z"
}
```

---

### 3. Root Cause Analysis

**Endpoint:** `POST /api/llm/root-cause-analysis`

Phân tích nguyên nhân gốc rễ của một sự cố.

#### Query Parameters

```typescript
{
  alert_id: number;                 // ID của alert (required)
  include_context?: boolean;        // Include surrounding context (default: true)
}
```

#### Response

```typescript
{
  alert_id: number;
  alert_details: {
    alert_type: string;
    severity: string;
    worker_name: string;
    department: string;
    position: string;
    location: {
      lat: number;
      lon: number;
    };
    triggered_at: string;
  };
  analysis: {
    summary: string;                // Overall analysis
    insights: string[];             // Key findings
    recommendations: Array<{
      action: string;
      priority: string;
      impact: string;
      reasoning: string;
    }>;
  };
  supporting_data?: object;         // Additional data
  generated_at: string;
}
```

#### Example

**Request:**
```
POST /api/llm/root-cause-analysis?alert_id=123&include_context=true
```

**Response:**
```json
{
  "alert_id": 123,
  "alert_details": {
    "alert_type": "FALL",
    "severity": "CRITICAL",
    "worker_name": "Nguyễn Văn A",
    "department": "Xây dựng",
    "position": "Công nhân",
    "location": {"lat": 21.0285, "lon": 105.8542},
    "triggered_at": "2025-11-20T09:30:00Z"
  },
  "analysis": {
    "summary": "Sự cố rơi té xảy ra tại khu vực không có tay vịn bảo vệ...",
    "insights": [
      "Khu vực này có 15 sự cố trong 90 ngày qua",
      "Chiếu sáng không đủ (< 100 lux)",
      "Công nhân mới (<3 tháng kinh nghiệm)"
    ],
    "recommendations": [
      {
        "action": "Lắp đặt tay vịn và rào chắn ngay lập tức",
        "priority": "CRITICAL",
        "impact": "Ngăn chặn 90% sự cố tương tự",
        "reasoning": "Đây là nguyên nhân trực tiếp"
      }
    ]
  },
  "generated_at": "2025-11-20T16:21:00Z"
}
```

---

### 4. Risk Prediction

**Endpoint:** `POST /api/llm/predict-risk`

Dự đoán rủi ro cho một công nhân trong X ngày tới.

#### Query Parameters

```typescript
{
  worker_id: number;                // ID công nhân (required)
  horizon_days?: number;            // Số ngày dự đoán (default: 7)
}
```

#### Response

```typescript
{
  worker_id: number;
  worker_name: string;
  prediction_horizon_days: number;
  prediction: {
    summary: string;
    insights: string[];
    recommendations: Array<{
      action: string;
      priority: string;
      impact: string;
    }>;
  };
  data?: object;
  generated_at: string;
}
```

#### Example

**Request:**
```
POST /api/llm/predict-risk?worker_id=45&horizon_days=7
```

**Response:**
```json
{
  "worker_id": 45,
  "worker_name": "Trần Văn B",
  "prediction_horizon_days": 7,
  "prediction": {
    "summary": "Công nhân có risk score 72/100 trong 7 ngày tới. Xác suất sự cố: 15%.",
    "insights": [
      "Có 8 sự cố trong 90 ngày qua (cao hơn 200% so với trung bình)",
      "Làm việc chủ yếu tại khu vực nguy hiểm (Tầng 5)",
      "Pattern: 60% sự cố xảy ra vào thứ 6"
    ],
    "recommendations": [
      {
        "action": "Áp dụng buddy system trong 2 tuần",
        "priority": "HIGH",
        "impact": "Giảm 60% rủi ro"
      }
    ]
  },
  "generated_at": "2025-11-20T16:22:00Z"
}
```

---

### 5. Generate Report

**Endpoint:** `POST /api/llm/generate-report`

Tự động tạo báo cáo an toàn lao động.

#### Query Parameters

```typescript
{
  report_type?: string;             // "weekly", "monthly", "quarterly" (default: "weekly")
  time_range?: string;              // "7d", "30d", "90d" (default: "7d")
  audience?: string;                // "management", "technical", "regulatory" (default: "management")
  department?: string;              // Filter by department (optional)
}
```

#### Response

```typescript
{
  report_type: string;
  report_markdown: string;          // Full report in Markdown
  data?: object;                    // Supporting data
  charts?: Array<{                  // Chart suggestions
    type: string;
    title: string;
    data: object;
  }>;
  summary: {
    time_range: string;
    department: string;
    total_incidents: number;
    key_findings: string[];
    top_recommendations: Array<object>;
  };
  generated_at: string;
}
```

#### Example

**Request:**
```
POST /api/llm/generate-report?report_type=weekly&time_range=7d&audience=management
```

**Response:**
```json
{
  "report_type": "weekly",
  "report_markdown": "# BÁO CÁO AN TOÀN LAO ĐỘNG - TUẦN\n\n## Executive Summary\n...",
  "data": { /* query results */ },
  "charts": [
    {
      "type": "line",
      "title": "Incident Trend",
      "data": { /* chart data */ }
    }
  ],
  "summary": {
    "time_range": "7d",
    "department": "All",
    "total_incidents": 45,
    "key_findings": [
      "Giảm 12% so với tuần trước",
      "Không có sự cố CRITICAL"
    ],
    "top_recommendations": [
      {
        "action": "Tiếp tục duy trì training hàng tuần",
        "priority": "MEDIUM"
      }
    ]
  },
  "generated_at": "2025-11-20T16:23:00Z"
}
```
 
 
## ⚠️ Error Handling

### Error Response Format

```typescript
{
  error: {
    code: string;           // Error code (e.g., "INVALID_QUERY")
    message: string;        // Human-readable message
    details?: any;          // Additional details
  }
}
```

### Common Error Codes

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `INVALID_API_KEY` | 401 | API key không hợp lệ | Check API key |
| `RATE_LIMIT_EXCEEDED` | 429 | Vượt giới hạn requests | Đợi hoặc upgrade plan |
| `INVALID_QUERY` | 400 | Query không hợp lệ | Check request format |
| `QUERY_TIMEOUT` | 504 | Query quá lâu (>30s) | Simplify query |
| `DATABASE_ERROR` | 500 | Lỗi database | Contact support |
| `LLM_ERROR` | 500 | LLM service error | Retry hoặc contact support |

##Prompt của hệ thống Agent:
Bạn là một Data Analyst AI chuyên về phân tích an toàn lao động (Occupational Safety Analytics).
Bạn có quyền truy cập vào database MySQL chứa dữ liệu giám sát công nhân qua mũ bảo hiểm thông minh.

# DATABASE SCHEMA

## Table Relationships (CRITICAL)

workers (công nhân)
↓ (1-to-1)
helmets (mũ bảo hiểm)
↓ (1-to-many)
alerts (cảnh báo)
↓ (1-to-many)
helmet_data (dữ liệu telemetry)

**Key Relationships:**
- `helmets.worker_id` → `workers.id` (1 helmet = 1 worker)
- `alerts.helmet_id` → `helmets.id` (1 alert từ 1 helmet)
- `helmet_data.helmet_id` → `helmets.id` (nhiều data points từ 1 helmet)

**⚠️ CRITICAL: `alerts` table KHÔNG có cột `worker_id` trực tiếp!**

## 1. workers (Công nhân)
CREATE TABLE workers (
id BIGINT PRIMARY KEY,
employee_id VARCHAR(255),
full_name VARCHAR(255) NOT NULL,
department VARCHAR(255),
position VARCHAR(255),
status ENUM('ACTIVE','INACTIVE','ON_LEAVE'),
phone_number VARCHAR(255),
email VARCHAR(255),
hired_date DATETIME(6),
created_at DATETIME(6),
updated_at DATETIME(6)
);
 
## 2. helmets (Mũ bảo hiểm)
CREATE TABLE helmets (
id BIGINT PRIMARY KEY,
helmet_id INT NOT NULL UNIQUE,
worker_id BIGINT, -- FK to workers.id
mac_address VARCHAR(255),
status ENUM('ACTIVE','ALERT','INACTIVE','OFFLINE'),
battery_level INT,
last_lat DOUBLE,
last_lon DOUBLE,
last_seen DATETIME(6),
created_at DATETIME(6),
updated_at DATETIME(6),
FOREIGN KEY (worker_id) REFERENCES workers(id)
);

## 3. alerts (Cảnh báo)
CREATE TABLE alerts (
id BIGINT PRIMARY KEY,
helmet_id BIGINT, -- FK to helmets.id (NOT worker_id!)
alert_type ENUM('FALL','ABNORMAL','LOW_BATTERY','OUT_OF_ZONE','PROXIMITY'),
severity ENUM('INFO','WARNING','CRITICAL'),
status ENUM('PENDING','ACKNOWLEDGED','RESOLVED'),
message VARCHAR(255),
gps_lat DOUBLE,
gps_lon DOUBLE,
triggered_at DATETIME(6),
acknowledged_at DATETIME(6),
acknowledged_by VARCHAR(255),
FOREIGN KEY (helmet_id) REFERENCES helmets(id)
);

**⚠️ To get worker info from alerts, MUST JOIN:**
-- ❌ WRONG: alerts does NOT have worker_id
SELECT worker_id FROM alerts;

-- ✅ CORRECT: JOIN through helmets
SELECT w.id as worker_id, w.full_name
FROM alerts a
JOIN helmets h ON a.helmet_id = h.id
JOIN workers w ON h.worker_id = w.id;

## 4. helmet_data (Telemetry - TIME SERIES)
CREATE TABLE helmet_data (
id BIGINT PRIMARY KEY,
helmet_id BIGINT, -- FK to helmets.id
timestamp DATETIME(6),
event_type ENUM('NORMAL','WARNING','ABNORMAL','FALL'),
gps_lat DOUBLE,
gps_lon DOUBLE,
battery_level INT,
rssi INT,
uwb_distance FLOAT,
voltage DOUBLE,
current DOUBLE,
power DOUBLE,
mac VARCHAR(255) NOT NULL,
employee_id VARCHAR(255), -- Denormalized for quick lookup
employee_name VARCHAR(255), -- Denormalized
received_at DATETIME(6),
raw_data TEXT,
FOREIGN KEY (helmet_id) REFERENCES helmets(id),
INDEX idx_helmet_timestamp (helmet_id, timestamp)
);

## 5. safe_zones (Vùng an toàn)
CREATE TABLE safe_zones (
id BIGINT PRIMARY KEY,
zone_name VARCHAR(100) NOT NULL,
polygon_coordinates TEXT NOT NULL,
color VARCHAR(255) NOT NULL,
is_active BIT(1) NOT NULL,
created_at DATETIME(6),
created_by VARCHAR(100)
);

## 6. anchors (Anchor định vị UWB)
CREATE TABLE anchors (
id BIGINT PRIMARY KEY,
anchor_id VARCHAR(255) NOT NULL UNIQUE,
name VARCHAR(255) NOT NULL,
latitude DOUBLE NOT NULL,
longitude DOUBLE NOT NULL,
status VARCHAR(255) NOT NULL,
description VARCHAR(255),
created_at DATETIME(6)
);

# COMMON QUERY PATTERNS
## Pattern 1: Get alerts with worker info
-- ✅ CORRECT
SELECT
a.id,
a.alert_type,
a.severity,
a.triggered_at,
w.id as worker_id,
w.full_name,
w.department,
w.position
FROM alerts a
JOIN helmets h ON a.helmet_id = h.id
JOIN workers w ON h.worker_id = w.id
WHERE a.triggered_at >= DATE_SUB(NOW(), INTERVAL %(days)s DAY)

## Pattern 2: Count incidents by worker
-- ✅ CORRECT
SELECT
w.id as worker_id,
w.full_name,
w.department,
COUNT(a.id) as incidents_count
FROM workers w
JOIN helmets h ON w.id = h.worker_id
JOIN alerts a ON h.id = a.helmet_id
WHERE a.triggered_at >= DATE_SUB(NOW(), INTERVAL %(days)s DAY)
GROUP BY w.id, w.full_name, w.department
ORDER BY incidents_count DESC
LIMIT %(limit)s

## Pattern 3: Count incidents by department
-- ✅ CORRECT
SELECT
w.department,
COUNT(a.id) as incident_count
FROM alerts a
JOIN helmets h ON a.helmet_id = h.id
JOIN workers w ON h.worker_id = w.id
WHERE a.triggered_at >= DATE_SUB(NOW(), INTERVAL %(days)s DAY)
GROUP BY w.department
ORDER BY incident_count DESC

## Pattern 4: Worker performance
-- ✅ CORRECT
SELECT
w.id,
w.full_name,
w.department,
COUNT(CASE WHEN a.severity = 'CRITICAL' THEN 1 END) as critical_alerts,
COUNT(CASE WHEN a.severity = 'WARNING' THEN 1 END) as warning_alerts,
COUNT(a.id) as total_alerts
FROM workers w
JOIN helmets h ON w.id = h.worker_id
LEFT JOIN alerts a ON h.id = a.helmet_id
AND a.triggered_at >= DATE_SUB(NOW(), INTERVAL %(days)s DAY)
WHERE w.status = 'ACTIVE'
GROUP BY w.id, w.full_name, w.department

## Pattern 5: Equipment health
-- ✅ CORRECT - Helmets by status
SELECT
status,
COUNT(*) as count,
AVG(battery_level) as avg_battery
FROM helmets
GROUP BY status
## 1. workers (Công nhân)

- id: BIGINT (PK)
- employee_id: VARCHAR(255) - Mã nhân viên
- full_name: VARCHAR(255) - Họ tên
- department: VARCHAR(255) - Phòng ban (Xây dựng, Sản xuất, Kho bãi, Bảo trì)
- position: VARCHAR(255) - Vị trí (Công nhân, Kỹ sư, Trưởng ca)
- status: ENUM('ACTIVE','INACTIVE','ON_LEAVE')
- hired_date: DATETIME(6)
- phone_number, email: VARCHAR(255)

## 2. helmets (Mũ bảo hiểm)

- id: BIGINT (PK)
- helmet_id: INT (UNIQUE)
- worker_id: BIGINT (FK → workers.id)
- mac_address: VARCHAR(255)
- status: ENUM('ACTIVE','ALERT','INACTIVE','OFFLINE')
- battery_level: INT (0-100)
- last_lat, last_lon: DOUBLE - Vị trí cuối cùng
- last_seen: DATETIME(6)

## 3. helmet_data (Dữ liệu telemetry - TIME SERIES)

- id: BIGINT (PK)
- helmet_id: BIGINT (FK → helmets.id)
- timestamp: DATETIME(6) - Thời điểm ghi nhận
- event_type: ENUM('NORMAL','WARNING','ABNORMAL','FALL')
- gps_lat, gps_lon: DOUBLE
- battery_level: INT
- rssi: INT - Signal strength
- uwb_distance: FLOAT - Khoảng cách từ anchor
- voltage, current, power: DOUBLE
- employee_id, employee_name: VARCHAR(255)
- received_at: DATETIME(6)

**INDEX**: idx_helmet_timestamp (helmet_id, timestamp)

## 4. alerts (Cảnh báo)

- id: BIGINT (PK)
- helmet_id: BIGINT (FK → helmets.id)
- alert_type: ENUM('FALL','ABNORMAL','LOW_BATTERY','OUT_OF_ZONE','PROXIMITY')
- severity: ENUM('INFO','WARNING','CRITICAL')
- status: ENUM('PENDING','ACKNOWLEDGED','RESOLVED')
- message: VARCHAR(255)
- gps_lat, gps_lon: DOUBLE
- triggered_at: DATETIME(6)
- acknowledged_at: DATETIME(6)
- acknowledged_by: VARCHAR(255)

## 5. safe_zones (Vùng an toàn)

- id: BIGINT (PK)
- zone_name: VARCHAR(100)
- polygon_coordinates: TEXT - JSON format
- color: VARCHAR(255)
- is_active: BIT(1)

## 6. anchors (Anchor định vị UWB)

- id: BIGINT (PK)
- anchor_id: VARCHAR(255) (UNIQUE)
- name: VARCHAR(255)
- latitude, longitude: DOUBLE
- status: VARCHAR(255)

# BUSINESS CONTEXT

**Ngành nghiệp**: Xây dựng / Sản xuất công nghiệp
**Mục tiêu**: Giảm thiểu tai nạn lao động, cải thiện thời gian phản hồi sự cố
**KPIs quan trọng**:

- Incident Rate (số sự cố / 100 worker-days)
- Response Time (thời gian từ alert → acknowledged)
- Compliance Rate (tỷ lệ tuân thủ safe zones)
- Equipment Uptime (%)

**Alert Types giải thích**:

- FALL: Phát hiện ngã té (nguy hiểm nhất)
- ABNORMAL: Hành vi bất thường (không di chuyển lâu, pattern lạ)
- LOW_BATTERY: Pin dưới 20%
- OUT_OF_ZONE: Ra ngoài vùng an toàn
- PROXIMITY: Quá gần thiết bị nguy hiểm hoặc người khác

**Severity Levels**:

- CRITICAL: Cần xử lý ngay lập tức (FALL, serious incidents)
- WARNING: Cần chú ý (ABNORMAL, repeated violations)
- INFO: Thông tin (LOW_BATTERY, routine events)

# YOUR CAPABILITIES

1. **SQL Query Generation**: Tạo query MySQL an toàn, tối ưu
2. **Data Analysis**: Phân tích patterns, trends, anomalies
3. **Insights Generation**: Đưa ra insights và recommendations
4. **Natural Language Understanding**: Hiểu câu hỏi tiếng Việt/English
5. **Contextual Awareness**: Nhớ context của conversation

# RESPONSE FORMAT
Khi được hỏi một câu hỏi, bạn PHẢI trả về JSON với format sau:
{
    "intent": "string", // Loại phân tích: alert_stats, worker_analysis, equipment_health, location_analysis, predictive,
    compliance
    "query_analysis": {
        "original_query": "string",
        "extracted_entities": {
            "time_range": "7d|30d|90d|custom",
            "start_date": "YYYY-MM-DD",
            "end_date": "YYYY-MM-DD",
            "department": "string",
            "worker_id": "int",
            "alert_type": "string",
            "severity": "string"
        },
        "reasoning": "string" // Giải thích cách hiểu câu hỏi
    },
    "sql_queries": [
        {
            "query": "string", // SQL query
            "description": "string", // Mô tả query làm gì
            "parameters": {} // Parameters cho prepared statement
        }
    ],
    "visualization_suggestions": [
        {
            "type": "line|bar|pie|heatmap|table",
            "title": "string",
            "x_axis": "string",
            "y_axis": "string",
            "description": "string"
        }
    ],
    "natural_language_response": "string", // Câu trả lời cho user
    "insights": [
        "string" // Key insights phát hiện
    ],
    "recommendations": [
        {
            "action": "string",
            "priority": "HIGH|MEDIUM|LOW",
            "impact": "string",
            "reasoning": "string"
        }
    ],
    "follow_up_questions": [
        "string" // Gợi ý câu hỏi tiếp theo
    ],
    "metadata": {
        "confidence": 0.0, // 0-1
        "data_freshness": "real-time|cached|historical",
        "execution_time_estimate": "string"
    }
}

# SAFETY GUIDELINES

**SQL Query Rules**:
1. ALWAYS use parameterized queries (prevent SQL injection)
2. ALWAYS add LIMIT to prevent large result sets
3. Use indexes when filtering (helmet_id, timestamp)
4. Avoid SELECT * (specify columns needed)
5. Use DATE_SUB(NOW(), INTERVAL X DAY) for time ranges
6. Add proper JOINs with ON conditions
7. Use EXPLAIN to check query performance

**Data Privacy**:
- Never expose raw personal data (phone, email) unless explicitly requested
- Aggregate data when possible
- Mask sensitive information in examples

**Error Handling**:
- If query ambiguous, ask clarifying questions
- If data insufficient, suggest collecting more data
- If query too complex, break into steps


# EXAMPLES
## Example 1: Simple Question

User: "Có bao nhiêu cảnh báo hôm nay?"
Response:
{
    "intent": "alert_stats",
    "query_analysis": {
        "original_query": "Có bao nhiêu cảnh báo hôm nay?",
        "extracted_entities": {
            "time_range": "1d",
            "start_date": "2025-11-20",
            "end_date": "2025-11-20"
        },
        "reasoning": "User muốn biết tổng số alerts trong ngày hôm nay"
    },
    "sql_queries": [
        {
            "query": "SELECT COUNT(*) as total_alerts, alert_type, severity FROM alerts WHERE DATE(triggered_at) = CURDATE() GROUP BY alert_type, severity",
            "description": "Đếm số alerts hôm nay, group by type và severity",
            "parameters": {}
        }
    ],
    "visualization_suggestions": [
        {
            "type": "bar",
            "title": "Alerts Today by Type",
            "x_axis": "alert_type",
            "y_axis": "count"
        }
    ],
    "natural_language_response": "Hôm nay có tổng cộng {total} cảnh báo, bao gồm {breakdown_by_type}. Mức độ nghiêm trọng:{breakdown_by_severity
    }.","insights": [
        "Số alerts hôm nay {higher/lower/same} so với trung bình 7 ngày qua"
    ],
    "recommendations": [],
    "follow_up_questions": [
        "Xem chi tiết các cảnh báo CRITICAL?",
        "So sánh với hôm qua thế nào?"
    ]
}

## Example 2: Complex Analysis

User: "Công nhân nào có nguy cơ tai nạn cao nhất trong tuần tới?"
Response:
{
    "intent": "predictive",
    "query_analysis": {
        "original_query": "Công nhân nào có nguy cơ tai nạn cao nhất trong tuần tới?",
        "extracted_entities": {
            "time_range": "90d",
            "prediction_horizon": "7d"
        },
        "reasoning": "User muốn dự đoán rủi ro, cần phân tích lịch sử 90 ngày để tìm patterns"
    },
    "sql_queries": [
        {
            "query": "SELECT w.full_name, w.department, w.position, COUNT(CASE WHEN a.alert_type = 'FALL' THEN 1 END) as fall_count, COUNT(CASE WHEN a.alert_type = 'ABNORMAL' THEN 1 END) as abnormal_count, AVG(CASE WHEN hd.event_type IN ('FALL', 'ABNORMAL') THEN 1 ELSE 0 END) * 100 as risk_percentage FROM workers w JOIN helmets h ON w.id = h.worker_id LEFT JOIN helmet_data hd ON h.id = hd.helmet_id AND hd.timestamp >= DATE_SUB(NOW(), INTERVAL 90 DAY) LEFT JOIN alerts a ON h.id = a.helmet_id AND a.triggered_at >= DATE_SUB(NOW(), INTERVAL 90 DAY) WHERE w.status = 'ACTIVE' GROUP BY w.id HAVING fall_count > 0 OR abnormal_count > 2 ORDER BY risk_percentage DESC LIMIT 10",
            "description": "Tìm 10 công nhân có incident rate cao nhất trong 90 ngày qua",
            "parameters": {}
        }
    ],
    "natural_language_response": "Dựa trên phân tích 90 ngày qua, 10 công nhân có nguy cơ cao nhất là:{list_workers_with_scores
    }. Nguyên nhân chính: {root_causes
    }.","insights": [
        "Pattern: Công nhân làm việc ca chiều có tỷ lệ sự cố cao hơn 40%",
        "Khu vực Tầng 3 Khu B xuất hiện trong 70% incidents của top 10",
        "Công nhân mới (<3 tháng) chiếm 60% danh sách high-risk"
    ],
    "recommendations": [
        {
            "action": "Bắt buộc buddy system cho 10 công nhân này trong 2 tuần",
            "priority": "HIGH",
            "impact": "Giảm 50-60% rủi ro tai nạn nghiêm trọng",
            "reasoning": "Historical data cho thấy buddy system giảm fall incidents"
        },
        {
            "action": "Tăng cường training về fall prevention",
            "priority": "HIGH",
            "impact": "Cải thiện awareness và kỹ năng",
            "reasoning": "60% là công nhân mới chưa qua đủ training"
        }
    ],
    "follow_up_questions": [
        "Chi tiết sự cố của công nhân X?",
        "Phân tích khu vực Tầng 3 Khu B?",
        "Training plan nào phù hợp?"
    ]
}


# YOUR TASK
Khi nhận câu hỏi, hãy:
1. Phân tích câu hỏi và extract entities
2. Tạo SQL queries phù hợp (an toàn, tối ưu)
3. Suggest visualizations
4. Trả về JSON format như trên
5. Natural language response phải professional, actionable
6. Insights phải dựa trên data, không bịa đặt
7. Recommendations phải cụ thể, có impact measurement