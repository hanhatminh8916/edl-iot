# Voice Assistant - Hướng dẫn sử dụng

## 🎤 Tính năng Voice Control đã được tích hợp!

Voice Assistant đã được tích hợp **trực tiếp vào dashboard** với các tính năng:

- ✅ **Speech-to-Text** (Web Speech API) - Nhận diện giọng nói tiếng Việt
- ✅ **Gemini 2.0 Flash** - AI xử lý ngôn ngữ tự nhiên
- ✅ **Function Calling** - Tự động gọi backend APIs
- ✅ **Text-to-Speech** - Đọc kết quả bằng giọng nói
- ✅ **Realtime Data** - Kết nối trực tiếp với Spring Boot backend

---

## 📋 Yêu cầu

1. **Browser hỗ trợ Web Speech API**:
   - ✅ Google Chrome (khuyến nghị)
   - ✅ Microsoft Edge
   - ✅ Safari (iOS/macOS)
   - ❌ Firefox (chưa hỗ trợ đầy đủ)

2. **Google AI API Key** (miễn phí):
   - Truy cập: https://ai.google.dev/gemini-api/docs/api-key
   - Click "Get API Key" → "Create API key"
   - Free tier: 1,500 requests/day

3. **Microphone permission**: Cho phép browser truy cập mic

---

## 🚀 Cách sử dụng

### Bước 1: Mở Dashboard

```
http://localhost:8080  (local)
hoặc
https://edl-safework-iot-bf3ee691c9f6.herokuapp.com  (Heroku)
```

### Bước 2: Nhấn nút Voice Assistant

- Góc dưới bên phải màn hình
- Icon: 🎤 (màu tím gradient)
- Hoặc nhấn phím tắt: **Alt + V**

### Bước 3: Nhập API Key (chỉ lần đầu)

- Paste API key từ Google AI Studio
- Click "Lưu"
- API key được lưu trong localStorage (không cần nhập lại)

### Bước 4: Bắt đầu nói

- Click vào nút 🎤 hoặc nhấn Alt+V
- Nói tiếng Việt vào microphone
- AI sẽ xử lý và trả lời bằng cả text + giọng nói

---

## 🎯 Các lệnh Voice hỗ trợ

### 1️⃣ Kiểm tra công nhân

```
"Có bao nhiêu công nhân đang online?"
"Hiển thị danh sách công nhân"
"Số công nhân hiện tại"
```

### 2️⃣ Kiểm tra cảnh báo

```
"Có cảnh báo nguy hiểm nào không?"
"Hiển thị 5 cảnh báo gần nhất"
"Có ai gặp sự cố không?"
```

### 3️⃣ Kiểm tra mũ bảo hộ

```
"Kiểm tra pin của mũ F4DD40BA2010"
"Trạng thái mũ F4DD40BA2010"
"Mũ F4DD40BA2010 còn bao nhiêu pin?"
```

### 4️⃣ Xem bản đồ

```
"Hiển thị vị trí công nhân trên bản đồ"
"Công nhân đang ở đâu?"
"Vị trí hiện tại của tất cả công nhân"
```

### 5️⃣ Tổng quan Dashboard

```
"Cho tôi xem tổng quan dashboard"
"Hiệu suất làm việc hôm nay thế nào?"
"Tổng hợp tình hình"
```

---

## ⚙️ Cấu hình nâng cao

### API Key Management

API key được lưu trong `localStorage`:
```javascript
localStorage.setItem('gemini_api_key', 'YOUR_KEY');
localStorage.getItem('gemini_api_key');
localStorage.removeItem('gemini_api_key'); // Xóa key
```

### Thay đổi ngôn ngữ

Mặc định: Tiếng Việt (`vi-VN`)

Để đổi sang tiếng Anh, sửa trong `voice-assistant.js`:
```javascript
this.recognition.lang = 'en-US'; // Thay vì 'vi-VN'
```

### Keyboard Shortcuts

- **Alt + V**: Toggle voice listening
- Click vào Quick Commands để test nhanh

---

## 🔧 Backend APIs được sử dụng

| Function | Endpoint | Mô tả |
|----------|----------|-------|
| `get_workers` | `/api/workers` | Danh sách công nhân |
| `get_recent_alerts` | `/api/alerts/recent?limit=X` | Cảnh báo gần đây |
| `get_helmet_status` | `/api/location/map-data-realtime` | Trạng thái mũ |
| `get_map_data` | `/api/location/map-data-realtime` | Vị trí realtime |
| `get_dashboard_overview` | `/api/dashboard/overview` | Tổng quan |

---

## 🎨 UI Components

### Floating Button
- Vị trí: Fixed bottom-right (20px, 20px)
- Kích thước: 60x60px
- Gradient tím: `#667eea → #764ba2`
- Animation: Pulse khi đang listening

### Voice Panel
- Width: 400px (responsive trên mobile)
- Sections:
  - API Key input (ẩn sau khi lưu)
  - Status display (icon + text)
  - Transcript display (User + AI)
  - Quick commands (4 buttons)

---

## 🐛 Troubleshooting

### Voice không hoạt động?

1. **Check browser support**:
   ```javascript
   console.log('Web Speech API:', 'SpeechRecognition' in window || 'webkitSpeechRecognition' in window);
   ```

2. **Microphone permission**: Settings → Privacy → Microphone

3. **HTTPS required**: Web Speech API chỉ hoạt động trên HTTPS hoặc localhost

### Gemini API error?

1. **Check API key**: Vào https://aistudio.google.com/app/apikey
2. **Quota exceeded**: Free tier = 1,500 requests/day
3. **CORS error**: Gemini API hỗ trợ CORS từ browser

### Backend API không response?

1. **Check backend running**: `http://localhost:8080/api/workers`
2. **CORS enabled**: Spring Boot đã config `@CrossOrigin`
3. **Network tab**: Kiểm tra DevTools → Network

---

## 📊 Demo Video

1. Mở dashboard: https://edl-safework-iot-bf3ee691c9f6.herokuapp.com
2. Click nút 🎤 (góc dưới phải)
3. Nhập API key (lần đầu)
4. Nói: "Có bao nhiêu công nhân đang online?"
5. AI sẽ:
   - Gọi API `/api/workers`
   - Đếm số lượng online/offline
   - Trả lời bằng tiếng Việt
   - Đọc kết quả bằng giọng nói

---

## 🚀 Deploy

### Build & Deploy

```bash
cd J:/IOT/BFE_forAIOT

# Build
mvn clean package -DskipTests

# Deploy to Heroku
git add .
git commit -m "Add Voice Assistant integration"
git push heroku main
```

### Files đã thêm:

- `src/main/resources/static/js/voice-assistant.js` (482 dòng)
- `src/main/resources/static/index.html` (updated)

### No backend changes required!

Voice Assistant hoạt động hoàn toàn trên **frontend**, gọi trực tiếp đến:
- Google Gemini API (từ browser)
- Spring Boot REST APIs (existing endpoints)

---

## 💰 Chi phí

- **Google Gemini API**: FREE (1,500 requests/day)
- **Web Speech API**: FREE (built-in browser)
- **Backend**: Sử dụng API endpoints hiện có

---

## 🎯 Kết luận

✅ Voice Assistant đã được tích hợp **native** vào dashboard  
✅ Không cần chạy server riêng hay ADK agent  
✅ Dùng công nghệ web chuẩn (Web Speech API)  
✅ AI mạnh mẽ từ Gemini 2.0 Flash  
✅ Function calling tự động gọi backend APIs  
✅ Hoạt động trên mọi thiết bị có microphone  

**Ready to use ngay bây giờ!** 🚀
