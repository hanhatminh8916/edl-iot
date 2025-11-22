# Test Fall Detection & Help Request Alert System
# Gửi dữ liệu MQTT thực tế đến HiveMQ Cloud để test cảnh báo ngã và SOS

Write-Host "🧪 TEST FALL DETECTION & HELP REQUEST ALERT SYSTEM" -ForegroundColor Cyan
Write-Host "=" * 60 -ForegroundColor Cyan

# MQTT Configuration
$MQTT_BROKER = "d0a82f39864c4e86a0551feaed97f7c5.s1.eu.hivemq.cloud"
$MQTT_PORT = 8883
$MQTT_USER = "truong123"
$MQTT_PASS = "Truong123"
$MAC = "A48D004AEC24"
$TOPIC = "helmet/$MAC"

# Test Cases
Write-Host "`n📋 Available Test Cases:" -ForegroundColor Yellow
Write-Host "  1. 🚨 FALL DETECTED (fallDetected: 1)" -ForegroundColor Red
Write-Host "  2. 🆘 HELP REQUEST (helpRequest: 1)" -ForegroundColor Red
Write-Host "  3. 🚨🆘 BOTH (fall + help)" -ForegroundColor Red
Write-Host "  4. ✅ NORMAL (no alert)" -ForegroundColor Green
Write-Host "  5. 🔄 AUTO TEST (all scenarios)" -ForegroundColor Magenta

$choice = Read-Host "`nChọn test case (1-5)"

function Send-MQTTMessage {
    param (
        [string]$Message,
        [string]$Description
    )
    
    Write-Host "`n📤 Sending: $Description" -ForegroundColor Cyan
    Write-Host "   Topic: $TOPIC" -ForegroundColor Gray
    Write-Host "   Payload: $Message" -ForegroundColor Gray
    
    $result = mqtt publish `
        -h $MQTT_BROKER `
        -p $MQTT_PORT `
        -u $MQTT_USER `
        -P $MQTT_PASS `
        --protocol mqtts `
        -t $TOPIC `
        -m $Message `
        2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   ✅ Sent successfully!" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Failed to send: $result" -ForegroundColor Red
    }
    
    Start-Sleep -Seconds 1
}

# Get current timestamp
$timestamp = Get-Date -Format "yyyy-MM-ddTHH:mm:ss.ffffff"

switch ($choice) {
    "1" {
        # Test Case 1: FALL DETECTED
        $message = @{
            mac = $MAC
            temp = 36.5
            voltage = 8.22
            current = -0.0
            battery = 95.0
            lat = 10.762400
            lon = 106.660050
            hr = 75.0
            spo2 = 98.0
            uwb = @{
                A0 = 2.09
                A1 = 2.02
                TAG2 = 4.26
                A2 = 3.58
                baseline_A1 = 0.99
                baseline_A2 = 1.52
                ready = 1
            }
            fallDetected = 1  # ⭐ NGÃ!
            helpRequest = 0
            timestamp = $timestamp
        } | ConvertTo-Json -Compress
        
        Send-MQTTMessage -Message $message -Description "🚨 FALL DETECTED ALERT"
    }
    
    "2" {
        # Test Case 2: HELP REQUEST (SOS)
        $message = @{
            mac = $MAC
            temp = 36.5
            voltage = 8.22
            current = -0.0
            battery = 92.0
            lat = 10.762600
            lon = 106.660150
            hr = 85.0
            spo2 = 96.0
            uwb = @{
                A0 = 2.03
                A1 = 2.07
                TAG2 = 4.62
                A2 = 4.24
                baseline_A1 = 0.99
                baseline_A2 = 1.52
                ready = 1
            }
            fallDetected = 0
            helpRequest = 1  # ⭐ SOS!
            timestamp = $timestamp
        } | ConvertTo-Json -Compress
        
        Send-MQTTMessage -Message $message -Description "🆘 HELP REQUEST (SOS) ALERT"
    }
    
    "3" {
        # Test Case 3: BOTH FALL + HELP REQUEST
        $message = @{
            mac = $MAC
            temp = 36.5
            voltage = 8.22
            current = -0.0
            battery = 88.0
            lat = 10.762800
            lon = 106.660250
            hr = 120.0  # Elevated heart rate!
            spo2 = 94.0  # Lower SpO2!
            uwb = @{
                A0 = 2.15
                A1 = 2.12
                TAG2 = 4.89
                A2 = 4.67
                baseline_A1 = 0.99
                baseline_A2 = 1.52
                ready = 1
            }
            fallDetected = 1  # ⭐ NGÃ!
            helpRequest = 1   # ⭐ SOS!
            timestamp = $timestamp
        } | ConvertTo-Json -Compress
        
        Send-MQTTMessage -Message $message -Description "🚨🆘 CRITICAL: FALL + HELP REQUEST!"
    }
    
    "4" {
        # Test Case 4: NORMAL (no alert)
        $message = @{
            mac = $MAC
            temp = 36.5
            voltage = 8.22
            current = -0.0
            battery = 100.0
            lat = 10.763000
            lon = 106.660350
            hr = 72.0
            spo2 = 99.0
            uwb = @{
                A0 = 2.01
                A1 = 2.05
                TAG2 = 4.35
                A2 = 3.95
                baseline_A1 = 0.99
                baseline_A2 = 1.52
                ready = 1
            }
            fallDetected = 0  # ✅ Bình thường
            helpRequest = 0   # ✅ Bình thường
            timestamp = $timestamp
        } | ConvertTo-Json -Compress
        
        Send-MQTTMessage -Message $message -Description "✅ NORMAL - No alerts"
    }
    
    "5" {
        # Test Case 5: AUTO TEST (all scenarios)
        Write-Host "`n🔄 AUTO TEST MODE - Testing all scenarios..." -ForegroundColor Magenta
        
        # 1. Normal
        Write-Host "`n--- Test 1/4: Normal ---" -ForegroundColor Yellow
        $message1 = @{
            mac = $MAC; temp = 36.5; voltage = 8.22; current = -0.0; battery = 100.0
            lat = 10.762400; lon = 106.660050; hr = 72.0; spo2 = 99.0
            uwb = @{ A0 = 2.01; A1 = 2.05; TAG2 = 4.35; A2 = 3.95; baseline_A1 = 0.99; baseline_A2 = 1.52; ready = 1 }
            fallDetected = 0; helpRequest = 0; timestamp = (Get-Date -Format "yyyy-MM-ddTHH:mm:ss.ffffff")
        } | ConvertTo-Json -Compress
        Send-MQTTMessage -Message $message1 -Description "✅ Normal"
        Start-Sleep -Seconds 3
        
        # 2. Fall Detected
        Write-Host "`n--- Test 2/4: Fall Detected ---" -ForegroundColor Yellow
        $message2 = @{
            mac = $MAC; temp = 36.5; voltage = 8.22; current = -0.0; battery = 95.0
            lat = 10.762600; lon = 106.660150; hr = 85.0; spo2 = 97.0
            uwb = @{ A0 = 2.09; A1 = 2.02; TAG2 = 4.26; A2 = 3.58; baseline_A1 = 0.99; baseline_A2 = 1.52; ready = 1 }
            fallDetected = 1; helpRequest = 0; timestamp = (Get-Date -Format "yyyy-MM-ddTHH:mm:ss.ffffff")
        } | ConvertTo-Json -Compress
        Send-MQTTMessage -Message $message2 -Description "🚨 Fall Detected"
        Start-Sleep -Seconds 3
        
        # 3. Help Request
        Write-Host "`n--- Test 3/4: Help Request (SOS) ---" -ForegroundColor Yellow
        $message3 = @{
            mac = $MAC; temp = 36.5; voltage = 8.22; current = -0.0; battery = 90.0
            lat = 10.762800; lon = 106.660250; hr = 95.0; spo2 = 95.0
            uwb = @{ A0 = 2.03; A1 = 2.07; TAG2 = 4.62; A2 = 4.24; baseline_A1 = 0.99; baseline_A2 = 1.52; ready = 1 }
            fallDetected = 0; helpRequest = 1; timestamp = (Get-Date -Format "yyyy-MM-ddTHH:mm:ss.ffffff")
        } | ConvertTo-Json -Compress
        Send-MQTTMessage -Message $message3 -Description "🆘 Help Request (SOS)"
        Start-Sleep -Seconds 3
        
        # 4. Critical: Both
        Write-Host "`n--- Test 4/4: CRITICAL (Fall + SOS) ---" -ForegroundColor Yellow
        $message4 = @{
            mac = $MAC; temp = 36.5; voltage = 8.22; current = -0.0; battery = 85.0
            lat = 10.763000; lon = 106.660350; hr = 120.0; spo2 = 93.0
            uwb = @{ A0 = 2.15; A1 = 2.12; TAG2 = 4.89; A2 = 4.67; baseline_A1 = 0.99; baseline_A2 = 1.52; ready = 1 }
            fallDetected = 1; helpRequest = 1; timestamp = (Get-Date -Format "yyyy-MM-ddTHH:mm:ss.ffffff")
        } | ConvertTo-Json -Compress
        Send-MQTTMessage -Message $message4 -Description "🚨🆘 CRITICAL: Fall + SOS"
        
        Write-Host "`n✅ All test scenarios completed!" -ForegroundColor Green
    }
    
    default {
        Write-Host "`n❌ Invalid choice!" -ForegroundColor Red
        exit 1
    }
}

Write-Host "`n" -NoNewline
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "✅ Test completed! Check your application:" -ForegroundColor Green
Write-Host "   1. Backend logs for MQTT processing" -ForegroundColor Gray
Write-Host "   2. Database alerts table for new records" -ForegroundColor Gray
Write-Host "   3. alerts.html page for realtime updates" -ForegroundColor Gray
Write-Host "   4. Messenger for notifications (if configured)" -ForegroundColor Gray
Write-Host "=" * 60 -ForegroundColor Cyan
