// Authentication middleware
// Check if user is logged in before accessing protected pages

(function() {
    // Get current page
    const currentPage = window.location.pathname.split('/').pop();
    
    // Pages that don't require authentication
    const publicPages = ['login.html', ''];
    
    // Check if current page requires authentication
    if (!publicPages.includes(currentPage)) {
        const isLoggedIn = sessionStorage.getItem('isLoggedIn');
        
        if (!isLoggedIn || isLoggedIn !== 'true') {
            // Redirect to login page
            window.location.href = 'login.html';
        }
    }
    
    // Add logout functionality
    function logout() {
        sessionStorage.removeItem('isLoggedIn');
        sessionStorage.removeItem('username');
        window.location.href = 'login.html';
    }
    
    // Make logout function globally available
    window.logout = logout;
    
    // Update header with username if logged in
    window.addEventListener('DOMContentLoaded', function() {
        const username = sessionStorage.getItem('username');
        if (username) {
            const adminProfile = document.querySelector('.admin-profile span');
            if (adminProfile) {
                adminProfile.textContent = username;
            }
            
            // Add logout button to admin profile
            const adminProfileDiv = document.querySelector('.admin-profile');
            if (adminProfileDiv && !adminProfileDiv.querySelector('.logout-btn')) {
                adminProfileDiv.style.cursor = 'pointer';
                adminProfileDiv.style.position = 'relative';
                
                adminProfileDiv.addEventListener('click', function(e) {
                    e.stopPropagation();
                    
                    // Create dropdown menu if not exists
                    let dropdown = document.querySelector('.profile-dropdown');
                    if (!dropdown) {
                        dropdown = document.createElement('div');
                        dropdown.className = 'profile-dropdown';
                        dropdown.innerHTML = `
                            <div class="dropdown-item" onclick="window.location.href='#settings'">
                                <i class="fas fa-user-cog"></i>
                                <span>Cài đặt tài khoản</span>
                            </div>
                            <div class="dropdown-divider"></div>
                            <div class="dropdown-item logout-item" onclick="logout()">
                                <i class="fas fa-sign-out-alt"></i>
                                <span>Đăng xuất</span>
                            </div>
                        `;
                        
                        // Add styles
                        const style = document.createElement('style');
                        style.textContent = `
                            .profile-dropdown {
                                position: absolute;
                                top: calc(100% + 0.5rem);
                                right: 0;
                                background: white;
                                border-radius: 8px;
                                box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                                min-width: 200px;
                                z-index: 1000;
                                opacity: 0;
                                visibility: hidden;
                                transform: translateY(-10px);
                                transition: all 0.3s;
                            }
                            
                            .profile-dropdown.show {
                                opacity: 1;
                                visibility: visible;
                                transform: translateY(0);
                            }
                            
                            .dropdown-item {
                                display: flex;
                                align-items: center;
                                gap: 0.75rem;
                                padding: 0.875rem 1.25rem;
                                color: #374151;
                                cursor: pointer;
                                transition: background-color 0.2s;
                            }
                            
                            .dropdown-item:first-child {
                                border-radius: 8px 8px 0 0;
                            }
                            
                            .dropdown-item:last-child {
                                border-radius: 0 0 8px 8px;
                            }
                            
                            .dropdown-item:hover {
                                background-color: #f3f4f6;
                            }
                            
                            .dropdown-item i {
                                width: 1.25rem;
                                text-align: center;
                                color: #6b7280;
                            }
                            
                            .logout-item {
                                color: #ef4444;
                            }
                            
                            .logout-item:hover {
                                background-color: #fee2e2;
                            }
                            
                            .logout-item i {
                                color: #ef4444;
                            }
                            
                            .dropdown-divider {
                                height: 1px;
                                background-color: #e5e7eb;
                                margin: 0.5rem 0;
                            }
                        `;
                        document.head.appendChild(style);
                        
                        adminProfileDiv.appendChild(dropdown);
                    }
                    
                    dropdown.classList.toggle('show');
                });
                
                // Close dropdown when clicking outside
                document.addEventListener('click', function(e) {
                    const dropdown = document.querySelector('.profile-dropdown');
                    if (dropdown && !adminProfileDiv.contains(e.target)) {
                        dropdown.classList.remove('show');
                    }
                });
            }
        }
    });
})();
