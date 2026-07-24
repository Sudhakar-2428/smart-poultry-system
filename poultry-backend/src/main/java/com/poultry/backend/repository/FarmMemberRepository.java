package com.poultry.backend.repository;

import com.poultry.backend.entity.FarmMember;
import com.poultry.backend.entity.FarmRole;
import com.poultry.backend.entity.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmMemberRepository extends JpaRepository<FarmMember, Long> {
    List<FarmMember> findByUserId(Long userId);
    List<FarmMember> findByFarmId(Long farmId);
    Optional<FarmMember> findByFarmIdAndUserId(Long farmId, Long userId);
    List<FarmMember> findByFarmIdAndRole(Long farmId, FarmRole role);
    List<FarmMember> findByFarmIdAndStatus(Long farmId, MembershipStatus status);
    boolean existsByFarmIdAndUserId(Long farmId, Long userId);
}
