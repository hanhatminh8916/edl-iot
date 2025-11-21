# 📚 Safety Analytics LLM API - Documentation

**Version**: 1.0.0  
**Base URL**: `http://localhost:8000`  
**Documentation**: `http://localhost:8000/docs` (Swagger UI)

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Rate Limits & Costs](#rate-limits--costs)
4. [Endpoints](#endpoints)
   - [POST /api/llm/query](#1-post-apillmquery)
   - [POST /api/llm/insights](#2-post-apillminsights)
   - [POST /api/llm/root-cause-analysis](#3-post-apillmroot-cause-analysis)
   - [POST /api/llm/generate-report](#4-post-apillmgenerate-report)
   - [POST /api/llm/generate-chart](#5-post-apillmgenerate-chart) 🆕
   - [POST /api/llm/compare](#7-post-apillmcompare) 🆕
   - [POST /api/llm/batch-query](#8-post-apillmbatch-query) 🆕
   - [DELETE /api/llm/conversation-history](#9-delete-apillmconversation-history)
5. [Response Models](#response-models)
6. [Error Handling](#error-handling)
7. [Best Practices](#best-practices)

---

## 🎯 Overview

Safety Analytics LLM API cung cấp AI-powered analytics cho hệ thống an toàn lao động, bao gồm:

- ✅ **Natural Language Queries**: Hỏi đáp bằng tiếng Việt tự nhiên
- ✅ **Auto SQL Generation**: Tự động tạo SQL queries từ câu hỏi
- ✅ **Data Enrichment**: Tự động tính toán metrics, insights, recommendations
- ✅ **Root Cause Analysis**: Phân tích nguyên nhân sâu xa của incidents
- ✅ **Auto Reports**: Tạo báo cáo tự động (weekly, monthly, quarterly)
- 🆕 **Chart Generation**: Tự động tạo biểu đồ (line, bar, pie, scatter)
- 🆕 **Entity Comparison**: So sánh performance giữa các đơn vị
- 🆕 **Batch Queries**: Execute nhiều queries cùng lúc

### 🔧 Tech Stack
- **LLM**: OpenAI GPT-4o-mini (optimized for cost)
- **Framework**: FastAPI + Pydantic
- **Database**: MySQL 8.0 with connection pooling
- **Language**: Vietnamese

---

## 🔐 Authentication

**Current Version**: No authentication required (internal API)

**Future**: Bearer token authentication
```http
Authorization: Bearer YOUR_API_TOKEN
```

---

## 💰 Rate Limits & Costs

### API Costs (OpenAI)
- **Model**: GPT-4o-mini
- **Cost per request**: ~$0.004 (optimized từ $0.126)
- **Token usage**: ~500-800 tokens/request

### Rate Limits
- **Requests**: 100/minute
- **Connection Pool**: 5 concurrent DB connections

### Optimization Tips
✅ Use `execute_queries=false` nếu chỉ cần SQL generation  
✅ Use `include_data=false` để giảm response size  
✅ Cache frequent queries ở client side

---

## 🚀 Endpoints

---

## 1. POST `/api/llm/query`

**Mục đích**: Natural language query - hỏi đáp bằng tiếng Việt về dữ liệu an toàn lao động

### Request Body

```json
{
  "query": "Có bao nhiêu cảnh báo hôm nay?",
  "context": {
    "department": "Sản xuất",
    "user_role": "manager"
  },
  "execute_queries": true,
  "include_data": true
}
```

#### Parameters

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `query` | string | ✅ Yes | - | Câu hỏi bằng tiếng Việt tự nhiên |
| `context` | object | ❌ No | `{}` | Context bổ sung (department, date range, etc) |
| `execute_queries` | boolean | ❌ No | `true` | Có execute SQL queries không |
| `include_data` | boolean | ❌ No | `true` | Có include query results không |

### Response (200 OK)

```json
{
  "intent": "alert_stats|worker_analysis",
  "query_analysis": {
    "time_range": "1d",
    "department": null,
    "entities": ["alerts"]
  },
  "sql_queries": [
    {
      "query": "SELECT COUNT(*) as alert_count FROM alerts WHERE triggered_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)",
      "description": "Đếm số cảnh báo trong ngày hôm nay",
      "parameters": {}
    }
  ],
  "natural_language_response": "📊 Hôm nay có **4 cảnh báo** được ghi nhận.\n   ⚠️ Mức độ cảnh báo: Trung bình - cần xem xét nguyên nhân",
  "insights": [
    "⚠️ Có 4 cảnh báo - cao hơn mức bình thường (3-5/ngày), cần theo dõi"
  ],
  "recommendations": [
    {
      "action": "Kiểm tra chi tiết các cảnh báo để đảm bảo đã xử lý đúng quy trình",
      "priority": "MEDIUM",
      "impact": "Đảm bảo tuân thủ quy trình an toàn",
      "reasoning": "Có 4 cảnh báo cần verify đã được xử lý",
      "timeline": "Cuối tuần"
    }
  ],
  "follow_up_questions": [
    "Xu hướng 7 ngày qua như thế nào?",
    "Phân bố theo loại cảnh báo (FALL, NO_HELMET, etc)?",
    "Những công nhân nào có nhiều cảnh báo nhất?"
  ],
  "data": {
    "results": [...],
    "total_queries": 1,
    "successful_queries": 1,
    "executed_at": "2025-11-20T20:48:15.441282"
  },
  "metrics": {
    "alert_count_sum": 4,
    "alert_count_avg": 4.0,
    "alert_count_min": 4,
    "alert_count_max": 4
  },
  "metadata": {
    "confidence": 1.0,
    "data_freshness": "real-time",
    "execution_time_estimate": "1 queries",
    "total_rows": 1,
    "query_success_rate": "1/1"
  },
  "generated_at": "2025-11-20T20:48:15.171761"
}
```

### Example Queries

```bash
# Example 1: Simple count
curl -X POST "http://localhost:8000/api/llm/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Có bao nhiêu cảnh báo hôm nay?"
  }'

# Example 2: Department-specific
curl -X POST "http://localhost:8000/api/llm/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Công nhân nào ở phòng Sản xuất có nhiều cảnh báo nhất tuần này?",
    "context": {"department": "Sản xuất"}
  }'

# Example 3: Only SQL generation (no execution)
curl -X POST "http://localhost:8000/api/llm/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Top 5 công nhân có nhiều ngã té nhất tháng này",
    "execute_queries": false
  }'
```

### Use Cases

✅ Dashboard analytics - real-time queries  
✅ Chatbot integration - conversational analytics  
✅ Ad-hoc reporting - quick data exploration  
✅ SQL generation - for non-technical users  

---

## 2. POST `/api/llm/insights`

**Mục đích**: Tự động tạo insights từ dữ liệu (trends, anomalies)

### Request Body

```json
{
  "time_range": "30d",
  "department": "Xây dựng",
  "insight_types": ["trends", "anomalies"]
}
```

#### Parameters

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `time_range` | string | ❌ No | `"30d"` | `7d`, `30d`, `90d`, `custom` |
| `department` | string | ❌ No | `null` | Filter by department |
| `insight_types` | array | ❌ No | `["trends", "anomalies"]` | Types of insights to generate |

### Response (200 OK)

```json
{
  "insights": [
    {
      "type": "trend",
      "title": "Tăng 40% cảnh báo FALL trong 30 ngày",
      "description": "Số vụ ngã té tăng từ 10 lên 14 incidents/tuần",
      "severity": "HIGH",
      "data_points": [...],
      "recommendation": "Kiểm tra hệ thống phòng ngã và tăng training"
    },
    {
      "type": "anomaly",
      "title": "Spike bất thường vào 15/11",
      "description": "8 cảnh báo trong 1 ngày (cao gấp 3x bình thường)",
      "severity": "CRITICAL",
      "root_cause": "Công trình mới bắt đầu, chưa có briefing an toàn"
    }
  ],
  "summary": {
    "total_insights": 2,
    "critical": 1,
    "high": 1,
    "time_range": "30d"
  }
}
```

### Use Cases

✅ Proactive monitoring - detect issues before they escalate  
✅ Executive dashboards - high-level trends  
✅ Weekly reports automation  

---

## 3. POST `/api/llm/root-cause-analysis`

**Mục đích**: Phân tích nguyên nhân sâu xa của một incident cụ thể

### Request Body

```json
{
  "alert_id": 123,
  "include_context": true
}
```

#### Parameters

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `alert_id` | integer | ✅ Yes | - | ID của alert cần phân tích |
| `include_context` | boolean | ❌ No | `true` | Bao gồm lịch sử worker và location data |

### Response (200 OK)

```json
{
  "alert_id": 123,
  "alert_details": {
    "alert_type": "FALL",
    "severity": "CRITICAL",
    "full_name": "Nguyễn Văn A",
    "department": "Xây dựng",
    "triggered_at": "2025-11-20T14:30:00"
  },
  "root_cause_analysis": {
    "summary": "Phân tích sự cố FALL (CRITICAL) của Nguyễn Văn A (Xây dựng):\n\n🔴 NGUYÊN NHÂN TRỰC TIẾP: Phát hiện ngã té\n\n📊 LỊCH SỬ: Công nhân này có 3 incidents trong 30 ngày qua\n  Phân loại: FALL=2, ABNORMAL=1\n\n📍 VỊ TRÍ: Khu vực này có 8 incidents trong 90 ngày (hotspot)",
    "insights": [
      "⚠️ PATTERN: Công nhân có 3 incidents trong 30 ngày - high risk individual",
      "🔄 REPEATED: FALL xảy ra 2 lần - cần can thiệp",
      "🔥 HOTSPOT: Vị trí này có 8 incidents (3 critical) - khu vực nguy hiểm"
    ],
    "recommendations": [
      {
        "action": "Training khẩn cấp về an toàn làm việc trên cao cho Nguyễn Văn A",
        "priority": "CRITICAL",
        "impact": "Giảm 70% rủi ro tai nạn nghiêm trọng",
        "reasoning": "Công nhân có 2 vụ FALL trong 30 ngày - very high risk",
        "timeline": "Trong vòng 24h"
      },
      {
        "action": "Kiểm tra và nâng cấp hệ thống phòng ngã tại khu vực hotspot",
        "priority": "HIGH",
        "impact": "Giảm 50% incidents tại location",
        "reasoning": "Khu vực này có 8 incidents với 3 critical - cần cải thiện ngay",
        "timeline": "Trong tuần này"
      }
    ]
  }
}
```

### Use Cases

✅ Incident investigation - sau khi có sự cố nghiêm trọng  
✅ Safety audits - identify systemic issues  
✅ Compliance reporting - detailed analysis for regulators  

---

## 4. POST `/api/llm/generate-report`

**Mục đích**: Tạo báo cáo tự động (weekly, monthly, quarterly) với full analysis

### Request Body

```http
POST /api/llm/generate-report?report_type=weekly&time_range=7d&audience=management&department=Sản xuất
```

#### Query Parameters

| Parameter | Type | Required | Default | Options | Description |
|-----------|------|----------|---------|---------|-------------|
| `report_type` | string | ❌ No | `"weekly"` | `weekly`, `monthly`, `quarterly`, `incident` | Loại báo cáo |
| `time_range` | string | ❌ No | `"7d"` | `7d`, `30d`, `90d` | Khoảng thời gian |
| `audience` | string | ❌ No | `"management"` | `management`, `technical`, `regulatory` | Đối tượng đọc |
| `department` | string | ❌ No | `null` | Any dept name | Filter by department |

### Response (200 OK)

```json
{
  "report_type": "weekly",
  "report_markdown": "# BÁOCÁO AN TOÀN LAO ĐỘNG - WEEKLY\n\n**Thời gian**: 7d...",
  "data": {
    "results": [...],
    "total_queries": 3,
    "successful_queries": 3
  },
  "charts": null,
  "summary": {
    "time_range": "7d",
    "department": "Sản xuất",
    "total_incidents": 6,
    "key_findings": [
      "⚠️ Có 6 cảnh báo - cao hơn mức bình thường",
      "Công nhân Lê Văn Cường có 4/6 alerts (high-risk)"
    ],
    "top_recommendations": [
      {
        "action": "Training khẩn cấp cho Lê Văn Cường",
        "priority": "CRITICAL",
        "impact": "Giảm 70% rủi ro tai nạn",
        "reasoning": "Công nhân có 4 alerts trong 7 ngày"
      }
    ]
  },
  "generated_at": "2025-11-20T20:53:49.076355"
}
```

### Report Markdown Structure

```markdown
# BÁOCÁO AN TOÀN LAO ĐỘNG - WEEKLY

**Thời gian**: 7d
**Phòng ban**: Sản xuất
**Đối tượng**: management
**Ngày tạo**: 20/11/2025 20:53

---

## 📊 Executive Summary

Trong 7d, phòng ban Sản xuất ghi nhận **6 cảnh báo**.
⚠️ **Mức độ: Trung bình** - cần xem xét nguyên nhân.

⚠️ **1 công nhân high-risk** (≥3 cảnh báo): Lê Văn Cường

📊 **Loại cảnh báo phổ biến**: FALL (3 lần)
  - 🚨 FALL: 3 vụ ngã té cần xử lý ngay

---

## 🔍 Chi Tiết Phân Tích

### Insights Chính

- ⚠️ Có 6 cảnh báo - cao hơn mức bình thường (3-5/ngày), cần theo dõi
- Phòng ban có nhiều sự cố nhất: Sản xuất (6 incidents)

### Số Liệu Quan Trọng

**Workers with Alerts**:
  - Lê Văn Cường: 4 alerts (FALL, ABNORMAL, ABNORMAL, FALL)
  - Phạm Thị Dung: 1 alerts (FALL)

**Department Incidents**:
  - Sản xuất: 6 incidents

- **Đếm số vi phạm vùng an toàn (OUT_OF_ZONE)**: 5 violation_count

---

## 🎯 Khuyến Nghị

### 1. [CRITICAL] Training khẩn cấp cho Lê Văn Cường
**Tác động**: Giảm 70% rủi ro tai nạn
**Lý do**: Công nhân có 4 alerts trong 7 ngày
**Timeline**: Trong vòng 24h

### 2. [HIGH] Kiểm tra hệ thống phòng ngã
**Tác động**: Ngăn chặn tai nạn nghiêm trọng
**Lý do**: Đã có 3 vụ ngã té
**Timeline**: Trong tuần này

---

## ✅ Action Items

1. [ ] **[CRITICAL]** Training khẩn cấp cho Lê Văn Cường
2. [ ] **[HIGH]** Kiểm tra hệ thống phòng ngã
```

### Use Cases

✅ Weekly safety meetings - automated reports  
✅ Management dashboards - executive summaries  
✅ Regulatory compliance - audit-ready reports  
✅ Email automation - scheduled report generation  

### Example cURL

```bash
# Weekly report for management
curl -X POST "http://localhost:8000/api/llm/generate-report?report_type=weekly&time_range=7d&audience=management&department=Sản%20xuất"

# Monthly technical report
curl -X POST "http://localhost:8000/api/llm/generate-report?report_type=monthly&time_range=30d&audience=technical"

# Quarterly compliance report
curl -X POST "http://localhost:8000/api/llm/generate-report?report_type=quarterly&time_range=90d&audience=regulatory"
```

---

## 5. POST `/api/llm/generate-chart` 🆕

**Mục đích**: Tự động tạo chart configuration cho frontend visualization (Chart.js, Recharts, etc)

### Request Parameters

```http
POST /api/llm/generate-chart?chart_type=line&time_range=7d&group_by=time
```

#### Query Parameters

| Parameter | Type | Required | Default | Options | Description |
|-----------|------|----------|---------|---------|-------------|
| `chart_type` | string | ❌ No | `"auto"` | `auto`, `line`, `bar`, `pie`, `scatter`, `heatmap` | Loại biểu đồ |
| `query` | string | ❌ No | `null` | Natural language | Câu hỏi để LLM tự tạo chart |
| `data_source` | string | ❌ No | `null` | `alerts`, `workers`, `departments` | Nguồn dữ liệu |
| `time_range` | string | ❌ No | `"7d"` | `7d`, `30d`, `90d` | Khoảng thời gian |
| `department` | string | ❌ No | `null` | Any dept name | Filter by department |
| `group_by` | string | ❌ No | `null` | `time`, `department`, `alert_type`, `worker` | Group data by |

### Response (200 OK)

```json
{
  "chart_type": "line",
  "config": {
    "type": "line",
    "title": "Xu hướng cảnh báo 7 ngày qua",
    "data": {
      "labels": ["2025-11-14", "2025-11-15", "2025-11-16", "2025-11-17", "2025-11-18", "2025-11-19", "2025-11-20"],
      "datasets": [{
        "label": "Số cảnh báo",
        "data": [3, 5, 2, 8, 4, 6, 4],
        "borderColor": "rgb(255, 99, 132)",
        "tension": 0.1
      }]
    },
    "options": {
      "responsive": true,
      "plugins": {
        "legend": {"position": "top"},
        "title": {"display": true, "text": "Xu hướng cảnh báo 7 ngày qua"}
      }
    }
  },
  "sql_queries": [...],
  "data": {
    "results": [...]
  }
}
```

### Chart Types

#### 1. Line Chart (Trends over time)
```bash
curl -X POST "http://localhost:8000/api/llm/generate-chart?chart_type=line&data_source=alerts&time_range=7d&group_by=time"
```

**Best for**: Time series, trends, historical data

#### 2. Bar Chart (Comparisons)
```bash
curl -X POST "http://localhost:8000/api/llm/generate-chart?chart_type=bar&data_source=alerts&group_by=department"
```

**Best for**: Comparing categories, ranking

#### 3. Pie Chart (Distribution)
```bash
curl -X POST "http://localhost:8000/api/llm/generate-chart?chart_type=pie&data_source=alerts&group_by=alert_type"
```

**Best for**: Percentages, parts of a whole

#### 4. Auto Detection (Let AI decide)
```bash
curl -X POST "http://localhost:8000/api/llm/generate-chart" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Xu hướng cảnh báo 30 ngày qua theo từng loại"
  }'
```

**AI will choose**: Best chart type based on data structure

### Use Cases

✅ Dashboard widgets - dynamic chart generation  
✅ Real-time monitoring - live data visualization  
✅ Custom reports - user-defined charts  
✅ Mobile apps - lightweight chart configs  

### Integration Example (React + Chart.js)

```jsx
import { Line, Bar, Pie } from 'react-chartjs-2';

function DynamicChart({query}) {
  const [chartConfig, setChartConfig] = useState(null);
  
  useEffect(() => {
    fetch(`/api/llm/generate-chart?query=${encodeURIComponent(query)}`)
      .then(res => res.json())
      .then(data => setChartConfig(data.config));
  }, [query]);
  
  if (!chartConfig) return <Spinner />;
  
  const ChartComponent = {
    line: Line,
    bar: Bar,
    pie: Pie
  }[chartConfig.type];
  
  return <ChartComponent data={chartConfig.data} options={chartConfig.options} />;
}

// Usage
<DynamicChart query="Xu hướng cảnh báo 7 ngày qua" />
```

---

## 6. POST `/api/llm/compare` 🆕

**Mục đích**: So sánh performance giữa nhiều entities (departments, workers, locations, time periods)

### Request Body

```json
{
  "entity_type": "department",
  "entity_ids": ["Sản xuất", "Xây dựng", "Kho bãi"],
  "metrics": ["alert_count", "fall_incidents", "compliance_rate"],
  "time_range": "30d"
}
```

#### Parameters

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `entity_type` | string | ✅ Yes | - | `department`, `worker`, `location`, `time_period` |
| `entity_ids` | array | ✅ Yes | - | IDs/names của entities cần so sánh (min 2) |
| `metrics` | array | ❌ No | `["alert_count"]` | Metrics để so sánh |
| `time_range` | string | ❌ No | `"30d"` | Khoảng thời gian |

### Response (200 OK)

```json
{
  "entity_type": "department",
  "entities": ["Sản xuất", "Xây dựng", "Kho bãi"],
  "metrics": ["alert_count"],
  "time_range": "30d",
  "comparison": {
    "data": [
      {"department": "Sản xuất", "alert_count": 12},
      {"department": "Xây dựng", "alert_count": 18},
      {"department": "Kho bãi", "alert_count": 5}
    ],
    "best_performer": {
      "entity": "Kho bãi",
      "value": 5
    },
    "worst_performer": {
      "entity": "Xây dựng",
      "value": 18
    },
    "average": 11.67
  },
  "insights": [
    "📊 Xây dựng có 18 incidents - cao hơn Kho bãi (5) tới 13 cases",
    "⚠️ Xây dựng vượt trung bình 11.7 tới 54% - cần can thiệp khẩn cấp"
  ],
  "winner": {
    "entity": "Kho bãi",
    "value": 5
  },
  "attention_needed": {
    "entity": "Xây dựng",
    "value": 18
  }
}
```

### Comparison Types

#### 1. Department Comparison
```bash
curl -X POST "http://localhost:8000/api/llm/compare" \
  -H "Content-Type: application/json" \
  -d '{
    "entity_type": "department",
    "entity_ids": ["Sản xuất", "Xây dựng", "Kho bãi"],
    "metrics": ["alert_count"],
    "time_range": "30d"
  }'
```

#### 2. Worker Comparison
```bash
curl -X POST "http://localhost:8000/api/llm/compare" \
  -H "Content-Type: application/json" \
  -d '{
    "entity_type": "worker",
    "entity_ids": ["worker_1", "worker_5", "worker_10"],
    "metrics": ["alert_count", "compliance_rate"],
    "time_range": "7d"
  }'
```

#### 3. Time Period Comparison (This week vs Last week)
```bash
curl -X POST "http://localhost:8000/api/llm/compare" \
  -H "Content-Type: application/json" \
  -d '{
    "entity_type": "time_period",
    "entity_ids": ["this_week", "last_week"],
    "metrics": ["alert_count", "severity_distribution"],
    "time_range": "14d"
  }'
```

### Use Cases

✅ Performance benchmarking - compare team safety records  
✅ Resource allocation - identify underperforming units  
✅ Executive dashboards - high-level comparisons  
✅ Incentive programs - reward best performers  

---

## 8. POST `/api/llm/batch-query` 🆕

**Mục đích**: Execute nhiều queries trong một request duy nhất (efficient for dashboards)

### Request Body

```json
{
  "queries": [
    "Có bao nhiêu cảnh báo hôm nay?",
    "Top 5 công nhân có nhiều cảnh báo nhất tuần này",
    "Phân bố cảnh báo theo loại"
  ],
  "execute_queries": true,
  "combine_results": false
}
```

#### Parameters

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `queries` | array | ✅ Yes | - | List of natural language queries (max 10) |
| `execute_queries` | boolean | ❌ No | `true` | Execute SQL or just generate |
| `combine_results` | boolean | ❌ No | `false` | Combine all results into summary |

### Response (200 OK)

```json
{
  "total_queries": 3,
  "successful": 3,
  "failed": 0,
  "results": [
    {
      "query": "Có bao nhiêu cảnh báo hôm nay?",
      "response": {
        "intent": "alert_stats",
        "natural_language_response": "📊 Hôm nay có **4 cảnh báo**...",
        "insights": [...],
        "recommendations": [...],
        "data": {...}
      },
      "success": true
    },
    {
      "query": "Top 5 công nhân có nhiều cảnh báo nhất tuần này",
      "response": {
        "intent": "worker_analysis",
        "natural_language_response": "Top 5 công nhân:\n1. Lê Văn Cường: 4 alerts...",
        "data": {...}
      },
      "success": true
    },
    {
      "query": "Phân bố cảnh báo theo loại",
      "response": {
        "intent": "alert_stats",
        "data": {...}
      },
      "success": true
    }
  ]
}
```

### With Combined Results

```json
{
  "total_queries": 3,
  "successful": 3,
  "failed": 0,
  "individual_results": [...],
  "combined_summary": {
    "combined_insights": [
      "⚠️ Có 4 cảnh báo - cao hơn mức bình thường",
      "⚠️ Lê Văn Cường có 4 alerts - high-risk worker",
      "📊 FALL là loại cảnh báo phổ biến nhất (40%)"
    ],
    "combined_recommendations": [
      {
        "action": "Training khẩn cấp cho Lê Văn Cường",
        "priority": "CRITICAL"
      },
      {
        "action": "Kiểm tra hệ thống phòng ngã",
        "priority": "HIGH"
      }
    ],
    "total_data_points": 23
  }
}
```

### Use Cases

✅ Dashboard initialization - load all widgets in one call  
✅ Report generation - gather multiple metrics efficiently  
✅ Mobile apps - reduce network requests  
✅ Caching - batch process for cache population  

### Performance Benefits

| Approach | Requests | Total Time | Network Overhead |
|----------|----------|------------|------------------|
| Sequential (3 calls) | 3 | ~9s | 3x handshake |
| Batch (1 call) | 1 | ~5s | 1x handshake |
| **Improvement** | **-66%** | **-44%** | **-66%** |

### Example: Dashboard Init

```javascript
async function initDashboard() {
  const response = await fetch('/api/llm/batch-query', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({
      queries: [
        'Tổng số cảnh báo hôm nay',
        'Top 5 công nhân high-risk',
        'Phân bố theo loại cảnh báo',
        'Xu hướng 7 ngày qua',
        'Phòng ban có nhiều sự cố nhất'
      ],
      execute_queries: true,
      combine_results: true
    })
  });
  
  const data = await response.json();
  
  // Populate dashboard widgets
  updateWidget('alert-count', data.results[0]);
  updateWidget('high-risk-workers', data.results[1]);
  updateWidget('alert-distribution', data.results[2]);
  updateWidget('trend-chart', data.results[3]);
  updateWidget('department-ranking', data.results[4]);
  
  // Show combined insights
  showInsights(data.combined_summary.combined_insights);
}
```

---

## 9. DELETE `/api/llm/conversation-history`

**Mục đích**: Clear LLM conversation history (for testing or privacy)

### Request

```bash
curl -X DELETE "http://localhost:8000/api/llm/conversation-history"
```

### Response (200 OK)

```json
{
  "message": "Conversation history cleared"
}
```

---

## 📦 Response Models

### NLQueryResponse

```typescript
interface NLQueryResponse {
  intent: string;                          // e.g. "alert_stats|worker_analysis"
  query_analysis: {
    time_range?: string;                   // "1d", "7d", "30d"
    department?: string;
    entities: string[];                     // ["alerts", "workers"]
  };
  sql_queries: Array<{
    query: string;                          // SQL query
    description: string;                    // Vietnamese description
    parameters: Record<string, any>;        // Query parameters
  }>;
  natural_language_response: string;       // Human-readable Vietnamese response
  insights: string[];                      // Key findings
  recommendations: Array<{
    action: string;
    priority: "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
    impact: string;
    reasoning: string;
    timeline?: string;                      // "Trong vòng 24h"
  }>;
  follow_up_questions: string[];          // Suggested next questions
  data?: {
    results: Array<{
      description: string;
      rows: Array<Record<string, any>>;
      row_count: number;
      columns: string[];
      query: string;
      success: boolean;
    }>;
    total_queries: number;
    successful_queries: number;
    executed_at: string;                   // ISO 8601
  };
  metrics?: Record<string, number>;       // Calculated metrics
  metadata: {
    confidence: number;                     // 0.0 - 1.0
    data_freshness: string;                // "real-time"
    execution_time_estimate: string;       // "1 queries"
    total_rows: number;
    query_success_rate: string;            // "1/1"
  };
  generated_at: string;                    // ISO 8601
}
```

---

## ⚠️ Error Handling

### Error Response Format

```json
{
  "detail": "Error message in Vietnamese or English"
}
```

### Common Error Codes

| Status Code | Meaning | Common Causes |
|------------|---------|---------------|
| `400` | Bad Request | Invalid query format, missing required fields |
| `404` | Not Found | Alert ID not found (root-cause-analysis) |
| `500` | Internal Server Error | Database connection failed, LLM API error |

### Error Examples

```json
// 400 - Bad Request
{
  "detail": "Query không được để trống"
}

// 404 - Not Found
{
  "detail": "Alert ID 999 không tồn tại"
}

// 500 - Internal Error
{
  "detail": "OpenAI API error: Rate limit exceeded"
}
```

---

## 🎯 Best Practices

### 1. Query Optimization

✅ **DO**: Use specific time ranges
```json
{"query": "Cảnh báo hôm nay trong phòng Sản xuất"}
```

❌ **DON'T**: Vague queries
```json
{"query": "Cho tôi dữ liệu"}
```

### 2. Context Usage

✅ **DO**: Provide context for better results
```json
{
  "query": "Top công nhân có nhiều sự cố nhất",
  "context": {
    "department": "Xây dựng",
    "time_range": "30d",
    "user_role": "safety_manager"
  }
}
```

### 3. Caching Strategy

```javascript
// Cache frequent queries client-side
const cache = new Map();
const cacheKey = `query:${query}:${JSON.stringify(context)}`;

if (cache.has(cacheKey)) {
  return cache.get(cacheKey);
}

const result = await fetch('/api/llm/query', {...});
cache.set(cacheKey, result);
```

### 4. Error Handling

```javascript
try {
  const response = await fetch('/api/llm/query', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({query: userQuery})
  });
  
  if (!response.ok) {
    const error = await response.json();
    console.error('API Error:', error.detail);
    // Show user-friendly message
    showError('Không thể xử lý câu hỏi. Vui lòng thử lại.');
  }
  
  const data = await response.json();
  return data;
} catch (err) {
  console.error('Network error:', err);
  showError('Mất kết nối. Vui lòng kiểm tra mạng.');
}
```

### 5. Rate Limiting

```javascript
// Debounce user input
const debouncedQuery = debounce(async (query) => {
  const result = await callAPI(query);
  displayResults(result);
}, 500); // Wait 500ms after user stops typing
```

---

## 📊 Performance Metrics

### Response Times (P95)

| Endpoint | Average | P95 | P99 |
|----------|---------|-----|-----|
| `/query` (no execution) | 2s | 3s | 5s |
| `/query` (with execution) | 3s | 5s | 8s |
| `/insights` | 4s | 7s | 10s |
| `/root-cause-analysis` | 5s | 8s | 12s |
| `/generate-report` | 6s | 10s | 15s |
| `/generate-chart` 🆕 | 2s | 4s | 6s |
| `/compare` 🆕 | 3s | 5s | 8s |
| `/batch-query` 🆕 | 5s | 9s | 14s |

### Cost Analysis

| Operation | OpenAI Cost | DB Queries | Total Cost |
|-----------|------------|------------|------------|
| Simple query | $0.004 | 1-2 | ~$0.004 |
| Complex query | $0.006 | 3-5 | ~$0.006 |
| Root cause | $0.008 | 5-8 | ~$0.008 |
| Report generation | $0.010 | 8-12 | ~$0.010 |
| Chart generation 🆕 | $0.005 | 1-2 | ~$0.005 |
| Comparison 🆕 | $0.004 | 1-2 | ~$0.004 |
| Batch query (3x) 🆕 | $0.012 | 3-6 | ~$0.012 |

**Monthly estimate** (1000 queries/month): **~$5-10**

---

## 🔧 Integration Examples

### Python

```python
import requests

API_BASE = "http://localhost:8000"

def query_safety_data(query: str, department: str = None):
    response = requests.post(
        f"{API_BASE}/api/llm/query",
        json={
            "query": query,
            "context": {"department": department} if department else {}
        }
    )
    response.raise_for_status()
    return response.json()

# Usage
result = query_safety_data("Có bao nhiêu cảnh báo hôm nay?", department="Sản xuất")
print(result['natural_language_response'])
print(result['insights'])
```

### JavaScript/Node.js

```javascript
async function querySafetyData(query, context = {}) {
  const response = await fetch('http://localhost:8000/api/llm/query', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({query, context})
  });
  
  if (!response.ok) throw new Error('API call failed');
  return await response.json();
}

// Usage
const result = await querySafetyData(
  'Top 5 công nhân có nhiều cảnh báo nhất',
  {department: 'Xây dựng', time_range: '30d'}
);

console.log(result.natural_language_response);
console.log(result.recommendations);
```

### React Hook

```typescript
import {useState, useEffect} from 'react';

function useSafetyQuery(query: string, context?: any) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!query) return;

    setLoading(true);
    fetch('http://localhost:8000/api/llm/query', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({query, context})
    })
      .then(res => res.json())
      .then(setData)
      .catch(setError)
      .finally(() => setLoading(false));
  }, [query, context]);

  return {data, loading, error};
}

// Usage in component
function SafetyDashboard() {
  const {data, loading, error} = useSafetyQuery(
    'Có bao nhiêu cảnh báo hôm nay?'
  );

  if (loading) return <Spinner />;
  if (error) return <Error message={error.message} />;
  
  return (
    <div>
      <h2>{data.natural_language_response}</h2>
      <Insights items={data.insights} />
      <Recommendations items={data.recommendations} />
    </div>
  );
}
```

---

## 🐛 Troubleshooting

### Issue: "Connection pool exhausted"

**Cause**: Too many concurrent requests  
**Solution**: Implement request queuing or increase pool size in `database.py`

```python
# database/database.py
pool_size=10,  # Increase from 5 to 10
```

### Issue: "OpenAI rate limit exceeded"

**Cause**: Too many LLM calls per minute  
**Solution**: Implement exponential backoff

```python
from tenacity import retry, stop_after_attempt, wait_exponential

@retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=4, max=10))
def call_openai_with_retry():
    return llm_service.analyze_query(query)
```

### Issue: "Query timeout"

**Cause**: Complex query taking too long  
**Solution**: Reduce `LIMIT` in queries or optimize indexes

```sql
-- Add indexes
CREATE INDEX idx_alerts_triggered_at ON alerts(triggered_at);
CREATE INDEX idx_workers_department ON workers(department);
```

---

## 📞 Support & Contact

**Technical Support**: [your-email@company.com]  
**API Issues**: Open issue on [GitHub repo]  
**Documentation Updates**: Submit PR to update this file

---

## 🔄 Changelog

### v1.1.0 (2025-11-20) 🆕
- ✅ **NEW**: Chart generation API - auto create visualizations
- ✅ **NEW**: Comparison API - benchmark entities
- ✅ **NEW**: Batch query API - efficient multi-query execution
- ✅ Improved response formatting with icons and severity levels
- ✅ Added contextual follow-up questions
- ✅ Enhanced metadata with confidence scores

### v1.0.0 (2025-11-20)
- ✅ Initial release
- ✅ 4 main endpoints: query, insights, root-cause, reports
- ✅ GPT-4o-mini integration (95% cost reduction)
- ✅ Connection pooling (fixed max_connections errors)
- ✅ Vietnamese language support
- ✅ Auto insights and recommendations

---

**Last Updated**: November 20, 2025  
**API Version**: 1.0.0
