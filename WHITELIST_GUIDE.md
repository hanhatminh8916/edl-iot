# HƯỚNG DẪN WHITELIST IP CHO JAWSDB MYSQL

## Tại sao cần Whitelist?
JawsDB MySQL trên AWS chỉ cho phép kết nối từ các IP được whitelist để bảo mật.

## Cách lấy IP công khai của bạn:

### Option 1: Qua website
1. Mở trình duyệt, vào: https://whatismyipaddress.com/
2. Copy địa chỉ IPv4 (ví dụ: 123.456.789.012)

### Option 2: Qua PowerShell
```powershell
Invoke-RestMethod -Uri "https://api.ipify.org?format=json"
```

### Option 3: Qua Command Line
```powershell
curl https://api.ipify.org
```

## Cách thêm IP vào Whitelist JawsDB:

### Nếu dùng Heroku + JawsDB Add-on:
1. Đăng nhập Heroku Dashboard: https://dashboard.heroku.com/
2. Chọn app của bạn
3. Vào tab "Resources"
4. Click vào "JawsDB MySQL"
5. Trong JawsDB Dashboard, tìm "Connection Settings" hoặc "Whitelist"
6. Click "Add IP Address"
7. Nhập IP của bạn (hoặc dùng 0.0.0.0/0 cho phép tất cả - không khuyến khích)
8. Click Save

### Nếu dùng JawsDB trực tiếp:
1. Đăng nhập: https://www.jawsdb.com/portal/
2. Chọn database instance của bạn
3. Tìm tab "Security" hoặc "Whitelist"
4. Thêm IP công khai của bạn
5. Click "Add" hoặc "Save"

### Nếu không tìm thấy Whitelist Settings:
JawsDB free tier có thể không hỗ trợ whitelist tùy chỉnh. Trong trường hợp này:
- Database đã mở cho tất cả IP (public access)
- Bạn có thể kết nối trực tiếp mà không cần whitelist
- Bảo mật dựa vào username/password mạnh

## Kiểm tra kết nối từ máy local:

### Sử dụng MySQL Client (nếu đã cài):
```bash
mysql -h l9dwvv6j64h1hpu1.cbetxkdyhwsb.us-east-1.rds.amazonaws.com -P 3306 -u zr4jwtp3sfgeb5sd -p
# Nhập password: krm9u1ielgncvzvu
```

### Sử dụng Telnet để test port:
```powershell
Test-NetConnection -ComputerName l9dwvv6j64h1hpu1.cbetxkdyhwsb.us-east-1.rds.amazonaws.com -Port 3306
```

Nếu thành công, bạn sẽ thấy:
```
TcpTestSucceeded : True
```

### Sử dụng HeidiSQL (GUI Tool):
1. Tải HeidiSQL: https://www.heidisql.com/download.php
2. Network type: MySQL (TCP/IP)
3. Hostname: l9dwvv6j64h1hpu1.cbetxkdyhwsb.us-east-1.rds.amazonaws.com
4. User: zr4jwtp3sfgeb5sd
5. Password: krm9u1ielgncvzvu
6. Port: 3306
7. Database: gve28c5a0gz2mc4b
8. Click "Open"

## Lưu ý quan trọng:

### 1. IP động (Dynamic IP):
Nếu dùng internet gia đình, IP có thể thay đổi khi restart router.
Giải pháp:
- Thêm lại IP mới vào whitelist mỗi khi thay đổi
- Hoặc dùng VPN với static IP
- Hoặc dùng AWS EC2 với Elastic IP

### 2. IP tĩnh cho production:
Khi deploy lên server thật (Heroku, AWS, etc.):
- Lấy IP của server đó
- Thêm vào whitelist
- Heroku dyno IPs thay đổi, nên cần whitelist nhiều IP hoặc dùng private space

### 3. Bảo mật:
- KHÔNG dùng 0.0.0.0/0 (cho phép tất cả IP) trong production
- Chỉ whitelist IP cần thiết
- Đổi password định kỳ
- Bật SSL connection (đã config trong application.properties: useSSL=true)

## Troubleshooting:

### Lỗi "Access denied for user":
- Kiểm tra lại username/password
- Chắc chắn username/password không có khoảng trắng thừa

### Lỗi "Cannot connect to MySQL server":
- Kiểm tra IP đã được whitelist chưa
- Kiểm tra firewall local có block port 3306 không:
  ```powershell
  netsh advfirewall firewall add rule name="MySQL" dir=out action=allow protocol=TCP localport=3306
  ```

### Lỗi "Too many connections":
- JawsDB free tier giới hạn số kết nối đồng thời
- Giảm `spring.datasource.hikari.maximum-pool-size` xuống 3-5

### Test kết nối nhanh:
```powershell
# Trong PowerShell tại thư mục project
mvn spring-boot:run
```
Xem log, nếu thấy:
- "HikariPool-1 - Start completed" → Kết nối thành công ✅
- "Communications link failure" → Không kết nối được, cần whitelist ❌

## Sau khi whitelist thành công:

1. Khởi động lại ứng dụng Spring Boot
2. Database tables sẽ tự động được tạo (ddl-auto=update)
3. DataInitializer sẽ chạy và tạo dữ liệu mẫu
4. Mở http://localhost:8080/location.html để xem bản đồ
5. Dữ liệu sẽ được lưu vĩnh viễn trên MySQL cloud 🎉

## Kiểm tra dữ liệu trong MySQL:
```sql
USE gve28c5a0gz2mc4b;
SHOW TABLES;
SELECT * FROM workers;
SELECT * FROM helmets;
SELECT * FROM alerts;
```
