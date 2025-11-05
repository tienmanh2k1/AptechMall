package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.dto.shop.ShopRequestDTO;
import com.aptech.aptechMall.dto.shop.ShopResponseDTO;
import com.aptech.aptechMall.service.admin.ShopManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/shops")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@PreAuthorize("hasRole('ADMIN')")
public class ShopController {

    private final ShopManagementService shopService;

    @GetMapping
    public ResponseEntity<List<ShopResponseDTO>> getAllShops() {
        List<ShopResponseDTO> shops = shopService.getAllShops();
        return ResponseEntity.ok(shops);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShopResponseDTO> getShopById(@PathVariable Long id) {
        return shopService.getShopById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ShopResponseDTO> createShop(@Valid @RequestBody ShopRequestDTO dto) {
        ShopResponseDTO created = shopService.createShop(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShopResponseDTO> updateShop(@PathVariable Long id, @Valid @RequestBody ShopRequestDTO dto) {
        try {
            ShopResponseDTO updated = shopService.updateShop(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShop(@PathVariable Long id) {
        try {
            shopService.deleteShop(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

