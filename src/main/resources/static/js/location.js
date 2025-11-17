console.log("location.js loaded");
var map, markers = [], workersData = [], drawnItems = null, activePolygon = null;
var anchorMarkers = []; // Store anchor markers
var isAnchorMode = false; // Toggle anchor placement mode
// Tọa độ tâm khu vực an toàn - ĐÀ NẴNG (cập nhật từ dữ liệu thực tế MQTT)
var safeZoneCenter = [15.97331, 108.25183];
var safeZoneRadius = 200; // Bán kính 200 mét (chỉ để tham khảo, giờ dùng polygon vẽ tay)

function initializeMap() {
    console.log("Init map with Geo-Fencing");
    map = L.map("map").setView(safeZoneCenter, 15);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {maxZoom: 19}).addTo(map);
    
    // ✅ Khởi tạo Leaflet Draw để vẽ polygon (vùng an toàn)
    drawnItems = new L.FeatureGroup();
    map.addLayer(drawnItems);

    const drawControl = new L.Control.Draw({
        draw: {
            polygon: {
                shapeOptions: {
                    color: '#10b981',
                    fillColor: '#10b981',
                    fillOpacity: 0.2
                }
            },
            marker: false,
            circle: false,
            rectangle: false,
            polyline: false,
            circlemarker: false
        },
        edit: { 
            featureGroup: drawnItems,
            remove: true
        }
    });
    map.addControl(drawControl);

    // ✅ Khi vẽ xong polygon → LƯU VÀO DATABASE
    map.on(L.Draw.Event.CREATED, function (e) {
        drawnItems.clearLayers(); // Xóa polygon cũ
        const layer = e.layer;
        drawnItems.addLayer(layer);
        activePolygon = layer;
        document.getElementById("alertBox").style.display = "none";
        console.log("✅ Polygon created:", layer.getLatLngs());
        
        // ✅ LƯU VÀO DATABASE qua API
        saveSafeZoneToDatabase(layer.getLatLngs());
    });

    // ✅ Khi chỉnh sửa polygon
    map.on(L.Draw.Event.EDITED, function (e) {
        const layers = e.layers;
        layers.eachLayer(function (layer) {
            activePolygon = layer;
            console.log("✅ Polygon edited:", layer.getLatLngs());
            saveSafeZoneToDatabase(layer.getLatLngs());
        });
    });

    // ✅ Khi xóa polygon
    map.on(L.Draw.Event.DELETED, function (e) {
        activePolygon = null;
        console.log("🗑️ Polygon deleted");
        // TODO: Có thể gọi API xóa zone nếu cần
    });

    // ✅ LOAD POLYGON TỪ DATABASE khi khởi động
    loadSafeZoneFromDatabase();
    
    // ✅ LOAD ANCHORS FROM DATABASE
    loadAnchorsFromDatabase();
    
    // ✅ ANCHOR PLACEMENT: Click on map to place anchor
    map.on('click', function(e) {
        if (isAnchorMode) {
            placeAnchor(e.latlng);
        }
    });
    
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
    
    // ALERT (đỏ) - Lỗi hệ thống (battery, voltage, current)
    if (status === "ALERT") {
        return '#ef4444'; // Đỏ
    }
    
    // Kiểm tra Geo-Fence (trong/ngoài polygon)
    const inside = isInsidePolygon(lat, lon, activePolygon);
    
    if (!inside) {
        return '#ef4444'; // Đỏ - Ra ngoài vùng an toàn
    }
    
    return '#10b981'; // Xanh lá - An toàn
}
async function loadWorkers() {
    console.log("Loading workers data...");
    try {
        var res = await fetch("/api/location/map-data");
        workersData = await res.json();
        console.log("Loaded:", workersData.length, "workers");
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
    markers.forEach(function(m) { map.removeLayer(m); });
    markers = [];
    
    const alertBox = document.getElementById("alertBox");
    let hasOutOfBounds = false;
    
    workers.forEach(function(w) {
        if (!w.helmet || !w.helmet.lastLocation) return;
        var lat = w.helmet.lastLocation.latitude;
        var lon = w.helmet.lastLocation.longitude;
        var battery = w.helmet.batteryLevel;
        var status = w.helmet.status; // ACTIVE, ALERT, INACTIVE
        
        // ✅ Xác định màu dựa trên polygon và status
        var color = getMarkerColor(lat, lon, status);
        
        // ✅ Kiểm tra ra ngoài vùng an toàn
        const inside = isInsidePolygon(lat, lon, activePolygon);
        if (!inside && status !== "INACTIVE") {
            hasOutOfBounds = true;
        }
        
        // ✅ Tạo text mô tả trạng thái
        var statusText = "";
        if (status === "INACTIVE") {
            statusText = "Offline (vị trí cuối cùng)";
        } else if (!inside) {
            statusText = "⚠️ Ra ngoài vùng an toàn!";
        } else if (status === "ALERT") {
            statusText = "⚠️ Cảnh báo hệ thống";
        } else {
            statusText = "✅ An toàn";
        }
        
        // ✅ Icon với % pin hiển thị
        var icon = L.divIcon({
            className: 'custom-marker-with-label',
            html: "<div style=\"text-align:center;\">" +
                  "<div style=\"background:" + color + ";width:32px;height:32px;border-radius:50%;border:3px solid white;box-shadow:0 2px 8px rgba(0,0,0,0.3);display:flex;align-items:center;justify-content:center;\">" +
                  "<span style=\"color:white;font-weight:bold;font-size:10px;text-shadow:0 1px 2px rgba(0,0,0,0.5);\">" + battery + "%</span>" +
                  "</div></div>",
            iconSize: [32,32], 
            iconAnchor: [16,16]
        });
        
        var m = L.marker([lat, lon], {icon: icon}).addTo(map);
        m.bindPopup("<b>" + w.name + "</b><br>" + 
                   "MAC: " + w.helmet.helmetId + "<br>" +
                   "Pin: " + w.helmet.batteryLevel + "%<br>" +
                   "<b>" + statusText + "</b>");
        m.workerId = w.id;
        markers.push(m);
    });
    
    // ✅ Hiển thị/ẩn Alert Box
    if (hasOutOfBounds) {
        alertBox.style.display = "block";
    } else {
        alertBox.style.display = "none";
    }
    
    if (markers.length) map.fitBounds(L.featureGroup(markers).getBounds().pad(0.1));
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
            
            console.log("✅ Polygon rendered on map");
            showNotification("✅ Đã tải khu vực an toàn từ server!", "success");
        }
        
    } catch (error) {
        console.error("❌ Error loading safe zone:", error);
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
        
    }, function(error) {
        console.error('❌ WebSocket connection error:', error);
        // Retry after 5 seconds
        setTimeout(connectWebSocket, 5000);
    });
}

function updateMarkerRealtime(data) {
    if (!data.latitude || !data.longitude) {
        console.log('⚠️ No GPS data for', data.mac);
        return;
    }
    
    const lat = parseFloat(data.latitude);
    const lon = parseFloat(data.longitude);
    
    // Find existing marker by MAC
    let existingMarker = markers.find(m => m.mac === data.mac);
    
    if (existingMarker) {
        // Update existing marker position
        existingMarker.marker.setLatLng([lat, lon]);
        
        // Update marker icon based on mode/status
        const isInDanger = data.mode === 'ANCHOR' || data.battery < 20;
        const iconUrl = isInDanger ? 
            'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png' :
            'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png';
        
        existingMarker.marker.setIcon(L.icon({
            iconUrl: iconUrl,
            shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
            iconSize: [25, 41],
            iconAnchor: [12, 41],
            popupAnchor: [1, -34],
            shadowSize: [41, 41]
        }));
        
        // Update popup content
        const status = isInDanger ? '🔴 NGUY HIỂM' : '🟢 AN TOÀN';
        const popupContent = `
            <strong>MAC: ${data.mac}</strong><br>
            Status: ${status}<br>
            Battery: ${data.battery}%<br>
            Mode: ${data.mode || 'N/A'}
        `;
        existingMarker.marker.setPopupContent(popupContent);
        
        console.log(`✅ Updated marker for ${data.mac}`);
    } else {
        // Create new marker if not exists
        console.log(`➕ Creating new marker for ${data.mac}`);
        loadWorkers(); // Reload all workers to get complete data
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
        
        console.log('✅ Polygon drawn:', safeZone.zoneName);
        
        // Fit map to polygon bounds
        if (coords && coords.length > 0) {
            map.fitBounds(polygon.getBounds());
        }
        
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
    
    // ✅ ANCHOR MODE TOGGLE BUTTON
    const toggleAnchorBtn = document.getElementById('toggleAnchorMode');
    if (toggleAnchorBtn) {
        toggleAnchorBtn.addEventListener('click', function() {
            isAnchorMode = !isAnchorMode;
            
            if (isAnchorMode) {
                this.classList.remove('btn-secondary');
                this.classList.add('btn-primary');
                this.innerHTML = '<i class="fas fa-map-pin"></i> Đang đặt Anchor (Click vào bản đồ)';
                map.getContainer().style.cursor = 'crosshair';
            } else {
                this.classList.remove('btn-primary');
                this.classList.add('btn-secondary');
                this.innerHTML = '<i class="fas fa-map-pin"></i> Đặt Anchor';
                map.getContainer().style.cursor = '';
            }
        });
    }
});

// ========== ANCHOR FUNCTIONS ==========

// Load all anchors from database
function loadAnchorsFromDatabase() {
    fetch('/api/anchors')
        .then(response => response.json())
        .then(anchors => {
            console.log('✅ Loaded anchors from DB:', anchors);
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
    const anchorIcon = L.divIcon({
        className: 'anchor-marker',
        html: `<div style="background: #2196F3; color: white; padding: 8px 12px; border-radius: 50%; border: 3px solid white; box-shadow: 0 2px 8px rgba(0,0,0,0.3); font-weight: bold; font-size: 12px; text-align: center; min-width: 40px;">
                    ${anchor.anchorId}
               </div>`,
        iconSize: [50, 50],
        iconAnchor: [25, 25]
    });
    
    const marker = L.marker([anchor.latitude, anchor.longitude], {
        icon: anchorIcon,
        draggable: false
    }).addTo(map);
    
    // Popup with anchor info
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
            <button onclick="deleteAnchor(${anchor.id})" style="background: #f44336; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; margin-top: 10px; width: 100%;">
                🗑️ Xóa Anchor
            </button>
        </div>
    `);
    
    anchorMarkers.push({ id: anchor.id, marker: marker });
}

// Place new anchor on map click
function placeAnchor(latlng) {
    const name = prompt('Nhập tên Anchor:', 'Anchor ' + (anchorMarkers.length + 1));
    if (!name) return;
    
    const description = prompt('Nhập mô tả (tùy chọn):', '');
    
    const anchorData = {
        name: name,
        latitude: latlng.lat,
        longitude: latlng.lng,
        description: description || '',
        status: 'online'
    };
    
    // Save to database
    fetch('/api/anchors', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(anchorData)
    })
    .then(response => response.json())
    .then(anchor => {
        console.log('✅ Anchor saved:', anchor);
        addAnchorMarker(anchor);
        
        // Turn off anchor mode
        isAnchorMode = false;
        const toggleBtn = document.getElementById('toggleAnchorMode');
        if (toggleBtn) {
            toggleBtn.classList.remove('btn-primary');
            toggleBtn.classList.add('btn-secondary');
            toggleBtn.innerHTML = '<i class="fas fa-map-pin"></i> Đặt Anchor';
            map.getContainer().style.cursor = '';
        }
    })
    .catch(error => {
        console.error('❌ Error saving anchor:', error);
        alert('Lỗi khi lưu Anchor!');
    });
}

// Delete anchor
function deleteAnchor(anchorId) {
    if (!confirm('Bạn có chắc muốn xóa Anchor này?')) return;
    
    fetch('/api/anchors/' + anchorId, {
        method: 'DELETE'
    })
    .then(response => {
        if (response.ok) {
            console.log('✅ Anchor deleted');
            
            // Remove marker from map
            const anchorMarker = anchorMarkers.find(a => a.id === anchorId);
            if (anchorMarker) {
                map.removeLayer(anchorMarker.marker);
                anchorMarkers = anchorMarkers.filter(a => a.id !== anchorId);
            }
        } else {
            alert('Lỗi khi xóa Anchor!');
        }
    })
    .catch(error => {
        console.error('❌ Error deleting anchor:', error);
        alert('Lỗi khi xóa Anchor!');
    });
}
