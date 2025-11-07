package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.dto.address.AddressDTO;
import com.aptech.aptechMall.dto.address.CreateAddressRequest;
import com.aptech.aptechMall.dto.address.UpdateAddressRequest;
import com.aptech.aptechMall.security.AuthenticationUtil;
import com.aptech.aptechMall.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for user address management
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;

    /**
     * Get all addresses for current user
     * @return List of addresses
     */
    @GetMapping
    public ResponseEntity<List<AddressDTO>> getAllAddresses() {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("GET /api/addresses - User: {}", userId);

        List<AddressDTO> addresses = addressService.getAllAddresses(userId);
        return ResponseEntity.ok(addresses);
    }

    /**
     * Get address by ID
     * @param id Address ID
     * @return Address DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<AddressDTO> getAddressById(@PathVariable Long id) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("GET /api/addresses/{} - User: {}", id, userId);

        AddressDTO address = addressService.getAddressById(userId, id);
        return ResponseEntity.ok(address);
    }

    /**
     * Get default address for current user
     * @return Default address DTO or 404 if not found
     */
    @GetMapping("/default")
    public ResponseEntity<AddressDTO> getDefaultAddress() {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("GET /api/addresses/default - User: {}", userId);

        AddressDTO address = addressService.getDefaultAddress(userId);
        if (address == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(address);
    }

    /**
     * Create new address
     * @param request Create address request
     * @return Created address DTO
     */
    @PostMapping
    public ResponseEntity<AddressDTO> createAddress(@Valid @RequestBody CreateAddressRequest request) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("POST /api/addresses - User: {}", userId);

        AddressDTO address = addressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(address);
    }

    /**
     * Update address
     * @param id Address ID
     * @param request Update address request
     * @return Updated address DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<AddressDTO> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAddressRequest request) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("PUT /api/addresses/{} - User: {}", id, userId);

        AddressDTO address = addressService.updateAddress(userId, id, request);
        return ResponseEntity.ok(address);
    }

    /**
     * Delete address
     * @param id Address ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("DELETE /api/addresses/{} - User: {}", id, userId);

        addressService.deleteAddress(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Set address as default
     * @param id Address ID
     * @return Updated address DTO
     */
    @PutMapping("/{id}/default")
    public ResponseEntity<AddressDTO> setDefaultAddress(@PathVariable Long id) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        log.info("PUT /api/addresses/{}/default - User: {}", id, userId);

        AddressDTO address = addressService.setDefaultAddress(userId, id);
        return ResponseEntity.ok(address);
    }
}
