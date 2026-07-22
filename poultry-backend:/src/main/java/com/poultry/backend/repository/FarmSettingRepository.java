package com.poultry.backend.repository;

import com.poultry.backend.entity.FarmSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FarmSettingRepository extends JpaRepository<FarmSetting, String> {
}
