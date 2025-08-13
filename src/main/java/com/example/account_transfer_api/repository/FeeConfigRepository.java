package com.example.account_transfer_api.repository;

import com.example.account_transfer_api.entity.FeeConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeConfigRepository extends JpaRepository<FeeConfig, Long> {
}
