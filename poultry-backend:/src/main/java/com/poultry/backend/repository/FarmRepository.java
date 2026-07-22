package com.poultry.backend.repository;

import com.poultry.backend.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FarmRepository extends JpaRepository<Farm, Long> {
    Optional<Farm> findByFarmUniqueId(String farmUniqueId);
    Optional<Farm> findByJoinCode(String joinCode);
    boolean existsByFarmUniqueId(String farmUniqueId);
    boolean existsByJoinCode(String joinCode);
}
