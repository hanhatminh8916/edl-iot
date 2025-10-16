// Reports Page JavaScript

let workingHoursChart = null;
let activityChart = null;

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
    
    // Handle table row clicks
    const reportRows = document.querySelectorAll('.report-row');
    reportRows.forEach(row => {
        row.addEventListener('click', function() {
            const employeeName = this.querySelector('.employee-name-cell span').textContent;
            showEmployeeDetails(employeeName);
        });
    });
});

// Initialize reports
function initializeReports() {
    console.log('Reports page initialized');
    
    // Create charts
    createWorkingHoursChart();
    createActivityChart();
}

// Create Working Hours Chart
function createWorkingHoursChart() {
    const ctx = document.getElementById('workingHoursChart');
    if (!ctx) return;
    
    // Destroy existing chart if any
    if (workingHoursChart) {
        workingHoursChart.destroy();
    }
    
    workingHoursChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['T6.1', 'T6.2', 'T6.3', 'T6.4'],
            datasets: [{
                label: 'Giờ làm việc',
                data: [85, 78, 90, 88],
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
    
    activityChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['15/01', '16/01', '17/01', '18/01', '19/01', '20/01', '21/01'],
            datasets: [{
                label: 'Hoạt động',
                data: [320, 340, 335, 355, 310, 330, 280],
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
    
    let newData = [];
    let newLabels = [];
    
    switch(period) {
        case 'week':
            newLabels = ['T6.1', 'T6.2', 'T6.3', 'T6.4'];
            newData = [85, 78, 90, 88];
            break;
        case 'month':
            newLabels = ['Tuần 1', 'Tuần 2', 'Tuần 3', 'Tuần 4'];
            newData = [340, 320, 360, 350];
            break;
        case 'quarter':
            newLabels = ['Tháng 1', 'Tháng 2', 'Tháng 3'];
            newData = [1020, 980, 1080];
            break;
    }
    
    if (workingHoursChart) {
        workingHoursChart.data.labels = newLabels;
        workingHoursChart.data.datasets[0].data = newData;
        
        // Adjust y-axis max based on data
        const maxValue = Math.max(...newData);
        workingHoursChart.options.scales.y.max = Math.ceil(maxValue / 100) * 100 + 100;
        
        workingHoursChart.update('active');
    }
    
    showNotification('Đã cập nhật biểu đồ giờ làm việc', 'info');
}

// Update Activity Chart
function updateActivityChart(period) {
    console.log('Updating activity chart for period:', period);
    
    let newData = [];
    let newLabels = [];
    
    switch(period) {
        case 'week':
            newLabels = ['15/01', '16/01', '17/01', '18/01', '19/01', '20/01', '21/01'];
            newData = [320, 340, 335, 355, 310, 330, 280];
            break;
        case 'month':
            newLabels = ['Tuần 1', 'Tuần 2', 'Tuần 3', 'Tuần 4'];
            newData = [2300, 2400, 2350, 2280];
            break;
    }
    
    if (activityChart) {
        activityChart.data.labels = newLabels;
        activityChart.data.datasets[0].data = newData;
        
        // Adjust y-axis max based on data
        const maxValue = Math.max(...newData);
        activityChart.options.scales.y.max = Math.ceil(maxValue / 500) * 500 + 500;
        
        activityChart.update('active');
    }
    
    showNotification('Đã cập nhật biểu đồ hoạt động', 'info');
}

// Export Report
function exportReport() {
    showNotification('Đang xuất báo cáo...', 'info');
    
    // Simulate export process
    setTimeout(() => {
        // In real implementation, this would generate and download a PDF or Excel file
        const reportData = {
            timestamp: new Date().toLocaleString('vi-VN'),
            employees: [],
            summary: {
                totalHours: '2091.7h',
                avgEfficiency: '87.5%',
                totalViolations: 17,
                totalAlerts: 4
            }
        };
        
        // Collect employee data from table
        const rows = document.querySelectorAll('.report-row');
        rows.forEach(row => {
            const cells = row.querySelectorAll('td');
            reportData.employees.push({
                name: row.querySelector('.employee-name-cell span').textContent,
                position: cells[1].textContent,
                hours: cells[2].textContent,
                violations: cells[3].textContent,
                dangerZones: cells[4].textContent,
                fatigue: cells[5].textContent,
                efficiency: cells[6].textContent
            });
        });
        
        console.log('Report data:', reportData);
        
        // Create a blob and download
        const dataStr = JSON.stringify(reportData, null, 2);
        const dataBlob = new Blob([dataStr], { type: 'application/json' });
        const url = URL.createObjectURL(dataBlob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `bao-cao-${Date.now()}.json`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
        
        showNotification('Đã xuất báo cáo thành công!', 'success');
    }, 1500);
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