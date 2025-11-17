package com.hatrustsoft.bfe_foraiot.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hatrustsoft.bfe_foraiot.entity.Anchor;
import com.hatrustsoft.bfe_foraiot.service.AnchorService;

@RestController
@RequestMapping("/api/anchors")
@CrossOrigin(origins = "*")
public class AnchorController {
    
    @Autowired
    private AnchorService anchorService;
    
    @GetMapping
    public ResponseEntity<List<Anchor>> getAllAnchors() {
        List<Anchor> anchors = anchorService.getAllAnchors();
        return ResponseEntity.ok(anchors);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Anchor> getAnchorById(@PathVariable Long id) {
        return anchorService.getAnchorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/by-anchor-id/{anchorId}")
    public ResponseEntity<Anchor> getAnchorByAnchorId(@PathVariable String anchorId) {
        return anchorService.getAnchorByAnchorId(anchorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Anchor> createAnchor(@RequestBody Anchor anchor) {
        try {
            Anchor createdAnchor = anchorService.createAnchor(anchor);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAnchor);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Anchor> updateAnchor(@PathVariable Long id, @RequestBody Anchor anchorDetails) {
        try {
            Anchor updatedAnchor = anchorService.updateAnchor(id, anchorDetails);
            return ResponseEntity.ok(updatedAnchor);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnchor(@PathVariable Long id) {
        try {
            anchorService.deleteAnchor(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Anchor>> getAnchorsByStatus(@PathVariable String status) {
        List<Anchor> anchors = anchorService.getAnchorsByStatus(status);
        return ResponseEntity.ok(anchors);
    }
}
