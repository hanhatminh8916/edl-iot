// Dashboard JavaScript
const API_BASE_URL = '/api';

// Navigation handling
document.addEventListener('DOMContentLoaded', function() {
    // Load dashboard data on page load
    loadDashboardData();
    
    // Connect WebSocket for realtime updates
    connectWebSocket();
    
    // Handle navigation items - remove preventDefault to allow links to work
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', function(e) {
            // Don't prevent default - let the link navigate
            // Only prevent for items with href="#"
            if (this.getAttribute('href') === '#') {
                e.preventDefault();
                alert('Chức năng đang được phát triển');
            }
            // The active class will be set by each page's own HTML
        });
    });

    // Handle notification icon
    const notificationIcon = document.querySelector('.notification-icon');
    if (notificationIcon) {
        notificationIcon.addEventListener('click', function() {
            alert('Bạn có 3 thông báo mới!');
        });
    }

    // Handle admin profile
    const adminProfile = document.querySelector('.admin-profile');
    if (adminProfile) {
        adminProfile.addEventListener('click', function() {
            alert('Chức năng quản lý tài khoản');
        });
    }

    // Handle export button
    const btnExport = document.querySelector('.btn-export');
    if (btnExport) {
        btnExport.addEventListener('click', function() {
            alert('Chuyển sang chế độ xem trực tiếp');
        });
    }

    // Handle alert items
    const alertItems = document.querySelectorAll('.alert-item');
    alertItems.forEach(item => {
        item.addEventListener('click', function() {
            const employeeName = this.querySelector('h4').textContent;
            const alertType = this.querySelector('p').textContent;
            alert(`Chi tiết cảnh báo:\nNhân viên: ${employeeName}\nLoại cảnh báo: ${alertType}`);
        });
    });

    // Handle action items
    const actionItems = document.querySelectorAll('.action-item');
    actionItems.forEach(item => {
        item.addEventListener('click', function() {
            const actionTitle = this.querySelector('h4').textContent;
            alert(`Thực hiện: ${actionTitle}`);
        });
    });

    // Handle view all alerts button
    const btnViewAlerts = document.querySelector('.btn-link');
    if (btnViewAlerts) {
        btnViewAlerts.addEventListener('click', function() {
            window.location.href = '/alerts.html';
        });
    }

    // Update time periodically (simulate real-time updates)
    setInterval(loadDashboardData, 30000); // Update every 30 seconds
});

// Function to load dashboard data from API
async function loadDashboardData() {
    try {
        // Fetch overview stats
        const statsResponse = await fetch(`${API_BASE_URL}/dashboard/overview`);
        const stats = await statsResponse.json();
        
        // Update statistics
        updateStatistics(stats);
        
        // Fetch pending alerts
        const alertsResponse = await fetch(`${API_BASE_URL}/alerts/pending`);
        const alerts = await alertsResponse.json();
        
        // Update alerts display
        updateCriticalAlerts(alerts);
        
        // Fetch active helmets for battery monitoring
        const helmetsResponse = await fetch(`${API_BASE_URL}/helmet/active`);
        const helmets = await helmetsResponse.json();
        
        // Update battery status
        updateBatteryStatus(helmets);
        
        console.log('Dashboard updated at:', new Date().toLocaleTimeString('vi-VN'));
    } catch (error) {
        console.error('Error loading dashboard data:', error);
        showNotification('Không thể tải dữ liệu dashboard', 'error');
    }
}

// Function to update statistics
function updateStatistics(stats) {
    const statElements = {
        totalWorkers: document.querySelector('.stat-card:nth-child(1) .stat-value'),
        activeWorkers: document.querySelector('.stat-card:nth-child(2) .stat-value'),
        pendingAlerts: document.querySelector('.stat-card:nth-child(3) .stat-value'),
        criticalAlerts: document.querySelector('.stat-card:nth-child(4) .stat-value')
    };
    
    if (statElements.totalWorkers) statElements.totalWorkers.textContent = stats.totalWorkers || 0;
    if (statElements.activeWorkers) statElements.activeWorkers.textContent = stats.activeWorkers || 0;
    if (statElements.pendingAlerts) statElements.pendingAlerts.textContent = stats.pendingAlerts || 0;
    
    // Calculate efficiency
    const efficiency = stats.totalWorkers > 0 
        ? Math.round((stats.activeWorkers / stats.totalWorkers) * 100) 
        : 0;
    if (statElements.criticalAlerts) {
        statElements.criticalAlerts.textContent = efficiency + '%';
    }
}

// Function to update critical alerts
function updateCriticalAlerts(alerts) {
    const alertsContainer = document.querySelector('.critical-alerts .alerts-list');
    if (!alertsContainer) return;
    
    // Filter for critical alerts only
    const criticalAlerts = alerts.filter(alert => 
        alert.severity === 'CRITICAL' || alert.alertType === 'FALL'
    ).slice(0, 5); // Show max 5 alerts
    
    if (criticalAlerts.length === 0) {
        alertsContainer.innerHTML = '<p style="text-align: center; color: #64748b;">Không có cảnh báo nguy hiểm</p>';
        return;
    }
    
    alertsContainer.innerHTML = criticalAlerts.map(alert => `
        <div class="alert-item">
            <div class="alert-icon">
                <i class="fas fa-exclamation-triangle"></i>
            </div>
            <div class="alert-content">
                <h4>Công nhân #${alert.helmet?.helmetId || 'N/A'}</h4>
                <p>${alert.message}</p>
                <div class="alert-details">
                    <span><i class="fas fa-clock"></i> ${formatTime(alert.triggeredAt)}</span>
                    <span><i class="fas fa-map-marker-alt"></i> ${alert.gpsLat?.toFixed(6)}, ${alert.gpsLon?.toFixed(6)}</span>
                </div>
            </div>
        </div>
    `).join('');
}

// Function to update battery status
function updateBatteryStatus(helmets) {
    const batteryTableBody = document.querySelector('.battery-status tbody');
    if (!batteryTableBody) return;
    
    const activeHelmets = helmets.slice(0, 5); // Show max 5 helmets
    
    if (activeHelmets.length === 0) {
        batteryTableBody.innerHTML = '<tr><td colspan="4" style="text-align: center;">Không có dữ liệu</td></tr>';
        return;
    }
    
    batteryTableBody.innerHTML = activeHelmets.map(helmet => {
        const batteryClass = helmet.batteryLevel > 50 ? 'battery-good' : 
                            helmet.batteryLevel > 20 ? 'battery-medium' : 'battery-low';
        const voltage = (helmet.batteryLevel / 100 * 4.2).toFixed(2); // Simulate voltage
        const current = (Math.random() * 0.5 + 0.1).toFixed(2); // Simulate current
        
        return `
            <tr>
                <td>Thiết bị #${helmet.helmetId}</td>
                <td>
                    <div class="battery-progress">
                        <div class="battery-fill ${batteryClass}" style="width: ${helmet.batteryLevel}%"></div>
                    </div>
                    <span>${helmet.batteryLevel}%</span>
                </td>
                <td>${voltage}V</td>
                <td>${current}A</td>
            </tr>
        `;
    }).join('');
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