package com.hatrustsoft.bfe_foraiot.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;

import com.hatrustsoft.bfe_foraiot.model.Alert;
import com.hatrustsoft.bfe_foraiot.model.AlertSeverity;
import com.hatrustsoft.bfe_foraiot.model.AlertStatus;
import com.hatrustsoft.bfe_foraiot.model.AlertType;
import com.hatrustsoft.bfe_foraiot.model.Helmet;
import com.hatrustsoft.bfe_foraiot.model.HelmetStatus;
import com.hatrustsoft.bfe_foraiot.model.Worker;
import com.hatrustsoft.bfe_foraiot.repository.AlertRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetDataRepository;
import com.hatrustsoft.bfe_foraiot.repository.HelmetRepository;
import com.hatrustsoft.bfe_foraiot.repository.WorkerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// @Component // DISABLED: Tắt để tránh vượt quá 3600 queries/giờ của JawsDB
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final WorkerRepository workerRepository;
    private final HelmetRepository helmetRepository;
    private final AlertRepository alertRepository;
    private final HelmetDataRepository helmetDataRepository;

    @Override
    public void run(String... args) {
        log.info("Initializing sample data...");

        // Check if data already exists
        if (workerRepository.count() > 0) {
            log.info("Data already exists. Skipping initialization.");
            return;
        }

        // Create Workers
        Worker worker1 = createWorker("Nguyễn Văn An", "NV001", "Kỹ thuật viên", "Phòng Kỹ Thuật", "0901234567");
        Worker worker2 = createWorker("Trần Thị Bình", "NV002", "Giám sát", "Phòng Quản Lý", "0902345678");
        Worker worker3 = createWorker("Lê Văn Cường", "NV003", "Công nhân", "Phòng Sản Xuất", "0903456789");
        Worker worker4 = createWorker("Phạm Thị Dung", "NV004", "Kỹ sư", "Phòng R&D", "0904567890");
        Worker worker5 = createWorker("Hoàng Văn Em", "NV005", "Thợ máy", "Phòng Bảo Trì", "0905678901");

        workerRepository.save(worker1);
        workerRepository.save(worker2);
        workerRepository.save(worker3);
        workerRepository.save(worker4);
        workerRepository.save(worker5);

        // Create Helmets with strategic positions
        // Tâm vòng tròn: 10.7626, 106.6601
        // Bán kính: 200m
        
        // Helmet 1: Trong vòng an toàn (50m từ tâm) - Màu xanh
        Helmet helmet1 = createHelmet(1, worker1, HelmetStatus.ACTIVE, 85, 10.762800, 106.660300);
        
        // Helmet 2: Gần biên, 85% bán kính (170m từ tâm) - Màu cam
        Helmet helmet2 = createHelmet(2, worker2, HelmetStatus.ACTIVE, 45, 10.764130, 106.660100);
        
        // Helmet 3: Ngoài vòng, 120% bán kính (240m từ tâm) - Màu đỏ
        Helmet helmet3 = createHelmet(3, worker3, HelmetStatus.ACTIVE, 92, 10.764760, 106.660100);
        
        // Helmet 4: Trong vòng an toàn (100m từ tâm) - Màu xanh (nhưng INACTIVE nên màu xám)
        Helmet helmet4 = createHelmet(4, worker4, HelmetStatus.INACTIVE, 15, 10.763500, 106.660100);
        
        // Helmet 5: Gần biên, 90% bán kính (180m từ tâm) - Màu cam
        Helmet helmet5 = createHelmet(5, worker5, HelmetStatus.ACTIVE, 78, 10.760980, 106.660100);

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

        // Create Helmet Data (COMMENTED OUT - using new schema from MQTT)
        /*
        Random random = new Random();
        for (Helmet helmet : helmetRepository.findAll()) {
            for (int i = 0; i < 5; i++) {
                HelmetData data = new HelmetData();
                data.setHelmet(helmet);
                data.setTimestamp(LocalDateTime.now().minusHours(i));
                data.setEventType(EventType.NORMAL);
                data.setGpsLat(helmet.getLastLat() + (random.nextDouble() - 0.5) * 0.001);
                data.setGpsLon(helmet.getLastLon() + (random.nextDouble() - 0.5) * 0.001);
                data.setBatteryLevel(helmet.getBatteryLevel() - i * 2);
                data.setUwbDistance(random.nextFloat() * 10);
                data.setRssi(-50 - random.nextInt(30));
                data.setGatewayId("GW-" + random.nextInt(5));
                helmetDataRepository.save(data);
            }
        }
        */

        log.info("Sample data initialized successfully!");
        log.info("Created {} workers, {} helmets, {} alerts", 
            workerRepository.count(), helmetRepository.count(), alertRepository.count());
    }

    private Worker createWorker(String fullName, String employeeId, String position, String department, String phone) {
        Worker worker = new Worker();
        worker.setFullName(fullName);
        worker.setEmployeeId(employeeId);
        worker.setPosition(position);
        worker.setDepartment(department);
        worker.setPhoneNumber(phone);
        worker.setStatus(Worker.WorkerStatus.ACTIVE);
        worker.setHiredDate(LocalDateTime.now().minusYears(1));
        worker.setCreatedAt(LocalDateTime.now());
        return worker;
    }

    private Helmet createHelmet(int helmetId, Worker worker, HelmetStatus status, int battery, double lat, double lon) {
        Helmet helmet = new Helmet();
        helmet.setHelmetId(helmetId);
        helmet.setWorker(worker);
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
