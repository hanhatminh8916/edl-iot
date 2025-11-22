// Alerts Page JavaScript
const API_BASE_URL = '/api';
let allAlerts = [];

document.addEventListener('DOMContentLoaded', function() {
    // Initialize alerts - load from API
    loadAlerts();
    
    // Connect WebSocket for realtime alerts
    connectWebSocket();
    
    // Handle search
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('input', function(e) {
            filterAlerts();
        });
    }
    
    // Handle filter changes
    const filterStatus = document.getElementById('filterStatus');
    const filterSeverity = document.getElementById('filterSeverity');
    
    if (filterStatus) {
        filterStatus.addEventListener('change', function() {
            filterAlerts();
        });
    }
    
    if (filterSeverity) {
        filterSeverity.addEventListener('change', function() {
            filterAlerts();
        });
    }
    
    // Handle checkboxes
    setupCheckboxes();
    
    // Auto refresh alerts every 30 seconds
    setInterval(loadAlerts, 30000);
});

// Load alerts from API
async function loadAlerts() {
    try {
        const response = await fetch(`${API_BASE_URL}/alerts/today`);
        allAlerts = await response.json();
        
        displayAlerts(allAlerts);
        updateAlertCounts();
        updateStatistics();
    } catch (error) {
        console.error('Error loading alerts:', error);
        showNotification('Không thể tải dữ liệu cảnh báo', 'error');
    }
}

// Display alerts in table
function displayAlerts(alerts) {
    const tableBody = document.querySelector('.alerts-table tbody');
    if (!tableBody) return;
    
    if (alerts.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 2rem;">
                    <i class="fas fa-inbox" style="font-size: 3rem; color: #cbd5e1; margin-bottom: 1rem;"></i>
                    <p style="color: #64748b;">Không có cảnh báo nào</p>
                </td>
            </tr>
        `;
        return;
    }
    
    tableBody.innerHTML = alerts.map(alert => {
        const severityClass = alert.severity?.toLowerCase() || 'info';
        const statusClass = alert.status?.toLowerCase() || 'pending';
        
        return `
            <tr data-alert-id="${alert.id}">
                <td><input type="checkbox" class="alert-checkbox" data-id="${alert.id}"></td>
                <td>#${alert.id}</td>
                <td>${formatDateTime(alert.triggeredAt)}</td>
                <td>Công nhân #${alert.helmet?.helmetId || 'N/A'}</td>
                <td>${getAlertTypeText(alert.alertType)}</td>
                <td><span class="badge badge-${severityClass}">${getSeverityText(alert.severity)}</span></td>
                <td><span class="badge badge-${statusClass}">${getStatusText(alert.status)}</span></td>
                <td>
                    <button class="btn-action view" onclick="viewAlert(${alert.id})" title="Xem chi tiết">
                        <i class="fas fa-eye"></i>
                    </button>
                    ${alert.status === 'PENDING' ? `
                        <button class="btn-action acknowledge" onclick="acknowledgeAlert(${alert.id})" title="Xác nhận">
                            <i class="fas fa-check"></i>
                        </button>
                        <button class="btn-action resolve" onclick="resolveAlert(${alert.id})" title="Giải quyết">
                            <i class="fas fa-check-double"></i>
                        </button>
                    ` : ''}
                </td>
            </tr>
        `;
    }).join('');
    
    setupActionButtons();
}

// Update alert statistics
function updateAlertCounts() {
    if (allAlerts.length === 0) return;
    
    const pending = allAlerts.filter(a => a.status === 'PENDING').length;
    const acknowledged = allAlerts.filter(a => a.status === 'ACKNOWLEDGED').length;
    const resolved = allAlerts.filter(a => a.status === 'RESOLVED').length;
    const critical = allAlerts.filter(a => a.severity === 'CRITICAL').length;
    
    const statCards = document.querySelectorAll('.stat-card .stat-value');
    if (statCards[0]) statCards[0].textContent = allAlerts.length;
    if (statCards[1]) statCards[1].textContent = pending;
    if (statCards[2]) statCards[2].textContent = acknowledged;
    if (statCards[3]) statCards[3].textContent = resolved;
    if (statCards[4]) statCards[4].textContent = critical;
}

async function updateStatistics() {
    try {
        const response = await fetch(`${API_BASE_URL}/alerts/statistics?days=7`);
        const stats = await response.json();
        // Update UI with stats if needed
    } catch (error) {
        console.error('Error loading statistics:', error);
    }
}

// Setup action buttons
function setupActionButtons() {
    // Buttons are now set up inline with onclick attributes
    // No additional setup needed
}

// Setup checkboxes
function setupCheckboxes() {
    const checkboxes = document.querySelectorAll('.alert-checkbox');
    checkboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            const checkedCount = document.querySelectorAll('.alert-checkbox:checked').length;
            if (checkedCount > 0) {
                console.log(`${checkedCount} alerts selected`);
            }
        });
    });
}

// Filter alerts
function filterAlerts() {
    const searchTerm = document.getElementById('searchInput')?.value.toLowerCase() || '';
    const statusFilter = document.getElementById('filterStatus')?.value || 'all';
    const severityFilter = document.getElementById('filterSeverity')?.value || 'all';
    
    let filtered = allAlerts.filter(alert => {
        // Search filter
        const matchesSearch = !searchTerm || 
            alert.id.toString().includes(searchTerm) ||
            alert.helmet?.helmetId?.toLowerCase().includes(searchTerm) ||
            getAlertTypeText(alert.alertType).toLowerCase().includes(searchTerm);
        
        // Status filter
        const matchesStatus = statusFilter === 'all' || alert.status === statusFilter;
        
        // Severity filter
        const matchesSeverity = severityFilter === 'all' || alert.severity === severityFilter;
        
        return matchesSearch && matchesStatus && matchesSeverity;
    });
    
    displayAlerts(filtered);
}

// View alert detail
function viewAlert(alertId) {
    const alert = allAlerts.find(a => a.id === alertId);
    if (!alert) return;
    
    const message = `Chi tiết cảnh báo #${alert.id}:\n\n` +
        `Công nhân: ${alert.helmet?.helmetId || 'N/A'}\n` +
        `Loại: ${getAlertTypeText(alert.alertType)}\n` +
        `Mức độ: ${getSeverityText(alert.severity)}\n` +
        `Trạng thái: ${getStatusText(alert.status)}\n` +
        `Thời gian: ${formatDateTime(alert.triggeredAt)}\n\n` +
        `Bạn có muốn xem vị trí trên bản đồ?`;
    
    if (confirm(message)) {
        window.location.href = `location.html?helmetId=${alert.helmet?.helmetId}`;
    }
}

// Acknowledge alert
async function acknowledgeAlert(alertId) {
    const alert = allAlerts.find(a => a.id === alertId);
    if (!alert) return;
    
    const workerName = alert.helmet?.helmetId || 'N/A';
    
    if (!confirm(`Xác nhận đã nhận cảnh báo từ Công nhân ${workerName}?`)) {
        return;
    }
    
    try {
        const username = 'Admin'; // In production, get from logged-in user
        const response = await fetch(`${API_BASE_URL}/alerts/${alertId}/acknowledge?username=${username}`, {
            method: 'POST'
        });
        
        if (response.ok) {
            showNotification(`Đã xác nhận cảnh báo từ Công nhân ${workerName}`, 'success');
            // Reload alerts to get updated data
            await loadAlerts();
        } else {
            throw new Error('Failed to acknowledge alert');
        }
    } catch (error) {
        console.error('Error acknowledging alert:', error);
        showNotification('Không thể xác nhận cảnh báo', 'error');
    }
}

// Resolve alert
async function resolveAlert(alertId) {
    const alert = allAlerts.find(a => a.id === alertId);
    if (!alert) return;
    
    const workerName = alert.helmet?.helmetId || 'N/A';
    
    if (!confirm(`Đánh dấu cảnh báo từ Công nhân ${workerName} đã được giải quyết?`)) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/alerts/${alertId}/resolve`, {
            method: 'POST'
        });
        
        if (response.ok) {
            showNotification(`Đã giải quyết cảnh báo từ Công nhân ${workerName}`, 'success');
            // Reload alerts to get updated data
            await loadAlerts();
        } else {
            throw new Error('Failed to resolve alert');
        }
    } catch (error) {
        console.error('Error resolving alert:', error);
        showNotification('Không thể giải quyết cảnh báo', 'error');
    }
}

// Helper functions for text formatting
function formatDateTime(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function getAlertTypeText(type) {
    const types = {
        'FALL': '🚨 Phát hiện ngã',
        'HELP_REQUEST': '🆘 Yêu cầu trợ giúp',
        'PROXIMITY': '⚠️ Gần khu vực nguy hiểm',
        'LOW_BATTERY': '🔋 Pin yếu',
        'OUT_OF_ZONE': '📍 Ra ngoài khu vực',
        'ABNORMAL': '⚡ Bất thường',
        'TEMPERATURE': '🌡️ Nhiệt độ cao',
        'IMPACT': '💥 Va chạm',
        'NO_SIGNAL': '📡 Mất tín hiệu'
    };
    return types[type] || type;
}

function getSeverityText(severity) {
    const severities = {
        'CRITICAL': 'Nghiêm trọng',
        'WARNING': 'Cảnh báo',
        'INFO': 'Thông tin'
    };
    return severities[severity] || severity;
}

function getStatusText(status) {
    const statuses = {
        'PENDING': 'Chờ xử lý',
        'ACKNOWLEDGED': 'Đã xác nhận',
        'RESOLVED': 'Đã giải quyết'
    };
    return statuses[status] || status;
}

// Show notification
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed;
        top: 80px;
        right: 20px;
        padding: 1rem 1.5rem;
        background-color: ${type === 'error' ? '#ef4444' : type === 'success' ? '#10b981' : '#2563eb'};
        color: white;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 1000;
        animation: slideIn 0.3s ease-out;
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease-out';
        setTimeout(() => {
            if (document.body.contains(notification)) {
                document.body.removeChild(notification);
            }
        }, 300);
    }, 3000);
}

// ==========================================
// WEBSOCKET REAL-TIME ALERTS UPDATES
// ==========================================
var stompClient = null;

function connectWebSocket() {
    console.log('🔌 Connecting Alerts WebSocket...');
    
    const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
    const host = window.location.host;
    const wsUrl = `${protocol}//${host}/ws`;
    
    const socket = new SockJS(wsUrl);
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        console.log('✅ Alerts WebSocket connected!');
        
        // Subscribe to new alerts
        stompClient.subscribe('/topic/alerts/new', function(message) {
            try {
                const alert = JSON.parse(message.body);
                console.log('🚨 New alert received realtime:', alert);
                
                // Reload alerts table
                loadAlerts();
                
                // Show notification
                showNotification(`Cảnh báo mới: ${alert.message || 'Phát hiện sự cố'}`, 'error');
                
                // Play sound notification (optional)
                playAlertSound();
                
            } catch (e) {
                console.error('❌ Error parsing alert message:', e);
            }
        });
        
        // Subscribe to alert status updates
        stompClient.subscribe('/topic/alerts/update', function(message) {
            try {
                const alert = JSON.parse(message.body);
                console.log('📝 Alert status updated:', alert.id);
                
                // Reload alerts to show updated status
                loadAlerts();
                
            } catch (e) {
                console.error('❌ Error parsing alert update:', e);
            }
        });
        
    }, function(error) {
        console.error('❌ Alerts WebSocket error:', error);
        // Retry connection after 5 seconds
        setTimeout(connectWebSocket, 5000);
    });
}

function playAlertSound() {
    // Optional: Play alert sound
    try {
        const audio = new Audio('data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhBjKJ0fPTgjMGHm7A7+OZRQ0PVqzn76NTDApDmuDyv2seBS+Py/PViTMGGmm+8OabRQ0PVqzo76NTDAtDmd/yv2seBS6Ny/PViTMGHW6/8OabRQ0OVqzo76NTDApDmd/yv2seBS+Ny/PWiTMGHW6/8OabRQ0OVqvn76NTDApDmd/yv2sfBS+Ny/PWiTMGHW6/8OebRQ0OVqvn76NTDApDmd/yv2sfBS+Ny/PWiTMGHW6/8OebRQ0OVqvn76NTDApDmd/yv2wfBS+Ny/PWiTMGHW6/8OebRQ0OVqvn76NTDApDmd/yv2wfBS+Ny/PWiTMGHW6/8OebRQ0OVqvn76NTDApDmd/yv2wfBS+Ny/PWiTMGHW6/8OebRQ0OVqvn76NTDApDmd/yv2wfBS+Ny/PWiTMGHW6/8OebRQ0OVqvn76NTDApDmd/yv2wfBS+Ny/PWiTMGHW6/8OebRQ0OVqvn76NTDApDmd/yv2wfBS+Ny/PWiTMGHW6/8OebRQ0OVqvn76NTDApDmd/yv2wfBS+Ny/PWiTMGHW6/8OebRQ0OVqvn76NTDApDmd/yv2wfBS+Ny/PWiTMGHW6/8OebRQ0OVqvn76NTDApDmd/yv2wfBS+Ny/PWiTMGHW6/8OebRQ0OVqvn76NTDApDmd/yv2wfBS+Ny/PWiTMGHW6/8OebRQ0OVqvn76NTDApDmd/yv2wfBS+Ny/PWiTMGHW6/8OebRQ0OVqvn76NTDApDmd/yv2wfBS+Ny/PWiTMGHW6/8OebRQ0OVqvn76NTDApDmd/yv2wfBS+Ny/PWiTMGHW6/8OebRQ0OVqvn76NTDAo=');
        audio.play().catch(e => console.log('Audio play prevented:', e));
    } catch (e) {
        console.log('Could not play alert sound:', e);
    }
}

// Make functions globally accessible for onclick handlers
window.viewAlert = viewAlert;
window.acknowledgeAlert = acknowledgeAlert;
window.resolveAlert = resolveAlert;