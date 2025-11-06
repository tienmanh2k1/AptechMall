package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.dto.systemfee.SystemFeeConfigRequest;
import com.aptech.aptechMall.dto.systemfee.SystemFeeConfigResponse;
import com.aptech.aptechMall.service.admin.SystemFeeConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing system fee configurations
 * Only accessible by admin users
 */
@RestController
@RequestMapping("/api/admin/fee-config")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@PreAuthorize("hasRole('ADMIN')")
public class SystemFeeConfigController {

    private final SystemFeeConfigService service;

    /**
     * Get all fee configurations
     * @return List of all fee configs
     */
    @GetMapping
    public ResponseEntity<List<SystemFeeConfigResponse>> getAllConfigs() {
        List<SystemFeeConfigResponse> configs = service.getAllConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * Get active fee configuration
     * @return Active fee config
     */
    @GetMapping("/active")
    public ResponseEntity<SystemFeeConfigResponse> getActiveConfig() {
        return service.getActiveConfig()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get fee configuration by ID
     * @param id Config ID
     * @return Fee config
     */
    @GetMapping("/{id}")
    public ResponseEntity<SystemFeeConfigResponse> getConfigById(@PathVariable Long id) {
        return service.getConfigById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new fee configuration
     * @param request Fee config data
     * @return Created config
     */
    @PostMapping
    public ResponseEntity<SystemFeeConfigResponse> createConfig(@Valid @RequestBody SystemFeeConfigRequest request) {
        SystemFeeConfigResponse created = service.createConfig(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update existing fee configuration
     * @param id Config ID
     * @param request Updated data
     * @return Updated config
     */
    @PutMapping("/{id}")
    public ResponseEntity<SystemFeeConfigResponse> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody SystemFeeConfigRequest request) {
        try {
            SystemFeeConfigResponse updated = service.updateConfig(id, request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Activate a fee configuration
     * @param id Config ID to activate
     * @return Activated config
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<SystemFeeConfigResponse> activateConfig(@PathVariable Long id) {
        try {
            SystemFeeConfigResponse activated = service.activateConfig(id);
            return ResponseEntity.ok(activated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete fee configuration
     * @param id Config ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        try {
            service.deleteConfig(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
