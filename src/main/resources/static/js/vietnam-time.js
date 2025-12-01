/**
 * Vietnam Timezone Utilities
 * Múi giờ Việt Nam (UTC+7)
 */

const VietnamTime = {
    timezone: 'Asia/Ho_Chi_Minh',
    
    /**
     * Format datetime to Vietnam timezone string
     * @param {string|Date} dateInput - Date string or Date object
     * @param {boolean} includeSeconds - Whether to include seconds
     * @returns {string} Formatted datetime string
     */
    format(dateInput, includeSeconds = true) {
        if (!dateInput) return 'Chưa có';
        
        const date = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
        
        if (isNaN(date.getTime())) return 'N/A';
        
        const options = {
            timeZone: this.timezone,
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        };
        
        if (includeSeconds) {
            options.second = '2-digit';
        }
        
        return date.toLocaleString('vi-VN', options);
    },
    
    /**
     * Format date only (no time)
     * @param {string|Date} dateInput 
     * @returns {string}
     */
    formatDate(dateInput) {
        if (!dateInput) return 'Chưa có';
        
        const date = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
        
        if (isNaN(date.getTime())) return 'N/A';
        
        return date.toLocaleString('vi-VN', {
            timeZone: this.timezone,
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });
    },
    
    /**
     * Format time only (no date)
     * @param {string|Date} dateInput 
     * @returns {string}
     */
    formatTime(dateInput) {
        if (!dateInput) return 'Chưa có';
        
        const date = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
        
        if (isNaN(date.getTime())) return 'N/A';
        
        return date.toLocaleString('vi-VN', {
            timeZone: this.timezone,
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    },
    
    /**
     * Get current Vietnam time
     * @returns {Date}
     */
    now() {
        return new Date(new Date().toLocaleString('en-US', { timeZone: this.timezone }));
    },
    
    /**
     * Get relative time string (e.g., "5 phút trước")
     * @param {string|Date} dateInput 
     * @returns {string}
     */
    relative(dateInput) {
        if (!dateInput) return 'Chưa có';
        
        const date = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
        
        if (isNaN(date.getTime())) return 'N/A';
        
        const now = new Date();
        const diffMs = now - date;
        const diffSec = Math.floor(diffMs / 1000);
        const diffMin = Math.floor(diffSec / 60);
        const diffHour = Math.floor(diffMin / 60);
        const diffDay = Math.floor(diffHour / 24);
        
        if (diffSec < 60) return 'Vừa xong';
        if (diffMin < 60) return `${diffMin} phút trước`;
        if (diffHour < 24) return `${diffHour} giờ trước`;
        if (diffDay < 7) return `${diffDay} ngày trước`;
        
        return this.format(date, false);
    }
};

// Global function for backward compatibility
function formatVietnamTime(dateString) {
    return VietnamTime.format(dateString);
}
