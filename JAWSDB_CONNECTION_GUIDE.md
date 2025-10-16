# 🔌 HƯỚNG DẪN KẾT NỐI JAWSDB MYSQL

## 📋 Thông Tin Database Của Bạn:
```
Username: zr4jwtp3sfgeb5sd
Password: krm9u1ielgncvzvu
Port:     3306
Database: gve28c5a0gz2mc4b
```

## 🔍 Bước 1: Lấy Hostname Chính Xác

### Option 1: JawsDB Dashboard
1. Truy cập: https://www.jawsdb.com/
2. Login với account của bạn
3. Chọn database `gve28c5a0gz2mc4b`
4. Copy **Connection String** hoặc **Host**

### Option 2: Heroku Dashboard (nếu dùng Heroku)
1. Truy cập: https://dashboard.heroku.com/
2. Chọn app của bạn
3. Vào tab **Resources**
4. Click vào **JawsDB MySQL**
5. Copy **Connection Info**

### Option 3: Heroku CLI
```bash
heroku config:get JAWSDB_URL -a your-app-name
```

Output sẽ có dạng:
```
mysql://username:password@hostname:3306/database
```

## 🎯 Hostname JawsDB Phổ Biến:

JawsDB thường có các hostname sau:

1. **US East (Virginia):**
   ```
   xxx.cbetxkdyhwsb.us-east-1.rds.amazonaws.com
   ```

2. **EU West (Ireland):**
   ```
   xxx.chzain6bshtu.eu-west-1.rds.amazonaws.com
   ```

3. **Shared Hosting:**
   ```
   xxx.jawsdb.com
   ```

## 🔧 Bước 2: Cập Nhật application.properties

Sau khi có hostname chính xác, mở file:
```
j:\IOT\BFE_forAIOT\src\main\resources\application.properties
```

Thay dòng này:
```properties
spring.datasource.url=jdbc:mysql://[HOSTNAME_CỦA_BẠN]:3306/gve28c5a0gz2mc4b?useSSL=true&serverTimezone=UTC
```

Ví dụ:
```properties
spring.datasource.url=jdbc:mysql://abc123xyz.cbetxkdyhwsb.us-east-1.rds.amazonaws.com:3306/gve28c5a0gz2mc4b?useSSL=true&serverTimezone=UTC
```

## 🧪 Bước 3: Test Connection

### Method 1: Chạy script test
```bash
cd j:\IOT\BFE_forAIOT
.\test-jawsdb-connection.bat
```

### Method 2: Test bằng MySQL Client
```bash
mysql -h [HOSTNAME] -P 3306 -u zr4jwtp3sfgeb5sd -p gve28c5a0gz2mc4b
# Nhập password: krm9u1ielgncvzvu
```

### Method 3: Test bằng Spring Boot
```bash
mvn spring-boot:run
```

## 🚀 Bước 4: Nếu Không Kết Nối Được JawsDB

### Sử dụng H2 Database để test local:

Uncomment các dòng H2 trong `application.properties`:
```properties
# Comment JawsDB config
# spring.datasource.url=jdbc:mysql://...

# Uncomment H2 config
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

Sau đó chạy:
```bash
mvn spring-boot:run
```

Truy cập H2 Console: http://localhost:8080/h2-console

## 📞 Liên Hệ JawsDB Support

Nếu vẫn không kết nối được:
- Email: support@jawsdb.com
- Website: https://www.jawsdb.com/support

## ✅ Checklist

- [ ] Đã lấy hostname chính xác từ JawsDB Dashboard
- [ ] Đã cập nhật application.properties
- [ ] Đã test ping hostname thành công
- [ ] Đã chạy mvn spring-boot:run không lỗi
- [ ] Application khởi động thành công trên port 8080

## 🔗 Connection String Format

JawsDB URL format:
```
mysql://username:password@hostname:port/database
```

Spring Boot JDBC URL format:
```
jdbc:mysql://hostname:port/database?useSSL=true&serverTimezone=UTC
```

Conversion example:
```
JawsDB URL:
mysql://zr4jwtp3sfgeb5sd:krm9u1ielgncvzvu@abc.us-east-1.rds.amazonaws.com:3306/gve28c5a0gz2mc4b

Spring Boot URL:
jdbc:mysql://abc.us-east-1.rds.amazonaws.com:3306/gve28c5a0gz2mc4b?useSSL=true&serverTimezone=UTC
```
