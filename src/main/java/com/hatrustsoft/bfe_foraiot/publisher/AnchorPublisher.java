package com.hatrustsoft.bfe_foraiot.publisher;

import com.hatrustsoft.bfe_foraiot.entity.Anchor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AnchorPublisher {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    public void publishAnchorCreate(Anchor anchor) {
        Map<String, Object> message = new HashMap<>();
        message.put("action", "CREATE");
        message.put("anchor", anchor);
        
        messagingTemplate.convertAndSend("/topic/anchor/update", message);
    }
    
    public void publishAnchorUpdate(Anchor anchor) {
        Map<String, Object> message = new HashMap<>();
        message.put("action", "UPDATE");
        message.put("anchor", anchor);
        
        messagingTemplate.convertAndSend("/topic/anchor/update", message);
    }
    
    public void publishAnchorDelete(Long anchorId) {
        Map<String, Object> message = new HashMap<>();
        message.put("action", "DELETE");
        message.put("anchorId", anchorId);
        
        messagingTemplate.convertAndSend("/topic/anchor/update", message);
    }
}
