# Test HELP_REQUEST Alert
# Gửi MQTT message với helpRequest: 1

$broker = "a398abeaaefc4d5fbe71c91009e952b2.s1.eu.hivemq.cloud"
$port = 8883
$username = "vku123"
$password = "Vku@2024"
$topic = "helmet/F4DD40BA2010"

# Message chỉ có helpRequest (không có fallDetected)
$message = @{
    mac = "F4DD40BA2010"
    temp = 33.85
    voltage = 3.48
    current = 510.8
    battery = 45.0
    lat = 15.945118
    lon = 108.254488
    hr = 0.0
    spo2 = 0.0
    uwb = @{
        A0 = -1.0
        A1 = -1.0
        TAG2 = -1.0
        A2 = -1.0
        baseline_A1 = 0.0
        baseline_A2 = 0.0
        ready = 1
    }
    fallDetected = 0
    helpRequest = 1
    timestamp = (Get-Date -Format "yyyy-MM-ddTHH:mm:ss.ffffff")
} | ConvertTo-Json -Depth 10 -Compress

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "TEST HELP_REQUEST ALERT" -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "Topic: $topic" -ForegroundColor Green
Write-Host "Message:" -ForegroundColor Green
Write-Host $message -ForegroundColor White
Write-Host ""

# Publish MQTT message
Write-Host "Publishing MQTT message..." -ForegroundColor Yellow

mosquitto_pub -h $broker -p $port `
    -u $username -P $password `
    --cafile "C:\mosquitto\certs\hivemq-com-chain.pem" `
    -t $topic `
    -m $message `
    -d

Write-Host ""
Write-Host "✅ Message published!" -ForegroundColor Green
Write-Host "⏳ Checking backend logs for:" -ForegroundColor Yellow
Write-Host "   - '🔍 Safety Check - MAC: F4DD40BA2010, fallDetected: 0, helpRequest: 1'" -ForegroundColor Cyan
Write-Host "   - '🆘 HELP REQUEST - Creating alert...'" -ForegroundColor Cyan
Write-Host "   - '💾 HELP_REQUEST alert saved to database'" -ForegroundColor Cyan
Write-Host ""
