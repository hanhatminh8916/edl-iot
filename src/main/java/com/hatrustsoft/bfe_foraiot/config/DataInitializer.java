package com.hatrustsoft.bfe_foraiot.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;

import com.hatrustsoft.bfe_foraiot.entity.Employee;
import com.hatrustsoft.bfe_foraiot.model.Alert;
import com.hatrustsoft.bfe_foraiot.model.AlertSeverity;
import com.hatrustsoft.bfe_foraiot.model.AlertStatus;
import com.hatrustsoft.bfe_foraiot.model.AlertType;
import com.hatrustsoft.bfe_foraiot.model.Helmet;
import com.hatrustsoft.bfe_foraiot.model.HelmetStatus;
import com.hatrustsoft.bfe_foraiot.repository.AlertRepository;
import com.hatrustsoft.bfe_foraiot.repository.EmployeeRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetDataRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// @Component // DISABLED: Tắt để tránh vượt quá 3600 queries/giờ của JawsDB
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final HelmetRepository helmetRepository;
    private final AlertRepository alertRepository;
    private final HelmetDataRepository helmetDataRepository;

    @Override
    public void run(String... args) {
        log.info("Initializing sample data...");

        // Check if data already exists
        if (employeeRepository.count() > 0) {
            log.info("Data already exists. Skipping initialization.");
            return;
        }

        // Create Employees
        Employee emp1 = createEmployee("Nguyễn Văn An", "REV01", "Kỹ thuật viên", "Phòng Kỹ Thuật", "0901234567");
        Employee emp2 = createEmployee("Trần Thị Bình", "REV02", "Giám sát", "Phòng Quản Lý", "0902345678");
        Employee emp3 = createEmployee("Lê Văn Cường", "REV03", "Công nhân", "Phòng Sản Xuất", "0903456789");
        Employee emp4 = createEmployee("Phạm Thị Dung", "REV04", "Kỹ sư", "Phòng R&D", "0904567890");
        Employee emp5 = createEmployee("Hoàng Văn Em", "REV05", "Thợ máy", "Phòng Bảo Trì", "0905678901");

        employeeRepository.save(emp1);
        employeeRepository.save(emp2);
        employeeRepository.save(emp3);
        employeeRepository.save(emp4);
        employeeRepository.save(emp5);

        // Create Helmets with strategic positions
        // Tâm vòng tròn: 10.7626, 106.6601
        // Bán kính: 200m
        
        // Helmet 1: Trong vòng an toàn (50m từ tâm) - Màu xanh
        Helmet helmet1 = createHelmet(1, emp1, HelmetStatus.ACTIVE, 85, 10.762800, 106.660300);
        
        // Helmet 2: Gần biên, 85% bán kính (170m từ tâm) - Màu cam
        Helmet helmet2 = createHelmet(2, emp2, HelmetStatus.ACTIVE, 45, 10.764130, 106.660100);
        
        // Helmet 3: Ngoài vòng, 120% bán kính (240m từ tâm) - Màu đỏ
        Helmet helmet3 = createHelmet(3, emp3, HelmetStatus.ACTIVE, 92, 10.764760, 106.660100);
        
        // Helmet 4: Trong vòng an toàn (100m từ tâm) - Màu xanh (nhưng INACTIVE nên màu xám)
        Helmet helmet4 = createHelmet(4, emp4, HelmetStatus.INACTIVE, 15, 10.763500, 106.660100);
        
        // Helmet 5: Gần biên, 90% bán kính (180m từ tâm) - Màu cam
        Helmet helmet5 = createHelmet(5, emp5, HelmetStatus.ACTIVE, 78, 10.760980, 106.660100);

        helmetRepository.save(helmet1);
        helmetRepository.save(helmet2);
        helmetRepository.save(helmet3);
        helmetRepository.save(helmet4);
        helmetRepository.save(helmet5);

        // Create Alerts
        createAlert(helmet2, AlertType.FALL, AlertSeverity.CRITICAL, 
            "Phát hiện té ngã tại vị trí (10.762800, 106.660300)", 10.762800, 106.660300);
        createAlert(helmet1, AlertType.OUT_OF_ZONE, AlertSeverity.WARNING,
            "Công nhân ra khỏi khu vực an toàn", 10.762622, 106.660172);
        createAlert(helmet4, AlertType.LOW_BATTERY, AlertSeverity.INFO,
            "Mức pin thấp - cần sạc ngay", 10.762400, 106.660050);

        log.info("Sample data initialized successfully!");
        log.info("Created {} employees, {} helmets, {} alerts", 
            employeeRepository.count(), helmetRepository.count(), alertRepository.count());
    }

    private Employee createEmployee(String name, String employeeId, String position, String department, String phone) {
        Employee emp = new Employee();
        emp.setName(name);
        emp.setEmployeeId(employeeId);
        emp.setPosition(position);
        emp.setDepartment(department);
        emp.setPhoneNumber(phone);
        emp.setStatus("ACTIVE");
        return emp;
    }

    private Helmet createHelmet(int helmetId, Employee employee, HelmetStatus status, int battery, double lat, double lon) {
        Helmet helmet = new Helmet();
        helmet.setHelmetId(helmetId);
        helmet.setEmployee(employee);
        helmet.setStatus(status);
        helmet.setBatteryLevel(battery);
        helmet.setLastLat(lat);
        helmet.setLastLon(lon);
        helmet.setLastSeen(LocalDateTime.now());
        helmet.setMacAddress("AA:BB:CC:DD:EE:" + String.format("%02d", helmetId));
        helmet.setCreatedAt(LocalDateTime.now());
        return helmet;
    }

    private void createAlert(Helmet helmet, AlertType type, AlertSeverity severity, String message, double lat, double lon) {
        Alert alert = new Alert();
        alert.setHelmet(helmet);
        alert.setAlertType(type);
        alert.setSeverity(severity);
        alert.setMessage(message);
        alert.setGpsLat(lat);
        alert.setGpsLon(lon);
        alert.setTriggeredAt(LocalDateTime.now().minusMinutes(30));
        alert.setStatus(AlertStatus.PENDING);
        alertRepository.save(alert);
    }
}
