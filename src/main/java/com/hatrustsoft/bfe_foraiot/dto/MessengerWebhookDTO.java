package com.hatrustsoft.bfe_foraiot.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho Webhook Callback từ Facebook Messenger
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessengerWebhookDTO {
    
    private String object;
    private List<Entry> entry;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        private String id;
        private Long time;
        private List<Messaging> messaging;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Messaging {
        private Sender sender;
        private Recipient recipient;
        private Long timestamp;
        private Message message;
        private Postback postback;
        
        @JsonProperty("read")
        private Read readStatus;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sender {
        private String id; // PSID
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recipient {
        private String id;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String mid;
        private String text;
        
        @JsonProperty("quick_reply")
        private QuickReply quickReply;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickReply {
        private String payload;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Postback {
        private String title;
        private String payload;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Read {
        private Long watermark;
    }
}
