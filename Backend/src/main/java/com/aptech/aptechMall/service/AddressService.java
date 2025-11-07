package com.aptech.aptechMall.service;

import com.aptech.aptechMall.dto.address.AddressDTO;
import com.aptech.aptechMall.dto.address.CreateAddressRequest;
import com.aptech.aptechMall.dto.address.UpdateAddressRequest;
import com.aptech.aptechMall.model.jpa.User;
import com.aptech.aptechMall.model.jpa.UserAddresses;
import com.aptech.aptechMall.repository.UserAddressesRepository;
import com.aptech.aptechMall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user addresses
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final UserAddressesRepository addressRepository;
    private final UserRepository userRepository;

    /**
     * Get all addresses for a user
     * @param userId User ID
     * @return List of addresses
     */
    public List<AddressDTO> getAllAddresses(Long userId) {
        log.info("Getting all addresses for user: {}", userId);
        List<UserAddresses> addresses = addressRepository
                .findByUserUserIdOrderByIsDefaultDescCreatedAtDesc(userId);

        return addresses.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get address by ID
     * @param userId User ID
     * @param addressId Address ID
     * @return Address DTO
     */
    public AddressDTO getAddressById(Long userId, Long addressId) {
        log.info("Getting address {} for user: {}", addressId, userId);
        UserAddresses address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        // Verify address belongs to user
        if (!address.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập địa chỉ này");
        }

        return convertToDTO(address);
    }

    /**
     * Create new address
     * @param userId User ID
     * @param request Create request
     * @return Created address DTO
     */
    @Transactional
    public AddressDTO createAddress(Long userId, CreateAddressRequest request) {
        log.info("Creating new address for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // If this is the first address or set as default, unset all other defaults
        if (request.isDefault()) {
            addressRepository.unsetAllDefaultForUser(userId);
        }

        // If this is the first address, make it default
        long addressCount = addressRepository.countByUserUserId(userId);
        boolean shouldBeDefault = addressCount == 0 || request.isDefault();

        UserAddresses address = UserAddresses.builder()
                .user(user)
                .receiverName(request.getReceiverName())
                .phone(request.getPhone())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .addressDetail(request.getAddressDetail())
                .addressType(request.getAddressType())
                .isDefault(shouldBeDefault)
                .build();

        UserAddresses savedAddress = addressRepository.save(address);
        log.info("Created address with ID: {}", savedAddress.getId());

        return convertToDTO(savedAddress);
    }

    /**
     * Update address
     * @param userId User ID
     * @param addressId Address ID
     * @param request Update request
     * @return Updated address DTO
     */
    @Transactional
    public AddressDTO updateAddress(Long userId, Long addressId, UpdateAddressRequest request) {
        log.info("Updating address {} for user: {}", addressId, userId);

        UserAddresses address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        // Verify address belongs to user
        if (!address.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền cập nhật địa chỉ này");
        }

        // Update fields if provided
        if (request.getReceiverName() != null) {
            address.setReceiverName(request.getReceiverName());
        }
        if (request.getPhone() != null) {
            address.setPhone(request.getPhone());
        }
        if (request.getProvince() != null) {
            address.setProvince(request.getProvince());
        }
        if (request.getDistrict() != null) {
            address.setDistrict(request.getDistrict());
        }
        if (request.getWard() != null) {
            address.setWard(request.getWard());
        }
        if (request.getAddressDetail() != null) {
            address.setAddressDetail(request.getAddressDetail());
        }
        if (request.getAddressType() != null) {
            address.setAddressType(request.getAddressType());
        }

        UserAddresses updatedAddress = addressRepository.save(address);
        log.info("Updated address: {}", addressId);

        return convertToDTO(updatedAddress);
    }

    /**
     * Delete address
     * @param userId User ID
     * @param addressId Address ID
     */
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        log.info("Deleting address {} for user: {}", addressId, userId);

        UserAddresses address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        // Verify address belongs to user
        if (!address.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa địa chỉ này");
        }

        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);
        log.info("Deleted address: {}", addressId);

        // If deleted address was default, set another address as default
        if (wasDefault) {
            List<UserAddresses> remainingAddresses = addressRepository
                    .findByUserUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            if (!remainingAddresses.isEmpty()) {
                UserAddresses newDefault = remainingAddresses.get(0);
                newDefault.setAsDefault();
                addressRepository.save(newDefault);
                log.info("Set new default address: {}", newDefault.getId());
            }
        }
    }

    /**
     * Set address as default
     * @param userId User ID
     * @param addressId Address ID
     * @return Updated address DTO
     */
    @Transactional
    public AddressDTO setDefaultAddress(Long userId, Long addressId) {
        log.info("Setting address {} as default for user: {}", addressId, userId);

        UserAddresses address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        // Verify address belongs to user
        if (!address.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền cập nhật địa chỉ này");
        }

        // Unset all other defaults
        addressRepository.unsetAllDefaultForUser(userId);

        // Set this address as default
        address.setAsDefault();
        UserAddresses updatedAddress = addressRepository.save(address);
        log.info("Set address {} as default", addressId);

        return convertToDTO(updatedAddress);
    }

    /**
     * Get default address for user
     * @param userId User ID
     * @return Default address DTO or null
     */
    public AddressDTO getDefaultAddress(Long userId) {
        log.info("Getting default address for user: {}", userId);
        return addressRepository.findByUserUserIdAndIsDefaultTrue(userId)
                .map(this::convertToDTO)
                .orElse(null);
    }

    /**
     * Convert entity to DTO
     * @param address Address entity
     * @return Address DTO
     */
    private AddressDTO convertToDTO(UserAddresses address) {
        return AddressDTO.builder()
                .id(address.getId())
                .receiverName(address.getReceiverName())
                .phone(address.getPhone())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .addressDetail(address.getAddressDetail())
                .addressType(address.getAddressType())
                .isDefault(address.isDefault())
                .fullAddress(address.getFullAddress())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}
