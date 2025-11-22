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
    
    // ✅ Store draw control globally to update later
    window.currentDrawControl = drawControl;

    // ✅ Khi vẽ xong polygon → LƯU VÀO DATABASE
    map.on(L.Draw.Event.CREATED, function (e) {
        const layer = e.layer;
        
        if (drawingMode === 'workzone') {
            // ✅ Vẽ Work Zone màu vàng
            layer.setStyle({
                color: '#FFA500',
                fillColor: '#FFA500',
                fillOpacity: 0.3
            });
            workZonesLayer.addLayer(layer);
            
            // ✅ TỰ ĐỘNG TẠO ANCHORS TỪ CÁC ĐIỂM POLYGON
            const vertices = layer.getLatLngs()[0]; // Lấy các đỉnh polygon
            const zoneName = prompt('Nhập tên khu vực:', `Khu ${workZonesLayer.getLayers().length}`);
            
            if (zoneName) {
                layer.bindPopup(`<b>${zoneName}</b><br><small>Double-click để xem chi tiết sơ đồ 2D</small>`).openPopup();
                layer.zoneName = zoneName;
                
                // ✅ Double-click vào zone để xem sơ đồ 2D (tránh conflict với edit mode)
                layer.on('dblclick', function(e) {
                    L.DomEvent.stopPropagation(e);
                    window.location.href = `positioning-2d.html?zone=${layer.zoneId || ''}`;
                });
                
                // ✅ LƯU WORK ZONE VÀO DATABASE trước
                saveWorkZoneToDatabase(layer.getLatLngs(), layer, zoneName).then(async zoneId => {
                    // ✅ Lấy số anchor hiện tại để tạo ID tuần tự
                    const currentMaxId = await getMaxAnchorId();
                    
                    // ✅ Tạo anchor cho mỗi đỉnh polygon
                    for (let index = 0; index < vertices.length; index++) {
                        const vertex = vertices[index];
                        const anchorNumber = currentMaxId + index + 1;
                        const anchorName = `${zoneName}-A${index + 1}`;
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
        layers.eachLayer(function (layer) {
            activePolygon = layer;
            console.log("✅ Polygon edited:", layer.getLatLngs());
            saveSafeZoneToDatabase(layer.getLatLngs());
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
                // ✅ Đây là safe zone
                activePolygon = null;
                console.log("🗑️ Safe zone deleted");
            }
        });
    });

    // ✅ LOAD POLYGON TỪ DATABASE khi khởi động
    loadSafeZoneFromDatabase();
    
    // ✅ LOAD WORK ZONES FROM DATABASE
    loadWorkZonesFromDatabase();
    
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
});

// ✅ Toggle Anchor Mode Function
function toggleAnchorMode() {
    isAnchorMode = !isAnchorMode;
    
    const anchorButton = document.querySelector('.leaflet-control-anchor');
    
    if (isAnchorMode) {
        if (anchorButton) {
            anchorButton.style.background = '#4CAF50';
            anchorButton.style.color = 'white';
        }
        map.getContainer().style.cursor = 'crosshair';
        showNotification('📍 Click vào bản đồ để đặt Anchor', 'info');
    } else {
        if (anchorButton) {
            anchorButton.style.background = 'white';
            anchorButton.style.color = '#2196F3';
        }
        map.getContainer().style.cursor = '';
    }
}

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
                fillOpacity: 0.3
            });
            
            polygon.zoneId = zone.id;
            polygon.zoneName = zone.name;
            polygon.bindPopup(`<b>${zone.name}</b><br><small>Double-click để xem chi tiết sơ đồ 2D</small>`);
            
            // Double-click to view 2D diagram
            polygon.on('dblclick', function(e) {
                L.DomEvent.stopPropagation(e);
                window.location.href = `positioning-2d.html?zone=${zone.id}`;
            });
            
            workZonesLayer.addLayer(polygon);
            console.log(`✅ Loaded work zone: ${zone.name}`);
        });
    } catch (error) {
        console.error('Error loading work zones:', error);
    }
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

// ✅ LẤY SỐ ANCHOR LỚN NHẤT HIỆN TẠI
async function getMaxAnchorId() {
    try {
        const response = await fetch('/api/anchors');
        const anchors = await response.json();
        
        // Tìm số lớn nhất từ anchorId dạng A1, A2, A3...
        const maxId = anchors
            .map(a => a.anchorId)
            .filter(id => id && id.match(/^A\d+$/))
            .map(id => parseInt(id.substring(1)))
            .reduce((max, num) => Math.max(max, num), 0);
        
        return maxId;
    } catch (error) {
        console.error('Error getting max anchor ID:', error);
        return 0;
    }
}

// ✅ TẠO ANCHOR TỪ ĐỈNH POLYGON
async function createAnchorFromVertex(vertex, anchorName, zoneId, anchorIndex) {
    try {
        // Generate sequential anchorId: A1, A2, A3...
        const anchorId = `A${anchorIndex}`;
        
        const payload = {
            anchorId: anchorId,  // A1, A2, A3...
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
    });
    
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
        
        // ❌ KHÔNG add marker ngay - để WebSocket broadcast handle
        // Refresh lại danh sách anchor để đồng bộ
        setTimeout(() => {
            loadAnchorsFromDatabase();
        }, 500);
        
        // Turn off anchor mode
        isAnchorMode = false;
        
        // Update button UI
        const anchorButton = document.querySelector('.leaflet-control-anchor');
        if (anchorButton) {
            anchorButton.style.background = 'white';
            anchorButton.style.color = '#2196F3';
        }
        map.getContainer().style.cursor = '';
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

// Enable drag mode for anchor
function enableAnchorDrag(anchorId) {
    const anchorMarker = anchorMarkers.find(a => a.id === anchorId);
    if (!anchorMarker) return;
    
    const marker = anchorMarker.marker;
    
    // Enable dragging
    marker.dragging.enable();
    marker.closePopup();
    
    // Change cursor
    map.getContainer().style.cursor = 'move';
    
    // Show notification
    alert('📌 Kéo thả Anchor đến vị trí mới, sau đó nhấn "Lưu vị trí"');
    
    // Update popup to show Save button
    marker.bindPopup(`
        <div style="min-width: 200px; text-align: center;">
            <h3 style="margin: 0 0 10px 0; color: #FF9800;">📌 Đang di chuyển...</h3>
            <p style="margin: 10px 0; font-size: 14px; color: #666;">Kéo marker đến vị trí mới</p>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 5px; margin-top: 10px;">
                <button onclick="saveAnchorPosition(${anchorId})" style="background: #4CAF50; color: white; border: none; padding: 8px 12px; border-radius: 4px; cursor: pointer; font-weight: bold;">
                    ✅ Lưu vị trí
                </button>
                <button onclick="cancelAnchorDrag(${anchorId})" style="background: #9E9E9E; color: white; border: none; padding: 8px 12px; border-radius: 4px; cursor: pointer;">
                    ❌ Hủy
                </button>
            </div>
        </div>
    `).openPopup();
}

// Save new anchor position
function saveAnchorPosition(anchorId) {
    const anchorMarker = anchorMarkers.find(a => a.id === anchorId);
    if (!anchorMarker) return;
    
    const marker = anchorMarker.marker;
    const newLatLng = marker.getLatLng();
    
    // Update anchor position via API
    fetch('/api/anchors/' + anchorId, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            latitude: newLatLng.lat,
            longitude: newLatLng.lng
        })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    })
    .then(updatedAnchor => {
        console.log('✅ Anchor position updated:', updatedAnchor);
        
        // Disable dragging
        marker.dragging.disable();
        map.getContainer().style.cursor = '';
        
        // Update anchor data
        anchorMarker.anchor = updatedAnchor;
        
        // Restore original popup
        marker.bindPopup(`
            <div style="min-width: 200px;">
                <h3 style="margin: 0 0 10px 0; color: #2196F3;">📍 ${updatedAnchor.name}</h3>
                <p style="margin: 5px 0;"><strong>ID:</strong> ${updatedAnchor.anchorId}</p>
                <p style="margin: 5px 0;"><strong>Vị trí:</strong><br>
                   Lat: ${updatedAnchor.latitude.toFixed(6)}<br>
                   Lng: ${updatedAnchor.longitude.toFixed(6)}</p>
                ${updatedAnchor.description ? `<p style="margin: 5px 0;"><strong>Mô tả:</strong> ${updatedAnchor.description}</p>` : ''}
                <p style="margin: 5px 0;"><strong>Trạng thái:</strong> 
                   <span style="color: ${updatedAnchor.status === 'online' ? '#4CAF50' : '#f44336'};">
                       ${updatedAnchor.status === 'online' ? '🟢 Online' : '🔴 Offline'}
                   </span>
                </p>
                <p style="margin: 10px 0 5px 0; font-size: 12px; color: #666; text-align: center;">
                    <i class="fas fa-info-circle"></i> Double-click để di chuyển
                </p>
                <button onclick="deleteAnchor(${updatedAnchor.id})" style="background: #f44336; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; margin-top: 5px; width: 100%;">
                    🗑️ Xóa Anchor
                </button>
            </div>
        `);
        
        // Close popup and show success message
        marker.closePopup();
        
        // Show success notification instead of alert
        showNotification('✅ Đã lưu vị trí Anchor mới!', 'success');
    })
    .catch(error => {
        console.error('❌ Error updating anchor position:', error);
        
        // Disable dragging on error
        marker.dragging.disable();
        map.getContainer().style.cursor = '';
        
        showNotification('❌ Lỗi khi lưu vị trí!', 'error');
    });
}

// Cancel anchor drag
function cancelAnchorDrag(anchorId) {
    const anchorMarker = anchorMarkers.find(a => a.id === anchorId);
    if (!anchorMarker) return;
    
    const marker = anchorMarker.marker;
    const anchor = anchorMarker.anchor;
    
    // Reset to original position
    marker.setLatLng([anchor.latitude, anchor.longitude]);
    
    // Disable dragging
    marker.dragging.disable();
    map.getContainer().style.cursor = '';
    
    // Restore original popup
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
    `).openPopup();
}
