# 🚀 TỔNG QUAN DEPLOY HEROKU

## ✅ ĐÃ HOÀN THÀNH

### 1. Chuẩn bị project
- ✅ Git repository initialized
- ✅ Procfile created (chỉ định cách chạy app)
- ✅ system.properties created (Java 17)
- ✅ HerokuDataSourceConfig.java (parse JAWSDB_URL)

### 2. Heroku setup
- ✅ Đăng nhập Heroku CLI
- ✅ Tạo app: `edl-safework-iot`
- ✅ URL: https://edl-safework-iot-bf3ee691c9f6.herokuapp.com/
- ✅ Git remote: https://git.heroku.com/edl-safework-iot.git

### 3. Database setup
- ✅ JawsDB MySQL add-on created
- ✅ Database: `jawsdb-transparent-45951`
- ✅ Plan: kitefin (free tier)

### 4. Database credentials
```
Host: tk3mehkfmmrhjg0b.cbetxkdyhwsb.us-east-1.rds.amazonaws.com
Username: i299o9m20iz3rx3f
Password: ebxs5nknkrgk349h
Port: 3306
Database: bsrxqa8k23608y3y
```

### 5. Deployment
- ✅ Code committed to Git
- 🔄 Đang deploy: `git push heroku master` (đang chạy)

---

## 🔄 ĐANG THỰC HIỆN

Maven đang download dependencies và build project...

---

## 📋 SAU KHI DEPLOY XONG

### Kiểm tra app
```powershell
# Mở app trong browser
heroku open --app edl-safework-iot

# Xem logs
heroku logs --tail --app edl-safework-iot

# Kiểm tra status
heroku ps --app edl-safework-iot
```

### URLs để truy cập
```
Home: https://edl-safework-iot-bf3ee691c9f6.herokuapp.com/
Dashboard: https://edl-safework-iot-bf3ee691c9f6.herokuapp.com/index.html
Map: https://edl-safework-iot-bf3ee691c9f6.herokuapp.com/location.html
API: https://edl-safework-iot-bf3ee691c9f6.herokuapp.com/api/dashboard/map-data
```

---

## 🎯 KIỂM TRA DATABASE

### Kết nối bằng HeidiSQL
```
Network type: MySQL (TCP/IP)
Hostname: tk3mehkfmmrhjg0b.cbetxkdyhwsb.us-east-1.rds.amazonaws.com
User: i299o9m20iz3rx3f
Password: ebxs5nknkrgk349h
Port: 3306
Database: bsrxqa8k23608y3y
```

### Hoặc dùng MySQL CLI
```bash
mysql -h tk3mehkfmmrhjg0b.cbetxkdyhwsb.us-east-1.rds.amazonaws.com -u i299o9m20iz3rx3f -p bsrxqa8k23608y3y
# Nhập password: ebxs5nknkrgk349h
```

---

## 📊 CẤU TRÚC DATABASE

Sau khi deploy, Spring Boot sẽ tự động tạo tables:
- `workers` - Danh sách công nhân
- `helmets` - Thông tin mũ bảo hộ
- `helmet_data` - Dữ liệu từ sensors
- `alerts` - Cảnh báo

DataInitializer sẽ tạo 5 workers, 5 helmets, 3 alerts mẫu.

---

## 🔧 CẬP NHẬT SAU NÀY

```powershell
# Sửa code
git add .
git commit -m "Your message"

# Deploy lại
git push heroku master

# Xem logs
heroku logs --tail --app edl-safework-iot

# Restart app (nếu cần)
heroku restart --app edl-safework-iot
```

---

## 💰 CHI PHÍ

### Free Tier
- ✅ 1000 dyno hours/month
- ✅ JawsDB 5MB
- ✅ SSL certificate
- ⚠️ App sleep sau 30 phút không dùng

---

## 🐛 TROUBLESHOOTING

### Nếu app error
```powershell
heroku logs --tail --app edl-safework-iot
```

### Nếu database error
```powershell
heroku config --app edl-safework-iot
```

### Restart app
```powershell
heroku restart --app edl-safework-iot
```

---

## 📝 GHI CHÚ

- App URL: https://edl-safework-iot-bf3ee691c9f6.herokuapp.com/
- Database: MySQL trên AWS RDS (JawsDB)
- Auto-deploy: Khi push code lên `heroku master`
- Dữ liệu: Persistent (không mất khi restart)

---

**Hãy đợi deploy hoàn tất và kiểm tra logs!** 🚀
