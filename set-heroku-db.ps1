$url = "jdbc:mysql://100.96.40.30:3306/hatrustsoft?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&autoReconnect=true"
$username = "remote_user"
$password = "MatKhauManh123! "

heroku config:set "SPRING_DATASOURCE_URL=$url" "SPRING_DATASOURCE_USERNAME=$username" "SPRING_DATASOURCE_PASSWORD=$password" --app edl-safework-iot
