// Dashboard JavaScript - REAL DATA
const API_BASE_URL = '/api';

// Navigation handling
document.addEventListener('DOMContentLoaded', function() {
    // Load dashboard data on page load
    loadDashboardData();
    
    // Connect WebSocket for realtime updates
    connectWebSocket();
    
    // Handle navigation items
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', function(e) {
            if (this.getAttribute('href') === '#') {
                e.preventDefault();
                alert('Chức năng đang được phát triển');
            }
        });
    });

    // Handle export button
    const btnExport = document.querySelector('.btn-export');
    if (btnExport) {
        btnExport.addEventListener('click', function() {
            window.location.href = 'location.html';
        });
    }

    // Update every 30 seconds
    setInterval(loadDashboardData, 45000); // Optimized: 45s instead of 30s
});

/**
 * 📊 Load tất cả dữ liệu dashboard từ API
 */
async function loadDashboardData() {
    try {
        // Load overview stats
        await loadOverviewStats();
        
        // Load recent alerts
        await loadRecentAlerts();
        
        // Load battery status
        await loadBatteryStatus();
        
        console.log('✅ Dashboard updated at:', new Date().toLocaleTimeString('vi-VN'));
    } catch (error) {
        console.error('❌ Error loading dashboard data:', error);
    }
}

/**
 * 📈 Load thống kê tổng quan
 */
async function loadOverviewStats() {
    try {
        const response = await fetch(`${API_BASE_URL}/dashboard/overview`);
        if (!response.ok) throw new Error('API error');
        
        const stats = await response.json();
        
        // Update UI
        document.getElementById('stat-total-workers').textContent = stats.totalWorkers || 0;
        document.getElementById('stat-active-workers').textContent = stats.activeWorkers || 0;
        document.getElementById('stat-alerts').textContent = stats.pendingAlerts || 0;
        document.getElementById('stat-efficiency').textContent = (stats.efficiency || 0) + '%';
        
    } catch (error) {
        console.error('Error loading overview stats:', error);
    }
}

/**
 * 🔔 Load cảnh báo gần đây
 */
async function loadRecentAlerts() {
    const container = document.getElementById('recent-alerts-list');
    if (!container) return;
    
    try {
        const response = await fetch(`${API_BASE_URL}/dashboard/alerts/recent`);
        if (!response.ok) throw new Error('API error');
        
        const alerts = await response.json();
        
        if (alerts.length === 0) {
            container.innerHTML = `
                <div style="text-align: center; padding: 20px; color: #22c55e;">
                    <i class="fas fa-check-circle" style="font-size: 24px;"></i>
                    <p style="margin-top: 10px;">Không có cảnh báo hôm nay</p>
                </div>
            `;
            return;
        }
        
        container.innerHTML = alerts.map(alert => {
            const isDanger = alert.severity === 'CRITICAL' || alert.severity === 'HIGH';
            return `
                <div class="alert-item">
                    <div class="alert-dot ${isDanger ? 'danger' : ''}"></div>
                    <div class="alert-content">
                        <h4>${alert.employeeName || 'Không xác định'}</h4>
                        <p>${alert.message || alert.type}</p>
                    </div>
                    <span class="alert-time ${isDanger ? 'danger-badge' : ''}">${alert.time}</span>
                </div>
            `;
        }).join('');
        
        // Update critical alerts section
        const criticalAlerts = alerts.filter(a => a.severity === 'CRITICAL');
        updateCriticalAlerts(criticalAlerts);
        
    } catch (error) {
        console.error('Error loading recent alerts:', error);
        container.innerHTML = `
            <div style="text-align: center; padding: 20px; color: #6b7280;">
                <p>Không thể tải cảnh báo</p>
            </div>
        `;
    }
}

/**
 * 🔴 Cập nhật cảnh báo nguy hiểm
 */
function updateCriticalAlerts(criticalAlerts) {
    const countEl = document.getElementById('critical-count');
    const listEl = document.getElementById('critical-alerts-list');
    
    if (!countEl || !listEl) return;
    
    countEl.textContent = `${criticalAlerts.length} cảnh báo`;
    
    if (criticalAlerts.length === 0) {
        listEl.innerHTML = `
            <div style="text-align: center; padding: 30px; color: #22c55e;">
                <i class="fas fa-check-circle" style="font-size: 24px;"></i>
                <p style="margin-top: 10px;">Không có cảnh báo nguy hiểm</p>
            </div>
        `;
        return;
    }
    
    listEl.innerHTML = criticalAlerts.map(alert => `
        <div class="critical-item">
            <div class="critical-avatar">
                <i class="fas fa-user"></i>
            </div>
            <div class="critical-info">
                <div class="critical-header">
                    <h4>${alert.employeeName}</h4>
                    <span class="critical-level danger">Nghiêm trọng</span>
                </div>
                <div class="critical-details">
                    <div class="critical-detail-item">
                        <i class="fas fa-exclamation-triangle"></i>
                        <span>${alert.message || alert.type}</span>
                    </div>
                    <div class="critical-detail-item">
                        <i class="fas fa-clock"></i>
                        <span>${alert.time}</span>
                    </div>
                </div>
            </div>
            <button class="btn-critical-action" onclick="alert('Đang gọi...')">
                <i class="fas fa-phone-alt"></i>
                Liên hệ
            </button>
        </div>
    `).join('');
}

/**
 * 🔋 Load trạng thái pin
 */
async function loadBatteryStatus() {
    const container = document.getElementById('battery-status-list');
    const countEl = document.getElementById('battery-device-count');
    
    if (!container) return;
    
    try {
        const response = await fetch(`${API_BASE_URL}/dashboard/battery-status`);
        if (!response.ok) throw new Error('API error');
        
        const batteries = await response.json();
        
        if (countEl) countEl.textContent = `${batteries.length} thiết bị`;
        
        if (batteries.length === 0) {
            container.innerHTML = `
                <div style="text-align: center; padding: 30px; color: #6b7280;">
                    <i class="fas fa-battery-empty" style="font-size: 24px;"></i>
                    <p style="margin-top: 10px;">Không có thiết bị online</p>
                </div>
            `;
            return;
        }
        
        container.innerHTML = batteries.map(b => {
            const battery = b.battery || 0;
            const status = b.batteryStatus || 'medium';
            const isLow = battery <= 20;
            
            return `
                <div class="battery-item ${isLow ? 'warning-bg' : ''}">
                    <div class="battery-worker">
                        <div class="battery-avatar">${b.initials || '??'}</div>
                        <div class="battery-worker-info">
                            <h4>${b.employeeName}</h4>
                            <span class="battery-device">ID: ${b.employeeId} • ESP32</span>
                        </div>
                    </div>
                    <div class="battery-status">
                        <div class="battery-bar">
                            <div class="battery-fill ${status}" style="width: ${battery}%"></div>
                        </div>
                        <div class="battery-info">
                            <span class="battery-percent ${status}">
                                ${isLow ? '<i class="fas fa-exclamation-triangle"></i> ' : ''}${Math.round(battery)}%
                            </span>
                            <span class="battery-voltage">${(b.voltage || 0).toFixed(1)}V / ${Math.round(b.current || 0)}mA</span>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
        
    } catch (error) {
        console.error('Error loading battery status:', error);
        container.innerHTML = `
            <div style="text-align: center; padding: 30px; color: #6b7280;">
                <p>Không thể tải trạng thái pin</p>
            </div>
        `;
    }
}

// Helper function to format time
function formatTime(timestamp) {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = Math.floor((now - date) / 1000 / 60); // minutes
    
    if (diff < 1) return 'Vừa xong';
    if (diff < 60) return `${diff} phút trước`;
    if (diff < 1440) return `${Math.floor(diff / 60)} giờ trước`;
    return date.toLocaleDateString('vi-VN');
}

// Function to update dashboard data
function updateDashboard() {
    loadDashboardData();
}

// Function to show notification
function showNotification(message, type = 'info') {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed;
        top: 80px;
        right: 20px;
        padding: 1rem 1.5rem;
        background-color: ${type === 'error' ? '#ef4444' : '#2563eb'};
        color: white;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 1000;
        animation: slideIn 0.3s ease-out;
    `;
    
    document.body.appendChild(notification);
    
    // Remove notification after 3 seconds
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease-out';
        setTimeout(() => {
            document.body.removeChild(notification);
        }, 300);
    }, 3000);
}

// Add CSS animations
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
    
    .stat-value {
        transition: transform 0.2s ease;
    }
`;
document.head.appendChild(style);

// ==========================================
// WEBSOCKET REAL-TIME DASHBOARD UPDATES
// ==========================================
var stompClient = null;

function connectWebSocket() {
    console.log('🔌 Connecting Dashboard WebSocket...');
    
    const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
    const host = window.location.host;
    const wsUrl = `${protocol}//${host}/ws`;
    
    const socket = new SockJS(wsUrl);
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        console.log('✅ Dashboard WebSocket connected!');
        
        // Subscribe to helmet data updates
        stompClient.subscribe('/topic/helmet/data', function(message) {
            try {
                const data = JSON.parse(message.body);
                console.log('📡 Dashboard received update:', data.mac);
                
                // Reload dashboard stats
                loadDashboardData();
                
            } catch (e) {
                console.error('❌ Error parsing WebSocket message:', e);
            }
        });
        
        // Subscribe to alert updates
        stompClient.subscribe('/topic/alerts/new', function(message) {
            try {
                const alert = JSON.parse(message.body);
                console.log('🚨 New alert received:', alert);
                
                // Reload alerts section
                loadDashboardData();
                
                // Show notification
                showNotification(`Cảnh báo mới: ${alert.message || 'Phát hiện sự cố'}`, 'error');
                
            } catch (e) {
                console.error('❌ Error parsing alert message:', e);
            }
        });
        
    }, function(error) {
        console.error('❌ Dashboard WebSocket error:', error);
        // Retry connection after 5 seconds
        setTimeout(connectWebSocket, 5000);
    });
}

// Export functions for external use
window.dashboardFunctions = {
    updateDashboard,
    showNotification
};