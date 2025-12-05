/**
 * 📱 PWA INITIALIZER - EDL SafeWork
 * 
 * Đăng ký Service Worker và khởi tạo PWA
 * Include file này vào tất cả các trang HTML (trước </body>)
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
            } catch (error) {
                console.error('❌ Service Worker registration failed:', error);
            }
        });
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
    `;
    document.head.appendChild(style);

    console.log('📱 PWA Initializer loaded');
})();
