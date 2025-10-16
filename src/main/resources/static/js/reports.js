// Reports Page JavaScript
const API_BASE_URL = '/api';
let workingHoursChart = null;
let activityChart = null;
let reportData = null;

document.addEventListener('DOMContentLoaded', function() {
    // Initialize reports page
    initializeReports();
    
    // Handle chart filters
    const hoursFilter = document.getElementById('hoursFilter');
    const activityFilter = document.getElementById('activityFilter');
    
    if (hoursFilter) {
        hoursFilter.addEventListener('change', function() {
            updateWorkingHoursChart(this.value);
        });
    }
    
    if (activityFilter) {
        activityFilter.addEventListener('change', function() {
            updateActivityChart(this.value);
        });
    }
    
    // Handle export button
    const btnExport = document.querySelector('.btn-export-report');
    if (btnExport) {
        btnExport.addEventListener('click', function() {
            exportReport();
        });
    }
    
    // Auto refresh every 5 minutes
    setInterval(loadReportData, 300000);
});

// Initialize reports
async function initializeReports() {
    console.log('Reports page initialized');
    
    // Load data from API first
    await loadReportData();
    
    // Create charts with real data
    createWorkingHoursChart();
    createActivityChart();
}

// Load report data from API
async function loadReportData() {
    try {
        // Get date range (last 7 days)
        const endDate = new Date();
        const startDate = new Date();
        startDate.setDate(startDate.getDate() - 7);
        
        const startDateStr = startDate.toISOString().split('T')[0];
        const endDateStr = endDate.toISOString().split('T')[0];
        
        const response = await fetch(`${API_BASE_URL}/dashboard/reports?startDate=${startDateStr}&endDate=${endDateStr}`);
        reportData = await response.json();
        
        // Update UI with real data
        updateReportTable();
        updateSummaryStats();
        
        console.log('Report data loaded:', reportData);
    } catch (error) {
        console.error('Error loading report data:', error);
        showNotification('Không thể tải dữ liệu báo cáo', 'error');
    }
}

// Update report table with real data
function updateReportTable() {
    if (!reportData || !reportData.workers) return;
    
    const tbody = document.querySelector('.report-table tbody');
    if (!tbody) return;
    
    tbody.innerHTML = reportData.workers.map(worker => `
        <tr class="report-row" data-worker-id="${worker.id}">
            <td class="employee-name-cell">
                <div class="employee-avatar-small">
                    <i class="fas fa-user"></i>
                </div>
                <span>${worker.name}</span>
            </td>
            <td>${worker.position || 'N/A'}</td>
            <td>${worker.workingHours || 0}h</td>
            <td>
                ${worker.violations > 0 
                    ? `<span class="badge badge-warning">${worker.violations}</span>` 
                    : '<span style="color: #10b981;">0</span>'}
            </td>
            <td>${worker.dangerZoneEntries || 0}</td>
            <td>
                <span class="fatigue-badge ${getFatigueClass(worker.fatigueLevel)}">
                    ${worker.fatigueLevel || 'N/A'}
                </span>
            </td>
            <td>
                <div class="efficiency-bar">
                    <div class="efficiency-fill ${getEfficiencyClass(worker.efficiency)}" 
                         style="width: ${worker.efficiency || 0}%"></div>
                    <span>${worker.efficiency || 0}%</span>
                </div>
            </td>
        </tr>
    `).join('');
    
    // Add click handlers to rows
    document.querySelectorAll('.report-row').forEach(row => {
        row.addEventListener('click', function() {
            const workerId = this.dataset.workerId;
            const worker = reportData.workers.find(w => w.id == workerId);
            if (worker) {
                showEmployeeDetails(worker.name);
            }
        });
    });
}

// Update summary statistics
function updateSummaryStats() {
    if (!reportData || !reportData.summary) return;
    
    const summary = reportData.summary;
    
    // Update stat cards
    const statValues = document.querySelectorAll('.stat-value');
    if (statValues.length >= 5) {
        statValues[0].textContent = summary.totalWorkers || 0;
        statValues[1].textContent = summary.activeWorkers || 0;
        statValues[2].textContent = `${summary.totalWorkingHours || 0}h`;
        statValues[3].textContent = `${summary.averageEfficiency || 0}%`;
        statValues[4].textContent = summary.totalViolations || 0;
    }
}

// Helper functions
function getFatigueClass(level) {
    if (!level) return '';
    const l = level.toLowerCase();
    if (l.includes('thấp') || l === 'low') return 'fatigue-low';
    if (l.includes('cao') || l === 'high') return 'fatigue-high';
    return 'fatigue-medium';
}

function getEfficiencyClass(efficiency) {
    if (efficiency >= 80) return 'efficiency-good';
    if (efficiency >= 60) return 'efficiency-medium';
    return 'efficiency-low';
}

// Create Working Hours Chart
function createWorkingHoursChart() {
    const ctx = document.getElementById('workingHoursChart');
    if (!ctx) return;
    
    // Destroy existing chart if any
    if (workingHoursChart) {
        workingHoursChart.destroy();
    }
    
    // Get working hours data from workers
    let labels = [];
    let data = [];
    
    if (reportData && reportData.workers) {
        // Take top 5 workers by working hours
        const topWorkers = [...reportData.workers]
            .sort((a, b) => (b.workingHours || 0) - (a.workingHours || 0))
            .slice(0, 5);
        
        labels = topWorkers.map(w => w.name.split(' ').slice(-1)[0]); // Last name only
        data = topWorkers.map(w => w.workingHours || 0);
    } else {
        // Fallback to sample data
        labels = ['CN 1', 'CN 2', 'CN 3', 'CN 4', 'CN 5'];
        data = [8.5, 7.8, 9.0, 8.8, 8.2];
    }
    
    workingHoursChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Giờ làm việc',
                data: data,
                backgroundColor: 'rgba(16, 185, 129, 0.8)',
                borderColor: 'rgba(16, 185, 129, 1)',
                borderWidth: 1,
                borderRadius: 8,
                barThickness: 60
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    padding: 12,
                    cornerRadius: 8,
                    titleFont: {
                        size: 14,
                        weight: 'bold'
                    },
                    bodyFont: {
                        size: 13
                    },
                    callbacks: {
                        label: function(context) {
                            return context.parsed.y + ' giờ';
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    max: 100,
                    ticks: {
                        stepSize: 25,
                        color: '#6b7280',
                        font: {
                            size: 12
                        }
                    },
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)',
                        drawBorder: false
                    }
                },
                x: {
                    ticks: {
                        color: '#6b7280',
                        font: {
                            size: 12
                        }
                    },
                    grid: {
                        display: false
                    }
                }
            }
        }
    });
}

// Create Activity Chart
function createActivityChart() {
    const ctx = document.getElementById('activityChart');
    if (!ctx) return;
    
    // Destroy existing chart if any
    if (activityChart) {
        activityChart.destroy();
    }
    
    // Generate activity data for last 7 days based on workers
    let labels = [];
    let data = [];
    
    for (let i = 6; i >= 0; i--) {
        const date = new Date();
        date.setDate(date.getDate() - i);
        labels.push(date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }));
        
        // Calculate activity count (mock: based on number of active workers)
        if (reportData && reportData.summary) {
            const baseActivity = reportData.summary.activeWorkers * 40; // Each worker ~40 activities/day
            const variance = Math.random() * 20 - 10; // +/- 10
            data.push(Math.round(baseActivity + variance));
        } else {
            data.push(Math.round(200 + Math.random() * 100)); // Fallback
        }
    }
    
    activityChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Hoạt động',
                data: data,
                backgroundColor: 'rgba(99, 102, 241, 0.8)',
                borderColor: 'rgba(99, 102, 241, 1)',
                borderWidth: 1,
                borderRadius: 8,
                barThickness: 40
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    padding: 12,
                    cornerRadius: 8,
                    titleFont: {
                        size: 14,
                        weight: 'bold'
                    },
                    bodyFont: {
                        size: 13
                    },
                    callbacks: {
                        label: function(context) {
                            return context.parsed.y + ' hoạt động';
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    max: 400,
                    ticks: {
                        stepSize: 100,
                        color: '#6b7280',
                        font: {
                            size: 12
                        }
                    },
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)',
                        drawBorder: false
                    }
                },
                x: {
                    ticks: {
                        color: '#6b7280',
                        font: {
                            size: 11
                        }
                    },
                    grid: {
                        display: false
                    }
                }
            }
        }
    });
}

// Update Working Hours Chart
function updateWorkingHoursChart(period) {
    console.log('Updating working hours chart for period:', period);
    
    if (!reportData || !reportData.workers) {
        showNotification('Chưa có dữ liệu báo cáo', 'error');
        return;
    }
    
    let newData = [];
    let newLabels = [];
    
    switch(period) {
        case 'week':
            // Show top 5 workers by hours
            const topWorkers = [...reportData.workers]
                .sort((a, b) => (b.workingHours || 0) - (a.workingHours || 0))
                .slice(0, 5);
            newLabels = topWorkers.map(w => w.name.split(' ').slice(-1)[0]);
            newData = topWorkers.map(w => w.workingHours || 0);
            break;
        case 'month':
            // Aggregate by week (mock data based on daily average)
            newLabels = ['Tuần 1', 'Tuần 2', 'Tuần 3', 'Tuần 4'];
            const avgHours = reportData.workers.reduce((sum, w) => sum + (w.workingHours || 0), 0) / reportData.workers.length;
            newData = [
                Math.round(avgHours * 5 * 10) / 10,
                Math.round(avgHours * 5 * 0.95 * 10) / 10,
                Math.round(avgHours * 5 * 1.05 * 10) / 10,
                Math.round(avgHours * 5 * 1.02 * 10) / 10
            ];
            break;
        case 'quarter':
            // Aggregate by month
            newLabels = ['Tháng 1', 'Tháng 2', 'Tháng 3'];
            const avgHoursMonth = reportData.workers.reduce((sum, w) => sum + (w.workingHours || 0), 0) / reportData.workers.length;
            newData = [
                Math.round(avgHoursMonth * 20 * 10) / 10,
                Math.round(avgHoursMonth * 20 * 0.96 * 10) / 10,
                Math.round(avgHoursMonth * 20 * 1.06 * 10) / 10
            ];
            break;
    }
    
    if (workingHoursChart) {
        workingHoursChart.data.labels = newLabels;
        workingHoursChart.data.datasets[0].data = newData;
        
        // Adjust y-axis max based on data
        const maxValue = Math.max(...newData);
        workingHoursChart.options.scales.y.max = Math.ceil(maxValue * 1.2 / 10) * 10;
        
        workingHoursChart.update('active');
    }
    
    showNotification('Đã cập nhật biểu đồ giờ làm việc', 'info');
}

// Update Activity Chart
function updateActivityChart(period) {
    console.log('Updating activity chart for period:', period);
    
    if (!reportData || !reportData.summary) {
        showNotification('Chưa có dữ liệu báo cáo', 'error');
        return;
    }
    
    let newData = [];
    let newLabels = [];
    const baseActivity = reportData.summary.activeWorkers * 40;
    
    switch(period) {
        case 'week':
            // Last 7 days
            for (let i = 6; i >= 0; i--) {
                const date = new Date();
                date.setDate(date.getDate() - i);
                newLabels.push(date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' }));
                newData.push(Math.round(baseActivity + (Math.random() * 40 - 20)));
            }
            break;
        case 'month':
            // Last 4 weeks
            newLabels = ['Tuần 1', 'Tuần 2', 'Tuần 3', 'Tuần 4'];
            newData = [
                Math.round(baseActivity * 5 * 1.05),
                Math.round(baseActivity * 5 * 1.10),
                Math.round(baseActivity * 5 * 1.08),
                Math.round(baseActivity * 5 * 1.04)
            ];
            break;
    }
    
    if (activityChart) {
        activityChart.data.labels = newLabels;
        activityChart.data.datasets[0].data = newData;
        
        // Adjust y-axis max based on data
        const maxValue = Math.max(...newData);
        activityChart.options.scales.y.max = Math.ceil(maxValue / 100) * 100 + 100;
        
        activityChart.update('active');
    }
    
    showNotification('Đã cập nhật biểu đồ hoạt động', 'info');
}

// Export Report
function exportReport() {
    if (!reportData) {
        showNotification('Chưa có dữ liệu để xuất báo cáo', 'error');
        return;
    }
    
    showNotification('Đang xuất báo cáo...', 'info');
    
    // Simulate export process
    setTimeout(() => {
        // Create export data with full report information
        const exportData = {
            title: 'Báo cáo Giám Sát An Toàn Lao Động',
            generatedAt: new Date().toLocaleString('vi-VN'),
            period: {
                startDate: reportData.startDate,
                endDate: reportData.endDate
            },
            summary: {
                totalWorkers: reportData.summary.totalWorkers,
                activeWorkers: reportData.summary.activeWorkers,
                totalWorkingHours: reportData.summary.totalWorkingHours + 'h',
                averageEfficiency: reportData.summary.averageEfficiency + '%',
                totalViolations: reportData.summary.totalViolations,
                totalCriticalAlerts: reportData.summary.totalCriticalAlerts
            },
            workers: reportData.workers.map(w => ({
                id: w.employeeId,
                name: w.name,
                position: w.position,
                department: w.department,
                helmetId: w.helmetId,
                workingHours: w.workingHours + 'h',
                violations: w.violations,
                dangerZoneEntries: w.dangerZoneEntries,
                fatigueLevel: w.fatigueLevel,
                efficiency: w.efficiency + '%',
                batteryLevel: w.batteryLevel + '%',
                status: w.status
            }))
        };
        
        console.log('Exporting report:', exportData);
        
        // Create a blob and download as JSON
        const dataStr = JSON.stringify(exportData, null, 2);
        const dataBlob = new Blob([dataStr], { type: 'application/json' });
        const url = URL.createObjectURL(dataBlob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `bao-cao-an-toan-${new Date().toISOString().split('T')[0]}.json`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
        
        showNotification('Đã xuất báo cáo thành công!', 'success');
    }, 1000);
}

// Show employee details
function showEmployeeDetails(employeeName) {
    const message = `Xem chi tiết báo cáo cho ${employeeName}?\n\nChức năng này sẽ hiển thị:\n- Biểu đồ chi tiết giờ làm việc\n- Lịch sử vi phạm\n- Xu hướng hiệu suất\n- Báo cáo an toàn`;
    
    if (confirm(message)) {
        showNotification(`Đang tải báo cáo chi tiết cho ${employeeName}...`, 'info');
        
        // In real implementation, this would navigate to a detailed report page
        setTimeout(() => {
            showNotification('Chức năng đang được phát triển', 'info');
        }, 1000);
    }
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

// Resize charts on window resize
window.addEventListener('resize', function() {
    if (workingHoursChart) {
        workingHoursChart.resize();
    }
    if (activityChart) {
        activityChart.resize();
    }
});

// Export functions
window.reportsFunctions = {
    updateWorkingHoursChart,
    updateActivityChart,
    exportReport
};