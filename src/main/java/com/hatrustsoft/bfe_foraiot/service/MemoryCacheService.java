package com.hatrustsoft.bfe_foraiot.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.hatrustsoft.bfe_foraiot.entity.Employee;
import com.hatrustsoft.bfe_foraiot.entity.MessengerUser;
import com.hatrustsoft.bfe_foraiot.repository.EmployeeRepository;
import com.hatrustsoft.bfe_foraiot.repository.MessengerUserRepository;
import com.hatrustsoft.bfe_foraiot.util.VietnamTimeUtils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 🚀 Memory Cache Service - Giảm queries đến database
 * 
 * Cache các dữ liệu ít thay đổi:
 * - Employee by MAC address (cache vĩnh viễn, refresh mỗi 5 phút)
 * - MessengerUsers (cache 5 phút)
 * - Helmet update tracking (chỉ update DB mỗi 30s)
 * - tag_last_position tracking (chỉ save mỗi 30s)
 */
@Service
@Slf4j
public class MemoryCacheService {

    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private MessengerUserRepository messengerUserRepository;

    // ========== EMPLOYEE CACHE ==========
    // Key: MAC address, Value: Employee (null = không tìm thấy)
    private final Map<String, Optional<Employee>> employeeByMacCache = new ConcurrentHashMap<>();
    
    // ========== MESSENGER USERS CACHE ==========
    private List<MessengerUser> messengerUsersCache = null;
    private LocalDateTime messengerUsersCacheTime = null;
    private static final long MESSENGER_USERS_CACHE_MINUTES = 5;
    
    // ========== HELMET UPDATE TRACKING ==========
    // Key: MAC address, Value: last update time
    private final Map<String, LocalDateTime> lastHelmetUpdateTime = new ConcurrentHashMap<>();
    private static final long HELMET_UPDATE_INTERVAL_SECONDS = 30;
    
    // ========== TAG POSITION TRACKING ==========
    // Key: MAC address, Value: last save time
    private final Map<String, LocalDateTime> lastTagPositionSaveTime = new ConcurrentHashMap<>();
    private static final long TAG_POSITION_SAVE_INTERVAL_SECONDS = 30;
    
    // ========== DANGER ALERT DEBOUNCE ==========
    // Key: MAC address, Value: last alert time
    private final Map<String, LocalDateTime> lastDangerAlertTime = new ConcurrentHashMap<>();
    private static final long DANGER_ALERT_DEBOUNCE_SECONDS = 60;
    
    // ========== MAC ADDRESS NOISE FILTER ==========
    // 🔧 Lọc nhiễu MAC: Chỉ thêm helmet mới khi nhận >= 3 message từ MAC đó
    // Key: MAC address, Value: số lần nhận được message
    private final Map<String, Integer> macMessageCounter = new ConcurrentHashMap<>();
    private static final int MAC_CONFIRMATION_THRESHOLD = 3; // Phải nhận 3 lần mới được thêm vào DB (~6 giây)

    /**
     * 🔄 Khởi tạo cache khi app start
     */
    @PostConstruct
    public void initCache() {
        log.info("🚀 Initializing memory cache...");
        refreshEmployeeCache();
        refreshMessengerUsersCache();
        log.info("✅ Memory cache initialized");
    }

    // ==================== EMPLOYEE CACHE ====================
    
    /**
     * 🔍 Tìm Employee theo MAC address (từ cache)
     * GIẢM: ~2 queries/message → 0 queries (cache hit)
     */
    public Optional<Employee> getEmployeeByMac(String macAddress) {
        if (macAddress == null) return Optional.empty();
        
        // Check cache first
        if (employeeByMacCache.containsKey(macAddress)) {
            return employeeByMacCache.get(macAddress);
        }
        
        // Cache miss - query DB và cache kết quả
        Optional<Employee> employee = employeeRepository.findByMacAddress(macAddress);
        employeeByMacCache.put(macAddress, employee);
        
        if (employee.isPresent()) {
            log.debug("📦 Cached employee for MAC: {} → {}", macAddress, employee.get().getName());
        }
        
        return employee;
    }
    
    /**
     * 🚀 Lấy toàn bộ Employee Map (MAC → Employee) - dùng cho batch lookup
     * GIẢM: N queries → 0 queries khi cần lookup nhiều employees
     */
    public Map<String, Employee> getEmployeeMap() {
        Map<String, Employee> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, Optional<Employee>> entry : employeeByMacCache.entrySet()) {
            entry.getValue().ifPresent(emp -> result.put(entry.getKey(), emp));
        }
        return result;
    }
    
    /**
     * 🔄 Refresh employee cache mỗi 5 phút
     */
    @Scheduled(fixedRate = 300000) // 5 phút
    public void refreshEmployeeCache() {
        try {
            List<Employee> employees = employeeRepository.findAll();
            employeeByMacCache.clear();
            
            for (Employee emp : employees) {
                if (emp.getMacAddress() != null) {
                    employeeByMacCache.put(emp.getMacAddress(), Optional.of(emp));
                }
            }
            
            log.info("🔄 Refreshed employee cache: {} employees", employees.size());
        } catch (Exception e) {
            log.error("❌ Error refreshing employee cache: {}", e.getMessage());
        }
    }
    
    /**
     * 🗑️ Invalidate employee cache (khi có thay đổi)
     */
    public void invalidateEmployeeCache(String macAddress) {
        employeeByMacCache.remove(macAddress);
    }

    // ==================== MESSENGER USERS CACHE ====================
    
    /**
     * 🔍 Lấy danh sách MessengerUsers (cache 5 phút)
     * GIẢM: ~1 query/alert → 1 query/5 phút
     */
    public List<MessengerUser> getMessengerUsers() {
        LocalDateTime now = VietnamTimeUtils.now();
        
        // Check cache validity
        if (messengerUsersCache != null && messengerUsersCacheTime != null) {
            if (messengerUsersCacheTime.plusMinutes(MESSENGER_USERS_CACHE_MINUTES).isAfter(now)) {
                return messengerUsersCache;
            }
        }
        
        // Cache expired - refresh
        refreshMessengerUsersCache();
        return messengerUsersCache;
    }
    
    /**
     * 🔄 Refresh messenger users cache
     */
    public void refreshMessengerUsersCache() {
        try {
            messengerUsersCache = messengerUserRepository.findBySubscribedTrue();
            messengerUsersCacheTime = VietnamTimeUtils.now();
            log.info("🔄 Refreshed messenger users cache: {} users", 
                messengerUsersCache != null ? messengerUsersCache.size() : 0);
        } catch (Exception e) {
            log.error("❌ Error refreshing messenger users cache: {}", e.getMessage());
            messengerUsersCache = List.of();
        }
    }

    // ==================== HELMET UPDATE TRACKING ====================
    
    /**
     * ✅ Kiểm tra xem có nên update helmet vào DB không (mỗi 30s)
     * GIẢM: ~2-3 queries/message → queries mỗi 30s
     */
    public boolean shouldUpdateHelmet(String macAddress) {
        LocalDateTime now = VietnamTimeUtils.now();
        LocalDateTime lastUpdate = lastHelmetUpdateTime.get(macAddress);
        
        if (lastUpdate == null || lastUpdate.plusSeconds(HELMET_UPDATE_INTERVAL_SECONDS).isBefore(now)) {
            lastHelmetUpdateTime.put(macAddress, now);
            return true;
        }
        
        return false;
    }

    // ==================== TAG POSITION TRACKING ====================
    
    /**
     * ✅ Kiểm tra xem có nên save tag position không (mỗi 30s)
     * GIẢM: ~2 queries/message → queries mỗi 30s
     */
    public boolean shouldSaveTagPosition(String macAddress) {
        LocalDateTime now = VietnamTimeUtils.now();
        LocalDateTime lastSave = lastTagPositionSaveTime.get(macAddress);
        
        if (lastSave == null || lastSave.plusSeconds(TAG_POSITION_SAVE_INTERVAL_SECONDS).isBefore(now)) {
            lastTagPositionSaveTime.put(macAddress, now);
            return true;
        }
        
        return false;
    }

    // ==================== DANGER ALERT DEBOUNCE ====================
    
    /**
     * ✅ Kiểm tra xem có nên gửi danger alert không (debounce 60s)
     * GIẢM: Gửi mỗi message → Gửi mỗi 60s
     */
    public boolean shouldSendDangerAlert(String macAddress) {
        LocalDateTime now = VietnamTimeUtils.now();
        LocalDateTime lastAlert = lastDangerAlertTime.get(macAddress);
        
        if (lastAlert == null || lastAlert.plusSeconds(DANGER_ALERT_DEBOUNCE_SECONDS).isBefore(now)) {
            lastDangerAlertTime.put(macAddress, now);
            return true;
        }
        
        return false;
    }
    
    // ==================== MAC ADDRESS NOISE FILTER ====================
    
    /**
     * 🔧 Tăng counter và kiểm tra xem MAC đã đủ điều kiện để thêm helmet chưa
     * Lọc nhiễu: Chỉ thêm helmet mới khi nhận được >= 9 message từ MAC đó
     * 
     * @param macAddress MAC address của thiết bị
     * @return true nếu MAC đã xác nhận (>= 9 lần), false nếu chưa
     */
    public boolean incrementAndCheckMacConfirmation(String macAddress) {
        int count = macMessageCounter.merge(macAddress, 1, Integer::sum);
        
        if (count == 1) {
            log.info("📡 [MAC FILTER] New MAC detected: {} (count: 1/{})", macAddress, MAC_CONFIRMATION_THRESHOLD);
        } else if (count < MAC_CONFIRMATION_THRESHOLD) {
            log.debug("📡 [MAC FILTER] MAC {} count: {}/{}", macAddress, count, MAC_CONFIRMATION_THRESHOLD);
        } else if (count == MAC_CONFIRMATION_THRESHOLD) {
            log.info("✅ [MAC FILTER] MAC {} confirmed after {} messages - will create helmet", 
                     macAddress, MAC_CONFIRMATION_THRESHOLD);
        }
        
        return count >= MAC_CONFIRMATION_THRESHOLD;
    }
    
    /**
     * 🔍 Kiểm tra xem MAC đã được xác nhận chưa (không tăng counter)
     */
    public boolean isMacConfirmed(String macAddress) {
        Integer count = macMessageCounter.get(macAddress);
        return count != null && count >= MAC_CONFIRMATION_THRESHOLD;
    }
    
    /**
     * 📊 Lấy số lần nhận được message từ MAC
     */
    public int getMacMessageCount(String macAddress) {
        return macMessageCounter.getOrDefault(macAddress, 0);
    }
    
    /**
     * 🔄 Reset counter cho MAC (khi cần test lại)
     */
    public void resetMacCounter(String macAddress) {
        macMessageCounter.remove(macAddress);
        log.info("🔄 [MAC FILTER] Reset counter for MAC: {}", macAddress);
    }
    
    /**
     * 🧹 Cleanup old entries mỗi 10 phút
     */
    @Scheduled(fixedRate = 600000) // 10 phút
    public void cleanupOldEntries() {
        LocalDateTime threshold = VietnamTimeUtils.now().minusMinutes(30);
        
        // Cleanup helmet update tracking
        lastHelmetUpdateTime.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
        
        // Cleanup tag position tracking
        lastTagPositionSaveTime.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
        
        // Cleanup danger alert tracking
        lastDangerAlertTime.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
        
        // Cleanup MAC counters chưa xác nhận (giữ lại những MAC đã đạt threshold)
        macMessageCounter.entrySet().removeIf(entry -> entry.getValue() < MAC_CONFIRMATION_THRESHOLD);
        
        log.debug("🧹 Cleaned up old cache entries");
    }
    
    /**
     * 📊 Lấy thống kê cache
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
            "employeeCacheSize", employeeByMacCache.size(),
            "messengerUsersCacheSize", messengerUsersCache != null ? messengerUsersCache.size() : 0,
            "helmetUpdateTrackingSize", lastHelmetUpdateTime.size(),
            "tagPositionTrackingSize", lastTagPositionSaveTime.size(),
            "dangerAlertTrackingSize", lastDangerAlertTime.size(),
            "macCounterSize", macMessageCounter.size(),
            "confirmedMacCount", macMessageCounter.values().stream().filter(c -> c >= MAC_CONFIRMATION_THRESHOLD).count()
        );
    }
    
    /**
     * 📊 Lấy chi tiết MAC counters đang chờ xác nhận
     */
    public Map<String, Integer> getPendingMacCounters() {
        Map<String, Integer> pending = new ConcurrentHashMap<>();
        macMessageCounter.forEach((mac, count) -> {
            if (count < MAC_CONFIRMATION_THRESHOLD) {
                pending.put(mac, count);
            }
        });
        return pending;
    }
}

