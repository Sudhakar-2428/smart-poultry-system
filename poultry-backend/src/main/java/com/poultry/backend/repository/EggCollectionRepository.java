package com.poultry.backend.repository;

import com.poultry.backend.entity.EggCollection;
import com.poultry.backend.entity.EggCollectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EggCollectionRepository extends JpaRepository<EggCollection, Long>, JpaSpecificationExecutor<EggCollection> {
    Optional<EggCollection> findByFemaleChickenIdAndStatus(Long henId, EggCollectionStatus status);
    List<EggCollection> findByStatus(EggCollectionStatus status);
    Page<EggCollection> findByStatus(EggCollectionStatus status, Pageable pageable);
    boolean existsByFemaleChickenIdAndStatus(Long henId, EggCollectionStatus status);
}
