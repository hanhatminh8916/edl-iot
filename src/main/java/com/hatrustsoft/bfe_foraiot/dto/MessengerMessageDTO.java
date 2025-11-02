package com.hatrustsoft.bfe_foraiot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho Facebook Messenger Send API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessengerMessageDTO {
    
    private Recipient recipient;
    private Message message;
    
    @JsonProperty("messaging_type")
    @Builder.Default
    private String messagingType = "RESPONSE"; // RESPONSE, UPDATE, MESSAGE_TAG
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recipient {
        private String id; // PSID của người nhận
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String text;
        
        @JsonProperty("quick_replies")
        private QuickReply[] quickReplies;
        
        private Attachment attachment;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickReply {
        
        @JsonProperty("content_type")
        private String contentType; // text, location, user_phone_number
        
        private String title;
        private String payload;
        
        @JsonProperty("image_url")
        private String imageUrl;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {
        private String type; // template, image, audio, video, file
        private Payload payload;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        
        @JsonProperty("template_type")
        private String templateType; // button, generic, list
        
        private String text;
        private Button[] buttons;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Button {
        private String type; // web_url, postback
        private String title;
        private String url;
        private String payload;
    }
}
