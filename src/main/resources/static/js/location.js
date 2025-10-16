console.log("location.js loaded");
var map, markers = [], workersData = [], safeZoneCircle = null;
function initializeMap() {
    console.log("Init map");
    map = L.map("map").setView([10.7626, 106.6601], 15);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {maxZoom: 19}).addTo(map);
    
    // Thêm vòng tròn màu xanh - khu vực an toàn
    safeZoneCircle = L.circle([10.7626, 106.6601], {
        color: '#10b981',      // Màu viền xanh
        fillColor: '#10b981',  // Màu tô xanh
        fillOpacity: 0.2,      // Độ trong suốt 20%
        radius: 200            // Bán kính 200 mét
    }).addTo(map);
    
    safeZoneCircle.bindPopup('<b>Khu vực an toàn</b><br>Bán kính: 200m');
    
    console.log("Map ready");
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
        var color = "#10b981";
        if (w.helmet.status === "ALERT") color = "#f59e0b";
        if (w.helmet.status === "INACTIVE") color = "#ef4444";
        
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
        m.bindPopup("<b>" + w.name + "</b><br>" + w.helmet.helmetId + "<br>Pin: " + w.helmet.batteryLevel + "%");
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
});