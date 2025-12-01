package com.hatrustsoft.bfe_foraiot.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.hatrustsoft.bfe_foraiot.dto.MessengerMessageDTO;
import com.hatrustsoft.bfe_foraiot.entity.MessengerUser;
import com.hatrustsoft.bfe_foraiot.repository.MessengerUserRepository;
import com.hatrustsoft.bfe_foraiot.util.VietnamTimeUtils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class MessengerService {

    @Value("${facebook.messenger.page-access-token}")
    private String pageAccessToken;

    @Value("${facebook.messenger.api-url}")
    private String apiUrl;

    private final MessengerUserRepository messengerUserRepository;
    private final WebClient webClient;
    
    // ⭐ CACHE subscribed users để giảm DB queries (refresh mỗi 5 phút)
    private List<MessengerUser> cachedSubscribedUsers = new ArrayList<>();
    private LocalDateTime lastCacheRefresh = null;
    private static final long CACHE_TTL_MINUTES = 5;

    public MessengerService(MessengerUserRepository messengerUserRepository, WebClient.Builder webClientBuilder) {
        this.messengerUserRepository = messengerUserRepository;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Gửi tin nhắn text đơn giản
     */
    public void sendTextMessage(String recipientId, String messageText) {
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
     * Gửi tin nhắn nguy hiểm với Quick Replies
     */
    public void sendDangerAlert(String recipientId, String employeeName, String alertType, String location) {
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

        MessengerMessageDTO.QuickReply[] quickReplies = {
                MessengerMessageDTO.QuickReply.builder()
                        .contentType("text")
                        .title("✅ Đã xử lý")
                        .payload("ALERT_HANDLED")
                        .build(),
                MessengerMessageDTO.QuickReply.builder()
                        .contentType("text")
                        .title("📞 Gọi khẩn cấp")
                        .payload("CALL_EMERGENCY")
                        .build(),
                MessengerMessageDTO.QuickReply.builder()
                        .contentType("text")
                        .title("📍 Xem vị trí")
                        .payload("VIEW_LOCATION")
                        .build()
        };

        MessengerMessageDTO message = MessengerMessageDTO.builder()
                .recipient(MessengerMessageDTO.Recipient.builder()
                        .id(recipientId)
                        .build())
                .message(MessengerMessageDTO.Message.builder()
                        .text(alertMessage)
                        .quickReplies(quickReplies)
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
}

