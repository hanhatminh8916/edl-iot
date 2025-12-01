console.log('manage-helmets.js loaded');

let helmets = [];
let stompClient = null;
let reloadTimeout = null;

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

// WebSocket initialization for realtime updates
function initWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Disable debug logs
    
    stompClient.connect({}, function(frame) {
        console.log('✅ WebSocket connected for helmets page');
        
        // Subscribe to helmet data updates
        stompClient.subscribe('/topic/helmet/data', function(message) {
            console.log('📡 Helmet data received');
            debounceReload();
        });
        
        // Subscribe to alert updates
        stompClient.subscribe('/topic/alerts/new', function(message) {
            console.log('🚨 New alert');
            debounceReload();
        });
        
        stompClient.subscribe('/topic/alerts/update', function(message) {
            console.log('🔄 Alert updated');
            debounceReload();
        });
    }, function(error) {
        console.error('WebSocket error:', error);
        // Retry connection after 5 seconds
        setTimeout(initWebSocket, 5000);
    });
}

// Debounced reload to prevent too many refreshes
function debounceReload() {
    if (reloadTimeout) {
        clearTimeout(reloadTimeout);
    }
    reloadTimeout = setTimeout(() => {
        loadHelmets();
    }, 1000); // Wait 1 second after last message
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
                <button class="btn-action btn-edit" onclick="editHelmet(${helmet.id})">
                    <i class="fas fa-edit"></i> Chỉnh sửa
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

// Edit helmet (placeholder)
function editHelmet(helmetId) {
    const helmet = helmets.find(h => h.id === helmetId);
    if (!helmet) return;
    
    // TODO: Implement edit dialog for helmet information
    alert(`Tính năng chỉnh sửa thông tin mũ #${helmet.helmetId} đang được phát triển.\n\nBạn có thể sửa MAC, trạng thái, và thông tin khác.`);
}

// Show helmet details with modern modal
function showHelmetDetails(helmetId) {
    const helmet = helmets.find(h => h.id === helmetId);
    if (!helmet) return;
    
    const statusClass = helmet.status?.toLowerCase() || 'inactive';
    const statusText = getStatusText(helmet.status);
    const batteryLevel = helmet.batteryLevel || 0;
    const batteryClass = batteryLevel < 20 ? 'low' : batteryLevel < 50 ? 'medium' : 'good';
    const workerName = helmet.worker ? helmet.worker.name : 'Chưa gán';
    const lastSeen = helmet.lastSeen ? new Date(helmet.lastSeen).toLocaleString('vi-VN') : 'Chưa có';
    const createdAt = helmet.createdAt ? new Date(helmet.createdAt).toLocaleString('vi-VN') : 'N/A';
    const location = helmet.lastLat && helmet.lastLon ? `(${helmet.lastLat}, ${helmet.lastLon})` : 'Chưa có';
    
    // Create modal HTML
    const modalHTML = `
        <div class="helmet-modal-overlay" id="helmetDetailModal" onclick="closeHelmetModal(event)">
            <div class="helmet-modal" onclick="event.stopPropagation()">
                <div class="helmet-modal-header">
                    <h3><i class="fas fa-hard-hat"></i> Mũ #${helmet.helmetId}</h3>
                    <span class="helmet-status ${statusClass}">${statusText}</span>
                    <button class="modal-close-btn" onclick="closeHelmetModal()">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="helmet-modal-body">
                    <div class="detail-grid">
                        <div class="detail-row">
                            <span class="detail-label"><i class="fas fa-network-wired"></i> MAC Address</span>
                            <span class="detail-value">${helmet.macAddress || 'N/A'}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label"><i class="fas fa-user"></i> Công nhân</span>
                            <span class="detail-value">${workerName}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label"><i class="fas fa-battery-three-quarters"></i> Pin</span>
                            <span class="detail-value">
                                <div class="battery-indicator ${batteryClass}">
                                    <div class="battery-fill-mini" style="width: ${batteryLevel}%"></div>
                                    <span>${batteryLevel}%</span>
                                </div>
                            </span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label"><i class="fas fa-map-marker-alt"></i> Vị trí cuối</span>
                            <span class="detail-value">${location}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label"><i class="fas fa-clock"></i> Lần cuối thấy</span>
                            <span class="detail-value">${lastSeen}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label"><i class="fas fa-calendar-plus"></i> Tạo lúc</span>
                            <span class="detail-value">${createdAt}</span>
                        </div>
                    </div>
                </div>
                <div class="helmet-modal-footer">
                    <button class="btn-modal btn-edit" onclick="editHelmet(${helmet.id}); closeHelmetModal();">
                        <i class="fas fa-edit"></i> Chỉnh sửa
                    </button>
                    <button class="btn-modal btn-close" onclick="closeHelmetModal()">
                        Đóng
                    </button>
                </div>
            </div>
        </div>
    `;
    
    // Remove existing modal if any
    const existingModal = document.getElementById('helmetDetailModal');
    if (existingModal) existingModal.remove();
    
    // Add modal to body
    document.body.insertAdjacentHTML('beforeend', modalHTML);
}

// Close helmet detail modal
function closeHelmetModal(event) {
    if (event && event.target !== event.currentTarget) return;
    const modal = document.getElementById('helmetDetailModal');
    if (modal) modal.remove();
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
    
    // Initialize WebSocket for realtime updates
    initWebSocket();
    
    // Auto-refresh every 60 seconds as fallback
    setInterval(loadHelmets, 60000);
});
