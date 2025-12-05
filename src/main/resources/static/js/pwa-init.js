/**
 * 📱 PWA INITIALIZER - EDL SafeWork
 * 
 * Đăng ký Service Worker và khởi tạo PWA
 * Include file này vào tất cả các trang HTML (trước </body>)
 * 
 * ⭐ Enhanced for iOS/iPhone:
 * - Notification permission request
 * - Add to Home Screen prompt for iOS
 * - Service Worker registration
 */

(function() {
    'use strict';

    // Register Service Worker
    if ('serviceWorker' in navigator) {
        window.addEventListener('load', async () => {
            try {
                const registration = await navigator.serviceWorker.register('/service-worker.js');
                console.log('✅ Service Worker registered:', registration.scope);

                // Check for updates
                registration.addEventListener('updatefound', () => {
                    const newWorker = registration.installing;
                    console.log('🔄 Service Worker update found');

                    newWorker.addEventListener('statechange', () => {
                        if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                            // New version available
                            showUpdateNotification();
                        }
                    });
                });
                
                // Request notification permission after SW is ready
                requestNotificationPermission();
                
            } catch (error) {
                console.error('❌ Service Worker registration failed:', error);
            }
        });
    }
    
    // ==================== NOTIFICATION PERMISSION ====================
    function requestNotificationPermission() {
        // Only ask if not already granted/denied
        if ('Notification' in window && Notification.permission === 'default') {
            // Don't ask immediately, wait for user interaction
            showNotificationPrompt();
        }
    }
    
    function showNotificationPrompt() {
        // Only show on first visit or if not already shown
        if (localStorage.getItem('notification-prompt-shown')) {
            return;
        }
        
        // Wait 5 seconds before showing prompt
        setTimeout(() => {
            const prompt = document.createElement('div');
            prompt.className = 'notification-permission-prompt';
            prompt.innerHTML = `
                <div class="notification-prompt-content">
                    <span class="notification-prompt-icon">🔔</span>
                    <div class="notification-prompt-text">
                        <strong>Bật thông báo</strong>
                        <p>Nhận cảnh báo ngay khi có sự cố</p>
                    </div>
                    <div class="notification-prompt-buttons">
                        <button class="btn-enable" onclick="enableNotifications(this)">Bật</button>
                        <button class="btn-later" onclick="dismissNotificationPrompt(this)">Sau</button>
                    </div>
                </div>
            `;
            document.body.appendChild(prompt);
            
            // Auto dismiss after 15 seconds
            setTimeout(() => {
                const existing = document.querySelector('.notification-permission-prompt');
                if (existing) {
                    existing.classList.add('hide');
                    setTimeout(() => existing.remove(), 300);
                }
            }, 15000);
        }, 5000);
    }
    
    // Expose functions globally
    window.enableNotifications = function(btn) {
        const prompt = btn.closest('.notification-permission-prompt');
        
        Notification.requestPermission().then(permission => {
            console.log('🔔 Notification permission:', permission);
            
            if (permission === 'granted') {
                // Show success notification
                new Notification('✅ Thông báo đã bật', {
                    body: 'Bạn sẽ nhận được cảnh báo khi có sự cố',
                    icon: '/images/icon-192.png'
                });
            }
            
            localStorage.setItem('notification-prompt-shown', 'true');
            
            if (prompt) {
                prompt.classList.add('hide');
                setTimeout(() => prompt.remove(), 300);
            }
        });
    };
    
    window.dismissNotificationPrompt = function(btn) {
        const prompt = btn.closest('.notification-permission-prompt');
        localStorage.setItem('notification-prompt-shown', 'true');
        
        if (prompt) {
            prompt.classList.add('hide');
            setTimeout(() => prompt.remove(), 300);
        }
    };
    
    // ==================== iOS ADD TO HOME SCREEN ====================
    function showIOSInstallPrompt() {
        // Check if iOS and not already installed
        const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent);
        const isStandalone = window.matchMedia('(display-mode: standalone)').matches;
        const promptShown = localStorage.getItem('ios-install-prompt-shown');
        
        if (isIOS && !isStandalone && !promptShown) {
            setTimeout(() => {
                const prompt = document.createElement('div');
                prompt.className = 'ios-install-prompt';
                prompt.innerHTML = `
                    <div class="ios-install-content">
                        <button class="ios-install-close" onclick="this.parentElement.parentElement.remove(); localStorage.setItem('ios-install-prompt-shown', 'true');">×</button>
                        <div class="ios-install-icon">📱</div>
                        <div class="ios-install-text">
                            <strong>Cài đặt SafeWork</strong>
                            <p>Nhấn <span class="ios-share-icon">⎙</span> rồi chọn "Thêm vào màn hình chính"</p>
                        </div>
                    </div>
                `;
                document.body.appendChild(prompt);
            }, 3000);
        }
    }
    
    // Show iOS prompt on load
    if (document.readyState === 'complete') {
        showIOSInstallPrompt();
    } else {
        window.addEventListener('load', showIOSInstallPrompt);
    }

    // Show update notification
    function showUpdateNotification() {
        const notification = document.createElement('div');
        notification.className = 'pwa-update-notification';
        notification.innerHTML = `
            <div class="pwa-update-content">
                <span>🔄 Phiên bản mới có sẵn!</span>
                <button onclick="window.location.reload()">Cập nhật</button>
            </div>
        `;
        document.body.appendChild(notification);

        // Auto-dismiss after 10 seconds
        setTimeout(() => {
            notification.classList.add('hide');
            setTimeout(() => notification.remove(), 300);
        }, 10000);
    }

    // Add install prompt
    let deferredPrompt;
    window.addEventListener('beforeinstallprompt', (e) => {
        e.preventDefault();
        deferredPrompt = e;
        showInstallButton();
    });

    function showInstallButton() {
        // Check if already installed
        if (window.matchMedia('(display-mode: standalone)').matches) {
            return;
        }

        const installBtn = document.createElement('button');
        installBtn.className = 'pwa-install-btn';
        installBtn.innerHTML = '<i class="fas fa-download"></i> Cài đặt App';
        installBtn.onclick = installPWA;
        document.body.appendChild(installBtn);
    }

    async function installPWA() {
        if (!deferredPrompt) return;

        deferredPrompt.prompt();
        const { outcome } = await deferredPrompt.userChoice;

        if (outcome === 'accepted') {
            console.log('✅ PWA installed');
        }

        deferredPrompt = null;
        const btn = document.querySelector('.pwa-install-btn');
        if (btn) btn.remove();
    }

    // Expose install function globally
    window.installPWA = installPWA;

    // Inject PWA styles
    const style = document.createElement('style');
    style.textContent = `
        /* PWA Update Notification */
        .pwa-update-notification {
            position: fixed;
            bottom: 20px;
            left: 50%;
            transform: translateX(-50%);
            z-index: 10002;
            animation: slideUp 0.3s ease;
        }

        .pwa-update-notification.hide {
            animation: slideDown 0.3s ease forwards;
        }

        .pwa-update-content {
            background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
            color: white;
            padding: 12px 20px;
            border-radius: 50px;
            display: flex;
            align-items: center;
            gap: 12px;
            box-shadow: 0 4px 20px rgba(30, 64, 175, 0.4);
        }

        .pwa-update-content button {
            background: white;
            color: #1e40af;
            border: none;
            padding: 6px 16px;
            border-radius: 20px;
            font-weight: 600;
            cursor: pointer;
            transition: transform 0.2s;
        }

        .pwa-update-content button:hover {
            transform: scale(1.05);
        }

        /* PWA Install Button */
        .pwa-install-btn {
            position: fixed;
            bottom: 80px;
            right: 20px;
            z-index: 9998;
            background: linear-gradient(135deg, #10b981 0%, #059669 100%);
            color: white;
            border: none;
            padding: 12px 20px;
            border-radius: 50px;
            font-weight: 600;
            cursor: pointer;
            box-shadow: 0 4px 15px rgba(16, 185, 129, 0.4);
            display: flex;
            align-items: center;
            gap: 8px;
            animation: pulse 2s infinite;
        }

        .pwa-install-btn:hover {
            animation: none;
            transform: scale(1.05);
        }

        @keyframes slideUp {
            from { transform: translate(-50%, 100px); opacity: 0; }
            to { transform: translate(-50%, 0); opacity: 1; }
        }

        @keyframes slideDown {
            from { transform: translate(-50%, 0); opacity: 1; }
            to { transform: translate(-50%, 100px); opacity: 0; }
        }

        @keyframes pulse {
            0%, 100% { box-shadow: 0 4px 15px rgba(16, 185, 129, 0.4); }
            50% { box-shadow: 0 4px 25px rgba(16, 185, 129, 0.6); }
        }

        @media (max-width: 480px) {
            .pwa-install-btn {
                bottom: 70px;
                right: 10px;
                padding: 10px 16px;
                font-size: 13px;
            }
        }
        
        /* Notification Permission Prompt */
        .notification-permission-prompt {
            position: fixed;
            bottom: 20px;
            left: 50%;
            transform: translateX(-50%);
            z-index: 10003;
            animation: slideUp 0.3s ease;
        }
        
        .notification-permission-prompt.hide {
            animation: slideDown 0.3s ease forwards;
        }
        
        .notification-prompt-content {
            background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
            color: white;
            padding: 16px 20px;
            border-radius: 16px;
            display: flex;
            align-items: center;
            gap: 12px;
            box-shadow: 0 4px 20px rgba(245, 158, 11, 0.4);
            max-width: 90vw;
        }
        
        .notification-prompt-icon {
            font-size: 28px;
        }
        
        .notification-prompt-text {
            flex: 1;
        }
        
        .notification-prompt-text strong {
            display: block;
            font-size: 15px;
        }
        
        .notification-prompt-text p {
            font-size: 13px;
            opacity: 0.9;
            margin: 2px 0 0 0;
        }
        
        .notification-prompt-buttons {
            display: flex;
            gap: 8px;
        }
        
        .notification-prompt-buttons button {
            border: none;
            padding: 8px 16px;
            border-radius: 20px;
            font-weight: 600;
            cursor: pointer;
            transition: transform 0.2s;
        }
        
        .notification-prompt-buttons .btn-enable {
            background: white;
            color: #d97706;
        }
        
        .notification-prompt-buttons .btn-later {
            background: rgba(255,255,255,0.2);
            color: white;
        }
        
        .notification-prompt-buttons button:hover {
            transform: scale(1.05);
        }
        
        /* iOS Install Prompt */
        .ios-install-prompt {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            z-index: 10004;
            animation: slideUp 0.3s ease;
        }
        
        .ios-install-content {
            background: white;
            padding: 20px;
            display: flex;
            align-items: center;
            gap: 12px;
            box-shadow: 0 -4px 20px rgba(0,0,0,0.15);
            border-radius: 20px 20px 0 0;
            position: relative;
        }
        
        .ios-install-close {
            position: absolute;
            top: 10px;
            right: 15px;
            background: none;
            border: none;
            font-size: 24px;
            color: #666;
            cursor: pointer;
        }
        
        .ios-install-icon {
            font-size: 40px;
        }
        
        .ios-install-text {
            flex: 1;
        }
        
        .ios-install-text strong {
            font-size: 16px;
            color: #1e40af;
        }
        
        .ios-install-text p {
            font-size: 13px;
            color: #666;
            margin: 4px 0 0 0;
        }
        
        .ios-share-icon {
            display: inline-block;
            background: #007aff;
            color: white;
            padding: 2px 6px;
            border-radius: 4px;
            font-size: 12px;
        }
        
        @media (max-width: 480px) {
            .notification-prompt-content {
                flex-wrap: wrap;
                justify-content: center;
                text-align: center;
            }
            
            .notification-prompt-text {
                width: 100%;
            }
        }
    `;
    document.head.appendChild(style);

    console.log('📱 PWA Initializer loaded');
})();
