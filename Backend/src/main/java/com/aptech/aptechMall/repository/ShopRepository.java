package com.aptech.aptechMall.repository;

import com.aptech.aptechMall.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Shop entity
 */
@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    List<Shop> findByIsActiveTrue();
    Optional<Shop> findByName(String name);
}

