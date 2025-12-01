/**
 * 🚨 GLOBAL ALERT BANNER - Hiển thị cảnh báo khẩn cấp trên TẤT CẢ các trang
 * 
 * File này được include vào tất cả các trang HTML để hiển thị banner cảnh báo
 * khi có sự cố như: ngã, SOS, vào vùng nguy hiểm...
 */

(function() {
    'use strict';
    
    // ==================== CONFIG ====================
    const ALERT_DISPLAY_TIME = 10000; // 10 giây hiển thị banner
    const RECONNECT_DELAY = 5000; // 5 giây retry khi mất kết nối
    
    // ==================== STATE ====================
    let stompClient = null;
    let isConnected = false;
    let currentBanner = null;
    let bannerQueue = [];
    let bannerTimeout = null;
    
    // ==================== INIT ====================
    document.addEventListener('DOMContentLoaded', function() {
        console.log('🔔 Global Alert System initializing...');
        injectStyles();
        createBannerContainer();
        connectGlobalWebSocket();
    });
    
    // ==================== WEBSOCKET ====================
    function connectGlobalWebSocket() {
        // Avoid duplicate connections if page already has one
        if (window.globalAlertConnected) {
            console.log('⚠️ Global alert WebSocket already connected');
            return;
        }
        
        console.log('🔌 Connecting Global Alert WebSocket...');
        
        const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
        const host = window.location.host;
        const wsUrl = `${protocol}//${host}/ws`;
        
        try {
            const socket = new SockJS(wsUrl);
            stompClient = Stomp.over(socket);
            
            // Disable debug logging
            stompClient.debug = null;
            
            stompClient.connect({}, function(frame) {
                console.log('✅ Global Alert WebSocket connected!');
                isConnected = true;
                window.globalAlertConnected = true;
                
                // Subscribe to new alerts - for banner display
                stompClient.subscribe('/topic/alerts/new', function(message) {
                    try {
                        const alert = JSON.parse(message.body);
                        console.log('🚨 [GLOBAL] New alert received:', alert);
                        showAlertBanner(alert);
                    } catch (e) {
                        console.error('❌ Error parsing global alert:', e);
                    }
                });
                
                // ⭐ Subscribe to alert acknowledgments from Messenger
                stompClient.subscribe('/topic/alert-acknowledgment', function(message) {
                    try {
                        const ack = JSON.parse(message.body);
                        console.log('✅ [GLOBAL] Alert acknowledgment received:', ack);
                        showAcknowledgmentBanner(ack);
                    } catch (e) {
                        console.error('❌ Error parsing alert acknowledgment:', e);
                    }
                });
                
            }, function(error) {
                console.error('❌ Global Alert WebSocket error:', error);
                isConnected = false;
                window.globalAlertConnected = false;
                // Retry connection
                setTimeout(connectGlobalWebSocket, RECONNECT_DELAY);
            });
            
        } catch (e) {
            console.error('❌ Failed to create WebSocket:', e);
            setTimeout(connectGlobalWebSocket, RECONNECT_DELAY);
        }
    }
    
    // ==================== BANNER DISPLAY ====================
    function showAlertBanner(alert) {
        // Format alert message
        const alertInfo = formatAlertInfo(alert);
        
        // Add to queue
        bannerQueue.push(alertInfo);
        
        // Display if no banner showing
        if (!currentBanner) {
            displayNextBanner();
        }
    }
    
    function displayNextBanner() {
        if (bannerQueue.length === 0) {
            currentBanner = null;
            return;
        }
        
        const alertInfo = bannerQueue.shift();
        currentBanner = alertInfo;
        
        const container = document.getElementById('global-alert-container');
        if (!container) return;
        
        // Clear any existing banner
        container.innerHTML = '';
        
        // Create banner element
        const banner = document.createElement('div');
        banner.className = `global-alert-banner severity-${alertInfo.severity.toLowerCase()}`;
        banner.innerHTML = `
            <div class="alert-banner-content">
                <span class="alert-icon">${alertInfo.icon}</span>
                <span class="alert-text">
                    <strong>Cảnh báo mới:</strong> ${alertInfo.message}
                </span>
                <a href="location.html" class="alert-link">
                    <i class="fas fa-cog"></i> Cài đặt
                </a>
            </div>
            <button class="alert-close" onclick="window.closeGlobalAlert()">
                <i class="fas fa-times"></i>
            </button>
        `;
        
        container.appendChild(banner);
        
        // Play sound
        playAlertSound(alertInfo.severity);
        
        // Update notification badge in header
        updateNotificationBadge();
        
        // Auto close after timeout
        bannerTimeout = setTimeout(() => {
            closeBanner();
            displayNextBanner();
        }, ALERT_DISPLAY_TIME);
    }
    
    function closeBanner() {
        const container = document.getElementById('global-alert-container');
        if (container) {
            const banner = container.querySelector('.global-alert-banner');
            if (banner) {
                banner.classList.add('closing');
                setTimeout(() => {
                    banner.remove();
                }, 300);
            }
        }
        
        if (bannerTimeout) {
            clearTimeout(bannerTimeout);
            bannerTimeout = null;
        }
    }
    
    // Expose close function globally
    window.closeGlobalAlert = function() {
        closeBanner();
        currentBanner = null;
        displayNextBanner();
    };
    
    // ==================== ACKNOWLEDGMENT BANNER ====================
    /**
     * Hiển thị thông báo khi có người xác nhận xử lý alert qua Messenger
     */
    function showAcknowledgmentBanner(ack) {
        const container = document.getElementById('global-alert-container');
        if (!container) return;
        
        // Tạo banner xác nhận (màu xanh lá)
        const banner = document.createElement('div');
        banner.className = 'global-alert-banner acknowledgment';
        
        // Format thời gian
        let timeStr = '';
        if (ack.timestamp) {
            const date = new Date(ack.timestamp);
            timeStr = date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
        }
        
        banner.innerHTML = `
            <div class="alert-banner-content">
                <span class="alert-icon">✅</span>
                <span class="alert-text">
                    <strong>Đã xử lý:</strong> 
                    ${ack.handlerName || 'Người dùng Messenger'} đã xác nhận xử lý cảnh báo 
                    <strong>${ack.alertType}</strong> cho <strong>${ack.employeeName}</strong>
                    ${ack.message ? `<br><em style="opacity: 0.9;">Ghi chú: "${ack.message}"</em>` : ''}
                    ${timeStr ? `<span style="margin-left: 10px; opacity: 0.7;">(${timeStr})</span>` : ''}
                </span>
            </div>
            <button class="alert-close" onclick="this.parentElement.remove()">
                <i class="fas fa-times"></i>
            </button>
        `;
        
        // Thêm vào container
        container.appendChild(banner);
        
        // Play success sound
        playAcknowledgmentSound();
        
        // Auto close after 15 seconds
        setTimeout(() => {
            if (banner.parentElement) {
                banner.classList.add('closing');
                setTimeout(() => banner.remove(), 300);
            }
        }, 15000);
    }
    
    /**
     * Phát âm thanh khi có xác nhận xử lý
     */
    function playAcknowledgmentSound() {
        try {
            // Tạo âm thanh success nhẹ nhàng
            const audioContext = new (window.AudioContext || window.webkitAudioContext)();
            const oscillator = audioContext.createOscillator();
            const gainNode = audioContext.createGain();
            
            oscillator.connect(gainNode);
            gainNode.connect(audioContext.destination);
            
            oscillator.frequency.value = 880; // A5 note
            oscillator.type = 'sine';
            gainNode.gain.value = 0.1;
            
            oscillator.start();
            gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.3);
            oscillator.stop(audioContext.currentTime + 0.3);
        } catch (e) {
            // Ignore audio errors
        }
    }
    
    // ==================== HELPERS ====================
    function formatAlertInfo(alert) {
        const typeMap = {
            'FALL': { icon: '🚨', text: 'PHÁT HIỆN NGÃ' },
            'HELP_REQUEST': { icon: '🆘', text: 'YÊU CẦU TRỢ GIÚP' },
            'PROXIMITY': { icon: '⚠️', text: 'GẦN VÙNG NGUY HIỂM' },
            'LOW_BATTERY': { icon: '🔋', text: 'PIN YẾU' },
            'OUT_OF_ZONE': { icon: '📍', text: 'RA NGOÀI KHU VỰC' },
            'ABNORMAL': { icon: '⚡', text: 'BẤT THƯỜNG' },
            'TEMPERATURE': { icon: '🌡️', text: 'NHIỆT ĐỘ CAO' },
            'IMPACT': { icon: '💥', text: 'VA CHẠM' },
            'NO_SIGNAL': { icon: '📡', text: 'MẤT TÍN HIỆU' }
        };
        
        const alertType = alert.alertType || 'ABNORMAL';
        const typeInfo = typeMap[alertType] || { icon: '⚠️', text: alertType };
        
        // Extract employee name from message or helmet info
        let employeeName = 'N/A';
        if (alert.message) {
            // Try to extract name from message like "🚨 PHÁT HIỆN NGÃ: Nguyễn Văn Test (TEST01)"
            const match = alert.message.match(/:\s*(.+?)(?:\s*\(|$)/);
            if (match) {
                employeeName = match[1].trim();
            }
        } else if (alert.helmet && alert.helmet.helmetId) {
            employeeName = alert.helmet.helmetId;
        }
        
        return {
            id: alert.id,
            icon: typeInfo.icon,
            type: typeInfo.text,
            message: `${typeInfo.icon} ${typeInfo.text}: ${employeeName}`,
            severity: alert.severity || 'WARNING',
            helmetId: alert.helmet?.helmetId || 'N/A'
        };
    }
    
    function playAlertSound(severity) {
        try {
            // Create beep sound
            const audioContext = new (window.AudioContext || window.webkitAudioContext)();
            const oscillator = audioContext.createOscillator();
            const gainNode = audioContext.createGain();
            
            oscillator.connect(gainNode);
            gainNode.connect(audioContext.destination);
            
            // Different tones for different severities
            if (severity === 'CRITICAL') {
                oscillator.frequency.value = 800; // Higher pitch for critical
                gainNode.gain.value = 0.3;
            } else {
                oscillator.frequency.value = 600;
                gainNode.gain.value = 0.2;
            }
            
            oscillator.type = 'sine';
            oscillator.start();
            
            // Short beep
            setTimeout(() => {
                oscillator.stop();
            }, 200);
            
        } catch (e) {
            console.log('Could not play alert sound:', e);
        }
    }
    
    function updateNotificationBadge() {
        const badge = document.querySelector('.notification-icon .badge');
        if (badge) {
            const current = parseInt(badge.textContent) || 0;
            badge.textContent = current + 1;
            badge.classList.add('pulse');
            setTimeout(() => badge.classList.remove('pulse'), 1000);
        }
    }
    
    // ==================== STYLES ====================
    function injectStyles() {
        const style = document.createElement('style');
        style.textContent = `
            /* Global Alert Banner Container */
            #global-alert-container {
                position: fixed;
                top: 10px;
                right: 10px;
                z-index: 10000;
                max-width: 500px;
            }
            
            /* Alert Banner */
            .global-alert-banner {
                display: flex;
                align-items: center;
                justify-content: space-between;
                padding: 12px 16px;
                border-radius: 8px;
                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.25);
                animation: slideInRight 0.3s ease-out;
                margin-bottom: 10px;
            }
            
            .global-alert-banner.closing {
                animation: slideOutRight 0.3s ease-out forwards;
            }
            
            /* Severity Colors */
            .global-alert-banner.severity-critical {
                background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
                color: white;
            }
            
            .global-alert-banner.severity-warning {
                background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
                color: white;
            }
            
            .global-alert-banner.severity-info {
                background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
                color: white;
            }
            
            /* Banner Content */
            .alert-banner-content {
                display: flex;
                align-items: center;
                gap: 12px;
                flex: 1;
            }
            
            .alert-icon {
                font-size: 20px;
            }
            
            .alert-text {
                font-size: 14px;
                font-weight: 500;
            }
            
            .alert-text strong {
                font-weight: 600;
            }
            
            .alert-link {
                display: flex;
                align-items: center;
                gap: 6px;
                padding: 6px 12px;
                background: rgba(255, 255, 255, 0.2);
                border-radius: 6px;
                color: white;
                text-decoration: none;
                font-size: 13px;
                transition: background 0.2s;
            }
            
            .alert-link:hover {
                background: rgba(255, 255, 255, 0.3);
            }
            
            /* Close Button */
            .alert-close {
                background: none;
                border: none;
                color: white;
                font-size: 18px;
                cursor: pointer;
                padding: 4px 8px;
                margin-left: 8px;
                opacity: 0.8;
                transition: opacity 0.2s;
            }
            
            .alert-close:hover {
                opacity: 1;
            }
            
            /* Animations */
            @keyframes slideInRight {
                from {
                    transform: translateX(100%);
                    opacity: 0;
                }
                to {
                    transform: translateX(0);
                    opacity: 1;
                }
            }
            
            @keyframes slideOutRight {
                from {
                    transform: translateX(0);
                    opacity: 1;
                }
                to {
                    transform: translateX(100%);
                    opacity: 0;
                }
            }
            
            /* Badge Pulse */
            .notification-icon .badge.pulse {
                animation: badgePulse 0.5s ease-out;
            }
            
            @keyframes badgePulse {
                0% { transform: scale(1); }
                50% { transform: scale(1.3); }
                100% { transform: scale(1); }
            }
            
            /* ⭐ Acknowledgment Banner - Màu xanh lá */
            .global-alert-banner.acknowledgment {
                background: linear-gradient(135deg, #10b981 0%, #059669 100%);
                color: white;
            }
            
            .global-alert-banner.acknowledgment .alert-text em {
                font-style: italic;
                opacity: 0.9;
            }
        `;
        document.head.appendChild(style);
    }
    
    function createBannerContainer() {
        // Check if container already exists
        if (document.getElementById('global-alert-container')) {
            return;
        }
        
        const container = document.createElement('div');
        container.id = 'global-alert-container';
        document.body.appendChild(container);
    }
    
})();
