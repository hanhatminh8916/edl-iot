# Script mô phỏng các tag di chuyển trên sơ đồ 2D
# Anchor positions: A1(2,20), A2(5,2), A3(20,17)

Write-Host "🎬 Starting tag movement simulation..." -ForegroundColor Cyan
Write-Host "📍 Anchor positions: A1(2,20), A2(5,2), A3(20,17)" -ForegroundColor Yellow
Write-Host "🔴 Simulating 3 moving tags..." -ForegroundColor Green
Write-Host ""

$apiUrl = "https://edl-safework-iot-bf3ee691c9f6.herokuapp.com/api/test/mqtt"

# Hàm tính khoảng cách từ tag đến anchor
function Get-Distance($x1, $y1, $x2, $y2) {
    return [Math]::Sqrt([Math]::Pow($x2 - $x1, 2) + [Math]::Pow($y2 - $y1, 2))
}

# Anchor positions
$A1_X = 2.0
$A1_Y = 20.0
$A2_X = 5.0
$A2_Y = 2.0
$A3_X = 20.0
$A3_Y = 17.0

# Tag 1: Di chuyển theo hình tròn
$tag1_mac = "TAG001"
$tag1_center_x = 10.0
$tag1_center_y = 10.0
$tag1_radius = 5.0
$tag1_angle = 0.0

# Tag 2: Di chuyển theo hình vuông
$tag2_mac = "TAG002"
$tag2_positions = @(
    @{x=6; y=6},
    @{x=15; y=6},
    @{x=15; y=15},
    @{x=6; y=15}
)
$tag2_index = 0
$tag2_progress = 0.0

# Tag 3: Di chuyển theo đường chéo qua lại
$tag3_mac = "TAG003"
$tag3_start_x = 3.0
$tag3_start_y = 3.0
$tag3_end_x = 18.0
$tag3_end_y = 18.0
$tag3_progress = 0.0
$tag3_direction = 1

Write-Host "Press Ctrl+C to stop" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Cyan

$iteration = 0
while ($true) {
    $iteration++
    Write-Host "`n🔄 Iteration $iteration - $(Get-Date -Format 'HH:mm:ss')" -ForegroundColor Cyan
    
    # ===== TAG 1: Di chuyển theo hình tròn =====
    $tag1_x = $tag1_center_x + $tag1_radius * [Math]::Cos($tag1_angle)
    $tag1_y = $tag1_center_y + $tag1_radius * [Math]::Sin($tag1_angle)
    $tag1_angle += 0.2  # Tăng góc
    
    $tag1_A0 = Get-Distance $tag1_x $tag1_y $A1_X $A1_Y
    $tag1_A1 = Get-Distance $tag1_x $tag1_y $A2_X $A2_Y
    $tag1_A2 = Get-Distance $tag1_x $tag1_y $A3_X $A3_Y
    
    $json1 = @{
        mac = $tag1_mac
        temp = 25.0
        voltage = 3.7
        current = 100.0
        battery = 85.0
        lat = 16.073 + ($tag1_y / 100000)
        lon = 108.15 + ($tag1_x / 100000)
        hr = 75.0
        spo2 = 98.0
        uwb = @{
            A0 = [Math]::Round($tag1_A0, 2)
            A1 = [Math]::Round($tag1_A1, 2)
            A2 = [Math]::Round($tag1_A2, 2)
            TAG2 = 0.0
            baseline_A1 = 0.0
            baseline_A2 = 0.0
            ready = 1
        }
        fallDetected = 0
        helpRequest = 0
        timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss.ffffff")
    } | ConvertTo-Json -Compress
    
    Invoke-RestMethod -Uri $apiUrl -Method POST -Body $json1 -ContentType "application/json" | Out-Null
    Write-Host "  🔴 TAG1 (Circle): ($([Math]::Round($tag1_x,1)), $([Math]::Round($tag1_y,1))) → A0:$([Math]::Round($tag1_A0,1))m A1:$([Math]::Round($tag1_A1,1))m A2:$([Math]::Round($tag1_A2,1))m" -ForegroundColor Green
    
    # ===== TAG 2: Di chuyển theo hình vuông =====
    $tag2_current = $tag2_positions[$tag2_index]
    $tag2_next = $tag2_positions[($tag2_index + 1) % $tag2_positions.Count]
    
    $tag2_x = $tag2_current.x + ($tag2_next.x - $tag2_current.x) * $tag2_progress
    $tag2_y = $tag2_current.y + ($tag2_next.y - $tag2_current.y) * $tag2_progress
    
    $tag2_progress += 0.1
    if ($tag2_progress -ge 1.0) {
        $tag2_progress = 0.0
        $tag2_index = ($tag2_index + 1) % $tag2_positions.Count
    }
    
    $tag2_A0 = Get-Distance $tag2_x $tag2_y $A1_X $A1_Y
    $tag2_A1 = Get-Distance $tag2_x $tag2_y $A2_X $A2_Y
    $tag2_A2 = Get-Distance $tag2_x $tag2_y $A3_X $A3_Y
    
    $json2 = @{
        mac = $tag2_mac
        temp = 26.0
        voltage = 3.8
        current = 120.0
        battery = 90.0
        lat = 16.073 + ($tag2_y / 100000)
        lon = 108.15 + ($tag2_x / 100000)
        hr = 80.0
        spo2 = 97.0
        uwb = @{
            A0 = [Math]::Round($tag2_A0, 2)
            A1 = [Math]::Round($tag2_A1, 2)
            A2 = [Math]::Round($tag2_A2, 2)
            TAG2 = 0.0
            baseline_A1 = 0.0
            baseline_A2 = 0.0
            ready = 1
        }
        fallDetected = 0
        helpRequest = 0
        timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss.ffffff")
    } | ConvertTo-Json -Compress
    
    Invoke-RestMethod -Uri $apiUrl -Method POST -Body $json2 -ContentType "application/json" | Out-Null
    Write-Host "  🔴 TAG2 (Square): ($([Math]::Round($tag2_x,1)), $([Math]::Round($tag2_y,1))) → A0:$([Math]::Round($tag2_A0,1))m A1:$([Math]::Round($tag2_A1,1))m A2:$([Math]::Round($tag2_A2,1))m" -ForegroundColor Yellow
    
    # ===== TAG 3: Di chuyển theo đường chéo qua lại =====
    $tag3_x = $tag3_start_x + ($tag3_end_x - $tag3_start_x) * $tag3_progress
    $tag3_y = $tag3_start_y + ($tag3_end_y - $tag3_start_y) * $tag3_progress
    
    $tag3_progress += 0.05 * $tag3_direction
    if ($tag3_progress -ge 1.0) {
        $tag3_direction = -1
        $tag3_progress = 1.0
    } elseif ($tag3_progress -le 0.0) {
        $tag3_direction = 1
        $tag3_progress = 0.0
    }
    
    $tag3_A0 = Get-Distance $tag3_x $tag3_y $A1_X $A1_Y
    $tag3_A1 = Get-Distance $tag3_x $tag3_y $A2_X $A2_Y
    $tag3_A2 = Get-Distance $tag3_x $tag3_y $A3_X $A3_Y
    
    $json3 = @{
        mac = $tag3_mac
        temp = 24.5
        voltage = 3.6
        current = 110.0
        battery = 95.0
        lat = 16.073 + ($tag3_y / 100000)
        lon = 108.15 + ($tag3_x / 100000)
        hr = 72.0
        spo2 = 99.0
        uwb = @{
            A0 = [Math]::Round($tag3_A0, 2)
            A1 = [Math]::Round($tag3_A1, 2)
            A2 = [Math]::Round($tag3_A2, 2)
            TAG2 = 0.0
            baseline_A1 = 0.0
            baseline_A2 = 0.0
            ready = 1
        }
        fallDetected = 0
        helpRequest = 0
        timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss.ffffff")
    } | ConvertTo-Json -Compress
    
    Invoke-RestMethod -Uri $apiUrl -Method POST -Body $json3 -ContentType "application/json" | Out-Null
    Write-Host "  🔴 TAG3 (Diagonal): ($([Math]::Round($tag3_x,1)), $([Math]::Round($tag3_y,1))) → A0:$([Math]::Round($tag3_A0,1))m A1:$([Math]::Round($tag3_A1,1))m A2:$([Math]::Round($tag3_A2,1))m" -ForegroundColor Magenta
    
    Start-Sleep -Seconds 2
}
