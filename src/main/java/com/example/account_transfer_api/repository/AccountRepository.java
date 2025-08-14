package com.example.account_transfer_api.repository;

import com.example.account_transfer_api.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    /**
     * Finds an account by ID with a pessimistic write lock for the entire transaction.
     *
     * NOTE: Don't override findById() with @Lock - the lock gets released immediately.
     * Use this separate method to hold the lock for the full transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") UUID id);
}