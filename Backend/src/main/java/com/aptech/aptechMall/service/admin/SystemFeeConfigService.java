package com.aptech.aptechMall.service.admin;

import com.aptech.aptechMall.dto.systemfee.SystemFeeConfigRequest;
import com.aptech.aptechMall.dto.systemfee.SystemFeeConfigResponse;
import com.aptech.aptechMall.entity.SystemFeeConfig;
import com.aptech.aptechMall.repository.SystemFeeConfigRepository;
import com.aptech.aptechMall.security.AuthenticationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing system fee configurations
 * Only admins can modify fee settings
 */
@Service
@RequiredArgsConstructor
public class SystemFeeConfigService {

    private final SystemFeeConfigRepository repository;

    /**
     * Get all fee configurations
     * @return List of all fee configs
     */
    public List<SystemFeeConfigResponse> getAllConfigs() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get active fee configuration
     * @return Active fee config or empty
     */
    public Optional<SystemFeeConfigResponse> getActiveConfig() {
        return repository.findByIsActiveTrue()
                .map(this::toResponse);
    }

    /**
     * Get fee configuration by ID
     * @param id Config ID
     * @return Fee config or empty
     */
    public Optional<SystemFeeConfigResponse> getConfigById(Long id) {
        return repository.findById(id)
                .map(this::toResponse);
    }

    /**
     * Create new fee configuration
     * Deactivates all other configs and activates this one
     * @param request Fee config data
     * @return Created config
     */
    @Transactional
    public SystemFeeConfigResponse createConfig(SystemFeeConfigRequest request) {
        // Get current admin user ID
        Long adminId = AuthenticationUtil.getCurrentUserId();

        // If this config should be active, deactivate all others
        if (request.getIsActive() != null && request.getIsActive()) {
            deactivateAllConfigs();
        }

        SystemFeeConfig config = SystemFeeConfig.builder()
                .serviceFeePercent(request.getServiceFeePercent())
                .domesticShippingRate(request.getDomesticShippingRate())
                .internationalShippingRate(request.getInternationalShippingRate())
                .vietnamDomesticShippingRate(request.getVietnamDomesticShippingRate())
                .depositPercent(request.getDepositPercent())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .updatedBy(adminId)
                .build();

        SystemFeeConfig saved = repository.save(config);
        return toResponse(saved);
    }

    /**
     * Update existing fee configuration
     * @param id Config ID
     * @param request Updated data
     * @return Updated config
     */
    @Transactional
    public SystemFeeConfigResponse updateConfig(Long id, SystemFeeConfigRequest request) {
        SystemFeeConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fee config not found with id: " + id));

        // Get current admin user ID
        Long adminId = AuthenticationUtil.getCurrentUserId();

        // If setting this config to active, deactivate all others
        if (request.getIsActive() != null && request.getIsActive() && !config.isActive()) {
            deactivateAllConfigs();
        }

        config.setServiceFeePercent(request.getServiceFeePercent());
        config.setDomesticShippingRate(request.getDomesticShippingRate());
        config.setInternationalShippingRate(request.getInternationalShippingRate());
        config.setVietnamDomesticShippingRate(request.getVietnamDomesticShippingRate());
        config.setDepositPercent(request.getDepositPercent());
        if (request.getIsActive() != null) {
            config.setActive(request.getIsActive());
        }
        config.setUpdatedBy(adminId);

        SystemFeeConfig updated = repository.save(config);
        return toResponse(updated);
    }

    /**
     * Activate a fee configuration (and deactivate all others)
     * @param id Config ID to activate
     * @return Activated config
     */
    @Transactional
    public SystemFeeConfigResponse activateConfig(Long id) {
        SystemFeeConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fee config not found with id: " + id));

        // Deactivate all configs
        deactivateAllConfigs();

        // Activate this config
        config.setActive(true);
        Long adminId = AuthenticationUtil.getCurrentUserId();
        config.setUpdatedBy(adminId);

        SystemFeeConfig updated = repository.save(config);
        return toResponse(updated);
    }

    /**
     * Delete fee configuration
     * Cannot delete if it's the only active config
     * @param id Config ID
     */
    @Transactional
    public void deleteConfig(Long id) {
        SystemFeeConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fee config not found with id: " + id));

        // Prevent deletion of the only active config
        if (config.isActive() && repository.count() == 1) {
            throw new RuntimeException("Cannot delete the only active fee configuration");
        }

        repository.delete(config);
    }

    /**
     * Deactivate all fee configurations
     */
    private void deactivateAllConfigs() {
        List<SystemFeeConfig> allConfigs = repository.findAll();
        for (SystemFeeConfig config : allConfigs) {
            config.setActive(false);
        }
        repository.saveAll(allConfigs);
    }

    /**
     * Convert entity to response DTO
     */
    private SystemFeeConfigResponse toResponse(SystemFeeConfig entity) {
        return SystemFeeConfigResponse.builder()
                .id(entity.getId())
                .serviceFeePercent(entity.getServiceFeePercent())
                .domesticShippingRate(entity.getDomesticShippingRate())
                .internationalShippingRate(entity.getInternationalShippingRate())
                .vietnamDomesticShippingRate(entity.getVietnamDomesticShippingRate())
                .depositPercent(entity.getDepositPercent())
                .isActive(entity.isActive())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
