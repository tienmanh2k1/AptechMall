package com.aptech.aptechMall.service.admin;

import com.aptech.aptechMall.dto.shop.ShopRequestDTO;
import com.aptech.aptechMall.dto.shop.ShopResponseDTO;
import com.aptech.aptechMall.entity.Shop;
import com.aptech.aptechMall.repository.ShopRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopManagementService {

    private final ShopRepository shopRepository;

    private ShopResponseDTO toDTO(Shop shop) {
        return ShopResponseDTO.builder()
                .id(shop.getId())
                .name(shop.getName())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .shopUrl(shop.getShopUrl())
                .contactEmail(shop.getContactEmail())
                .contactPhone(shop.getContactPhone())
                .address(shop.getAddress())
                .isActive(shop.getIsActive())
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .build();
    }

    public List<ShopResponseDTO> getAllShops() {
        return shopRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<ShopResponseDTO> getShopById(Long id) {
        return shopRepository.findById(id)
                .map(this::toDTO);
    }

    @Transactional
    public ShopResponseDTO createShop(ShopRequestDTO dto) {
        Shop shop = new Shop();
        shop.setName(dto.getName());
        shop.setDescription(dto.getDescription());
        shop.setLogoUrl(dto.getLogoUrl());
        shop.setShopUrl(dto.getShopUrl());
        shop.setContactEmail(dto.getContactEmail());
        shop.setContactPhone(dto.getContactPhone());
        shop.setAddress(dto.getAddress());
        shop.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        Shop saved = shopRepository.save(shop);
        return toDTO(saved);
    }

    @Transactional
    public ShopResponseDTO updateShop(Long id, ShopRequestDTO dto) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop not found with id: " + id));

        if (dto.getName() != null) shop.setName(dto.getName());
        if (dto.getDescription() != null) shop.setDescription(dto.getDescription());
        if (dto.getLogoUrl() != null) shop.setLogoUrl(dto.getLogoUrl());
        if (dto.getShopUrl() != null) shop.setShopUrl(dto.getShopUrl());
        if (dto.getContactEmail() != null) shop.setContactEmail(dto.getContactEmail());
        if (dto.getContactPhone() != null) shop.setContactPhone(dto.getContactPhone());
        if (dto.getAddress() != null) shop.setAddress(dto.getAddress());
        if (dto.getIsActive() != null) shop.setIsActive(dto.getIsActive());

        Shop updated = shopRepository.save(shop);
        return toDTO(updated);
    }

    @Transactional
    public void deleteShop(Long id) {
        if (!shopRepository.existsById(id)) {
            throw new RuntimeException("Shop not found with id: " + id);
        }
        shopRepository.deleteById(id);
    }
}

