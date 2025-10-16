# HƯỚNG DẪN DEPLOY LÊN HEROKU

## Yêu cầu trước khi bắt đầu:
1. ✅ Tài khoản Heroku (miễn phí): https://signup.heroku.com/
2. ✅ Git đã cài đặt
3. ✅ Heroku CLI đã cài đặt: https://devcenter.heroku.com/articles/heroku-cli

---

## PHẦN 1: CÀI ĐẶT HEROKU CLI

### Windows (PowerShell):
```powershell
# Tải và cài đặt từ: https://devcenter.heroku.com/articles/heroku-cli
# Hoặc dùng winget:
winget install Heroku.HerokuCLI
```

### Kiểm tra cài đặt:
```powershell
heroku --version
# Kết quả: heroku/8.x.x win32-x64 node-v18.x.x
```

---

## PHẦN 2: KHỞI TẠO GIT REPOSITORY (nếu chưa có)

```powershell
# Di chuyển vào thư mục project
cd J:\IOT\BFE_forAIOT

# Khởi tạo Git (nếu chưa có)
git init

# Thêm tất cả files
git add .

# Commit
git commit -m "Initial commit for Heroku deployment"
```

---

## PHẦN 3: ĐĂNG NHẬP HEROKU

```powershell
heroku login
# Browser sẽ mở, đăng nhập vào Heroku
```

---

## PHẦN 4: TẠO APP HEROKU

```powershell
# Tạo app mới (tên app phải unique, Heroku sẽ tự sinh nếu không chỉ định)
heroku create edl-safework-iot

# Hoặc để Heroku tự sinh tên:
# heroku create
```

Kết quả sẽ hiển thị:
```
Creating ⬢ edl-safework-iot... done
https://edl-safework-iot-xxxxx.herokuapp.com/ | https://git.heroku.com/edl-safework-iot.git
```

---

## PHẦN 5: THÊM JAWSDB MYSQL ADD-ON

### Option A: JawsDB MySQL (Free tier: 5MB)
```powershell
heroku addons:create jawsdb:kitefin
```

### Option B: ClearDB MySQL (Free tier: 5MB)
```powershell
heroku addons:create cleardb:ignite
```

### Kiểm tra add-on đã được thêm:
```powershell
heroku addons
```

### Xem thông tin kết nối database:
```powershell
heroku config:get JAWSDB_URL
# Hoặc
heroku config:get CLEARDB_DATABASE_URL
```

---

## PHẦN 6: CẤU HÌNH ENVIRONMENT VARIABLES

### Lấy database URL:
```powershell
heroku config
```

Bạn sẽ thấy:
```
JAWSDB_URL: mysql://username:password@hostname:3306/database_name
```

### Set các biến môi trường (nếu cần):
```powershell
# Heroku tự động dùng JAWSDB_URL, nhưng nếu cần override:
heroku config:set SPRING_DATASOURCE_URL="jdbc:mysql://hostname:3306/database?useSSL=true"
heroku config:set SPRING_DATASOURCE_USERNAME="username"
heroku config:set SPRING_DATASOURCE_PASSWORD="password"
```

---

## PHẦN 7: CẬP NHẬT APPLICATION.PROPERTIES

Tạo file `application-production.properties` để Heroku tự động dùng database URL:

```properties
# Heroku sẽ tự inject DATABASE_URL
spring.datasource.url=${JDBC_DATABASE_URL}
spring.datasource.username=${JDBC_DATABASE_USERNAME}
spring.datasource.password=${JDBC_DATABASE_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
```

Hoặc cấu hình để Spring Boot tự parse JAWSDB_URL trong `application.properties`:

```properties
# application.properties đã có sẵn, chỉ cần đảm bảo:
spring.jpa.hibernate.ddl-auto=update
```

---

## PHẦN 8: BUILD PROJECT (Local test trước khi deploy)

```powershell
# Clean và build
mvn clean package -DskipTests

# Kiểm tra file JAR đã được tạo:
ls target\BFE_forAIOT-0.0.1-SNAPSHOT.jar
```

Nếu thành công, bạn sẽ thấy:
```
BUILD SUCCESS
```

---

## PHẦN 9: DEPLOY LÊN HEROKU

```powershell
# Push code lên Heroku
git push heroku main

# Nếu branch của bạn là master:
# git push heroku master
```

### Theo dõi quá trình deploy:
```
remote: -----> Building on the Heroku-22 stack
remote: -----> Using buildpack: heroku/java
remote: -----> Java app detected
remote: -----> Installing JDK 17... done
remote: -----> Executing Maven
remote:        [INFO] BUILD SUCCESS
remote: -----> Discovering process types
remote:        Procfile declares types -> web
remote: -----> Compressing...
remote: -----> Launching...
remote:        https://edl-safework-iot.herokuapp.com/ deployed to Heroku
```

---

## PHẦN 10: KIỂM TRA VÀ MỞ APP

### Mở app trong browser:
```powershell
heroku open
```

### Xem logs real-time:
```powershell
heroku logs --tail
```

### Kiểm tra status:
```powershell
heroku ps
```

Kết quả:
```
=== web (Free): java -Dserver.port=$PORT -jar target/BFE_forAIOT-0.0.1-SNAPSHOT.jar (1)
web.1: up 2025/10/16 15:30:00 +0700 (~ 1m ago)
```

---

## PHẦN 11: TRUY CẬP ỨNG DỤNG

Sau khi deploy thành công, truy cập:

```
https://edl-safework-iot-xxxxx.herokuapp.com/
https://edl-safework-iot-xxxxx.herokuapp.com/location.html
https://edl-safework-iot-xxxxx.herokuapp.com/api/dashboard/map-data
```

---

## PHẦN 12: QUẢN LÝ DATABASE

### Kết nối vào MySQL:
```powershell
heroku config:get JAWSDB_URL
# Copy connection string và dùng HeidiSQL hoặc MySQL Workbench
```

### Chạy SQL commands:
```powershell
# Cài MySQL client nếu chưa có
# Sau đó kết nối:
mysql -h hostname -u username -p database_name
```

### Xem tables:
```sql
SHOW TABLES;
SELECT * FROM workers;
SELECT * FROM helmets;
SELECT * FROM alerts;
```

---

## PHẦN 13: CẬP NHẬT CODE (Deploy lại)

Sau khi sửa code:

```powershell
# 1. Add changes
git add .

# 2. Commit
git commit -m "Update: Thêm feature XYZ"

# 3. Push lên Heroku
git push heroku main

# 4. Xem logs để kiểm tra
heroku logs --tail
```

---

## PHẦN 14: SCALE DYNOS (nếu cần)

```powershell
# Xem số dynos hiện tại
heroku ps

# Scale up (nếu cần nhiều workers)
heroku ps:scale web=2

# Scale down
heroku ps:scale web=1

# Restart app
heroku restart
```

---

## TROUBLESHOOTING - XỬ LÝ LỖI THƯỜNG GẶP

### 1. Lỗi "Application error" khi mở app:
```powershell
# Xem logs để tìm lỗi
heroku logs --tail

# Kiểm tra Procfile
cat Procfile

# Kiểm tra system.properties
cat system.properties
```

### 2. Lỗi "No web processes running":
```powershell
# Scale web dyno lên
heroku ps:scale web=1
```

### 3. Lỗi build Maven:
```powershell
# Build local trước để test
mvn clean package -DskipTests

# Xem log chi tiết
heroku logs --tail
```

### 4. Lỗi kết nối database:
```powershell
# Kiểm tra JAWSDB_URL
heroku config:get JAWSDB_URL

# Restart app
heroku restart

# Xem logs
heroku logs --tail | Select-String "MySQL"
```

### 5. Lỗi "Port already in use":
```powershell
# Heroku tự động set port qua $PORT
# Đảm bảo application.properties có:
server.port=${PORT:8080}
```

### 6. App bị sleep sau 30 phút không dùng (Free tier):
```
Đây là giới hạn của Heroku free tier.
Lần truy cập đầu tiên sau khi sleep sẽ mất 10-20s để wake up.
```

---

## PHẦN 15: MONITORING

### Dashboard:
```powershell
heroku open
# Click "More" -> "View logs"
```

### Metrics (nếu có add-on):
```powershell
heroku addons:create papertrail
heroku addons:open papertrail
```

---

## PHẦN 16: CUSTOM DOMAIN (nếu có domain riêng)

```powershell
# Thêm domain
heroku domains:add www.edl-safework.com

# Xem DNS target
heroku domains

# Cấu hình CNAME record tại nhà cung cấp domain:
# CNAME: www -> edl-safework-iot.herokuapp.com
```

---

## PHẦN 17: SSL/HTTPS

Heroku tự động cung cấp SSL certificate cho:
- `*.herokuapp.com` domains
- Custom domains (với ACM)

Không cần cấu hình gì thêm! 🎉

---

## QUICK REFERENCE - LỆNH THƯỜNG DÙNG

```powershell
# Logs
heroku logs --tail
heroku logs --tail --dyno web

# Restart
heroku restart

# Run commands on Heroku
heroku run bash
heroku run java -version

# Database
heroku config:get JAWSDB_URL

# List apps
heroku apps

# Open app
heroku open

# Config vars
heroku config
heroku config:set KEY=VALUE
heroku config:unset KEY

# Releases
heroku releases
heroku rollback v123

# Add-ons
heroku addons
heroku addons:open jawsdb
```

---

## COST - CHI PHÍ

### Free Tier (Eco Dynos):
- ✅ 1000 dyno hours/month (đủ cho 1 app chạy 24/7)
- ✅ JawsDB MySQL 5MB free
- ✅ SSL certificate tự động
- ⚠️ App sleep sau 30 phút không dùng
- ⚠️ Wake up time: 10-20 giây

### Paid Tier (Nếu cần):
- Basic Dyno: $7/month (không sleep)
- JawsDB Kitefin: $10/month (1GB storage)

---

## NEXT STEPS - SAU KHI DEPLOY

1. ✅ Test tất cả endpoints
2. ✅ Kiểm tra bản đồ có hiển thị markers không
3. ✅ Verify database có lưu dữ liệu không
4. ✅ Setup CI/CD với GitHub Actions (nếu cần)
5. ✅ Configure monitoring và alerting
6. ✅ Backup database định kỳ

---

## BACKUP DATABASE

```bash
# Export database từ Heroku
heroku run "mysqldump -h hostname -u username -p database > backup.sql"

# Hoặc dùng HeidiSQL để export
```

---

## Chúc bạn deploy thành công! 🚀
Nếu gặp lỗi gì, hãy chạy `heroku logs --tail` và gửi log cho tôi!
