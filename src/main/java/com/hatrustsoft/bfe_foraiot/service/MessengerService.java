package com.hatrustsoft.bfe_foraiot.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.hatrustsoft.bfe_foraiot.dto.MessengerMessageDTO;
import com.hatrustsoft.bfe_foraiot.entity.MessengerUser;
import com.hatrustsoft.bfe_foraiot.repository.MessengerUserRepository;
import com.hatrustsoft.bfe_foraiot.util.VietnamTimeUtils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class MessengerService {

    @Value("${facebook.messenger.page-access-token:DISABLED}")
    private String pageAccessToken;

    @Value("${facebook.messenger.api-url:https://graph.facebook.com/v21.0/me/messages}")
    private String apiUrl;
    
    private boolean messengerEnabled = false;

    private final MessengerUserRepository messengerUserRepository;
    private final WebClient webClient;
    
    // ⭐ CACHE subscribed users để giảm DB queries (refresh mỗi 5 phút)
    private List<MessengerUser> cachedSubscribedUsers = new ArrayList<>();
    private LocalDateTime lastCacheRefresh = null;
    private static final long CACHE_TTL_MINUTES = 5;
    
    // ⭐ Cache thông tin alert đang chờ xử lý cho mỗi user
    private final ConcurrentHashMap<String, AlertPendingInfo> pendingAlerts = new ConcurrentHashMap<>();
    
    // Inner class để lưu thông tin alert đang chờ
    public static class AlertPendingInfo {
        public String employeeName;
        public String alertType;
        public String location;
        public LocalDateTime timestamp;
        
        public AlertPendingInfo(String employeeName, String alertType, String location) {
            this.employeeName = employeeName;
            this.alertType = alertType;
            this.location = location;
            this.timestamp = VietnamTimeUtils.now();
        }
    }

    public MessengerService(MessengerUserRepository messengerUserRepository, WebClient.Builder webClientBuilder) {
        this.messengerUserRepository = messengerUserRepository;
        this.webClient = webClientBuilder.build();
    }
    
    @PostConstruct
    public void init() {
        if ("DISABLED".equals(pageAccessToken)) {
            log.warn("⚠️ MessengerService DISABLED: page-access-token not configured");
            log.warn("💡 Set FACEBOOK_MESSENGER_PAGE_ACCESS_TOKEN to enable Messenger notifications");
            messengerEnabled = false;
        } else {
            messengerEnabled = true;
            log.info("✅ MessengerService initialized with Page Access Token");
        }
    }

    /**
     * Gửi tin nhắn text đơn giản
     */
    public void sendTextMessage(String recipientId, String messageText) {
        if (!messengerEnabled) {
            log.debug("⏭️ Messenger disabled - skipping message");
            return;
        }
        MessengerMessageDTO message = MessengerMessageDTO.builder()
                .recipient(MessengerMessageDTO.Recipient.builder()
                        .id(recipientId)
                        .build())
                .message(MessengerMessageDTO.Message.builder()
                        .text(messageText)
                        .build())
                .messagingType("RESPONSE")
                .build();

        sendMessage(message);
    }

    /**
     * Gửi tin nhắn nguy hiểm với Button để mở Google Maps
     */
    public void sendDangerAlert(String recipientId, String employeeName, String alertType, String location) {
        if (!messengerEnabled) {
            log.debug("⏭️ Messenger disabled - skipping alert");
            return;
        }
        
        // Lưu thông tin alert để xử lý khi user click "Đã xử lý"
        pendingAlerts.put(recipientId, new AlertPendingInfo(employeeName, alertType, location));
        
        String alertMessage = String.format(
                "🚨 CẢNH BÁO NGUY HIỂM!\n\n" +
                        "Nhân viên: %s\n" +
                        "Loại cảnh báo: %s\n" +
                        "Vị trí: %s\n" +
                        "Thời gian: %s\n\n" +
                        "Vui lòng kiểm tra ngay!",
                employeeName,
                alertType,
                location,
                com.hatrustsoft.bfe_foraiot.util.VietnamTimeUtils.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        );

        // Tạo Google Maps URL từ location (format: "lat, lon")
        String googleMapsUrl = "https://www.google.com/maps?q=" + location.replace(" ", "");

        // Buttons cho phép mở link Google Maps
        MessengerMessageDTO.Button[] buttons = {
                MessengerMessageDTO.Button.builder()
                        .type("web_url")
                        .title("📍 Xem vị trí")
                        .url(googleMapsUrl)
                        .build(),
                MessengerMessageDTO.Button.builder()
                        .type("postback")
                        .title("✅ Đã xử lý")
                        .payload("ALERT_HANDLED")
                        .build()
        };

        // Tạo Button Template
        MessengerMessageDTO.Attachment attachment = MessengerMessageDTO.Attachment.builder()
                .type("template")
                .payload(MessengerMessageDTO.Payload.builder()
                        .templateType("button")
                        .text(alertMessage)
                        .buttons(buttons)
                        .build())
                .build();

        MessengerMessageDTO message = MessengerMessageDTO.builder()
                .recipient(MessengerMessageDTO.Recipient.builder()
                        .id(recipientId)
                        .build())
                .message(MessengerMessageDTO.Message.builder()
                        .attachment(attachment)
                        .build())
                .messagingType("UPDATE")
                .build();

        sendMessage(message);
    }

    /**
     * Gửi tin nhắn với buttons
     */
    public void sendButtonMessage(String recipientId, String text, String buttonTitle, String buttonUrl) {
        MessengerMessageDTO.Button[] buttons = {
                MessengerMessageDTO.Button.builder()
                        .type("web_url")
                        .title(buttonTitle)
                        .url(buttonUrl)
                        .build()
        };

        MessengerMessageDTO.Attachment attachment = MessengerMessageDTO.Attachment.builder()
                .type("template")
                .payload(MessengerMessageDTO.Payload.builder()
                        .templateType("button")
                        .text(text)
                        .buttons(buttons)
                        .build())
                .build();

        MessengerMessageDTO message = MessengerMessageDTO.builder()
                .recipient(MessengerMessageDTO.Recipient.builder()
                        .id(recipientId)
                        .build())
                .message(MessengerMessageDTO.Message.builder()
                        .attachment(attachment)
                        .build())
                .messagingType("RESPONSE")
                .build();

        sendMessage(message);
    }

    /**
     * Gửi cảnh báo tới tất cả người dùng đã đăng ký
     * ⭐ OPTIMIZED: Cache subscribed users để giảm DB queries
     */
    public void broadcastDangerAlert(String employeeName, String alertType, String location) {
        // Refresh cache nếu cần
        LocalDateTime now = VietnamTimeUtils.now();
        if (lastCacheRefresh == null || 
            java.time.Duration.between(lastCacheRefresh, now).toMinutes() >= CACHE_TTL_MINUTES) {
            cachedSubscribedUsers = messengerUserRepository.findBySubscribedTrue();
            lastCacheRefresh = now;
            log.debug("🔄 Refreshed subscribed users cache: {} users", cachedSubscribedUsers.size());
        }
        
        log.info("Broadcasting danger alert to {} subscribed users", cachedSubscribedUsers.size());
        
        cachedSubscribedUsers.forEach(user -> {
            try {
                sendDangerAlert(user.getPsid(), employeeName, alertType, location);
                log.info("Sent alert to user: {}", user.getPsid());
            } catch (Exception e) {
                log.error("Failed to send alert to user {}: {}", user.getPsid(), e.getMessage());
            }
        });
    }
    
    /**
     * Invalidate cache khi có user mới subscribe
     */
    public void invalidateCache() {
        lastCacheRefresh = null;
        cachedSubscribedUsers.clear();
    }

    /**
     * Gửi message request tới Facebook API
     */
    private void sendMessage(MessengerMessageDTO message) {
        try {
            String response = webClient.post()
                    .uri(apiUrl + "?access_token=" + pageAccessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(message)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
                        log.error("Error sending message. Status: {}", clientResponse.statusCode());
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Error body: {}", errorBody);
                                    return Mono.error(new RuntimeException("Failed to send message: " + errorBody));
                                });
                    })
                    .bodyToMono(String.class)
                    .block();

            log.info("Message sent successfully. Response: {}", response);
        } catch (Exception e) {
            log.error("Error sending message to Messenger API: {}", e.getMessage(), e);
        }
    }

    /**
     * Lưu hoặc cập nhật thông tin người dùng Messenger
     */
    public MessengerUser saveOrUpdateUser(String psid) {
        return messengerUserRepository.findByPsid(psid)
                .map(user -> {
                    user.setLastInteraction(com.hatrustsoft.bfe_foraiot.util.VietnamTimeUtils.now());
                    return messengerUserRepository.save(user);
                })
                .orElseGet(() -> {
                    MessengerUser newUser = new MessengerUser();
                    newUser.setPsid(psid);
                    newUser.setSubscribed(true);
                    return messengerUserRepository.save(newUser);
                });
    }

/**
     * Link Messenger user với Employee ID
     */
    public void linkUserToEmployee(String psid, String employeeId) {
        messengerUserRepository.findByPsid(psid).ifPresent(user -> {
            user.setEmployeeId(employeeId);
            messengerUserRepository.save(user);
            log.info("Linked Messenger user {} to employee {}", psid, employeeId);
        });
    }
    
    /**
     * Bắt đầu flow xác nhận xử lý alert
     * Gửi prompt yêu cầu nhập message
     */
    public void startHandleAlertFlow(String psid) {
        Optional<MessengerUser> userOpt = messengerUserRepository.findByPsid(psid);
        if (userOpt.isEmpty()) {
            log.warn("User not found for PSID: {}", psid);
            sendTextMessage(psid, "❌ Không tìm thấy thông tin người dùng.");
            return;
        }
        
        // Lấy thông tin alert đang chờ
        AlertPendingInfo alertInfo = pendingAlerts.get(psid);
        if (alertInfo == null) {
            sendTextMessage(psid, "✅ Cảm ơn bạn đã xác nhận xử lý cảnh báo!");
            return;
        }
        
        // Cập nhật trạng thái user đang chờ nhập message
        MessengerUser user = userOpt.get();
        user.setPendingState("AWAITING_HANDLE_MESSAGE");
        user.setPendingAlertType(alertInfo.alertType);
        user.setPendingEmployeeName(alertInfo.employeeName);
        messengerUserRepository.save(user);
        
        // Gửi prompt yêu cầu nhập message
        String promptMessage = String.format(
            "📝 XÁC NHẬN XỬ LÝ CẢNH BÁO\n\n" +
            "Nhân viên: %s\n" +
            "Loại cảnh báo: %s\n\n" +
            "Vui lòng nhập ghi chú về cách bạn đã xử lý tình huống này:\n" +
            "(Ví dụ: Đã kiểm tra, nhân viên ổn định)",
            alertInfo.employeeName,
            alertInfo.alertType
        );
        
        sendTextMessage(psid, promptMessage);
        
        // Xóa pending alert sau khi đã bắt đầu flow
        pendingAlerts.remove(psid);
    }
    
    /**
     * Xử lý message từ user đang trong trạng thái chờ
     * Trả về true nếu đã xử lý (user đang trong pending state)
     */
    public boolean handlePendingMessage(String psid, String message, WebSocketAlertCallback callback) {
        Optional<MessengerUser> userOpt = messengerUserRepository.findByPsid(psid);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        MessengerUser user = userOpt.get();
        String pendingState = user.getPendingState();
        
        if (pendingState == null) {
            return false; // Không có pending state, xử lý như message bình thường
        }
        
        if ("AWAITING_HANDLE_MESSAGE".equals(pendingState)) {
            // User đã nhập message xác nhận
            String alertType = user.getPendingAlertType();
            String employeeName = user.getPendingEmployeeName();
            
            // Clear pending state
            user.setPendingState(null);
            user.setPendingAlertId(null);
            user.setPendingAlertType(null);
            user.setPendingEmployeeName(null);
            messengerUserRepository.save(user);
            
            // Gọi callback để gửi lên dashboard
            if (callback != null) {
                callback.onAlertHandled(psid, user.getFirstName(), employeeName, alertType, message);
            }
            
            // Gửi xác nhận cho user
            sendTextMessage(psid, 
                "✅ Đã ghi nhận!\n\n" +
                "Ghi chú của bạn: \"" + message + "\"\n\n" +
                "Thông tin đã được gửi đến dashboard.");
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Interface callback để gửi thông báo lên dashboard
     */
    public interface WebSocketAlertCallback {
        void onAlertHandled(String handlerPsid, String handlerName, String employeeName, String alertType, String message);
    }
    
    /**
     * Lấy thông tin pending alert cho user
     */
    public AlertPendingInfo getPendingAlert(String psid) {
        return pendingAlerts.get(psid);
    }
}