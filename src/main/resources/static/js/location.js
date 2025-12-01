console.log("location.js loaded");
var map, markers = [], workersData = [], drawnItems = null, activePolygon = null;
var anchorMarkers = []; // Store anchor markers
var anchorLayer = null; // Separate layer for anchors
var isAnchorMode = false; // Toggle anchor placement mode
// Work zones (màu vàng) - khu vực làm việc nhỏ
var workZonesLayer = null; // Layer for work zones
var drawingMode = 'safezone'; // 'safezone' (green) or 'workzone' (yellow)
// Tọa độ tâm khu vực an toàn - ĐÀ NẴNG (cập nhật từ dữ liệu thực tế MQTT)
var safeZoneCenter = [15.97331, 108.25183];
var safeZoneRadius = 200; // Bán kính 200 mét (chỉ để tham khảo, giờ dùng polygon vẽ tay)

// 🚨 Track active alerts for fall/help detection
var activeAlerts = {}; // { mac: { type: 'fall'|'help', timestamp: Date } }

/**
 * 🚨 Xử lý cảnh báo té ngã/cầu cứu - tạo hiệu ứng radar wave
 */
function handleAlertUpdate(alert) {
    if (!alert || !alert.mac) return;
    
    console.log('🚨 ALERT received:', alert);
    
    const mac = alert.mac;
    const alertType = alert.type || 'FALL_DETECTED';
    
    // ✅ Lưu trạng thái cảnh báo
    activeAlerts[mac] = {
        type: alertType,
        timestamp: new Date(),
        lat: alert.lat,
        lon: alert.lon
    };
    
    // ✅ Cập nhật lại markers để hiển thị hiệu ứng
    updateMapMarkers(workersData);
    
    // ✅ Phát âm thanh cảnh báo
    playAlertSound();
    
    // ✅ Hiển thị notification
    showAlertNotification(mac, alertType);
    
    console.log('🚨 Active alerts:', Object.keys(activeAlerts).length);
}

/**
 * 🔊 Phát âm thanh cảnh báo
 */
function playAlertSound() {
    try {
        // Tạo beep sound bằng Web Audio API
        const audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const oscillator = audioContext.createOscillator();
        const gainNode = audioContext.createGain();
        
        oscillator.connect(gainNode);
        gainNode.connect(audioContext.destination);
        
        oscillator.frequency.value = 800; // Hz
        oscillator.type = 'sine';
        gainNode.gain.value = 0.3;
        
        oscillator.start();
        setTimeout(() => oscillator.stop(), 300);
        
        // Beep 2 lần
        setTimeout(() => {
            const osc2 = audioContext.createOscillator();
            osc2.connect(gainNode);
            osc2.frequency.value = 1000;
            osc2.type = 'sine';
            osc2.start();
            setTimeout(() => osc2.stop(), 300);
        }, 400);
    } catch(e) {
        console.log('Audio not supported:', e);
    }
}

/**
 * 📢 Hiển thị thông báo cảnh báo
 */
function showAlertNotification(mac, type) {
    // Tìm worker theo MAC
    const worker = workersData.find(w => w.helmet && w.helmet.helmetId === mac);
    const workerName = worker ? worker.name : mac;
    
    const message = type === 'FALL_DETECTED' 
        ? `🚨 CẢNH BÁO TÉ NGÃ: ${workerName}` 
        : `🆘 CẦU CỨU: ${workerName}`;
    
    // Hiển thị alert box
    const alertBox = document.getElementById('alertBox');
    if (alertBox) {
        alertBox.style.display = 'block';
        alertBox.innerHTML = `<strong>${message}</strong>`;
        alertBox.style.backgroundColor = type === 'FALL_DETECTED' ? '#dc3545' : '#ff6600';
    }
}

/**
 * ✅ Xóa cảnh báo khi đã xác nhận
 */
function clearAlert(mac) {
    if (activeAlerts[mac]) {
        delete activeAlerts[mac];
        console.log('✅ Alert cleared for:', mac);
        updateMapMarkers(workersData);
    }
}

function initializeMap() {
    console.log("Init map with Geo-Fencing");
    map = L.map("map").setView(safeZoneCenter, 15);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {maxZoom: 19}).addTo(map);
    
    // ✅ Layer riêng cho Anchors (không bị xóa khi vẽ SafeZone)
    anchorLayer = new L.FeatureGroup();
    map.addLayer(anchorLayer);
    
    // ✅ Layer riêng cho Work Zones (màu vàng)
    workZonesLayer = new L.FeatureGroup();
    map.addLayer(workZonesLayer);
    
    // ✅ Khởi tạo Leaflet Draw để vẽ polygon (vùng an toàn màu xanh)
    drawnItems = new L.FeatureGroup();
    map.addLayer(drawnItems);

    // ✅ Don't create draw control here - will be created by updateDrawControl()
    // This ensures the correct featureGroup is used based on drawingMode
    window.currentDrawControl = null;
    
    // ✅ Initialize with safe zone mode (default)
    updateDrawControl('safezone');

    // ✅ Add zoom event listener to update anchor sizes
    map.on('zoomend', function() {
        updateAnchorSizes();
    });

    // ✅ Khi vẽ xong polygon → LƯU VÀO DATABASE
    map.on(L.Draw.Event.CREATED, function (e) {
        const layer = e.layer;
        
        if (drawingMode === 'workzone') {
            // ✅ Vẽ Work Zone màu vàng
            layer.setStyle({
                color: '#FFA500',
                fillColor: '#FFA500',
                fillOpacity: 0.3,
                interactive: true
            });
            
            // Ensure work zone is on top
            if (layer.bringToFront) {
                layer.bringToFront();
            }
            
            workZonesLayer.addLayer(layer);
            
            // ✅ TỰ ĐỘNG TẠO ANCHORS TỪ CÁC ĐIỂM POLYGON
            const vertices = layer.getLatLngs()[0]; // Lấy các đỉnh polygon
            const zoneName = prompt('Nhập tên khu vực:', `Khu ${workZonesLayer.getLayers().length}`);
            
            if (zoneName) {
                layer.bindPopup(`<b>${zoneName}</b><br><small>Double-click để xem chi tiết sơ đồ 2D</small>`).openPopup();
                layer.zoneName = zoneName;
                
                // ✅ LƯU WORK ZONE VÀO DATABASE trước
                saveWorkZoneToDatabase(layer.getLatLngs(), layer, zoneName).then(async zoneId => {
                    if (!zoneId) {
                        console.error('❌ Failed to save zone, cannot create anchors');
                        return;
                    }
                    
                    // ✅ Gán zoneId ngay sau khi lưu thành công
                    layer.zoneId = zoneId;
                    
                    // ✅ Double-click vào zone để xem sơ đồ 2D (đặt sau khi có zoneId)
                    layer.on('dblclick', function(e) {
                        L.DomEvent.stopPropagation(e);
                        L.DomEvent.preventDefault(e);
                        console.log('🖱️ Double-click on zone:', layer.zoneName, 'ID:', layer.zoneId);
                        
                        // Bring to front to ensure visibility
                        if (layer.bringToFront) {
                            layer.bringToFront();
                        }
                        
                        window.location.href = `positioning-2d.html?zone=${layer.zoneId}`;
                    });
                    
                    // ✅ Lấy số anchor hiện tại để tạo ID tuần tự (bắt đầu từ A0)
                    const currentMaxId = await getMaxAnchorId();
                    
                    // ✅ Tạo anchor cho mỗi đỉnh polygon (A0, A1, A2...)
                    for (let index = 0; index < vertices.length; index++) {
                        const vertex = vertices[index];
                        const anchorNumber = currentMaxId + index; // Bắt đầu từ 0
                        const anchorName = `${zoneName}-A${index}`; // A0, A1, A2...
                        await createAnchorFromVertex(vertex, anchorName, zoneId, anchorNumber);
                    }
                    
                    showNotification(`✅ Đã tạo ${vertices.length} anchors cho ${zoneName}`, 'success');
                });
            }
        } else {
            // ✅ Vẽ Safe Zone màu xanh (như cũ)
            drawnItems.clearLayers(); // Xóa safe zone cũ (chỉ 1 safe zone)
            drawnItems.addLayer(layer);
            activePolygon = layer;
            document.getElementById("alertBox").style.display = "none";
            
            // ✅ LƯU SAFE ZONE VÀO DATABASE
            saveSafeZoneToDatabase(layer.getLatLngs());
        }
    });

    // ✅ Khi chỉnh sửa polygon
    map.on(L.Draw.Event.EDITED, function (e) {
        const layers = e.layers;
        layers.eachLayer(async function (layer) {
            // Kiểm tra xem layer này thuộc work zone hay safe zone
            if (layer.zoneId) {
                // ✅ Đây là work zone → cập nhật zone và anchors
                console.log('✏️ Editing work zone:', layer.zoneName);
                
                const newCoords = layer.getLatLngs()[0];
                
                // Cập nhật work zone trong database
                await updateWorkZoneInDatabase(layer.zoneId, newCoords, layer);
                
                // Xóa anchors cũ
                await deleteAnchorsByZoneId(layer.zoneId);
                
                // Tạo anchors mới tại các đỉnh mới (A0, A1, A2...)
                const currentMaxId = await getMaxAnchorId();
                for (let index = 0; index < newCoords.length; index++) {
                    const vertex = newCoords[index];
                    const anchorNumber = currentMaxId + index; // Bắt đầu từ 0
                    const anchorName = `${layer.zoneName}-A${index}`; // A0, A1, A2...
                    await createAnchorFromVertex(vertex, anchorName, layer.zoneId, anchorNumber);
                }
                
                // ✅ RELOAD ANCHORS để hiển thị realtime
                setTimeout(() => {
                    loadAnchorsFromDatabase();
                }, 500);
                
                showNotification(`✅ Đã cập nhật khu vực ${layer.zoneName} và ${newCoords.length} anchors`, 'success');
            } else {
                // ✅ Đây là safe zone
                activePolygon = layer;
                console.log("✅ Safe zone edited:", layer.getLatLngs());
                saveSafeZoneToDatabase(layer.getLatLngs());
            }
        });
    });

    // ✅ Khi xóa polygon
    map.on(L.Draw.Event.DELETED, function (e) {
        const layers = e.layers;
        layers.eachLayer(async function (layer) {
            // Kiểm tra xem layer này thuộc work zone hay safe zone
            if (layer.zoneId) {
                // ✅ Đây là work zone → xóa cả anchors
                console.log('🗑️ Deleting work zone and its anchors:', layer.zoneName);
                
                // Xóa tất cả anchors thuộc zone này
                await deleteAnchorsByZoneId(layer.zoneId);
                
                // Xóa zone từ database
                await deleteZoneFromDatabase(layer.zoneId);
                
                showNotification(`✅ Đã xóa khu vực ${layer.zoneName} và các anchors`, 'success');
            } else {
                // ✅ Đây là safe zone → xóa khỏi database
                activePolygon = null;
                console.log("🗑️ Deleting safe zone from database");
                
                // Xóa safe zone từ database
                await deleteSafeZoneFromDatabase();
                
                showNotification('✅ Đã xóa vùng an toàn', 'success');
            }
        });
    });

    // ✅ LOAD POLYGON TỪ DATABASE khi khởi động
    loadSafeZoneFromDatabase().then(() => {
        // Load work zones after safe zone to ensure proper layering
        loadWorkZonesFromDatabase().then(() => {
            // Bring all work zones to front after loading
            bringWorkZonesToFront();
        });
    });
    
    // ✅ LOAD ANCHORS FROM DATABASE
    loadAnchorsFromDatabase();
    
    // ✅ ADD ZONE MODE TOGGLE BUTTON
    var ZoneModeControl = L.Control.extend({
        options: {
            position: 'topleft'
        },
        onAdd: function(map) {
            var container = L.DomUtil.create('div', 'leaflet-bar leaflet-control');
            var button = L.DomUtil.create('a', 'leaflet-control-zone-mode', container);
            button.innerHTML = '<i class="fas fa-square" style="font-size: 16px;"></i>';
            button.href = '#';
            button.title = 'Vẽ Khu Vực (Vàng)';
            button.style.width = '30px';
            button.style.height = '30px';
            button.style.lineHeight = '30px';
            button.style.textAlign = 'center';
            button.style.background = 'white';
            button.style.color = '#FFA500';
            
            L.DomEvent.on(button, 'click', function(e) {
                L.DomEvent.preventDefault(e);
                L.DomEvent.stopPropagation(e);
                toggleZoneMode(button);
            });
            
            return container;
        }
    });
    
    map.addControl(new ZoneModeControl());
    
    // Thêm nhãn Hoàng Sa, Trường Sa
    var hoangSaIcon = L.divIcon({
        className: 'island-label',
        html: '<div style="background:#ffffff;padding:12px 20px;border-radius:8px;border:3px solid #ef4444;box-shadow:0 4px 12px rgba(0,0,0,0.3);white-space:nowrap;font-weight:bold;color:#1f2937;font-size:16px;">🇻🇳 Quần đảo HOÀNG SA<br><span style="font-size:14px;color:#6b7280;">(Việt Nam)</span></div>',
        iconSize: [240, 70],
        iconAnchor: [120, 35]
    });
    L.marker([16.5, 112.0], {icon: hoangSaIcon}).addTo(map);
    
    var truongSaIcon = L.divIcon({
        className: 'island-label',
        html: '<div style="background:#ffffff;padding:12px 20px;border-radius:8px;border:3px solid #ef4444;box-shadow:0 4px 12px rgba(0,0,0,0.3);white-space:nowrap;font-weight:bold;color:#1f2937;font-size:16px;">🇻🇳 Quần đảo TRƯỜNG SA<br><span style="font-size:14px;color:#6b7280;">(Việt Nam)</span></div>',
        iconSize: [240, 70],
        iconAnchor: [120, 0]
    });
    L.marker([9.8, 113.9], {icon: truongSaIcon}).addTo(map);
    
    var sovereigntyControl = L.control({position: 'bottomright'});
    sovereigntyControl.onAdd = function(map) {
        var div = L.DomUtil.create('div', 'sovereignty-note');
        div.innerHTML = '<span style="font-size:10px;color:#6b7280;background:rgba(255,255,255,0.9);padding:4px 8px;border-radius:4px;box-shadow:0 1px 4px rgba(0,0,0,0.1);display:inline-block;">🇻🇳 Hoàng Sa, Trường Sa thuộc Việt Nam</span>';
        return div;
    };
    sovereigntyControl.addTo(map);
    
    console.log("Map ready with Geo-Fencing");
}

// ✅ Hàm kiểm tra điểm có nằm trong polygon không (Point in Polygon algorithm)
function isInsidePolygon(lat, lon, polygon) {
    if (!polygon) return true; // Nếu chưa vẽ polygon thì coi như luôn an toàn
    
    const x = lon, y = lat;
    let inside = false;
    const vs = polygon.getLatLngs()[0];
    
    for (let i = 0, j = vs.length - 1; i < vs.length; j = i++) {
        const xi = vs[i].lng, yi = vs[i].lat;
        const xj = vs[j].lng, yj = vs[j].lat;
        const intersect = ((yi > y) !== (yj > y)) &&
                          (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
        if (intersect) inside = !inside;
    }
    return inside;
}

// Hàm tính khoảng cách giữa 2 điểm (Haversine formula) - giữ lại cho tham khảo
function calculateDistance(lat1, lon1, lat2, lon2) {
    var R = 6371e3; // Bán kính trái đất (mét)
    var φ1 = lat1 * Math.PI / 180;
    var φ2 = lat2 * Math.PI / 180;
    var Δφ = (lat2 - lat1) * Math.PI / 180;
    var Δλ = (lon2 - lon1) * Math.PI / 180;
    
    var a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
            Math.cos(φ1) * Math.cos(φ2) *
            Math.sin(Δλ/2) * Math.sin(Δλ/2);
    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    
    return R * c; // Khoảng cách (mét)
}

// ✅ Xác định màu marker dựa trên polygon và status
function getMarkerColor(lat, lon, status) {
    // OFFLINE (xám) - Ưu tiên cao nhất
    if (status === "INACTIVE") {
        return '#6b7280'; // Xám
    }
    
    // ✅ ƯU TIÊN KIỂM TRA SAFE ZONE TRƯỚC
    const inside = isInsidePolygon(lat, lon, activePolygon);
    
    // 🔍 DEBUG LOG
    console.log('🎨 Worker color check:', {
        lat, lon, 
        status,
        hasPolygon: !!activePolygon,
        inside: inside,
        finalColor: inside ? 'GREEN #10b981' : (status === 'ALERT' ? 'RED #ef4444 (ALERT)' : 'RED #ef4444 (OUT OF ZONE)')
    });
    
    // Nếu TRONG safe zone → XANH (bỏ qua ALERT)
    if (inside) {
        return '#10b981'; // Xanh lá - An toàn trong khu vực
    }
    
    // Nếu NGOÀI safe zone → ĐỎ
    return '#ef4444'; // Đỏ - Ra ngoài vùng an toàn hoặc ALERT
}
async function loadWorkers() {
    console.log("Loading workers data...");
    try {
        // ✅ Dùng API realtime từ Redis
        var res = await fetch("/api/location/map-data-realtime");
        workersData = await res.json();
        console.log("Loaded:", workersData.length, "workers (from Redis cache)");
        updateMapMarkers(workersData);
        displayWorkersList(workersData);
        
        // Cập nhật thời gian
        const now = new Date();
        document.getElementById("lastUpdate").textContent = 
            now.getHours().toString().padStart(2, '0') + ':' + 
            now.getMinutes().toString().padStart(2, '0') + ':' + 
            now.getSeconds().toString().padStart(2, '0');
    } catch(e) { 
        console.error("Error loading workers:", e); 
    }
}
function updateMapMarkers(workers) {
    console.log('🗺️ updateMapMarkers called with', workers.length, 'workers');
    markers.forEach(function(m) { map.removeLayer(m); });
    markers = [];
    
    // ✅ Xóa các radar wave cũ
    document.querySelectorAll('.radar-container').forEach(el => el.remove());
    
    const alertBox = document.getElementById("alertBox");
    let hasOutOfBounds = false;
    let hasActiveAlert = false;
    
    workers.forEach(function(w) {
        console.log('🔍 Processing worker:', w.name, 'helmet:', w.helmet);
        if (!w.helmet || !w.helmet.lastLocation) return;
        var lat = w.helmet.lastLocation.latitude;
        var lon = w.helmet.lastLocation.longitude;
        var battery = w.helmet.batteryLevel;
        var status = w.helmet.status; // ACTIVE, ALERT, INACTIVE
        var mac = w.helmet.helmetId;
        
        console.log('📍 Worker location:', {name: w.name, lat, lon, status});
        
        // 🚨 Kiểm tra xem worker này có cảnh báo không
        var hasAlert = activeAlerts[mac] !== undefined;
        var alertType = hasAlert ? activeAlerts[mac].type : null;
        
        if (hasAlert) {
            hasActiveAlert = true;
        }
        
        // ✅ Xác định màu dựa trên polygon và status
        var color = getMarkerColor(lat, lon, status);
        
        // 🚨 Nếu có cảnh báo, đổi màu đỏ
        if (hasAlert) {
            color = alertType === 'FALL_DETECTED' ? '#dc3545' : '#ff6600'; // Đỏ cho té ngã, cam cho cầu cứu
        }
        
        // ✅ Kiểm tra ra ngoài vùng an toàn
        const inside = isInsidePolygon(lat, lon, activePolygon);
        if (!inside && status !== "INACTIVE") {
            hasOutOfBounds = true;
        }
        
        // ✅ Tạo text mô tả trạng thái
        var statusText = "";
        if (hasAlert) {
            statusText = alertType === 'FALL_DETECTED' ? "🚨 TÉ NGÃ!" : "🆘 CẦU CỨU!";
        } else if (status === "INACTIVE") {
            statusText = "Offline (vị trí cuối cùng)";
        } else if (!inside) {
            statusText = "⚠️ Ra ngoài vùng an toàn!";
        } else if (status === "ALERT") {
            statusText = "⚠️ Cảnh báo hệ thống";
        } else {
            statusText = "✅ An toàn";
        }
        
        // 🚨 Tạo icon với hiệu ứng radar wave nếu có cảnh báo
        var markerHtml;
        if (hasAlert) {
            // ✅ Marker với hiệu ứng radar wave phát sóng từ tâm ra ngoài
            var waveClass = alertType === 'HELP_REQUEST' ? 'radar-wave help-radar-wave' : 'radar-wave';
            markerHtml = `
                <div class="radar-container">
                    <div class="${waveClass}"></div>
                    <div class="${waveClass}" style="animation-delay: 0.4s;"></div>
                    <div class="${waveClass}" style="animation-delay: 0.8s;"></div>
                    <div class="${waveClass}" style="animation-delay: 1.2s;"></div>
                    <div class="fall-alert-marker" style="background:${color};width:44px;height:44px;border-radius:50%;border:4px solid white;display:flex;align-items:center;justify-content:center;position:relative;z-index:1000;">
                        <span style="color:white;font-weight:bold;font-size:18px;text-shadow: 0 0 10px rgba(0,0,0,0.5);">!</span>
                    </div>
                </div>
            `;
        } else {
            // ✅ Marker bình thường
            markerHtml = `
                <div style="text-align:center;">
                    <div style="background:${color};width:32px;height:32px;border-radius:50%;border:3px solid white;box-shadow:0 2px 8px rgba(0,0,0,0.3);display:flex;align-items:center;justify-content:center;">
                        <span style="color:white;font-weight:bold;font-size:10px;text-shadow:0 1px 2px rgba(0,0,0,0.5);">${battery}%</span>
                    </div>
                </div>
            `;
        }
        
        var icon = L.divIcon({
            className: hasAlert ? 'alert-marker-icon' : 'custom-marker-with-label',
            html: markerHtml,
            iconSize: hasAlert ? [120, 120] : [32, 32], 
            iconAnchor: hasAlert ? [60, 60] : [16, 16]
        });
        
        var m = L.marker([lat, lon], {icon: icon}).addTo(map);
        
        // ✅ Popup với nút xác nhận cảnh báo
        var popupContent = "<b>" + w.name + "</b><br>" + 
                   "MAC: " + mac + "<br>" +
                   "Pin: " + battery + "%<br>" +
                   "<b>" + statusText + "</b>";
        
        if (hasAlert) {
            popupContent += `<br><br><button onclick="clearAlert('${mac}')" style="background:#28a745;color:white;border:none;padding:8px 16px;border-radius:4px;cursor:pointer;">✓ Xác nhận đã xử lý</button>`;
        }
        
        m.bindPopup(popupContent);
        m.workerId = w.id;
        m.mac = mac;
        markers.push(m);
    });
    
    // ✅ Hiển thị/ẩn Alert Box dựa trên cảnh báo hoặc ngoài vùng an toàn
    if (hasActiveAlert) {
        // Đã được xử lý bởi handleAlertUpdate
    } else if (hasOutOfBounds) {
        alertBox.style.display = "block";
        alertBox.innerHTML = "<strong>⚠️ Có người ngoài vùng an toàn!</strong>";
        alertBox.style.backgroundColor = '#dc3545';
    } else {
        alertBox.style.display = "none";
    }
    
    // ❌ TẮT auto-zoom khi update markers - để người dùng tự điều chỉnh map
    // if (markers.length) map.fitBounds(L.featureGroup(markers).getBounds().pad(0.1));
}
function displayWorkersList(workers) {
    var c = document.getElementById("workers-list");
    if (!c) return;
    var html = "";
    workers.forEach(function(w) {
        if (!w.helmet) return;
        var initials = w.name.split(" ").map(function(n){return n[0];}).join("").substring(0,2).toUpperCase();
        var cls = "status-safe", txt = "An toàn";
        if (w.helmet.status === "INACTIVE") { cls = "status-offline"; txt = "Offline"; }
        if (w.helmet.status === "ALERT") { cls = "status-warning"; txt = "Cảnh báo"; }
        html += "<div class=\"worker-item\" onclick=\"centerMapOnWorker(" + w.id + ")\"><div class=\"worker-avatar\">" + initials + "</div><div class=\"worker-info\"><div class=\"worker-name\">" + w.name + "</div><div class=\"worker-id\">ID: " + w.id + "</div></div><div class=\"worker-status\"><span class=\"status-badge " + cls + "\">" + txt + "</span><div>Pin: " + w.helmet.batteryLevel + "%</div></div></div>";
    });
    c.innerHTML = html;
}
function centerMapOnWorker(id) {
    var w = workersData.find(function(x){return x.id === id;});
    if (w && w.helmet && w.helmet.lastLocation) {
        map.setView([w.helmet.lastLocation.latitude, w.helmet.lastLocation.longitude], 18);
        markers.find(function(m){return m.workerId === id;}).openPopup();
    }
}

// ========================================
// ⭐ DATABASE API - Lưu/Load Safe Zone
// ========================================

/**
 * Lưu polygon vào database
 */
async function saveSafeZoneToDatabase(latlngs) {
    try {
        console.log("💾 Saving safe zone to database...");
        
        // Convert LatLng array sang JSON string
        const coordinates = JSON.stringify(latlngs[0].map(point => [point.lat, point.lng]));
        
        const response = await fetch('/api/safe-zones', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                zoneName: 'Khu vực sản xuất chính',
                polygonCoordinates: coordinates,
                color: '#10b981',
                createdBy: 'admin'
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            console.log("✅ Safe zone saved to database:", result.data);
            showNotification("✅ Đã lưu khu vực an toàn!", "success");
        } else {
            console.error("❌ Failed to save:", result.message);
            showNotification("❌ Lỗi lưu: " + result.message, "error");
        }
        
    } catch (error) {
        console.error("❌ Error saving safe zone:", error);
        showNotification("❌ Lỗi kết nối server!", "error");
    }
}

/**
 * Load polygon từ database
 */
async function loadSafeZoneFromDatabase() {
    try {
        console.log("📥 Loading safe zone from database...");
        
        const response = await fetch('/api/safe-zones/active');
        
        if (response.status === 204) {
            // No content - chưa có polygon nào
            console.log("ℹ️ No active safe zone found");
            return;
        }
        
        const safeZone = await response.json();
        
        if (safeZone && safeZone.polygonCoordinates) {
            console.log("✅ Loaded safe zone from database:", safeZone);
            
            // Parse JSON coordinates
            const coordinates = JSON.parse(safeZone.polygonCoordinates);
            
            // Convert [[lat,lng],...] → [L.LatLng,...]
            const latlngs = coordinates.map(coord => L.latLng(coord[0], coord[1]));
            
            // Vẽ polygon lên bản đồ
            activePolygon = L.polygon(latlngs, {
                color: safeZone.color || '#10b981',
                fillColor: safeZone.color || '#10b981',
                fillOpacity: 0.2
            }).addTo(drawnItems);
            
            // Ensure work zones stay on top after safe zone is rendered
            setTimeout(() => {
                bringWorkZonesToFront();
            }, 100);
            
            console.log("✅ Polygon rendered on map");
            showNotification("✅ Đã tải khu vực an toàn từ server!", "success");
        }
        
    } catch (error) {
        console.error("❌ Error loading safe zone:", error);
    }
}

/**
 * ✅ Xóa Safe Zone từ database
 */
async function deleteSafeZoneFromDatabase() {
    try {
        console.log("🗑️ Deleting safe zone from database...");
        
        const response = await fetch('/api/safe-zones/active', {
            method: 'DELETE'
        });
        
        if (response.ok) {
            console.log("✅ Safe zone deleted from database");
            // Clear from map
            drawnItems.clearLayers();
            activePolygon = null;
            return true;
        } else {
            console.error("❌ Failed to delete safe zone");
            showNotification("❌ Lỗi khi xóa khu vực an toàn", "error");
            return false;
        }
        
    } catch (error) {
        console.error("❌ Error deleting safe zone:", error);
        showNotification("❌ Lỗi kết nối server!", "error");
        return false;
    }
}

/**
 * Hiển thị thông báo toast
 */
function showNotification(message, type = "info") {
    const toast = document.createElement('div');
    toast.style.cssText = `
        position: fixed;
        top: 80px;
        right: 20px;
        background: ${type === 'success' ? '#10b981' : type === 'error' ? '#ef4444' : '#3b82f6'};
        color: white;
        padding: 12px 20px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.2);
        z-index: 10000;
        font-size: 14px;
        font-weight: 500;
        animation: slideIn 0.3s ease-out;
    `;
    toast.textContent = message;
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease-out';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// CSS animations
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from { transform: translateX(400px); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
    }
    @keyframes slideOut {
        from { transform: translateX(0); opacity: 1; }
        to { transform: translateX(400px); opacity: 0; }
    }
`;
document.head.appendChild(style);

// ==========================================
// WEBSOCKET REAL-TIME UPDATE
// ==========================================
var stompClient = null;

function connectWebSocket() {
    console.log('🔌 Connecting to WebSocket...');
    
    const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
    const host = window.location.host;
    const wsUrl = `${protocol}//${host}/ws`;
    
    const socket = new SockJS(wsUrl);
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        console.log('✅ WebSocket connected!');
        
        // Subscribe to helmet data updates
        stompClient.subscribe('/topic/helmet/data', function(message) {
            try {
                const data = JSON.parse(message.body);
                console.log('📡 Received real-time update:', data.mac);
                
                // Update marker on map in real-time
                updateMarkerRealtime(data);
                
            } catch (e) {
                console.error('❌ Error parsing WebSocket message:', e);
            }
        });
        
        // 🆕 Subscribe to SafeZone updates (vẽ polygon realtime)
        stompClient.subscribe('/topic/safezone/update', function(message) {
            try {
                const update = JSON.parse(message.body);
                console.log('🟢 Received SafeZone update:', update.action);
                
                // Xử lý SafeZone realtime
                handleSafeZoneUpdate(update);
                
            } catch (e) {
                console.error('❌ Error parsing SafeZone message:', e);
            }
        });
        
        // 🆕 Subscribe to Anchor updates (đặt/xóa anchor realtime)
        stompClient.subscribe('/topic/anchor/update', function(message) {
            try {
                const update = JSON.parse(message.body);
                console.log('📍 Received Anchor update:', update.action);
                
                // Xử lý Anchor realtime
                handleAnchorUpdate(update);
                
            } catch (e) {
                console.error('❌ Error parsing Anchor message:', e);
            }
        });
        
        // ✅ Subscribe to Work Zone updates
        stompClient.subscribe('/topic/zone/update', function(message) {
            try {
                const update = JSON.parse(message.body);
                console.log('🟨 Received Work Zone update:', update.action);
                
                // Xử lý Work Zone realtime
                handleWorkZoneUpdate(update);
                
            } catch (e) {
                console.error('❌ Error parsing Work Zone message:', e);
            }
        });
        
        // 🚨 Subscribe to Alert updates (Fall, Help Request)
        stompClient.subscribe('/topic/alerts/new', function(message) {
            try {
                const alert = JSON.parse(message.body);
                console.log('🚨 Received Alert:', alert);
                
                // Lấy MAC từ alert.helmet.helmetId hoặc alert.mac
                const mac = alert.helmet?.helmetId || alert.mac;
                const alertType = alert.alertType || alert.type || 'FALL_DETECTED';
                const lat = alert.gpsLat || alert.lat;
                const lon = alert.gpsLon || alert.lon;
                
                console.log('🚨 Alert details - MAC:', mac, 'Type:', alertType);
                
                if (mac) {
                    // Xử lý alert animation
                    handleAlertUpdate({ 
                        mac: mac, 
                        type: alertType, 
                        lat: lat, 
                        lon: lon 
                    });
                }
                
            } catch (e) {
                console.error('❌ Error parsing Alert message:', e);
            }
        });
        
    }, function(error) {
        console.error('❌ WebSocket connection error:', error);
        // Retry after 5 seconds
        setTimeout(connectWebSocket, 5000);
    });
}

function updateMarkerRealtime(data) {
    if (!data.lat || !data.lon) {
        console.log('⚠️ No GPS data for', data.mac);
        return;
    }
    
    console.log('🔄 Updating marker realtime for', data.mac, 'at', data.lat, data.lon);
    
    // 🚨 Check for fall/help alerts in realtime data
    if (data.fallDetected) {
        handleAlertUpdate({ mac: data.mac, type: 'FALL_DETECTED', lat: data.lat, lon: data.lon });
    }
    if (data.helpRequest) {
        handleAlertUpdate({ mac: data.mac, type: 'HELP_REQUEST', lat: data.lat, lon: data.lon });
    }
    
    // Tìm worker theo MAC và cập nhật dữ liệu
    const workerIndex = workersData.findIndex(w => w.helmet && w.helmet.helmetId === data.mac);
    
    if (workerIndex >= 0) {
        // Cập nhật thông tin worker
        const worker = workersData[workerIndex];
        if (worker.helmet.lastLocation) {
            worker.helmet.lastLocation.latitude = parseFloat(data.lat);
            worker.helmet.lastLocation.longitude = parseFloat(data.lon);
        }
        worker.helmet.batteryLevel = data.battery || worker.helmet.batteryLevel;
        
        console.log('✅ Updated worker data for', worker.name);
        
        // Vẽ lại TẤT CẢ markers với màu mới
        updateMapMarkers(workersData);
        
    } else {
        console.log('⚠️ Worker not found for MAC:', data.mac, '- reloading all workers');
        loadWorkers(); // Reload nếu không tìm thấy
    }
}

// ==========================================
// END WEBSOCKET
// ==========================================

// ==========================================
// SAFEZONE REALTIME UPDATES
// ==========================================

/**
 * Xử lý SafeZone updates từ WebSocket
 */
function handleSafeZoneUpdate(update) {
    console.log('🟢 Processing SafeZone update:', update.action);
    
    const action = update.action;
    const safeZone = update.safeZone;
    
    if (action === 'CREATE' || action === 'UPDATE') {
        // Vẽ lại polygon mới
        drawPolygonFromData(safeZone);
        
        showNotification(`Khu vực an toàn "${safeZone.zoneName}" đã được cập nhật!`, 'success');
        
    } else if (action === 'DELETE') {
        // Xóa polygon
        if (drawnItems) {
            drawnItems.clearLayers();
            activePolygon = null;
        }
        
        showNotification(`Khu vực an toàn "${safeZone.zoneName}" đã bị xóa!`, 'warning');
        
    } else if (action === 'DELETE_ALL') {
        // Xóa tất cả polygon
        if (drawnItems) {
            drawnItems.clearLayers();
            activePolygon = null;
        }
        
        showNotification('Đã xóa tất cả khu vực an toàn!', 'info');
    }
    
    // Kiểm tra lại tất cả workers sau khi polygon thay đổi
    setTimeout(() => {
        markers.forEach(m => {
            checkAndUpdateWorkerStatus(m);
        });
    }, 500);
}

/**
 * ✅ Xử lý Anchor updates từ WebSocket (realtime)
 */
function handleAnchorUpdate(update) {
    const action = update.action;
    
    if (action === 'CREATE') {
        // Thêm anchor mới
        const anchor = update.anchor;
        
        // Kiểm tra xem anchor đã tồn tại chưa (tránh duplicate khi tự mình tạo)
        const exists = anchorMarkers.find(a => a.id === anchor.id);
        if (!exists) {
            addAnchorMarker(anchor);
            console.log('✅ Anchor created from WebSocket:', anchor.anchorId);
        } else {
            console.log('⚠️ Anchor already exists, skipping:', anchor.anchorId);
        }
        
    } else if (action === 'UPDATE') {
        // Cập nhật anchor
        const anchor = update.anchor;
        
        // Xóa marker cũ và thêm mới
        const existingMarker = anchorMarkers.find(a => a.id === anchor.id);
        if (existingMarker) {
            anchorLayer.removeLayer(existingMarker.marker);
            anchorMarkers = anchorMarkers.filter(a => a.id !== anchor.id);
        }
        addAnchorMarker(anchor);
        console.log('✅ Anchor updated from WebSocket:', anchor.anchorId);
        
    } else if (action === 'DELETE') {
        // Xóa anchor
        const anchorId = update.anchorId;
        
        const anchorMarker = anchorMarkers.find(a => a.id === anchorId);
        if (anchorMarker) {
            anchorLayer.removeLayer(anchorMarker.marker);
            anchorMarkers = anchorMarkers.filter(a => a.id !== anchorId);
            console.log('✅ Anchor deleted from WebSocket:', anchorId);
        }
    }
}

/**
 * ✅ Xử lý Work Zone updates từ WebSocket (realtime)
 */
function handleWorkZoneUpdate(update) {
    const action = update.action;
    
    if (action === 'CREATE') {
        // Thêm work zone mới
        const zone = update.zone;
        
        // Kiểm tra xem zone đã tồn tại chưa
        const exists = workZonesLayer.getLayers().find(layer => layer.zoneId === zone.id);
        if (!exists) {
            const coords = JSON.parse(zone.polygonCoordinates);
            const polygon = L.polygon(coords, {
                color: zone.color || '#FFA500',
                fillColor: zone.color || '#FFA500',
                fillOpacity: 0.3,
                interactive: true
            });
            
            polygon.zoneId = zone.id;
            polygon.zoneName = zone.name;
            polygon.bindPopup(`<b>${zone.name}</b><br><small>Double-click để xem chi tiết sơ đồ 2D</small>`);
            
            polygon.on('dblclick', function(e) {
                L.DomEvent.stopPropagation(e);
                L.DomEvent.preventDefault(e);
                console.log('🖱️ Double-click on WebSocket zone:', zone.name, 'ID:', zone.id);
                
                if (polygon.bringToFront) {
                    polygon.bringToFront();
                }
                
                window.location.href = `positioning-2d.html?zone=${zone.id}`;
            });
            
            workZonesLayer.addLayer(polygon);
            
            if (polygon.bringToFront) {
                polygon.bringToFront();
            }
            
            console.log('✅ Work zone created from WebSocket:', zone.name);
        }
        
    } else if (action === 'UPDATE') {
        // Cập nhật work zone
        const zone = update.zone;
        
        // Tìm và xóa polygon cũ
        const layers = workZonesLayer.getLayers();
        const existingLayer = layers.find(layer => layer.zoneId === zone.id);
        
        if (existingLayer) {
            workZonesLayer.removeLayer(existingLayer);
        }
        
        // Thêm polygon mới với tọa độ mới
        const coords = JSON.parse(zone.polygonCoordinates);
        const polygon = L.polygon(coords, {
            color: zone.color || '#FFA500',
            fillColor: zone.color || '#FFA500',
            fillOpacity: 0.3
        });
        
        polygon.zoneId = zone.id;
        polygon.zoneName = zone.name;
        polygon.bindPopup(`<b>${zone.name}</b><br><small>Double-click để xem chi tiết sơ đồ 2D</small>`);
        
        polygon.on('dblclick', function(e) {
            L.DomEvent.stopPropagation(e);
            window.location.href = `positioning-2d.html?zone=${zone.id}`;
        });
        
        workZonesLayer.addLayer(polygon);
        console.log('✅ Work zone updated from WebSocket:', zone.name);
        
    } else if (action === 'DELETE') {
        // Xóa work zone
        const zoneId = update.zoneId;
        
        const layers = workZonesLayer.getLayers();
        const layerToRemove = layers.find(layer => layer.zoneId === zoneId);
        
        if (layerToRemove) {
            workZonesLayer.removeLayer(layerToRemove);
            console.log('✅ Work zone deleted from WebSocket:', zoneId);
        }
    }
}

/**
 * Vẽ polygon từ SafeZone data
 */
function drawPolygonFromData(safeZone) {
    if (!safeZone || !safeZone.polygonCoordinates) {
        console.error('❌ Invalid SafeZone data');
        return;
    }
    
    try {
        // Parse coordinates từ JSON string
        const coords = JSON.parse(safeZone.polygonCoordinates);
        
        // Clear old polygon
        if (drawnItems) {
            drawnItems.clearLayers();
        }
        
        // Vẽ polygon mới
        const polygon = L.polygon(coords, {
            color: safeZone.color || '#10b981',
            fillColor: safeZone.color || '#10b981',
            fillOpacity: 0.2,
            weight: 2
        });
        
        drawnItems.addLayer(polygon);
        activePolygon = polygon;
        
        // Ensure work zones stay on top after safe zone is redrawn
        setTimeout(() => {
            bringWorkZonesToFront();
        }, 100);
        
        console.log('✅ Polygon drawn:', safeZone.zoneName);
        
        // ❌ TẮT auto-zoom khi load safe zone - để người dùng tự điều chỉnh map
        // if (coords && coords.length > 0) {
        //     map.fitBounds(polygon.getBounds());
        // }
        
    } catch (e) {
        console.error('❌ Error drawing polygon:', e);
    }
}

/**
 * Show notification toast
 */
function showNotification(message, type = 'info') {
    // Simple alert for now (có thể thay bằng toast library)
    console.log(`📢 ${type.toUpperCase()}: ${message}`);
    
    // Optional: Sử dụng browser notification nếu có permission
    if ('Notification' in window && Notification.permission === 'granted') {
        new Notification('SafeWork IoT', {
            body: message,
            icon: '/images/helmet-icon.png',
            badge: '/images/badge-icon.png'
        });
    }
}

// ==========================================
// END SAFEZONE REALTIME
// ==========================================

window.addEventListener("load", function() {
    console.log("Page loaded");
    if (typeof L !== "undefined") {
        initializeMap();
        setTimeout(loadWorkers, 500);
    }
    
    // Connect WebSocket for real-time updates
    connectWebSocket();
    
    // Keep polling as fallback (reduced frequency)
    setInterval(loadWorkers, 30000); // Every 30s instead of 10s
    
    // Fullscreen button handler
    const btnExpand = document.querySelector('.btn-expand');
    if (btnExpand) {
        btnExpand.addEventListener('click', function() {
            const mapCard = document.querySelector('.card');
            const mapContainer = document.getElementById('map');
            
            if (!document.fullscreenElement) {
                // Enter fullscreen
                if (mapCard.requestFullscreen) {
                    mapCard.requestFullscreen();
                } else if (mapCard.webkitRequestFullscreen) { // Safari
                    mapCard.webkitRequestFullscreen();
                } else if (mapCard.msRequestFullscreen) { // IE11
                    mapCard.msRequestFullscreen();
                }
                this.querySelector('i').classList.replace('fa-expand', 'fa-compress');
            } else {
                // Exit fullscreen
                if (document.exitFullscreen) {
                    document.exitFullscreen();
                } else if (document.webkitExitFullscreen) { // Safari
                    document.webkitExitFullscreen();
                } else if (document.msExitFullscreen) { // IE11
                    document.msExitFullscreen();
                }
                this.querySelector('i').classList.replace('fa-compress', 'fa-expand');
            }
        });
        
        // Handle ESC key to change icon back
        document.addEventListener('fullscreenchange', function() {
            if (!document.fullscreenElement) {
                const icon = btnExpand.querySelector('i');
                if (icon.classList.contains('fa-compress')) {
                    icon.classList.replace('fa-compress', 'fa-expand');
                }
            }
        });
    }
});

// ✅ Anchor mode removed - use polygon drawing (yellow mode) to create anchors

// ========== ZONE MODE TOGGLE ==========
function toggleZoneMode(button) {
    if (drawingMode === 'safezone') {
        drawingMode = 'workzone';
        button.style.color = '#FFA500';
        button.style.background = '#FFF3CD';
        button.title = 'Chế độ: Vẽ Khu Vực (Vàng)';
        
        // ✅ Chuyển draw control để edit/delete work zones
        updateDrawControl('workzone');
        
        showNotification('🟨 Chế độ vẽ Khu Vực làm việc (màu vàng) - Nút thùng rác chỉ xóa khu vàng', 'info');
    } else {
        drawingMode = 'safezone';
        button.style.color = '#10b981';
        button.style.background = 'white';
        button.title = 'Chế độ: Vẽ Vùng An Toàn (Xanh)';
        
        // ✅ Chuyển draw control để edit/delete safe zones
        updateDrawControl('safezone');
        
        showNotification('🟩 Chế độ vẽ Vùng An Toàn (màu xanh)', 'info');
    }
}

// ✅ CẬP NHẬT DRAW CONTROL THEO CHẾ ĐỘ
function updateDrawControl(mode) {
    // Remove old control
    if (window.currentDrawControl) {
        map.removeControl(window.currentDrawControl);
    }
    
    // Create new control with appropriate featureGroup
    const featureGroup = mode === 'workzone' ? workZonesLayer : drawnItems;
    
    window.currentDrawControl = new L.Control.Draw({
        draw: {
            polygon: {
                shapeOptions: {
                    color: mode === 'workzone' ? '#FFA500' : '#10b981',
                    fillColor: mode === 'workzone' ? '#FFA500' : '#10b981',
                    fillOpacity: mode === 'workzone' ? 0.3 : 0.2
                }
            },
            marker: false,
            circle: false,
            rectangle: false,
            polyline: false,
            circlemarker: false
        },
        edit: { 
            featureGroup: featureGroup,
            remove: true
        }
    });
    
    map.addControl(window.currentDrawControl);
}

// ========== WORK ZONE FUNCTIONS ==========

// Load work zones from database
async function loadWorkZonesFromDatabase() {
    try {
        const response = await fetch('/api/zones/active');
        const zones = await response.json();
        console.log('✅ Loaded work zones from DB:', zones);
        
        zones.forEach(zone => {
            // ✅ Kiểm tra xem zone đã tồn tại chưa (tránh duplicate)
            const exists = workZonesLayer.getLayers().find(layer => layer.zoneId === zone.id);
            if (exists) {
                console.log(`⚠️ Zone ${zone.name} already exists, skipping`);
                return;
            }
            
            const coords = JSON.parse(zone.polygonCoordinates);
            const polygon = L.polygon(coords, {
                color: zone.color || '#FFA500',
                fillColor: zone.color || '#FFA500',
                fillOpacity: 0.3,
                interactive: true
            });
            
            polygon.zoneId = zone.id;
            polygon.zoneName = zone.name;
            polygon.bindPopup(`<b>${zone.name}</b><br><small>Double-click để xem chi tiết sơ đồ 2D</small>`);
            
            // Double-click to view 2D diagram
            polygon.on('dblclick', function(e) {
                L.DomEvent.stopPropagation(e);
                L.DomEvent.preventDefault(e);
                console.log('🖱️ Double-click on loaded zone:', zone.name, 'ID:', zone.id);
                
                // Bring to front to ensure visibility
                if (polygon.bringToFront) {
                    polygon.bringToFront();
                }
                
                window.location.href = `positioning-2d.html?zone=${zone.id}`;
            });
            
            workZonesLayer.addLayer(polygon);
            
            // Ensure work zone is on top of safe zone
            if (polygon.bringToFront) {
                polygon.bringToFront();
            }
            
            console.log(`✅ Loaded work zone: ${zone.name}`);
        });
    } catch (error) {
        console.error('Error loading work zones:', error);
    }
}

// ✅ Bring all work zones to front (ensure they're above safe zone)
function bringWorkZonesToFront() {
    const layers = workZonesLayer.getLayers();
    layers.forEach(layer => {
        if (layer.bringToFront) {
            layer.bringToFront();
        }
    });
    console.log(`✅ Brought ${layers.length} work zones to front`);
}

// Save work zone to database
async function saveWorkZoneToDatabase(latlngs, layer, zoneName) {
    try {
        const coords = latlngs[0].map(ll => [ll.lat, ll.lng]);
        
        const payload = {
            name: zoneName || layer.zoneName || `Khu ${workZonesLayer.getLayers().length}`,
            polygonCoordinates: JSON.stringify(coords),
            color: '#FFA500',
            description: 'Work zone'
        };
        
        const response = await fetch('/api/zones', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        
        if (response.ok) {
            const savedZone = await response.json();
            layer.zoneId = savedZone.id;
            console.log('✅ Work zone saved to DB:', savedZone);
            return savedZone.id; // Trả về zoneId
        }
        return null;
    } catch (error) {
        console.error('Error saving work zone:', error);
        showNotification('❌ Lỗi khi lưu khu vực', 'error');
        return null;
    }
}

// ✅ CẬP NHẬT WORK ZONE TRONG DATABASE
async function updateWorkZoneInDatabase(zoneId, latlngs, layer) {
    try {
        const coords = latlngs.map(ll => [ll.lat, ll.lng]);
        
        const payload = {
            name: layer.zoneName,
            polygonCoordinates: JSON.stringify(coords),
            color: '#FFA500',
            description: 'Work zone'
        };
        
        const response = await fetch(`/api/zones/${zoneId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        
        if (response.ok) {
            const updatedZone = await response.json();
            console.log('✅ Work zone updated in DB:', updatedZone);
            return true;
        }
        return false;
    } catch (error) {
        console.error('Error updating work zone:', error);
        showNotification('❌ Lỗi khi cập nhật khu vực', 'error');
        return false;
    }
}

// ✅ XÓA TẤT CẢ ANCHORS THUỘC ZONE
async function deleteAnchorsByZoneId(zoneId) {
    try {
        // Lấy tất cả anchors
        const response = await fetch('/api/anchors');
        const anchors = await response.json();
        
        // Tìm anchors thuộc zone này
        const zoneAnchors = anchors.filter(a => a.zoneId === zoneId);
        
        console.log(`🗑️ Deleting ${zoneAnchors.length} anchors from zone ${zoneId}`);
        
        // Xóa từng anchor
        for (const anchor of zoneAnchors) {
            // Xóa marker khỏi bản đồ TRƯỚC khi xóa database
            const markerData = anchorMarkers.find(am => am.id === anchor.id);
            if (markerData) {
                anchorLayer.removeLayer(markerData.marker);
                anchorMarkers = anchorMarkers.filter(am => am.id !== anchor.id);
                console.log(`✅ Removed anchor marker ${anchor.anchorId} from map`);
            }
            
            // Xóa từ database (WebSocket sẽ broadcast nhưng marker đã xóa rồi)
            await fetch(`/api/anchors/${anchor.id}`, { method: 'DELETE' });
        }
        
        return true;
    } catch (error) {
        console.error('Error deleting anchors by zone:', error);
        return false;
    }
}

// ✅ XÓA ZONE TỪ DATABASE
async function deleteZoneFromDatabase(zoneId) {
    try {
        const response = await fetch(`/api/zones/${zoneId}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            console.log('✅ Zone deleted from DB:', zoneId);
            return true;
        }
        return false;
    } catch (error) {
        console.error('Error deleting zone:', error);
        return false;
    }
}

// ✅ LẤY SỐ ANCHOR LỚN NHẤT HIỆN TẠI (hỗ trợ A0, A1, A2...)
async function getMaxAnchorId() {
    try {
        const response = await fetch('/api/anchors');
        const anchors = await response.json();
        
        if (anchors.length === 0) {
            return 0; // Bắt đầu từ A0
        }
        
        // Tìm số lớn nhất từ anchorId dạng A0, A1, A2, A3...
        const maxId = anchors
            .map(a => a.anchorId)
            .filter(id => id && id.match(/^A\d+$/))
            .map(id => parseInt(id.substring(1)))
            .reduce((max, num) => Math.max(max, num), -1); // -1 để khi + 1 = 0
        
        return maxId + 1; // Trả về số tiếp theo
    } catch (error) {
        console.error('Error getting max anchor ID:', error);
        return 0;
    }
}

// ✅ TẠO ANCHOR TỪ ĐỈNH POLYGON
async function createAnchorFromVertex(vertex, anchorName, zoneId, anchorIndex) {
    try {
        // Generate sequential anchorId: A0, A1, A2, A3...
        const anchorId = `A${anchorIndex}`;
        
        const payload = {
            anchorId: anchorId,  // A0, A1, A2, A3...
            name: anchorName,
            latitude: vertex.lat,
            longitude: vertex.lng,
            zoneId: zoneId // Liên kết anchor với zone
        };
        
        console.log('🔄 Creating anchor:', payload);
        
        const response = await fetch('/api/anchors', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        
        if (response.ok) {
            const savedAnchor = await response.json();
            console.log('✅ Anchor created:', savedAnchor);
            
            // ❌ KHÔNG add marker ngay - để WebSocket broadcast handle (tránh duplicate)
            // WebSocket sẽ broadcast CREATE event và add marker ở tất cả clients
            return savedAnchor;
        } else {
            const errorText = await response.text();
            console.error('❌ Failed to create anchor:', response.status, errorText);
        }
    } catch (error) {
        console.error('❌ Error creating anchor:', error);
    }
    return null;
}

// ========== ANCHOR FUNCTIONS ==========

// Load all anchors from database
function loadAnchorsFromDatabase() {
    fetch('/api/anchors')
        .then(response => response.json())
        .then(anchors => {
            console.log('✅ Loaded anchors from DB:', anchors);
            
            // ✅ Clear existing markers first
            anchorMarkers.forEach(am => {
                anchorLayer.removeLayer(am.marker);
            });
            anchorMarkers = [];
            
            // Add all anchors from DB
            anchors.forEach(anchor => {
                addAnchorMarker(anchor);
            });
        })
        .catch(error => {
            console.error('❌ Error loading anchors:', error);
        });
}

// Add anchor marker to map
function addAnchorMarker(anchor) {
    // Get current zoom level to calculate size
    const currentZoom = map ? map.getZoom() : 15;
    const baseZoom = 15; // Base zoom level
    const zoomDiff = currentZoom - baseZoom;
    
    // Calculate size based on zoom (min 12px, max 28px)
    const baseSize = 20;
    const size = Math.max(12, Math.min(28, baseSize + (zoomDiff * 2)));
    
    // Calculate font size and padding proportionally
    const fontSize = Math.max(6, Math.min(9, 7 + (zoomDiff * 0.4)));
    const padding = Math.max(1, Math.min(4, 2 + (zoomDiff * 0.4)));
    const borderWidth = Math.max(1, Math.min(2, 1.5 + (zoomDiff * 0.15)));
    
    const anchorIcon = L.divIcon({
        className: 'anchor-marker',
        html: `<div style="background: #2196F3; color: white; padding: ${padding}px ${padding + 2}px; border-radius: 50%; border: ${borderWidth}px solid white; box-shadow: 0 1px 4px rgba(0,0,0,0.3); font-weight: bold; font-size: ${fontSize}px; text-align: center; min-width: ${size - 6}px;">
                    ${anchor.anchorId}
               </div>`,
        iconSize: [size, size],
        iconAnchor: [size / 2, size / 2]
    });
    
    const marker = L.marker([anchor.latitude, anchor.longitude], {
        icon: anchorIcon,
        draggable: false
    });
    
    // Store anchor data for re-rendering on zoom
    marker.anchorData = anchor;
    
    // ✅ Add to anchorLayer instead of map directly
    anchorLayer.addLayer(marker);
    
    // Popup with anchor info (single click)
    marker.bindPopup(`
        <div style="min-width: 200px;">
            <h3 style="margin: 0 0 10px 0; color: #2196F3;">📍 ${anchor.name}</h3>
            <p style="margin: 5px 0;"><strong>ID:</strong> ${anchor.anchorId}</p>
            <p style="margin: 5px 0;"><strong>Vị trí:</strong><br>
               Lat: ${anchor.latitude.toFixed(6)}<br>
               Lng: ${anchor.longitude.toFixed(6)}</p>
            ${anchor.description ? `<p style="margin: 5px 0;"><strong>Mô tả:</strong> ${anchor.description}</p>` : ''}
            <p style="margin: 5px 0;"><strong>Trạng thái:</strong> 
               <span style="color: ${anchor.status === 'online' ? '#4CAF50' : '#f44336'};">
                   ${anchor.status === 'online' ? '🟢 Online' : '🔴 Offline'}
               </span>
            </p>
            <p style="margin: 10px 0 5px 0; font-size: 12px; color: #666; text-align: center;">
                <i class="fas fa-info-circle"></i> Double-click để di chuyển
            </p>
            <button onclick="deleteAnchor(${anchor.id})" style="background: #f44336; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; margin-top: 5px; width: 100%;">
                🗑️ Xóa Anchor
            </button>
        </div>
    `);
    
    // ✅ Double-click to enable drag mode
    marker.on('dblclick', function(e) {
        L.DomEvent.stopPropagation(e);
        enableAnchorDrag(anchor.id);
    });
    
    anchorMarkers.push({ id: anchor.id, marker: marker, anchor: anchor });
}

// ✅ Update all anchor marker sizes based on current zoom level
function updateAnchorSizes() {
    const currentZoom = map.getZoom();
    const baseZoom = 15;
    const zoomDiff = currentZoom - baseZoom;
    
    // Calculate size based on zoom
    const baseSize = 20;
    const size = Math.max(12, Math.min(28, baseSize + (zoomDiff * 2)));
    const fontSize = Math.max(6, Math.min(9, 7 + (zoomDiff * 0.4)));
    const padding = Math.max(1, Math.min(4, 2 + (zoomDiff * 0.4)));
    const borderWidth = Math.max(1, Math.min(2, 1.5 + (zoomDiff * 0.15)));
    
    // Update all anchor markers
    anchorMarkers.forEach(am => {
        const anchor = am.anchor;
        const newIcon = L.divIcon({
            className: 'anchor-marker',
            html: `<div style="background: #2196F3; color: white; padding: ${padding}px ${padding + 2}px; border-radius: 50%; border: ${borderWidth}px solid white; box-shadow: 0 1px 4px rgba(0,0,0,0.3); font-weight: bold; font-size: ${fontSize}px; text-align: center; min-width: ${size - 6}px;">
                        ${anchor.anchorId}
                   </div>`,
            iconSize: [size, size],
            iconAnchor: [size / 2, size / 2]
        });
        
        am.marker.setIcon(newIcon);
    });
}

// ✅ Anchors are now created automatically when drawing work zone polygons
// Each polygon vertex becomes an anchor point

// Delete anchor
function deleteAnchor(anchorId) {
    if (!confirm('Bạn có chắc muốn xóa Anchor này?')) return;
    
    fetch('/api/anchors/' + anchorId, {
        method: 'DELETE'
    })
    .then(response => {
        if (response.ok) {
            console.log('✅ Anchor deleted - waiting for WebSocket sync');
            
            // ❌ KHÔNG xóa marker ngay - để WebSocket broadcast handle
            // WebSocket sẽ broadcast DELETE event và xóa ở tất cả clients
        } else {
            alert('Lỗi khi xóa Anchor!');
        }
    })
    .catch(error => {
        console.error('❌ Error deleting anchor:', error);
        alert('Lỗi khi xóa Anchor!');
    });
}

// ✅ Anchor drag functionality removed - anchors are fixed at polygon vertices
// Anchors are created automatically when drawing work zone polygons

