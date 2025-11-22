console.log('manage-helmets.js loaded');

let helmets = [];

// Load all helmets on page load
async function loadHelmets() {
    try {
        const response = await fetch('/api/helmet/all');
        helmets = await response.json();
        console.log('✅ Loaded helmets:', helmets);
        
        renderHelmets();
        updateStatistics();
    } catch (error) {
        console.error('❌ Error loading helmets:', error);
        showNotification('Lỗi khi tải danh sách mũ', 'error');
    }
}

// Render helmets as cards
function renderHelmets() {
    const grid = document.getElementById('helmetGrid');
    
    if (helmets.length === 0) {
        grid.innerHTML = `
            <div style="grid-column: 1 / -1; text-align: center; padding: 40px; color: #6b7280;">
                <i class="fas fa-hard-hat" style="font-size: 3rem; margin-bottom: 10px;"></i>
                <p>Chưa có mũ bảo hiểm nào trong hệ thống</p>
                <p style="font-size: 0.9rem;">Mũ sẽ tự động được thêm khi nhận dữ liệu từ MQTT</p>
            </div>
        `;
        return;
    }
    
    grid.innerHTML = helmets.map(helmet => createHelmetCard(helmet)).join('');
}

// Create helmet card HTML
function createHelmetCard(helmet) {
    const batteryLevel = helmet.batteryLevel || 0;
    const batteryClass = batteryLevel < 20 ? 'low' : batteryLevel < 50 ? 'medium' : '';
    const statusText = getStatusText(helmet.status);
    const workerName = helmet.worker ? helmet.worker.name : 'Chưa gán';
    const lastSeen = helmet.lastSeen ? new Date(helmet.lastSeen).toLocaleString('vi-VN') : 'Chưa có';
    
    return `
        <div class="helmet-card">
            <div class="helmet-header">
                <div class="helmet-id">
                    <i class="fas fa-hard-hat"></i> Mũ #${helmet.helmetId}
                </div>
                <span class="helmet-status ${helmet.status}">${statusText}</span>
            </div>
            
            <div class="helmet-info">
                <div class="helmet-info-row">
                    <i class="fas fa-network-wired"></i>
                    <span><strong>MAC:</strong> ${helmet.macAddress || 'N/A'}</span>
                </div>
                <div class="helmet-info-row">
                    <i class="fas fa-user"></i>
                    <span><strong>Công nhân:</strong> ${workerName}</span>
                </div>
                <div class="helmet-info-row">
                    <i class="fas fa-clock"></i>
                    <span><strong>Lần cuối:</strong> ${lastSeen}</span>
                </div>
                <div class="helmet-info-row">
                    <i class="fas fa-battery-three-quarters"></i>
                    <span><strong>Pin:</strong></span>
                </div>
                <div class="battery-bar">
                    <div class="battery-fill ${batteryClass}" style="width: ${batteryLevel}%">
                        ${batteryLevel}%
                    </div>
                </div>
            </div>
            
            <div class="helmet-actions">
                <button class="btn-action btn-assign" onclick="showAssignDialog(${helmet.id})">
                    <i class="fas fa-user-plus"></i> Gán
                </button>
                <button class="btn-action btn-details" onclick="showHelmetDetails(${helmet.id})">
                    <i class="fas fa-info-circle"></i> Chi tiết
                </button>
            </div>
        </div>
    `;
}

// Get status text in Vietnamese
function getStatusText(status) {
    const statusMap = {
        'ACTIVE': 'Hoạt động',
        'INACTIVE': 'Không hoạt động',
        'ALERT': 'Cảnh báo',
        'OFFLINE': 'Offline'
    };
    return statusMap[status] || status;
}

// Update statistics
function updateStatistics() {
    const total = helmets.length;
    const active = helmets.filter(h => h.status === 'ACTIVE').length;
    const assigned = helmets.filter(h => h.worker !== null).length;
    const lowBattery = helmets.filter(h => (h.batteryLevel || 0) < 20).length;
    
    document.getElementById('totalHelmets').textContent = total;
    document.getElementById('activeHelmets').textContent = active;
    document.getElementById('assignedHelmets').textContent = assigned;
    document.getElementById('lowBatteryHelmets').textContent = lowBattery;
}

// Refresh helmets
function refreshHelmets() {
    loadHelmets();
    showNotification('Đã làm mới danh sách mũ', 'success');
}

// Show assign dialog (placeholder)
function showAssignDialog(helmetId) {
    const helmet = helmets.find(h => h.id === helmetId);
    if (!helmet) return;
    
    // TODO: Implement assign dialog with employee selection
    alert(`Tính năng gán mũ #${helmet.helmetId} cho công nhân đang được phát triển.\n\nSẽ hiển thị modal chọn công nhân.`);
}

// Show helmet details (placeholder)
function showHelmetDetails(helmetId) {
    const helmet = helmets.find(h => h.id === helmetId);
    if (!helmet) return;
    
    const details = `
Thông tin chi tiết Mũ #${helmet.helmetId}
━━━━━━━━━━━━━━━━━━━━━━━━━━
MAC Address: ${helmet.macAddress || 'N/A'}
Trạng thái: ${getStatusText(helmet.status)}
Pin: ${helmet.batteryLevel || 0}%
Công nhân: ${helmet.worker ? helmet.worker.name : 'Chưa gán'}
Vị trí cuối: ${helmet.lastLat && helmet.lastLon ? `(${helmet.lastLat}, ${helmet.lastLon})` : 'N/A'}
Lần cuối thấy: ${helmet.lastSeen ? new Date(helmet.lastSeen).toLocaleString('vi-VN') : 'N/A'}
Tạo lúc: ${helmet.createdAt ? new Date(helmet.createdAt).toLocaleString('vi-VN') : 'N/A'}
    `;
    
    alert(details);
}

// Show notification
function showNotification(message, type = 'info') {
    // Simple console notification for now
    console.log(`[${type.toUpperCase()}] ${message}`);
    
    // TODO: Implement toast notification UI
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    console.log('📱 Helmet management page loaded');
    loadHelmets();
    
    // Auto-refresh every 30 seconds
    setInterval(loadHelmets, 30000);
});
