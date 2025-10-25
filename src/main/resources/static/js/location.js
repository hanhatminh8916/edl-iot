console.log("location.js loaded");
var map, markers = [], workersData = [], safeZoneCircle = null;
// Tọa độ tâm khu vực an toàn (sau này sẽ lấy từ thiết bị beacon/anchor)
var safeZoneCenter = [10.7626, 106.6601];
var safeZoneRadius = 200; // Bán kính 200 mét

function initializeMap() {
    console.log("Init map");
    map = L.map("map").setView(safeZoneCenter, 15);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {maxZoom: 19}).addTo(map);
    
    // Thêm vòng tròn màu xanh - khu vực an toàn
    safeZoneCircle = L.circle(safeZoneCenter, {
        color: '#10b981',      // Màu viền xanh
        fillColor: '#10b981',  // Màu tô xanh
        fillOpacity: 0.2,      // Độ trong suốt 20%
        radius: safeZoneRadius // Bán kính 200 mét
    }).addTo(map);
    
    safeZoneCircle.bindPopup('<b>Khu vực an toàn</b><br>Bán kính: ' + safeZoneRadius + 'm');
    
    // Thêm nhãn Hoàng Sa (Paracel Islands)
    var hoangSaIcon = L.divIcon({
        className: 'island-label',
        html: '<div style="background:rgba(255,255,255,0.95);padding:6px 12px;border-radius:6px;border:2px solid #ef4444;box-shadow:0 2px 8px rgba(0,0,0,0.2);white-space:nowrap;font-weight:bold;color:#1f2937;font-size:13px;">🇻🇳 Quần đảo HOÀNG SA<br><span style="font-size:11px;color:#6b7280;">(Việt Nam)</span></div>',
        iconSize: [140, 45],
        iconAnchor: [70, 22]
    });
    L.marker([16.5, 112.0], {icon: hoangSaIcon}).addTo(map);
    
    // Thêm nhãn Trường Sa (Spratly Islands)
    var truongSaIcon = L.divIcon({
        className: 'island-label',
        html: '<div style="background:rgba(255,255,255,0.95);padding:6px 12px;border-radius:6px;border:2px solid #ef4444;box-shadow:0 2px 8px rgba(0,0,0,0.2);white-space:nowrap;font-weight:bold;color:#1f2937;font-size:13px;">🇻🇳 Quần đảo TRƯỜNG SA<br><span style="font-size:11px;color:#6b7280;">(Việt Nam)</span></div>',
        iconSize: [140, 45],
        iconAnchor: [70, 22]
    });
    L.marker([10.0, 114.0], {icon: truongSaIcon}).addTo(map);
    
    // Thêm text Hoàng Sa, Trường Sa thuộc Việt Nam
    var sovereigntyControl = L.control({position: 'bottomright'});
    sovereigntyControl.onAdd = function(map) {
        var div = L.DomUtil.create('div', 'sovereignty-note');
        div.innerHTML = '<span style="font-size:10px;color:#6b7280;background:rgba(255,255,255,0.9);padding:4px 8px;border-radius:4px;box-shadow:0 1px 4px rgba(0,0,0,0.1);display:inline-block;">🇻🇳 Hoàng Sa, Trường Sa thuộc Việt Nam</span>';
        return div;
    };
    sovereigntyControl.addTo(map);
    
    console.log("Map ready");
}

// Hàm tính khoảng cách giữa 2 điểm (Haversine formula)
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

// Hàm xác định màu marker dựa trên khoảng cách từ tâm
function getMarkerColorByDistance(distance) {
    var percent = (distance / safeZoneRadius) * 100;
    
    if (percent <= 80) {
        return '#10b981'; // Xanh lá - An toàn (0-80%)
    } else if (percent <= 100) {
        return '#f97316'; // Cam - Gần ra ngoài (80-100%)
    } else {
        return '#ef4444'; // Đỏ - Ngoài vòng (>100%)
    }
}
async function loadWorkers() {
    console.log("Loading...");
    try {
        var res = await fetch("/api/dashboard/map-data");
        workersData = await res.json();
        console.log("Loaded:", workersData.length);
        updateMapMarkers(workersData);
        displayWorkersList(workersData);
    } catch(e) { console.error(e); }
}
function updateMapMarkers(workers) {
    markers.forEach(function(m) { map.removeLayer(m); });
    markers = [];
    workers.forEach(function(w) {
        if (!w.helmet || !w.helmet.lastLocation) return;
        var lat = w.helmet.lastLocation.latitude;
        var lon = w.helmet.lastLocation.longitude;
        var battery = w.helmet.batteryLevel;
        
        // Tính khoảng cách từ worker đến tâm vòng tròn an toàn
        var distance = calculateDistance(
            safeZoneCenter[0], safeZoneCenter[1],
            lat, lon
        );
        
        // Xác định màu dựa trên khoảng cách (ưu tiên cao hơn status)
        var color = getMarkerColorByDistance(distance);
        
        // Nếu helmet INACTIVE (offline) thì vẫn hiển thị màu xám
        if (w.helmet.status === "INACTIVE") {
            color = "#6b7280"; // Xám - Offline
        }
        
        // Tạo text mô tả trạng thái
        var statusText = "";
        var distancePercent = Math.round((distance / safeZoneRadius) * 100);
        if (w.helmet.status === "INACTIVE") {
            statusText = "Offline";
        } else if (distance > safeZoneRadius) {
            statusText = "Ngoài khu vực (" + Math.round(distance) + "m)";
        } else if (distancePercent > 80) {
            statusText = "Gần biên (" + distancePercent + "%)";
        } else {
            statusText = "An toàn (" + Math.round(distance) + "m)";
        }
        
        // Icon với % pin hiển thị
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
                   w.helmet.helmetId + "<br>" +
                   "Pin: " + w.helmet.batteryLevel + "%<br>" +
                   "<b>" + statusText + "</b>");
        m.workerId = w.id;
        markers.push(m);
    });
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
window.addEventListener("load", function() {
    console.log("Page loaded");
    if (typeof L !== "undefined") {
        initializeMap();
        setTimeout(loadWorkers, 500);
    }
    setInterval(loadWorkers, 10000);
    
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