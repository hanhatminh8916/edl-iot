/**
 * 📱 MOBILE NAVIGATION - Hamburger Menu
 * 
 * Tự động inject hamburger menu cho mobile
 * Include file này vào tất cả các trang HTML
 */

(function() {
    'use strict';
    
    // Menu items configuration
    const MENU_ITEMS = [
        { href: 'index.html', icon: 'fa-house', text: 'Tổng quan', id: 'index' },
        { href: 'location.html', icon: 'fa-location-dot', text: 'Giám sát vị trí', id: 'location' },
        { href: 'positioning-2d.html', icon: 'fa-map', text: 'Sơ đồ 2D', id: 'positioning-2d' },
        { href: 'manage-employees.html', icon: 'fa-users-gear', text: 'Quản lý công nhân', id: 'manage-employees' },
        { href: 'alerts.html', icon: 'fa-bell', text: 'Cảnh báo', id: 'alerts' },
        { href: 'reports.html', icon: 'fa-chart-bar', text: 'Báo cáo', id: 'reports' },
        { divider: true },
        { href: 'manage-helmets.html', icon: 'fa-hard-hat', text: 'Quản lý mũ', id: 'manage-helmets' },
        { href: 'employees.html', icon: 'fa-id-card', text: 'Danh sách NV', id: 'employees' },
        { href: 'ai-analytics.html', icon: 'fa-robot', text: 'AI Analytics', id: 'ai-analytics' }
    ];
    
    // Get current page
    function getCurrentPage() {
        const path = window.location.pathname;
        const filename = path.substring(path.lastIndexOf('/') + 1);
        return filename.replace('.html', '') || 'index';
    }
    
    // Create hamburger button
    function createHamburgerButton() {
        const btn = document.createElement('button');
        btn.className = 'mobile-menu-btn';
        btn.id = 'mobileMenuBtn';
        btn.innerHTML = '<i class="fas fa-bars"></i>';
        btn.setAttribute('aria-label', 'Menu');
        return btn;
    }
    
    // Create mobile menu overlay
    function createMobileMenu() {
        const currentPage = getCurrentPage();
        
        const overlay = document.createElement('div');
        overlay.className = 'mobile-menu-overlay';
        overlay.id = 'mobileMenuOverlay';
        
        const menu = document.createElement('div');
        menu.className = 'mobile-menu';
        menu.id = 'mobileMenu';
        
        // Header
        menu.innerHTML = `
            <div class="mobile-menu-header">
                <div class="mobile-menu-logo">
                    <span class="logo-icon">🏗️</span>
                    <span class="logo-text">EDL SafeWork</span>
                </div>
                <button class="mobile-menu-close" id="mobileMenuClose">
                    <i class="fas fa-times"></i>
                </button>
            </div>
            <nav class="mobile-menu-nav">
                ${MENU_ITEMS.map(item => {
                    if (item.divider) {
                        return '<div class="mobile-menu-divider"></div>';
                    }
                    const isActive = item.id === currentPage;
                    return `
                        <a href="${item.href}" class="mobile-menu-item ${isActive ? 'active' : ''}">
                            <i class="fas ${item.icon}"></i>
                            <span>${item.text}</span>
                            ${isActive ? '<span class="active-indicator"></span>' : ''}
                        </a>
                    `;
                }).join('')}
            </nav>
            <div class="mobile-menu-footer">
                <p>© 2024 EDL SafeWork</p>
                <p class="version">v1.0.0 PWA</p>
            </div>
        `;
        
        overlay.appendChild(menu);
        return overlay;
    }
    
    // Inject styles
    function injectStyles() {
        const style = document.createElement('style');
        style.id = 'mobile-nav-styles';
        style.textContent = `
            /* ==================== HAMBURGER BUTTON ==================== */
            .mobile-menu-btn {
                display: none;
                position: fixed;
                top: 12px;
                left: 12px;
                z-index: 9999;
                width: 44px;
                height: 44px;
                border: none;
                border-radius: 12px;
                background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
                color: white;
                font-size: 20px;
                cursor: pointer;
                box-shadow: 0 4px 15px rgba(30, 64, 175, 0.4);
                transition: all 0.3s ease;
            }
            
            .mobile-menu-btn:hover {
                transform: scale(1.05);
                box-shadow: 0 6px 20px rgba(30, 64, 175, 0.5);
            }
            
            .mobile-menu-btn:active {
                transform: scale(0.95);
            }
            
            /* ==================== OVERLAY ==================== */
            .mobile-menu-overlay {
                display: none;
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: rgba(0, 0, 0, 0.5);
                z-index: 10000;
                opacity: 0;
                transition: opacity 0.3s ease;
            }
            
            .mobile-menu-overlay.active {
                display: block;
                opacity: 1;
            }
            
            /* ==================== MOBILE MENU ==================== */
            .mobile-menu {
                position: fixed;
                top: 0;
                left: -300px;
                width: 280px;
                height: 100%;
                background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%);
                z-index: 10001;
                display: flex;
                flex-direction: column;
                transition: left 0.3s ease;
                box-shadow: 5px 0 30px rgba(0, 0, 0, 0.3);
            }
            
            .mobile-menu-overlay.active .mobile-menu {
                left: 0;
            }
            
            /* Header */
            .mobile-menu-header {
                display: flex;
                align-items: center;
                justify-content: space-between;
                padding: 16px 20px;
                border-bottom: 1px solid rgba(255, 255, 255, 0.1);
            }
            
            .mobile-menu-logo {
                display: flex;
                align-items: center;
                gap: 10px;
            }
            
            .mobile-menu-logo .logo-icon {
                font-size: 24px;
            }
            
            .mobile-menu-logo .logo-text {
                font-size: 18px;
                font-weight: 700;
                color: white;
            }
            
            .mobile-menu-close {
                width: 36px;
                height: 36px;
                border: none;
                border-radius: 8px;
                background: rgba(255, 255, 255, 0.1);
                color: white;
                font-size: 18px;
                cursor: pointer;
                transition: background 0.2s;
            }
            
            .mobile-menu-close:hover {
                background: rgba(255, 255, 255, 0.2);
            }
            
            /* Navigation */
            .mobile-menu-nav {
                flex: 1;
                overflow-y: auto;
                padding: 12px 0;
            }
            
            .mobile-menu-item {
                display: flex;
                align-items: center;
                gap: 14px;
                padding: 14px 20px;
                color: rgba(255, 255, 255, 0.8);
                text-decoration: none;
                font-size: 15px;
                transition: all 0.2s;
                position: relative;
            }
            
            .mobile-menu-item:hover {
                background: rgba(255, 255, 255, 0.08);
                color: white;
            }
            
            .mobile-menu-item.active {
                background: linear-gradient(90deg, rgba(59, 130, 246, 0.3) 0%, transparent 100%);
                color: #60a5fa;
            }
            
            .mobile-menu-item i {
                width: 24px;
                text-align: center;
                font-size: 16px;
            }
            
            .active-indicator {
                position: absolute;
                left: 0;
                top: 50%;
                transform: translateY(-50%);
                width: 4px;
                height: 24px;
                background: #3b82f6;
                border-radius: 0 4px 4px 0;
            }
            
            .mobile-menu-divider {
                height: 1px;
                background: rgba(255, 255, 255, 0.1);
                margin: 8px 20px;
            }
            
            /* Footer */
            .mobile-menu-footer {
                padding: 16px 20px;
                border-top: 1px solid rgba(255, 255, 255, 0.1);
                text-align: center;
            }
            
            .mobile-menu-footer p {
                color: rgba(255, 255, 255, 0.5);
                font-size: 12px;
                margin: 0;
            }
            
            .mobile-menu-footer .version {
                margin-top: 4px;
                color: #3b82f6;
            }
            
            /* ==================== RESPONSIVE ==================== */
            @media (max-width: 768px) {
                .mobile-menu-btn {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                
                /* Hide desktop sidebar */
                .sidebar, .nav-sidebar, aside.sidebar {
                    display: none !important;
                }
                
                /* Adjust main content */
                .main-content, main, .content {
                    margin-left: 0 !important;
                    padding-left: 16px !important;
                    padding-right: 16px !important;
                }
                
                /* Adjust header for mobile */
                .header, header, .top-header {
                    padding-left: 60px !important;
                }
                
                /* Dashboard cards */
                .dashboard-cards, .stats-grid, .card-grid {
                    grid-template-columns: 1fr !important;
                    gap: 12px !important;
                }
                
                /* Tables responsive */
                .table-responsive, .data-table-container {
                    overflow-x: auto;
                    -webkit-overflow-scrolling: touch;
                }
                
                /* Map container */
                #map, .map-container {
                    height: 50vh !important;
                    min-height: 300px;
                }
                
                /* Worker list on location page */
                .worker-list, .sidebar-content {
                    max-height: 40vh;
                }
            }
            
            @media (max-width: 480px) {
                .mobile-menu {
                    width: 100%;
                    left: -100%;
                }
                
                .mobile-menu-btn {
                    top: 8px;
                    left: 8px;
                    width: 40px;
                    height: 40px;
                    font-size: 18px;
                }
                
                /* Smaller padding on very small screens */
                .main-content, main, .content {
                    padding: 8px !important;
                    padding-top: 60px !important;
                }
                
                .header, header, .top-header {
                    padding: 8px !important;
                    padding-left: 56px !important;
                }
            }
        `;
        document.head.appendChild(style);
    }
    
    // Initialize mobile navigation
    function init() {
        // Only init on pages that don't already have mobile nav
        if (document.getElementById('mobileMenuBtn')) {
            return;
        }
        
        // Inject styles
        injectStyles();
        
        // Create and add hamburger button
        const hamburgerBtn = createHamburgerButton();
        document.body.appendChild(hamburgerBtn);
        
        // Create and add mobile menu
        const mobileMenu = createMobileMenu();
        document.body.appendChild(mobileMenu);
        
        // Event listeners
        const overlay = document.getElementById('mobileMenuOverlay');
        const closeBtn = document.getElementById('mobileMenuClose');
        
        hamburgerBtn.addEventListener('click', () => {
            overlay.classList.add('active');
            document.body.style.overflow = 'hidden';
        });
        
        closeBtn.addEventListener('click', closeMobileMenu);
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) {
                closeMobileMenu();
            }
        });
        
        // Close on ESC key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && overlay.classList.contains('active')) {
                closeMobileMenu();
            }
        });
        
        // Close menu on swipe left
        let touchStartX = 0;
        overlay.addEventListener('touchstart', (e) => {
            touchStartX = e.touches[0].clientX;
        });
        
        overlay.addEventListener('touchmove', (e) => {
            const touchX = e.touches[0].clientX;
            const diff = touchStartX - touchX;
            if (diff > 50) {
                closeMobileMenu();
            }
        });
        
        console.log('📱 Mobile navigation initialized');
    }
    
    function closeMobileMenu() {
        const overlay = document.getElementById('mobileMenuOverlay');
        if (overlay) {
            overlay.classList.remove('active');
            document.body.style.overflow = '';
        }
    }
    
    // Expose close function globally
    window.closeMobileMenu = closeMobileMenu;
    
    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
    
})();
