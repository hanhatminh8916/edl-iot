package com.hatrustsoft.bfe_foraiot.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hatrustsoft.bfe_foraiot.entity.Anchor;
import com.hatrustsoft.bfe_foraiot.publisher.AnchorPublisher;
import com.hatrustsoft.bfe_foraiot.repository.AnchorRepository;

@Service
public class AnchorService {
    
    @Autowired
    private AnchorRepository anchorRepository;
    
    @Autowired
    private AnchorPublisher anchorPublisher;
    
    public List<Anchor> getAllAnchors() {
        return anchorRepository.findAll();
    }
    
    public Optional<Anchor> getAnchorById(Long id) {
        return anchorRepository.findById(id);
    }
    
    public Optional<Anchor> getAnchorByAnchorId(String anchorId) {
        return anchorRepository.findByAnchorId(anchorId);
    }
    
    @Transactional
    public Anchor createAnchor(Anchor anchor) {
        // Auto-generate anchorId if not provided
        if (anchor.getAnchorId() == null || anchor.getAnchorId().isEmpty()) {
            anchor.setAnchorId(generateNextAnchorId());
        }
        Anchor savedAnchor = anchorRepository.save(anchor);
        
        // ✅ Publish to WebSocket for real-time sync
        anchorPublisher.publishAnchorCreate(savedAnchor);
        
        return savedAnchor;
    }
    
    @Transactional
    public Anchor updateAnchor(Long id, Anchor anchorDetails) {
        Anchor anchor = anchorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Anchor not found with id: " + id));
        
        if (anchorDetails.getName() != null) {
            anchor.setName(anchorDetails.getName());
        }
        if (anchorDetails.getLatitude() != null) {
            anchor.setLatitude(anchorDetails.getLatitude());
        }
        if (anchorDetails.getLongitude() != null) {
            anchor.setLongitude(anchorDetails.getLongitude());
        }
        if (anchorDetails.getDescription() != null) {
            anchor.setDescription(anchorDetails.getDescription());
        }
        if (anchorDetails.getStatus() != null) {
            anchor.setStatus(anchorDetails.getStatus());
        }
        
        Anchor updatedAnchor = anchorRepository.save(anchor);
        
        // ✅ Publish to WebSocket for real-time sync
        anchorPublisher.publishAnchorUpdate(updatedAnchor);
        
        return updatedAnchor;
    }
    
    @Transactional
    public void deleteAnchor(Long id) {
        anchorRepository.deleteById(id);
        
        // ✅ Publish to WebSocket for real-time sync
        anchorPublisher.publishAnchorDelete(id);
    }
    
    public List<Anchor> getAnchorsByStatus(String status) {
        return anchorRepository.findByStatus(status);
    }
    
    private String generateNextAnchorId() {
        List<Anchor> allAnchors = anchorRepository.findAll();
        int maxNumber = 0;
        
        for (Anchor anchor : allAnchors) {
            String anchorId = anchor.getAnchorId();
            if (anchorId != null && anchorId.startsWith("A")) {
                try {
                    int number = Integer.parseInt(anchorId.substring(1));
                    maxNumber = Math.max(maxNumber, number);
                } catch (NumberFormatException e) {
                    // Skip invalid format
                }
            }
        }
        
        return "A" + (maxNumber + 1);
    }
}
