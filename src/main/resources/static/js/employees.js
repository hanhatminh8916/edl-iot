// Employees Page JavaScript
const API_BASE_URL = '/api';
let allWorkers = [];
let stompClient = null;
let reloadTimeout = null;

document.addEventListener('DOMContentLoaded', function() {
    // Load workers from API
    loadWorkers();
    
    // Initialize WebSocket for realtime updates
    initWebSocket();
    
    // Handle add employee button
    const btnAddEmployee = document.querySelector('.btn-add-employee');
    if (btnAddEmployee) {
        btnAddEmployee.addEventListener('click', function() {
            openEmployeeModal('add');
        });
    }
    
    // Handle search
    const searchInput = document.getElementById('employeeSearchInput');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            filterEmployees();
        });
    }
    
    // Handle filter
    const filterStatus = document.getElementById('filterEmployeeStatus');
    if (filterStatus) {
        filterStatus.addEventListener('change', function() {
            filterEmployees();
        });
    }
    
    // Handle modal
    setupModal();
    
    // Handle form submission
    const employeeForm = document.getElementById('employeeForm');
    if (employeeForm) {
        employeeForm.addEventListener('submit', function(e) {
            e.preventDefault();
            saveEmployee();
        });
    }
    
    // Auto refresh every 60 seconds as fallback
    setInterval(loadWorkers, 60000);
});

// WebSocket initialization for realtime updates
function initWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Disable debug logs
    
    stompClient.connect({}, function(frame) {
        console.log('✅ WebSocket connected for employees page');
        
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
        loadWorkers();
    }, 1000); // Wait 1 second after last message
}

// Load workers from API
async function loadWorkers() {
    try {
        const response = await fetch(`${API_BASE_URL}/workers`);
        allWorkers = await response.json();

        // Backend returns list of worker objects with helmet info
        displayWorkers(allWorkers);
        updateEmployeeStats();
    } catch (error) {
        console.error('Error loading workers:', error);
        showNotification('Không thể tải dữ liệu công nhân', 'error');
    }
}

// Load available helmets for dropdown
async function loadAvailableHelmets() {
    try {
        const response = await fetch('/api/helmet/all');
        const helmets = await response.json();
        
        const helmetSelect = document.getElementById('employeeHelmet');
        if (!helmetSelect) return;
        
        // Clear existing options except the first (placeholder)
        helmetSelect.innerHTML = '<option value="">Chọn mũ bảo hiểm (tùy chọn)</option>';
        
        // Add only unassigned helmets or show all based on mode
        helmets.forEach(helmet => {
            const isAssigned = helmet.worker != null;
            const statusText = helmet.status || 'INACTIVE';
            const batteryText = helmet.batteryLevel ? `${helmet.batteryLevel}%` : 'N/A';
            const label = `#${helmet.helmetId} - ${helmet.macAddress} (${statusText}, Pin: ${batteryText})`;
            
            // Show unassigned helmets or all if editing
            if (!isAssigned || helmetSelect.dataset.editMode) {
                const option = document.createElement('option');
                option.value = helmet.id;
                option.textContent = label;
                if (isAssigned) {
                    option.textContent += ' [Đã gán]';
                }
                helmetSelect.appendChild(option);
            }
        });
    } catch (error) {
        console.error('Error loading helmets:', error);
    }
}

// Display workers in grid
function displayWorkers(workers) {
    const grid = document.querySelector('.employees-list');
    if (!grid) return;
    
    if (workers.length === 0) {
        grid.innerHTML = `
            <div style="grid-column: 1/-1; text-align: center; padding: 3rem;">
                <i class="fas fa-users" style="font-size: 4rem; color: #cbd5e1; margin-bottom: 1rem;"></i>
                <p style="color: #64748b; font-size: 1.1rem;">Chưa có công nhân nào</p>
            </div>
        `;
        return;
    }
    
    grid.innerHTML = workers.map(worker => {
        const helmet = worker.helmet;
        const statusClass = helmet?.status?.toLowerCase() || 'inactive';
        const batteryLevel = helmet?.batteryLevel || 0;
        const batteryClass = batteryLevel > 50 ? 'good' : batteryLevel > 20 ? 'medium' : 'low';
        const alertType = helmet?.alertType || null;
        
        // ⭐ Safe extraction of employee info - handle both 'name' and 'fullName' properties
        const workerName = worker.name || worker.fullName || 'N/A';
        const workerPhone = worker.phone || worker.phoneNumber || 'N/A';
        const workerPosition = worker.position || 'N/A';
        const workerEmployeeId = worker.employeeId || 'N/A';
        
        // ⭐ Escape single quotes in name for onclick handler
        const escapedName = workerName.replace(/'/g, "\\'");
        
        // Alert badge for FALL or HELP_REQUEST
        let alertBadge = '';
        if (alertType === 'FALL') {
            alertBadge = '<span class="alert-badge fall">🚨 Té ngã</span>';
        } else if (alertType === 'HELP_REQUEST') {
            alertBadge = '<span class="alert-badge sos">🆘 Cầu cứu</span>';
        }
        
        return `
            <div class="employee-card ${alertType ? 'has-alert' : ''}" data-worker-id="${worker.id}" data-status="${statusClass}">
                <div class="employee-header">
                    <div class="employee-avatar">
                        <i class="fas fa-user"></i>
                        ${alertBadge}
                    </div>
                    <div class="employee-actions">
                        <button class="btn-icon edit" onclick="openEmployeeModal('edit', ${worker.id})" title="Chỉnh sửa">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn-icon delete" onclick="deleteEmployee(${worker.id}, '${escapedName}')" title="Xóa">
                            <i class="fas fa-trash-alt"></i>
                        </button>
                    </div>
                </div>
                <div class="employee-info">
                    <h3>${workerName}</h3>
                    <div class="employee-meta">
                        <span class="meta-item">
                            <i class="fas fa-phone"></i>
                            ${workerPhone}
                        </span>
                        <span class="meta-item">
                            <i class="fas fa-map-marker-alt"></i>
                            ${workerPosition}
                        </span>
                    </div>
                </div>
                <div class="employee-details">
                    <div class="detail-item">
                        <span>Mã CN:</span>
                        <strong>${workerEmployeeId}</strong>
                    </div>
                    <div class="detail-item">
                        <span>Mũ:</span>
                        <strong>${helmet?.helmetId || 'Chưa gán'}</strong>
                    </div>
                    <div class="detail-item">
                        <span>Pin:</span>
                        <div class="battery-indicator ${batteryClass}">
                            <i class="fas fa-battery-${batteryLevel > 80 ? 'full' : batteryLevel > 60 ? 'three-quarters' : batteryLevel > 40 ? 'half' : batteryLevel > 20 ? 'quarter' : 'empty'}"></i>
                            <span>${batteryLevel}%</span>
                        </div>
                    </div>
                    <div class="detail-item">
                        <span>Trạng thái:</span>
                        <span class="status-badge ${statusClass}">
                            ${getStatusText(helmet?.status)}
                        </span>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function getStatusText(status) {
    const statuses = {
        'ACTIVE': 'Đang làm',
        'INACTIVE': 'Offline',
        'OFFLINE': 'Ngoại tuyến',
        'ALERT': 'Cảnh báo',
        'DANGER': 'Nguy hiểm'
    };
    return statuses[status] || 'Không hoạt động';
}

// Open employee modal
function openEmployeeModal(mode, workerId = null) {
    const modal = document.getElementById('employeeModal');
    const modalTitle = document.getElementById('modalTitle');
    const form = document.getElementById('employeeForm');
    const helmetSelect = document.getElementById('employeeHelmet');
    
    if (mode === 'add') {
        modalTitle.textContent = 'Thêm công nhân mới';
        form.reset();
        delete form.dataset.workerId;
        if (helmetSelect) helmetSelect.dataset.editMode = '';
        loadAvailableHelmets(); // Load helmets for new worker
    } else if (mode === 'edit' && workerId) {
        modalTitle.textContent = 'Sửa thông tin công nhân';
        
        const worker = allWorkers.find(w => w.id === workerId);
        if (worker) {
            document.getElementById('employeeName').value = worker.name || worker.fullName || '';
            document.getElementById('employeePhone').value = worker.phone || worker.phoneNumber || '';
            document.getElementById('employeePosition').value = worker.position || '';
            document.getElementById('employeeLocation').value = worker.location || '';
            
            form.dataset.workerId = workerId;
            
            // Load helmets and select current one if exists
            if (helmetSelect) {
                helmetSelect.dataset.editMode = 'true';
                loadAvailableHelmets().then(() => {
                    if (worker.helmet && worker.helmet.id) {
                        helmetSelect.value = worker.helmet.id;
                    }
                });
            }
        }
    }
    
    modal.classList.add('active');
}

// Close modal
function closeEmployeeModal() {
    const modal = document.getElementById('employeeModal');
    modal.classList.remove('active');
}

// Setup modal
function setupModal() {
    const modal = document.getElementById('employeeModal');
    const closeBtn = document.querySelector('.modal-close');
    const cancelBtn = document.querySelector('.btn-cancel');
    
    // Close button
    if (closeBtn) {
        closeBtn.addEventListener('click', closeEmployeeModal);
    }
    
    // Cancel button
    if (cancelBtn) {
        cancelBtn.addEventListener('click', closeEmployeeModal);
    }
    
    // Click outside to close
    modal.addEventListener('click', function(e) {
        if (e.target === modal) {
            closeEmployeeModal();
        }
    });
}

// Save employee
async function saveEmployee() {
    const form = document.getElementById('employeeForm');
    const formData = new FormData(form);
    
    const workerData = {
        name: formData.get('employeeName'),
        phone: formData.get('employeePhone'),
        position: formData.get('employeePosition') || '',
        location: formData.get('employeeLocation') || ''
    };
    
    // Get selected helmet ID (optional)
    const helmetId = formData.get('employeeHelmet');
    if (helmetId) {
        workerData.helmetId = parseInt(helmetId);
    }
    
    // Validate
    if (!workerData.name || !workerData.phone || !workerData.position) {
        showNotification('Vui lòng điền đầy đủ thông tin bắt buộc!', 'error');
        return;
    }
    
    try {
        const workerId = form.dataset.workerId;
        let response;
        
        if (workerId) {
            // Update existing worker
            const res = await fetch(`${API_BASE_URL}/workers/${workerId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(workerData)
            });

            if (res.ok) {
                showNotification('Đã cập nhật công nhân thành công', 'success');
                closeEmployeeModal();
                form.reset();
                delete form.dataset.workerId;
                await loadWorkers(); // Reload the list
            } else {
                const errorText = await res.text();
                console.error('Server error:', errorText);
                showNotification('Lỗi khi cập nhật công nhân: ' + res.status, 'error');
            }
        } else {
            // Create new worker - call backend
            const res = await fetch(`${API_BASE_URL}/workers`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(workerData)
            });

            if (res.ok) {
                showNotification('Đã thêm công nhân thành công', 'success');
                closeEmployeeModal();
                form.reset();
                delete form.dataset.workerId;
                await loadWorkers(); // Reload the list
            } else {
                const errorText = await res.text();
                console.error('Server error:', errorText);
                showNotification('Lỗi khi thêm công nhân: ' + res.status, 'error');
            }
        }
        
    } catch (error) {
        console.error('Error saving worker:', error);
        showNotification('Không thể lưu thông tin công nhân', 'error');
    }
}

// Add employee card to the list
function addEmployeeCard(employee) {
    const employeesList = document.querySelector('.employees-list');
    
    const statusClass = employee.status === 'active' ? 'active' : 'inactive';
    const statusText = employee.status === 'active' ? 'Đang làm việc' : 'Không hoạt động';
    const statusIcon = employee.status === 'active' ? 'fa-circle' : 'fa-circle';
    
    const locationText = getLocationText(employee.location);
    
    // Random battery level for new employee
    const batteryLevel = Math.floor(Math.random() * 40) + 60; // 60-100%
    const batteryClass = getBatteryClass(batteryLevel);
    const batteryIcon = getBatteryIcon(batteryLevel);
    
    const cardHTML = `
        <div class="employee-card" data-status="${employee.status}">
            <div class="employee-header">
                <div class="employee-main-info">
                    <div class="employee-avatar-large">
                        <i class="fas fa-user"></i>
                    </div>
                    <div class="employee-info">
                        <h3>${employee.name}</h3>
                        <div class="employee-meta">
                            <span class="meta-item">
                                <i class="fas fa-phone"></i>
                                ${employee.phone}
                            </span>
                            <span class="meta-item">
                                <i class="fas fa-map-marker-alt"></i>
                                ${locationText}
                            </span>
                        </div>
                    </div>
                </div>
                <div class="employee-actions">
                    <button class="btn-icon edit" title="Sửa thông tin">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn-icon delete" title="Xóa">
                        <i class="fas fa-trash-alt"></i>
                    </button>
                </div>
            </div>
            <div class="employee-details">
                <div class="detail-item">
                    <i class="fas fa-id-card"></i>
                    <span>ID:</span>
                    <strong>${employee.id}</strong>
                </div>
                <div class="detail-item">
                    <i class="fas fa-clock"></i>
                    <span>Thời gian:</span>
                    <strong>0h</strong>
                </div>
                <div class="detail-item">
                    <i class="fas fa-calendar-alt"></i>
                    <span>Làm việc:</span>
                    <strong>Mới thêm</strong>
                </div>
                <div class="detail-item">
                    <i class="fas ${batteryIcon}"></i>
                    <span>Pin:</span>
                    <strong class="${batteryClass}">${batteryLevel}%</strong>
                </div>
                <div class="detail-item status">
                    <span class="status-badge-large ${statusClass}">
                        <i class="fas ${statusIcon}"></i>
                        ${statusText}
                    </span>
                </div>
            </div>
        </div>
    `;
    
    employeesList.insertAdjacentHTML('afterbegin', cardHTML);
    
    // Re-setup action buttons
    setupEmployeeActions();
}

// Get battery class based on level
function getBatteryClass(level) {
    if (level >= 60) return 'battery-good';
    if (level >= 30) return 'battery-medium';
    return 'battery-low';
}

// Get battery icon based on level
function getBatteryIcon(level) {
    if (level >= 80) return 'fa-battery-full';
    if (level >= 60) return 'fa-battery-three-quarters';
    if (level >= 40) return 'fa-battery-half';
    if (level >= 20) return 'fa-battery-quarter';
    return 'fa-battery-empty';
}

// Get location text from value
function getLocationText(value) {
    const locations = {
        '1': '1 - Khu đông',
        '2': '2 - Bắc',
        '3': '3 - Khu đông',
        '4': '4 - Nam'
    };
    return locations[value] || value;
}

// Delete employee
async function deleteEmployee(workerId, workerName) {
    if (!confirm(`Bạn có chắc chắn muốn xóa công nhân ${workerName}?`)) {
        return;
    }
    
    try {
        // Note: Delete API endpoint needs to be created in backend
        showNotification('Chức năng xóa đang được phát triển', 'info');
        // When API is ready:
        // const response = await fetch(`${API_BASE_URL}/workers/${workerId}`, {
        //     method: 'DELETE'
        // });
        // if (response.ok) {
        //     showNotification(`Đã xóa công nhân ${workerName}`, 'success');
        //     await loadWorkers();
        // }
    } catch (error) {
        console.error('Error deleting worker:', error);
        showNotification('Không thể xóa công nhân', 'error');
    }
}

// Filter employees
function filterEmployees() {
    const searchTerm = document.getElementById('employeeSearchInput')?.value.toLowerCase() || '';
    const statusFilter = document.getElementById('filterEmployeeStatus')?.value || 'all';
    
    let filtered = allWorkers.filter(worker => {
        // ⭐ Support both 'name' and 'fullName' properties
        const workerName = (worker.name || worker.fullName || '').toLowerCase();
        const workerPhone = (worker.phone || worker.phoneNumber || '').toLowerCase();
        const workerEmployeeId = (worker.employeeId || '').toLowerCase();
        const workerPosition = (worker.position || '').toLowerCase();
        
        // Search filter
        const matchesSearch = !searchTerm ||
            workerName.includes(searchTerm) ||
            workerPhone.includes(searchTerm) ||
            workerEmployeeId.includes(searchTerm) ||
            workerPosition.includes(searchTerm);
        
        // Status filter
        const workerStatus = worker.helmet?.status || 'INACTIVE';
        const matchesStatus = statusFilter === 'all' ||
            (statusFilter === 'ACTIVE' && workerStatus === 'ACTIVE') ||
            (statusFilter === 'INACTIVE' && workerStatus !== 'ACTIVE');
        
        return matchesSearch && matchesStatus;
    });
    
    displayWorkers(filtered);
    console.log(`Showing ${filtered.length} employees`);
}

// Update employee statistics
function updateEmployeeStats() {
    const cards = document.querySelectorAll('.employee-card');
    
    let total = cards.length;
    let active = 0;
    let inactive = 0;
    let offline = 0;
    
    cards.forEach(card => {
        const status = card.dataset.status;
        if (status === 'active') {
            active++;
        } else if (status === 'inactive') {
            inactive++;
        } else {
            offline++;
        }
    });
    
    // Update stat cards
    const statValues = document.querySelectorAll('.employee-stat-value');
    if (statValues.length >= 4) {
        statValues[0].textContent = total;
        statValues[1].textContent = active;
        statValues[2].textContent = inactive;
        statValues[3].textContent = offline;
    }
    
    // Animate stats
    statValues.forEach(value => {
        value.style.transform = 'scale(1.1)';
        setTimeout(() => {
            value.style.transform = 'scale(1)';
        }, 200);
    });
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
        z-index: 1001;
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

// Make functions globally accessible for onclick handlers
window.openEmployeeModal = openEmployeeModal;
window.deleteEmployee = deleteEmployee;