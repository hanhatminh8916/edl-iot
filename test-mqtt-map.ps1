# Script test MQTT realtime map - Da Nang location
# Chay: .\test-mqtt-map.ps1

Write-Host "🗺️  Testing MQTT Realtime Map - Da Nang" -ForegroundColor Cyan
Write-Host ""

# Check if mqtt-cli is installed
$mqttInstalled = Get-Command mqtt -ErrorAction SilentlyContinue
if (-not $mqttInstalled) {
    Write-Host "❌ MQTT client not found!" -ForegroundColor Red
    Write-Host "Install: npm install -g mqtt" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ MQTT client found" -ForegroundColor Green
Write-Host ""

# MQTT Config
$BROKER = "d0a82f39864c4e86a0551feaed97f7c5.s1.eu.hivemq.cloud"
$PORT = 8883
$USERNAME = "truong123"
$PASSWORD = "Truong123"
$TOPIC = "helmet/A48D004AEC24"

# Test data - Da Nang coordinates
Write-Host "📍 Sending test data to MQTT..." -ForegroundColor Yellow
Write-Host "   Lat: 15.97331, Lon: 108.25183 (Da Nang)" -ForegroundColor Gray
Write-Host ""

$payload = @"
{"mac":"A48D004AEC24","voltage":11.51,"current":-35.6,"power":410.0,"battery":100.0,"lat":15.97331,"lon":108.25183,"counter":1409,"timestamp":"2025-11-10T01:27:41.987164"}
"@

# Publish message
mqtt publish -h $BROKER -p $PORT -u $USERNAME -P $PASSWORD --protocol mqtts -t $TOPIC -m $payload

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Message sent successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "🌐 Open browser: http://localhost:8080/location.html" -ForegroundColor Cyan
    Write-Host "   → Map will show marker at Da Nang coordinates" -ForegroundColor Gray
    Write-Host "   → Green marker with 100% battery" -ForegroundColor Green
    Write-Host "   → Auto refresh every 10 seconds" -ForegroundColor Gray
} else {
    Write-Host ""
    Write-Host "❌ Failed to send message!" -ForegroundColor Red
}

Write-Host ""
Write-Host "📊 Check backend logs for:" -ForegroundColor Yellow
Write-Host "   📩 Received MQTT message from topic: helmet/A48D004AEC24" -ForegroundColor Gray
Write-Host "   👤 Mapped MAC A48D004AEC24 to Employee: Nguyen Van An (NV001)" -ForegroundColor Gray
Write-Host "   ✅ Saved helmet data: MAC=A48D004AEC24, Battery=100.0%, Voltage=11.51V" -ForegroundColor Gray
